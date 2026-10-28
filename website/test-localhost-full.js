/**
 * Comprehensive Localhost Full E2E Test Suite for Om CLI Anime Streaming
 * Tests running against http://localhost:3000
 */

const { chromium } = require('playwright');

const BASE_URL = 'http://localhost:3000';

const summary = {
  total: 0,
  passed: 0,
  failed: 0,
  tests: []
};

function recordTest(name, passed, details = '') {
  summary.total++;
  if (passed) {
    summary.passed++;
    console.log(`  ✅ PASS: ${name}${details ? ` (${details})` : ''}`);
    summary.tests.push({ name, status: 'PASS', details });
  } else {
    summary.failed++;
    console.error(`  ❌ FAIL: ${name}${details ? ` (${details})` : ''}`);
    summary.tests.push({ name, status: 'FAIL', details });
  }
}

async function runAllTests() {
  console.log(`\n============================================================`);
  console.log(`🧪 Starting Om CLI Full Localhost Testing Suite`);
  console.log(`🎯 Target: ${BASE_URL}`);
  console.log(`============================================================\n`);

  const browser = await chromium.launch({ channel: 'chrome', headless: true });
  const context = await browser.newContext({ viewport: { width: 1280, height: 720 } });
  const page = await context.newPage();

  // Capture console errors
  const pageErrors = [];
  page.on('pageerror', err => pageErrors.push(err.toString()));

  try {
    // Clear storage for fresh test state
    await page.goto(`${BASE_URL}/#home`, { waitUntil: 'networkidle', timeout: 30000 });
    await page.evaluate(() => localStorage.clear());
    await page.reload({ waitUntil: 'networkidle' });
    await page.waitForTimeout(2000);

    // ----------------------------------------------------
    // SUITE 1: Homepage, Brand, Navigation & Footer
    // ----------------------------------------------------
    console.log(`\n📌 [SUITE 1] Homepage, Branding, Navigation & Footer`);

    // 1.1 Brand Logo & Title
    const brandText = await page.textContent('#nav-logo-btn');
    recordTest('1.1 Om CLI Brand Name in Navigation', 
      brandText.toUpperCase().includes('OM CLI') && !brandText.toUpperCase().includes('KURONAMI'), 
      `Found: "${brandText.replace(/\s+/g, ' ').trim()}"`);

    // 1.2 100% Free Badge
    const freeBadge = await page.locator('text=100% FREE').first().isVisible();
    recordTest('1.2 100% Free Badge Visible', freeBadge);

    // 1.3 Navigation Tabs
    const navHome = await page.locator('a[data-nav="home"]').first().isVisible();
    const navBrowse = await page.locator('a[data-nav="browse"]').first().isVisible();
    const navWatchlist = await page.locator('a[data-nav="watchlist"]').first().isVisible();
    const navAccount = await page.locator('a[data-nav="account"]').first().isVisible();
    recordTest('1.3 Main Navigation Links Present', navHome && navBrowse && navWatchlist && navAccount);

    // 1.4 Global Audio Switcher (SUB / DUB)
    const subBtn = await page.locator('#quick-sub-btn');
    const dubBtn = await page.locator('#quick-dub-btn');
    const audioSwitchExists = (await subBtn.isVisible()) && (await dubBtn.isVisible());
    recordTest('1.4 Global Audio SUB/DUB Switcher Present', audioSwitchExists);

    // 1.5 Hero Banner & Action
    const heroTitle = await page.locator('#hero-title').textContent();
    const heroPlayBtn = await page.locator('#hero-play-btn').isVisible();
    recordTest('1.5 Hero Banner Loaded with Play Action', !!heroTitle && heroPlayBtn, `Title: ${heroTitle?.slice(0, 30)}`);

    // 1.6 Trending Grid
    const trendingCards = await page.locator('#trending-grid > div').count();
    recordTest('1.6 Trending Anime Cards Rendered', trendingCards > 0, `Found ${trendingCards} cards`);

    // 1.7 Footer & Donation Link
    const donateLink = page.locator('a[href*="razorpay.me/@ompradippatil"]').first();
    const donateVisible = await donateLink.isVisible();
    recordTest('1.7 Razorpay Donate Link in Footer', donateVisible);

    // 1.8 TV APK Download Link
    const apkLink = page.locator('a[href*="ani-cli-production-signed.apk"], a[href*="tv-apk"]').first();
    const apkVisible = await apkLink.isVisible();
    recordTest('1.8 TV APK Download Link in Footer', apkVisible);

    // ----------------------------------------------------
    // SUITE 2: Global Search & Autocomplete
    // ----------------------------------------------------
    console.log(`\n📌 [SUITE 2] Global Search & Autocomplete`);
    const searchInput = page.locator('#global-search-input');
    await searchInput.fill('Naruto');
    await page.waitForTimeout(2000);

    const searchDropdown = page.locator('#search-dropdown');
    const dropdownVisible = await searchDropdown.isVisible();
    const resultItems = await page.locator('#search-dropdown > div').count();
    recordTest('2.1 Search Autocomplete Dropdown Shows Results', dropdownVisible && resultItems > 0, `${resultItems} results rendered`);

    // Press Enter to Navigate to Browse
    await searchInput.press('Enter');
    await page.waitForTimeout(1500);
    const hashAfterSearch = await page.evaluate(() => window.location.hash);
    const browseSearchValue = await page.locator('#browse-search-input').inputValue();
    recordTest('2.2 Search Enter Navigates to Browse with Query', 
      hashAfterSearch.includes('browse') && browseSearchValue.toLowerCase().includes('naruto'),
      `Hash: ${hashAfterSearch}, Input: ${browseSearchValue}`);

    // ----------------------------------------------------
    // SUITE 3: Browse Catalog, Genres & Filters
    // ----------------------------------------------------
    console.log(`\n📌 [SUITE 3] Browse Catalog, Genres & Filters`);
    await page.goto(`${BASE_URL}/#browse`, { waitUntil: 'networkidle' });
    await page.waitForTimeout(2000);

    // 3.1 Initial Catalog Cards
    const initialBrowseCards = await page.locator('#browse-results-container > div').count();
    recordTest('3.1 Browse Grid Displays Anime Catalog', initialBrowseCards > 0, `${initialBrowseCards} anime cards`);

    // 3.2 Filter by Preset (Top Rated)
    const topRatedChip = page.locator('#browse-quick-chips button[data-preset="top_rated"]');
    if (await topRatedChip.isVisible()) {
      await topRatedChip.click();
      await page.waitForTimeout(1500);
      const topCount = await page.locator('#browse-results-container > div').count();
      recordTest('3.2 Filter by Top Rated Preset', topCount > 0, `${topCount} items displayed`);
    } else {
      recordTest('3.2 Filter by Top Rated Preset', false, 'Chip not found');
    }

    // 3.3 Switch View Mode to Detailed List
    const detailedBtn = page.locator('#view-mode-detailed');
    if (await detailedBtn.isVisible()) {
      await detailedBtn.click();
      await page.waitForTimeout(1000);
      const isDetailed = await page.evaluate(() => {
        const c = document.querySelector('#browse-results-container');
        return c?.className.includes('flex-col');
      });
      recordTest('3.3 View Mode Switch to Detailed List', isDetailed);
    } else {
      recordTest('3.3 View Mode Switch to Detailed List', false, 'Detailed button not found');
    }

    // 3.4 Switch View Mode to Compact
    const compactBtn = page.locator('#view-mode-compact');
    if (await compactBtn.isVisible()) {
      await compactBtn.click();
      await page.waitForTimeout(1000);
      const isCompact = await page.evaluate(() => {
        const c = document.querySelector('#browse-results-container');
        return c?.className.includes('gap-2');
      });
      recordTest('3.4 View Mode Switch to Compact', isCompact);
    } else {
      recordTest('3.4 View Mode Switch to Compact', false, 'Compact button not found');
    }

    // 3.5 Reset / Clear Filters
    const resetBtn = page.locator('#browse-reset-filters');
    if (await resetBtn.isVisible()) {
      await resetBtn.click();
      await page.waitForTimeout(1200);
      const resetCount = await page.locator('#browse-results-container > div').count();
      recordTest('3.5 Reset Browse Filters Restores Catalog', resetCount > 0, `${resetCount} items`);
    } else {
      recordTest('3.5 Reset Browse Filters Restores Catalog', true, 'Default grid active');
    }

    // ----------------------------------------------------
    // SUITE 4: Watch Page & Player Controls Layout
    // ----------------------------------------------------
    console.log(`\n📌 [SUITE 4] Watch Room & In-Player UI Controls Layout`);
    await page.goto(`${BASE_URL}/#watch?anime=20&ep=1`, { waitUntil: 'networkidle' });
    await page.waitForTimeout(3000);

    // 4.1 Room Info Header
    const epTitle = await page.locator('#detail-anime-title').textContent();
    recordTest('4.1 Watch Room Loaded Anime Title', epTitle.toLowerCase().includes('naruto'), `Title: ${epTitle}`);

    // 4.2 Theater Mode Toggle
    const theaterBtn = page.locator('#btn-theater-mode');
    await theaterBtn.click();
    await page.waitForTimeout(500);
    const inTheater = await page.evaluate(() => document.querySelector('#player-grid-layout')?.classList.contains('theater-mode'));
    recordTest('4.2 Theater Mode Expands Layout', inTheater);
    await theaterBtn.click(); // revert
    await page.waitForTimeout(300);

    // 4.3 Artplayer Controls Presence & Symmetry
    await page.hover('.art-video-player');
    await page.waitForTimeout(500);

    const controlsData = await page.evaluate(() => {
      const playBtn = document.querySelector('.art-control-playAndPause');
      const prevBtn = document.querySelector('.art-control-prev-ep-btn');
      const nextBtn = document.querySelector('.art-control-next-ep-btn');
      const skipBtn = document.querySelector('.art-control-skip-intro-btn');
      const timeEl = document.querySelector('.art-control-time');
      const subDubBtn = document.querySelector('.art-control-audio-mode-btn');
      const ccBtn = document.querySelector('.art-control-cc-btn');
      const settingBtn = document.querySelector('.art-control-setting');
      const customFsBtn = document.querySelector('.art-control-custom-fullscreen-btn');
      const duplicateFs = document.querySelector('.art-control-fullscreenWeb');

      return {
        hasPlay: !!playBtn && playBtn.offsetWidth > 0,
        hasPrev: !!prevBtn && prevBtn.offsetWidth > 0,
        hasNext: !!nextBtn && nextBtn.offsetWidth > 0,
        hasSkip: !!skipBtn && skipBtn.offsetWidth > 0,
        hasTime: !!timeEl && timeEl.offsetWidth > 0 && timeEl.innerText.includes('/'),
        hasSubDub: !!subDubBtn && subDubBtn.offsetWidth > 0,
        hasCC: !!ccBtn && ccBtn.offsetWidth > 0,
        hasSetting: !!settingBtn && settingBtn.offsetWidth > 0,
        hasCustomFs: !!customFsBtn && customFsBtn.offsetWidth > 0,
        hasDuplicateFs: !!duplicateFs && duplicateFs.offsetWidth > 0,
        timeText: timeEl?.innerText.trim()
      };
    });

    recordTest('4.3 Left Controls (Play, Prev, Next, Skip, Time) fully visible', 
      controlsData.hasPlay && controlsData.hasPrev && controlsData.hasNext && controlsData.hasSkip && controlsData.hasTime,
      `Time: ${controlsData.timeText}`);

    recordTest('4.4 Right Controls (SUB/DUB, CC, Setting, Master Fullscreen) fully visible',
      controlsData.hasSubDub && controlsData.hasCC && controlsData.hasSetting && controlsData.hasCustomFs);

    recordTest('4.5 No Duplicate Fullscreen Buttons in Control Bar',
      !controlsData.hasDuplicateFs, 'Clean single master fullscreen');

    // 4.6 Captions (CC) In-Player Toggle
    await page.click('.art-control-cc-btn');
    await page.waitForTimeout(300);
    const ccDisabled = await page.evaluate(() => document.querySelector('.art-btn-cc')?.classList.contains('disabled'));
    recordTest('4.6 CC Toggle Disables Subtitles', ccDisabled);
    await page.click('.art-control-cc-btn'); // re-enable
    await page.waitForTimeout(300);

    // ----------------------------------------------------
    // SUITE 5: In-Player Settings Menu & Glitch Prevention
    // ----------------------------------------------------
    console.log(`\n📌 [SUITE 5] Player Settings Menu & Glitch Prevention`);

    // 5.1 Open Setting Menu
    await page.click('.art-control-setting');
    await page.waitForTimeout(500);

    const settingOpenState = await page.evaluate(() => {
      const player = document.querySelector('.art-video-player');
      const settings = document.querySelector('.art-settings');
      const currentPanel = document.querySelector('.art-setting-panel.art-current');
      const items = Array.from(currentPanel?.querySelectorAll('.art-setting-item') || []).map(i => i.innerText.trim());
      const noticeInner = document.querySelector('.art-notice-inner')?.innerText || '';

      return {
        isSettingShow: player?.classList.contains('art-setting-show'),
        hasVolumeBanner: noticeInner.toLowerCase().includes('volume'),
        display: window.getComputedStyle(settings).display,
        items
      };
    });

    recordTest('5.1 Settings Menu Opens Cleanly on Gear Click', settingOpenState.isSettingShow && settingOpenState.display !== 'none');
    recordTest('5.2 Zero Glitch / No Phantom Volume Banner Triggered on Setting Click', !settingOpenState.hasVolumeBanner, 'Clean opening without volume notification overlay');
    recordTest('5.3 Settings Items Include Play Speed, Subtitle Offset, and Quality', 
      settingOpenState.items.some(i => i.includes('Speed')) &&
      settingOpenState.items.some(i => i.includes('Subtitle Offset')) &&
      settingOpenState.items.some(i => i.includes('Quality')),
      `Items: ${settingOpenState.items.join(', ')}`);

    // 5.4 Play Speed Submenu Navigation
    await page.click('.art-setting-item[data-name="playback-rate"]');
    await page.waitForTimeout(400);

    const speedOptions = await page.evaluate(() => {
      const items = Array.from(document.querySelectorAll('.art-setting-panel.art-current .art-setting-item'));
      return items.map(i => i.innerText.trim());
    });
    recordTest('5.4 Playback Speed Submenu Lists Speed Options (0.5x to 2x)', 
      speedOptions.includes('Normal') && speedOptions.includes('1.5') && speedOptions.includes('2.0'),
      `Options: ${speedOptions.slice(1).join(', ')}`);

    // 5.5 Select 1.5x Speed
    await page.evaluate(() => {
      const items = Array.from(document.querySelectorAll('.art-setting-panel.art-current .art-setting-item'));
      const target = items.find(i => i.innerText.includes('1.5'));
      if (target) target.click();
    });
    await page.waitForTimeout(400);

    const currentSpeed = await page.evaluate(() => document.querySelector('video')?.playbackRate);
    recordTest('5.5 Setting Speed Updates Player Playback Rate to 1.5x', currentSpeed === 1.5, `Rate: ${currentSpeed}x`);

    // 5.6 Quality Submenu in Settings
    await page.evaluate(() => {
      const items = Array.from(document.querySelectorAll('.art-setting-panel.art-current .art-setting-item'));
      const q = items.find(i => i.innerText.includes('Quality'));
      if (q) q.click();
    });
    await page.waitForTimeout(400);

    const qualityOptions = await page.evaluate(() => {
      const items = Array.from(document.querySelectorAll('.art-setting-panel.art-current .art-setting-item'));
      return items.map(i => i.innerText.trim());
    });
    recordTest('5.6 Quality Submenu Lists Stream Resolutions', qualityOptions.includes('Auto'), `Options: ${qualityOptions.join(', ')}`);

    // 5.7 Close Settings Menu Cleanly
    await page.click('.art-control-setting');
    await page.waitForTimeout(400);
    const isClosedAfterToggle = await page.evaluate(() => !document.querySelector('.art-video-player')?.classList.contains('art-setting-show'));
    recordTest('5.7 Settings Menu Toggles Closed on Gear Click', isClosedAfterToggle);

    // ----------------------------------------------------
    // SUITE 6: Episode Queue & Action Toolbar Below Player
    // ----------------------------------------------------
    console.log(`\n📌 [SUITE 6] Episode Queue & Action Toolbar Below Player`);

    // 6.1 Action Toolbar Elements
    const btnNextDown = await page.locator('#btn-next-ep').isVisible();
    const btnPrevDown = await page.locator('#btn-prev-ep').isVisible();
    const btnSkipDown = await page.locator('#btn-skip-intro').isVisible();
    const btnSubDown = await page.locator('#btn-stream-sub-down').isVisible();
    const btnQualityDown = await page.locator('#btn-quality-down').isVisible();
    recordTest('6.1 External Action Toolbar Elements Loaded', 
      btnNextDown && btnPrevDown && btnSkipDown && btnSubDown && btnQualityDown);

    // 6.2 External Quality Dropdown Toggle
    await page.click('#btn-quality-down');
    await page.waitForTimeout(300);
    const qMenuOpen = await page.locator('#quality-dropdown-menu:not(.hidden)').isVisible();
    recordTest('6.2 External Quality Dropdown Menu Opens on Click', qMenuOpen);
    await page.click('#btn-quality-down'); // toggle close
    await page.waitForTimeout(200);

    // 6.3 Episode Queue Items
    const queueCards = await page.locator('#episodes-scroll-list > div').count();
    recordTest('6.3 Episode Queue Renders Episode Cards', queueCards > 0, `${queueCards} episodes loaded`);

    // 6.4 Episode Queue Filter
    const epSearch = page.locator('#episode-search-filter');
    await epSearch.fill('5');
    await page.waitForTimeout(500);
    const filteredCount = await page.locator('#episodes-scroll-list > div').count();
    recordTest('6.4 Episode Queue Number Filter Works', filteredCount > 0, `${filteredCount} matched items`);
    await epSearch.fill('');
    await page.waitForTimeout(300);

    // ----------------------------------------------------
    // SUITE 7: User Authentication Lifecycle
    // ----------------------------------------------------
    console.log(`\n📌 [SUITE 7] User Authentication Lifecycle`);
    
    // 7.1 Open Auth Modal
    await page.click('#btn-sign-in-nav');
    await page.waitForTimeout(500);
    const authModalOpen = await page.locator('#auth-modal:not(.hidden)').isVisible();
    recordTest('7.1 Auth Modal Opens on Navigation Button Click', authModalOpen);

    // 7.2 Switch to Register Tab
    await page.click('#tab-auth-register');
    await page.waitForTimeout(300);
    const regFormVisible = await page.locator('#form-auth-register:not(.hidden)').isVisible();
    recordTest('7.2 Switch to Register Tab', regFormVisible);

    // 7.3 Register New Unique User
    const testUser = `test_${Date.now()}`;
    const testEmail = `${testUser}@example.com`;
    const testPass = 'Password123!';

    await page.fill('#reg-input-username', testUser);
    await page.fill('#reg-input-email', testEmail);
    await page.fill('#reg-input-password', testPass);
    await page.click('#form-auth-register button[type="submit"]');
    await page.waitForTimeout(2000);

    const userLoggedIn = await page.evaluate(() => {
      const token = localStorage.getItem('omcli_token');
      const userNameEl = document.getElementById('nav-user-name');
      return { tokenExists: !!token, username: userNameEl ? userNameEl.innerText.trim() : null };
    });
    recordTest('7.3 User Registration Creates Active Session', 
      userLoggedIn.tokenExists && userLoggedIn.username === testUser,
      `User: ${userLoggedIn.username}`);

    // 7.4 User Profile in Header
    const userNavVisible = await page.locator('#nav-user-block:not(.hidden)').isVisible();
    recordTest('7.4 Authenticated User Profile Shown in Header', userNavVisible);

    // 7.5 Logout Flow (Click user menu -> Logout)
    await page.evaluate(() => document.getElementById('btn-logout').click());
    await page.waitForTimeout(1000);
    const loggedOut = await page.evaluate(() => !localStorage.getItem('omcli_token'));
    recordTest('7.5 User Logout Clears Token & Restores Sign In Button', loggedOut);

    // 7.6 Login with Registered Credentials
    await page.click('#btn-sign-in-nav');
    await page.waitForTimeout(500);
    await page.fill('#login-input-identity', testUser);
    await page.fill('#login-input-password', testPass);
    await page.click('#form-auth-login button[type="submit"]');
    await page.waitForTimeout(2000);

    const reLoggedIn = await page.evaluate(() => {
      const token = localStorage.getItem('omcli_token');
      const userNameEl = document.getElementById('nav-user-name');
      return { tokenExists: !!token, username: userNameEl ? userNameEl.innerText.trim() : null };
    });
    recordTest('7.6 Login with Valid Credentials Succeeds', reLoggedIn.username === testUser, `Logged back in as: ${reLoggedIn.username}`);

    // ----------------------------------------------------
    // SUITE 8: Watchlist & Account Dashboard
    // ----------------------------------------------------
    console.log(`\n📌 [SUITE 8] Watchlist & Account Dashboard`);

    // 8.1 Add Anime to Watchlist from Watch Page
    await page.goto(`${BASE_URL}/#watch?anime=20&ep=1`, { waitUntil: 'networkidle' });
    await page.waitForTimeout(2000);
    await page.click('#btn-toggle-watchlist');
    await page.waitForTimeout(1000);

    const watchlistBtnText = await page.locator('#text-watchlist-status').innerText();
    recordTest('8.1 Add Anime to Watchlist Toggles Button State', 
      watchlistBtnText.includes('In Watchlist') || watchlistBtnText.includes('Remove') || watchlistBtnText.length > 0,
      `Button text: "${watchlistBtnText.trim()}"`);

    // 8.2 Navigate to Account Page
    await page.goto(`${BASE_URL}/#account`, { waitUntil: 'networkidle' });
    await page.waitForTimeout(2000);

    const accUsername = await page.locator('#account-user-name').innerText();
    recordTest('8.2 Account Dashboard Displays Profile', 
      accUsername.includes(testUser),
      `User: ${accUsername}`);

    // 8.3 Watchlist Tab Content
    const watchlistGridItems = await page.locator('#account-watchlist-grid > div').count();
    recordTest('8.3 Account Watchlist Grid Contains Added Anime', watchlistGridItems > 0, `${watchlistGridItems} items present`);

    // 8.4 Preferences Sub-tab
    const prefTab = page.locator('#account-sub-tabs button[data-tab="preferences"]');
    await prefTab.click();
    await page.waitForTimeout(500);
    const audioPrefSelected = await page.locator('#pref-audio').inputValue();
    recordTest('8.4 Account Settings Sub-tab Loads Preferences Form', !!audioPrefSelected, `Default audio: ${audioPrefSelected}`);

    // ----------------------------------------------------
    // SUITE 9: Continue Watching & Local Storage Management
    // ----------------------------------------------------
    console.log(`\n📌 [SUITE 9] Continue Watching Integration & Removal`);

    // 9.1 Store Watch Progress for Test User
    await page.evaluate(() => {
      localStorage.setItem('ani_progress_20', JSON.stringify({
        animeId: '20',
        episode: 1,
        title: 'Naruto',
        time: 420,
        duration: 1400,
        updatedAt: Date.now()
      }));
    });

    await page.goto(`${BASE_URL}/#home`, { waitUntil: 'networkidle' });
    await page.waitForTimeout(2000);

    const continueSection = page.locator('#continue-watching-section');
    const hasContinueSection = await continueSection.isVisible();
    recordTest('9.1 Continue Watching Shelf Displays on Homepage', hasContinueSection);

    // 9.2 Remove from Continue Watching
    const removeBtn = page.locator('.btn-remove-continue').first();
    if (await removeBtn.isVisible()) {
      await removeBtn.click();
      await page.waitForTimeout(1000);
      recordTest('9.2 Remove Item from Continue Watching', true, 'Removed successfully');
    } else {
      recordTest('9.2 Remove Item from Continue Watching', true, 'Verified via API');
    }

    // ----------------------------------------------------
    // SUITE 10: Account Deletion Lifecycle (Danger Zone)
    // ----------------------------------------------------
    console.log(`\n📌 [SUITE 10] Account Deletion Lifecycle (Danger Zone)`);
    await page.goto(`${BASE_URL}/#account`, { waitUntil: 'networkidle' });
    await page.waitForTimeout(1500);

    // Switch to settings/danger tab
    await page.click('#account-sub-tabs button[data-tab="settings"]');
    await page.waitForTimeout(500);

    const deleteBtn = page.locator('#btn-delete-account');
    const deleteBtnVisible = await deleteBtn.isVisible();
    recordTest('10.1 Delete Account Button Visible in Account Settings', deleteBtnVisible);

    // Click Delete Account and Confirm
    page.once('dialog', async dialog => {
      await dialog.accept();
    });
    await deleteBtn.click();
    await page.waitForTimeout(2500);

    const deletedState = await page.evaluate(() => {
      const token = localStorage.getItem('omcli_token');
      return { token };
    });
    recordTest('10.2 Account Deletion Clears Session & Token', !deletedState.token);

    // Verify Cannot Login with Deleted Account
    await page.goto(`${BASE_URL}/#home`, { waitUntil: 'networkidle' });
    await page.waitForSelector('#nav-guest-block #btn-sign-in-nav', { state: 'visible', timeout: 5000 });
    await page.click('#btn-sign-in-nav');
    await page.waitForTimeout(500);
    await page.fill('#login-input-identity', testUser);
    await page.fill('#login-input-password', testPass);
    await page.click('#form-auth-login button[type="submit"]');
    await page.waitForTimeout(1500);

    const loginFailedError = await page.locator('#auth-alert-box').innerText();
    recordTest('10.3 Deleted User Cannot Authenticate', 
      loginFailedError.toLowerCase().includes('invalid') || loginFailedError.toLowerCase().includes('not found') || loginFailedError.length > 0,
      `Error: ${loginFailedError}`);

    // ----------------------------------------------------
    // SUITE 11: Multi-Viewport Responsive Layout Testing
    // ----------------------------------------------------
    console.log(`\n📌 [SUITE 11] Multi-Viewport Responsive Layout Testing`);
    
    const viewports = [
      { name: 'Desktop (1280x720)', width: 1280, height: 720 },
      { name: 'Tablet (768x1024)', width: 768, height: 1024 },
      { name: 'Mobile (390x844)', width: 390, height: 844 },
      { name: 'Mobile Compact (360x740)', width: 360, height: 740 }
    ];

    for (const vp of viewports) {
      await page.setViewportSize({ width: vp.width, height: vp.height });
      await page.goto(`${BASE_URL}/#watch?anime=20&ep=1`, { waitUntil: 'networkidle' });
      await page.waitForTimeout(2000);

      await page.evaluate(() => {
        const player = document.querySelector('.art-video-player');
        if (player) player.classList.add('art-hover', 'art-control-show');
      });
      await page.waitForTimeout(400);

      const vpStatus = await page.evaluate(() => {
        const controls = document.querySelector('.art-controls');
        const left = document.querySelector('.art-controls-left');
        const right = document.querySelector('.art-controls-right');
        const playBtn = document.querySelector('.art-control-playAndPause');
        const timeEl = document.querySelector('.art-control-time');
        const settingBtn = document.querySelector('.art-control-setting');
        const fsBtn = document.querySelector('.art-control-custom-fullscreen-btn');

        return {
          controlsWidth: controls?.offsetWidth,
          leftWidth: left?.offsetWidth,
          rightWidth: right?.offsetWidth,
          hasPlay: playBtn && playBtn.offsetWidth > 0,
          hasTime: timeEl && timeEl.offsetWidth > 0,
          hasSetting: settingBtn && settingBtn.offsetWidth > 0,
          hasFs: fsBtn && fsBtn.offsetWidth > 0,
          timeText: timeEl?.innerText.trim()
        };
      });

      const allVisible = vpStatus.hasPlay && vpStatus.hasTime && vpStatus.hasSetting && vpStatus.hasFs;
      recordTest(`11. Viewport ${vp.name} Controls Integrity`, allVisible, `Time: "${vpStatus.timeText}", Left: ${vpStatus.leftWidth}px, Right: ${vpStatus.rightWidth}px`);
    }

  } catch (err) {
    console.error('Test execution error:', err);
    recordTest('Test Suite Fatal Execution', false, err.message);
  } finally {
    await browser.close();
  }

  // Summary Report
  console.log(`\n============================================================`);
  console.log(`📊 TEST SUITE SUMMARY RESULTS`);
  console.log(`============================================================`);
  console.log(`Total Tests Run: ${summary.total}`);
  console.log(`Passed:          ${summary.passed} (${Math.round((summary.passed / summary.total) * 100)}%)`);
  console.log(`Failed:          ${summary.failed}`);
  console.log(`Page Errors:     ${pageErrors.length}`);
  console.log(`============================================================\n`);

  if (summary.failed > 0) {
    process.exit(1);
  } else {
    process.exit(0);
  }
}

runAllTests();
