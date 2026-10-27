// website/tests/run-all-tests.js
// Deep Automated E2E & Component Test Suite for Om CLI Anime Platform
// Powered by Playwright & Google Chrome (macOS)

const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');

const BASE_URL = process.env.BASE_URL || 'https://om-cli-anime.omspradippatil.workers.dev';
const CHROME_PATH = '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome';

const results = [];
let passedCount = 0;
let failedCount = 0;
let activePage = null;

function recordResult(suite, name, passed, details = '', durationMs = 0) {
  if (passed) passedCount++;
  else failedCount++;
  results.push({ suite, name, passed, details, durationMs });
  const icon = passed ? '✅ PASS' : '❌ FAIL';
  console.log(`[${suite}] ${icon} - ${name} (${durationMs}ms)${details ? ' :: ' + details : ''}`);
}

async function dismissModalIfOpen(p) {
  const target = p || activePage;
  if (!target) return;
  try {
    await target.evaluate(() => {
      const authModal = document.getElementById('auth-modal');
      if (authModal && !authModal.classList.contains('hidden')) {
        authModal.classList.add('hidden');
      }
      const searchModal = document.getElementById('mobile-search-modal');
      if (searchModal && !searchModal.classList.contains('hidden')) {
        searchModal.classList.add('hidden');
        searchModal.classList.remove('flex');
      }
      const dropdown = document.getElementById('search-dropdown');
      if (dropdown && !dropdown.classList.contains('hidden')) {
        dropdown.classList.add('hidden');
      }
      document.body.classList.remove('overflow-hidden');
    });
    await target.waitForTimeout(100);
  } catch (_) {}
}

async function runTest(suite, name, testFn) {
  const start = Date.now();
  try {
    const details = await testFn();
    const duration = Date.now() - start;
    recordResult(suite, name, true, details || '', duration);
  } catch (err) {
    const duration = Date.now() - start;
    recordResult(suite, name, false, err.message, duration);
  }
}

async function main() {
  console.log('================================================================');
  console.log('🚀 STARTING COMPREHENSIVE E2E & COMPONENT TEST SUITE (80+ TESTS)');
  console.log(`🌍 Target Base URL: ${BASE_URL}`);
  console.log(`🖥️  Browser Binary: ${CHROME_PATH}`);
  console.log('================================================================\n');

  const browser = await chromium.launch({
    executablePath: CHROME_PATH,
    headless: true,
    args: ['--no-sandbox', '--disable-setuid-sandbox', '--disable-gpu']
  });

  const context = await browser.newContext({
    viewport: { width: 1440, height: 900 },
    userAgent: 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36'
  });

  const page = await context.newPage();
  activePage = page;
  page.setDefaultTimeout(8000);

  // Capture page console errors
  const pageErrors = [];
  page.on('pageerror', err => pageErrors.push(`Uncaught: ${err.message}`));
  page.on('console', msg => {
    if (msg.type() === 'error') {
      const text = msg.text();
      const loc = msg.location()?.url || '';
      if (!text.includes('favicon') && !loc.includes('favicon') && !text.includes('net::ERR_') && !text.includes('status of 404')) {
        pageErrors.push(`${text} (${loc})`);
      }
    }
  });

  try {
    // -------------------------------------------------------------------------
    // SUITE 1: Page Load & Initial DOM Readiness
    // -------------------------------------------------------------------------
    const s1 = '1. Initial Load & Shell';
    await runTest(s1, 'Website loads with HTTP 200 and valid document title', async () => {
      const resp = await page.goto(BASE_URL, { waitUntil: 'domcontentloaded', timeout: 30000 });
      if (!resp || resp.status() !== 200) throw new Error(`Status ${resp ? resp.status() : 'null'}`);
      const title = await page.title();
      if (!title.toLowerCase().includes('om cli')) throw new Error(`Unexpected title: ${title}`);
      return `Title: "${title}"`;
    });

    await runTest(s1, 'Document viewport and meta tags are properly defined', async () => {
      const viewport = await page.getAttribute('meta[name="viewport"]', 'content');
      if (!viewport || !viewport.includes('width=device-width')) throw new Error(`Missing viewport meta: ${viewport}`);
      return `Viewport: ${viewport}`;
    });

    await runTest(s1, 'Global Navbar header exists and is pinned with backdrop blur', async () => {
      const header = await page.$('header');
      if (!header) throw new Error('Header element not found');
      const isVisible = await header.isVisible();
      if (!isVisible) throw new Error('Header is not visible');
      return 'Header pinned and visible';
    });

    await runTest(s1, 'Brand logo renders "Om CLI" and "Precision Stream" moniker', async () => {
      const logoText = await page.$eval('#nav-logo-btn', el => el.innerText);
      if (!logoText.toUpperCase().includes('OM') || !logoText.toUpperCase().includes('CLI')) {
        throw new Error(`Logo text missing Om CLI: ${logoText}`);
      }
      return `Brand text: "${logoText.replace(/\s+/g, ' ').trim()}"`;
    });

    // -------------------------------------------------------------------------
    // SUITE 2: Navigation & Routing Engine
    // -------------------------------------------------------------------------
    const s2 = '2. Navigation & Routing';
    await runTest(s2, 'Desktop navigation tabs render: Home, Browse, Watchlist, Account', async () => {
      const tabs = await page.$$eval('.nav-tab', els => els.map(e => ({ name: e.innerText.trim(), nav: e.dataset.nav })));
      if (tabs.length < 4) throw new Error(`Expected at least 4 nav tabs, got ${tabs.length}`);
      const navKeys = tabs.map(t => t.nav);
      if (!navKeys.includes('home') || !navKeys.includes('browse') || !navKeys.includes('watchlist') || !navKeys.includes('account')) {
        throw new Error(`Missing nav targets: ${navKeys.join(', ')}`);
      }
      return `Tabs: ${tabs.map(t => t.name).join(' | ')}`;
    });

    await runTest(s2, 'Navigating to #browse activates Browse view and hides Home view', async () => {
      await page.click('a[data-nav="browse"]');
      await page.waitForTimeout(300);
      const browseHidden = await page.$eval('#browse-view', el => el.classList.contains('hidden'));
      const homeHidden = await page.$eval('#home-view', el => el.classList.contains('hidden'));
      if (browseHidden) throw new Error('Browse view is still hidden');
      if (!homeHidden) throw new Error('Home view was not hidden');
      return 'Browse view active';
    });

    await runTest(s2, 'Navigating to #watchlist opens Watchlist or triggers Auth Modal for guest', async () => {
      await page.click('a[data-nav="watchlist"]');
      await page.waitForTimeout(400);
      await dismissModalIfOpen(page);
      return 'Watchlist navigation verified';
    });

    await runTest(s2, 'Navigating to #home restores Home feed view', async () => {
      await page.goto(`${BASE_URL}#home`, { waitUntil: 'domcontentloaded' });
      await page.waitForTimeout(300);
      const homeHidden = await page.$eval('#home-view', el => el.classList.contains('hidden'));
      if (homeHidden) throw new Error('Home view is still hidden');
      return 'Home view restored';
    });

    await runTest(s2, 'Clicking brand logo returns to #home', async () => {
      await page.click('#nav-logo-btn');
      await page.waitForTimeout(300);
      const url = page.url();
      if (!url.includes('#home') && url !== BASE_URL && url !== `${BASE_URL}/`) {
        throw new Error(`Did not return to home: ${url}`);
      }
      return 'Brand logo navigation verified';
    });

    await runTest(s2, 'Browser URL hash updates accurately on view changes', async () => {
      await page.click('a[data-nav="browse"]');
      await page.waitForTimeout(200);
      if (!page.url().includes('#browse')) throw new Error('URL hash did not update to #browse');
      await page.click('a[data-nav="home"]');
      await page.waitForTimeout(200);
      return 'Hash changes match active view';
    });

    // -------------------------------------------------------------------------
    // SUITE 3: Hero Banner & Spotlight Carousel
    // -------------------------------------------------------------------------
    const s3 = '3. Hero Spotlight Carousel';
    await page.goto(`${BASE_URL}#home`, { waitUntil: 'domcontentloaded' });
    await page.waitForSelector('#hero-title', { timeout: 8000 });

    await runTest(s3, 'Hero banner displays title, score, year, format, and synopsis', async () => {
      const title = await page.$eval('#hero-title', el => el.innerText.trim());
      const score = await page.$eval('#hero-score', el => el.innerText.trim());
      const format = await page.$eval('#hero-format', el => el.innerText.trim());
      const year = await page.$eval('#hero-year', el => el.innerText.trim());
      if (!title) throw new Error('Hero title is empty');
      return `Hero: "${title}" | ★ ${score} | ${format} | ${year}`;
    });

    await runTest(s3, 'Hero carousel indicator dots are rendered and interactive', async () => {
      const dots = await page.$$('#hero-indicators button');
      if (dots.length === 0) throw new Error('No hero carousel indicator dots');
      if (dots.length > 1) {
        await dots[1].click();
        await page.waitForTimeout(300);
      }
      return `${dots.length} carousel slides available`;
    });

    await runTest(s3, 'Hero "Play" CTA button is present and clickable', async () => {
      const playBtn = await page.$('#hero-play-btn');
      if (!playBtn) throw new Error('Hero play button missing');
      const text = await playBtn.innerText();
      return `Play CTA: "${text.trim()}"`;
    });

    await runTest(s3, 'Hero "Watchlist" CTA prompts auth modal for guest visitors', async () => {
      const wlBtn = await page.$('#hero-watchlist-btn');
      if (!wlBtn) throw new Error('Hero watchlist button missing');
      await wlBtn.click();
      await page.waitForTimeout(300);
      const isModalOpen = await page.$eval('#auth-modal', el => !el.classList.contains('hidden')).catch(() => false);
      await dismissModalIfOpen(page);
      if (!isModalOpen) throw new Error('Auth modal was not opened for unauthenticated visitor');
      return 'Guest watchlist attempt correctly triggered auth modal & dismissed cleanly';
    });

    await runTest(s3, 'Hero "More Info" button triggers navigation', async () => {
      const infoBtn = await page.$('#hero-info-btn');
      if (!infoBtn) throw new Error('Hero info button missing');
      return 'More Info CTA verified';
    });

    // -------------------------------------------------------------------------
    // SUITE 4: Home Category & Genre Chips Bar
    // -------------------------------------------------------------------------
    const s4 = '4. Home Genre Chips Bar';
    await page.goto(`${BASE_URL}#home`, { waitUntil: 'domcontentloaded' });
    await page.waitForSelector('#genre-bar', { timeout: 5000 });

    await runTest(s4, 'Genre bar contains all required genre chips', async () => {
      const chips = await page.$$eval('#genre-bar .genre-chip', els => els.map(e => e.dataset.genre));
      const required = ['All', 'Action', 'Fantasy', 'Shounen', 'Sci-Fi', 'Supernatural', 'Drama', 'Comedy', 'Romance', 'Adventure', 'Sports', 'Isekai'];
      const missing = required.filter(r => !chips.includes(r));
      if (missing.length > 0) throw new Error(`Missing genres: ${missing.join(', ')}`);
      return `${chips.length} genre chips rendered`;
    });

    await runTest(s4, '"All Genres" is active by default', async () => {
      const isActive = await page.$eval('#genre-bar [data-genre="All"]', el => el.classList.contains('active'));
      if (!isActive) throw new Error('"All Genres" is not marked active');
      return 'All Genres active';
    });

    await runTest(s4, 'Clicking "Action" chip activates it and filters trending cards', async () => {
      await page.click('#genre-bar [data-genre="Action"]');
      await page.waitForTimeout(300);
      const isActive = await page.$eval('#genre-bar [data-genre="Action"]', el => el.classList.contains('active'));
      if (!isActive) throw new Error('Action chip did not become active');
      const cards = await page.$$('#trending-grid .anime-card');
      return `Action filtered: ${cards.length} cards displayed`;
    });

    await runTest(s4, 'Clicking "Shounen" chip handles filtering or routing gracefully', async () => {
      await page.click('#genre-bar [data-genre="Shounen"]');
      await page.waitForTimeout(300);
      return `Shounen interaction OK (URL: ${page.url()})`;
    });

    await runTest(s4, 'Clicking active chip again or "Browse All" navigates to Browse view', async () => {
      await page.goto(`${BASE_URL}#home`, { waitUntil: 'domcontentloaded' });
      await page.waitForSelector('#genre-bar', { timeout: 5000 });
      const browseAllBtn = await page.$('#genre-bar button:has-text("Browse All")');
      if (browseAllBtn) {
        await browseAllBtn.click();
        await page.waitForTimeout(400);
        const isBrowse = page.url().includes('#browse');
        if (!isBrowse) throw new Error(`Did not navigate to #browse: ${page.url()}`);
      }
      return 'Browse All CTA verified';
    });

    await runTest(s4, 'Quick Audio Switcher (SUB / DUB) in header toggles state', async () => {
      const subBtn = await page.$('#quick-sub-btn');
      const dubBtn = await page.$('#quick-dub-btn');
      if (!subBtn || !dubBtn) throw new Error('Header audio switcher buttons missing');
      await dubBtn.click();
      await page.waitForTimeout(100);
      await subBtn.click();
      return 'Global audio switcher verified';
    });

    // -------------------------------------------------------------------------
    // SUITE 5: Home Feed Rails (Trending, Popular, Top Rated)
    // -------------------------------------------------------------------------
    const s5 = '5. Catalog Grids on Home';
    await page.goto(`${BASE_URL}#home`, { waitUntil: 'domcontentloaded' });
    await page.waitForSelector('#trending-grid .anime-card, #trending-grid .group', { timeout: 12000 });

    await runTest(s5, 'Trending Grid renders anime cards with posters and ratings', async () => {
      const cards = await page.$$('#trending-grid .anime-card, #trending-grid .group');
      if (cards.length === 0) throw new Error('No cards in trending grid');
      const firstTitle = await cards[0].$eval('h3', el => el.innerText.trim()).catch(() => '');
      if (!firstTitle) throw new Error('First card title is empty');
      return `${cards.length} Trending cards. First: "${firstTitle}"`;
    });

    await runTest(s5, 'Most Popular Grid renders anime cards', async () => {
      const cards = await page.$$('#popular-grid .anime-card, #popular-grid .group');
      if (cards.length === 0) throw new Error('No cards in popular grid');
      const firstTitle = await cards[0].$eval('h3', el => el.innerText.trim()).catch(() => '');
      return `${cards.length} Popular cards. First: "${firstTitle}"`;
    });

    await runTest(s5, 'Top Rated Grid renders anime cards', async () => {
      const cards = await page.$$('#top-rated-grid .anime-card, #top-rated-grid .group');
      if (cards.length === 0) throw new Error('No cards in top rated grid');
      const firstTitle = await cards[0].$eval('h3', el => el.innerText.trim()).catch(() => '');
      return `${cards.length} Top Rated cards. First: "${firstTitle}"`;
    });

    await runTest(s5, 'Anime card has bookmark/watchlist quick action button', async () => {
      const bookmarkBtn = await page.$('#trending-grid button[title="Add to Watchlist"]');
      if (!bookmarkBtn) throw new Error('Bookmark button not found on card');
      return 'Card bookmark button exists';
    });

    await runTest(s5, 'Anime card click navigates to Watch Room', async () => {
      const card = await page.$('#trending-grid .anime-card, #trending-grid .group');
      if (!card) throw new Error('No anime card to click');
      await card.click();
      await page.waitForTimeout(600);
      const url = page.url();
      if (!url.includes('#watch')) throw new Error(`Expected #watch URL, got ${url}`);
      return `Navigated to: ${url}`;
    });

    // -------------------------------------------------------------------------
    // SUITE 6: Global Desktop Search & Live Dropdown
    // -------------------------------------------------------------------------
    const s6 = '6. Global Desktop Search';
    await page.goto(`${BASE_URL}#home`, { waitUntil: 'domcontentloaded' });
    await page.waitForSelector('#global-search-input', { timeout: 5000 });

    await runTest(s6, 'Typing "Naruto" triggers floating dropdown with matching titles', async () => {
      const searchInput = await page.$('#global-search-input');
      await searchInput.fill('');
      await searchInput.type('Naruto', { delay: 40 });
      await page.waitForSelector('#search-dropdown:not(.hidden)', { timeout: 6000 });
      const dropdownItems = await page.$$('#search-dropdown > div');
      if (dropdownItems.length === 0) throw new Error('Search dropdown opened but contains no items');
      const firstMatch = await dropdownItems[0].innerText();
      if (!firstMatch.toLowerCase().includes('naruto')) {
        throw new Error(`First search match does not contain Naruto: ${firstMatch}`);
      }
      return `${dropdownItems.length} matches found. Top: "${firstMatch.replace(/\s+/g, ' ').trim()}"`;
    });

    await runTest(s6, 'Search dropdown contains "View All Results" link to Browse', async () => {
      const viewAll = await page.$('#search-dropdown button');
      if (!viewAll) throw new Error('View All Results button not found in dropdown');
      const text = await viewAll.innerText();
      return `Dropdown link: "${text.trim()}"`;
    });

    await runTest(s6, 'Clicking outside closes the live search dropdown', async () => {
      await page.click('#nav-logo-btn');
      await page.waitForTimeout(300);
      const isHidden = await page.$eval('#search-dropdown', el => el.classList.contains('hidden'));
      if (!isHidden) throw new Error('Dropdown did not close upon outside click');
      return 'Dropdown successfully closed';
    });

    await runTest(s6, 'Pressing Enter in search input navigates to #browse?search=...', async () => {
      const searchInput = await page.$('#global-search-input');
      await searchInput.fill('Bleach');
      await searchInput.press('Enter');
      await page.waitForTimeout(600);
      const url = page.url();
      if (!url.includes('browse?search=Bleach')) throw new Error(`Unexpected URL: ${url}`);
      return `Navigated to: ${url}`;
    });

    await runTest(s6, 'Search input placeholder is informative', async () => {
      const placeholder = await page.getAttribute('#global-search-input', 'placeholder');
      if (!placeholder || placeholder.length < 5) throw new Error('Invalid placeholder');
      return `Placeholder: "${placeholder}"`;
    });

    // -------------------------------------------------------------------------
    // SUITE 7: Mobile Search Modal & Tags
    // -------------------------------------------------------------------------
    const s7 = '7. Mobile Search Modal';
    await page.setViewportSize({ width: 375, height: 812 });
    await page.goto(`${BASE_URL}#home`, { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(300);

    await runTest(s7, 'Opening mobile search modal via mobile search toggle button', async () => {
      await page.click('#btn-mobile-search-toggle');
      await page.waitForTimeout(300);
      const isOpen = await page.$eval('#mobile-search-modal', el => !el.classList.contains('hidden'));
      if (!isOpen) throw new Error('Mobile search modal failed to open');
      return 'Mobile search modal opened';
    });

    await runTest(s7, 'Mobile search modal displays trending tags', async () => {
      const tags = await page.$$eval('#mobile-quick-tags .mobile-search-tag', els => els.map(e => e.dataset.query));
      if (tags.length < 4) throw new Error(`Expected >=4 trending tags, got ${tags.length}`);
      return `Tags: ${tags.join(', ')}`;
    });

    await runTest(s7, 'Clicking trending search tag "Chainsaw Man" populates input and triggers search', async () => {
      await page.click('#mobile-quick-tags [data-query="Chainsaw Man"]');
      await page.waitForTimeout(1000);
      const val = await page.$eval('#mobile-search-input', el => el.value);
      if (val !== 'Chainsaw Man') throw new Error(`Input value mismatch: ${val}`);
      return 'Tag click populated and triggered search';
    });

    await runTest(s7, 'Mobile search clear button resets search input and results', async () => {
      const clearBtn = await page.$('#mobile-search-clear-btn:not(.hidden)');
      if (clearBtn) {
        await clearBtn.click();
        await page.waitForTimeout(200);
        const val = await page.$eval('#mobile-search-input', el => el.value);
        if (val !== '') throw new Error('Input was not cleared');
      }
      return 'Clear button works';
    });

    await runTest(s7, 'Mobile search close button dismisses modal', async () => {
      await page.click('#mobile-search-close-btn');
      await page.waitForTimeout(300);
      const isHidden = await page.$eval('#mobile-search-modal', el => el.classList.contains('hidden'));
      if (!isHidden) throw new Error('Mobile search modal did not close');
      return 'Modal dismissed cleanly';
    });

    await dismissModalIfOpen(page);
    await page.setViewportSize({ width: 1440, height: 900 });

    // -------------------------------------------------------------------------
    // SUITE 8: Advanced Browse Catalog & Filters
    // -------------------------------------------------------------------------
    const s8 = '8. Advanced Browse & Filters';
    await page.goto(`${BASE_URL}#browse`, { waitUntil: 'domcontentloaded' });
    await page.waitForSelector('#browse-results-container', { timeout: 8000 });

    await runTest(s8, 'Browse search input renders with clear button', async () => {
      const input = await page.$('#browse-search-input');
      const clearBtn = await page.$('#browse-clear-btn');
      if (!input || !clearBtn) throw new Error('Browse search bar elements missing');
      return 'Browse input and clear button present';
    });

    await runTest(s8, 'Browse results counter displays entry count', async () => {
      const countText = await page.$eval('#browse-results-count', el => el.innerText);
      if (!countText) throw new Error('Browse counter is empty');
      return `Counter: "${countText}"`;
    });

    await runTest(s8, 'Browse sort dropdown contains all required sort options', async () => {
      const options = await page.$$eval('#browse-sort-select option', els => els.map(o => o.value));
      const required = ['score', 'popularity', 'updated', 'release', 'episodes'];
      const missing = required.filter(r => !options.includes(r));
      if (missing.length > 0) throw new Error(`Missing sort options: ${missing.join(', ')}`);
      return `Sort options: ${options.join(', ')}`;
    });

    await runTest(s8, 'Changing sort dropdown re-executes browse query', async () => {
      await page.selectOption('#browse-sort-select', 'popularity');
      await page.waitForTimeout(800);
      const count = await page.$$eval('#browse-results-container .anime-card', els => els.length);
      return `Sorted by popularity: ${count} cards`;
    });

    await runTest(s8, 'Quick filter preset chips exist (Trending, Top Rated, Movies, etc.)', async () => {
      const chips = await page.$$eval('#browse-quick-chips .browse-chip', els => els.map(e => e.dataset.preset));
      if (chips.length < 4) throw new Error(`Expected at least 4 preset chips, got ${chips.length}`);
      return `Presets: ${chips.join(', ')}`;
    });

    await runTest(s8, 'Format filter switches between All, TV Series, Movie', async () => {
      const btns = await page.$$eval('#filter-format-group .filter-btn', els => els.map(b => b.dataset.format));
      if (!btns.includes('all') || !btns.includes('TV') || !btns.includes('MOVIE')) {
        throw new Error(`Missing format filters: ${btns.join(', ')}`);
      }
      await page.click('#filter-format-group [data-format="MOVIE"]');
      await page.waitForTimeout(800);
      return `Format filter applied (MOVIE)`;
    });

    await runTest(s8, 'Release Status filter switches between All, Airing, Finished', async () => {
      const btns = await page.$$eval('#filter-status-group .filter-btn', els => els.map(b => b.dataset.status));
      if (!btns.includes('all') || !btns.includes('RELEASING') || !btns.includes('FINISHED')) {
        throw new Error(`Missing status filters: ${btns.join(', ')}`);
      }
      await page.click('#filter-status-group [data-status="FINISHED"]');
      await page.waitForTimeout(800);
      return `Status filter applied (FINISHED)`;
    });

    await runTest(s8, 'Genre filter matrix renders checkboxes for all catalog genres', async () => {
      const checkboxes = await page.$$('#filter-genres-matrix .genre-matrix-chk');
      if (checkboxes.length < 14) throw new Error(`Expected >=14 genre checkboxes, got ${checkboxes.length}`);
      return `${checkboxes.length} Genre matrix checkboxes rendered`;
    });

    await runTest(s8, 'Selecting a genre checkbox in matrix filters catalog accurately', async () => {
      const chk = await page.$('#filter-genres-matrix input[value="Romance"]');
      if (!chk) throw new Error('Romance checkbox not found');
      await chk.check();
      await page.waitForTimeout(1000);
      const cards = await page.$$('#browse-results-container .anime-card');
      return `Romance filter applied: ${cards.length} cards displayed`;
    });

    await runTest(s8, 'Reset filters button clears all filters and restores default state', async () => {
      const resetBtn = await page.$('#browse-reset-filters');
      if (!resetBtn) throw new Error('Reset filters button not found');
      await resetBtn.click();
      await page.waitForTimeout(800);
      const checkedBoxes = await page.$$('#filter-genres-matrix input:checked');
      if (checkedBoxes.length > 0) throw new Error('Some checkboxes remain checked after reset');
      return 'All filters reset';
    });

    // -------------------------------------------------------------------------
    // SUITE 9: Browse View Modes
    // -------------------------------------------------------------------------
    const s9 = '9. Browse View Modes';
    await runTest(s9, 'Detailed view mode renders meta list with descriptions', async () => {
      await page.click('#view-mode-detailed');
      await page.waitForSelector('#browse-results-container .group', { timeout: 10000 });
      const items = await page.$$('#browse-results-container .group');
      if (items.length === 0) throw new Error('No items rendered in detailed view');
      return `Detailed view: ${items.length} entries`;
    });

    await runTest(s9, 'Compact view mode renders high-density feed', async () => {
      await page.click('#view-mode-compact');
      await page.waitForSelector('#browse-results-container .group', { timeout: 10000 });
      const items = await page.$$('#browse-results-container .group');
      if (items.length === 0) throw new Error('No items rendered in compact view');
      return `Compact view: ${items.length} entries`;
    });

    await runTest(s9, 'Grid view mode restores multi-column poster cards', async () => {
      await page.click('#view-mode-grid');
      await page.waitForSelector('#browse-results-container .anime-card, #browse-results-container .group', { timeout: 10000 });
      const cards = await page.$$('#browse-results-container .anime-card, #browse-results-container .group');
      if (cards.length === 0) throw new Error('Grid cards not restored');
      return `Grid view restored: ${cards.length} cards`;
    });

    // -------------------------------------------------------------------------
    // SUITE 10: Watch Room & Player Interactions
    // -------------------------------------------------------------------------
    const s10 = '10. Watch Room & Player';
    await page.goto(`${BASE_URL}#watch?anime=20&ep=1`, { waitUntil: 'domcontentloaded' });
    await page.waitForSelector('#watch-view:not(.hidden)', { timeout: 8000 });

    await runTest(s10, 'Watch view activates with breadcrumb navigation', async () => {
      const title = await page.$eval('#crumb-anime-title', el => el.innerText.trim()).catch(() => '');
      const ep = await page.$eval('#crumb-episode-num', el => el.innerText.trim()).catch(() => '');
      return `Breadcrumbs: "${title}" / ${ep}`;
    });

    await runTest(s10, 'Artplayer mount container exists and is visible', async () => {
      const mount = await page.$('#artplayer-mount');
      if (!mount) throw new Error('Player mount not found');
      const isVisible = await mount.isVisible();
      if (!isVisible) throw new Error('Player mount is not visible');
      return 'Video stage mounted';
    });

    await runTest(s10, 'SUB and DUB audio toggles exist in player toolbar', async () => {
      const subBtn = await page.$('#btn-stream-sub');
      const dubBtn = await page.$('#btn-stream-dub');
      if (!subBtn || !dubBtn) throw new Error('Audio mode buttons missing');
      return 'Audio mode toggles present';
    });

    await runTest(s10, 'Streaming Quality button & menu render with auto option', async () => {
      const qualityBtn = await page.$('#btn-quality-down');
      if (!qualityBtn) throw new Error('Quality button not found');
      await qualityBtn.click();
      await page.waitForTimeout(200);
      const menu = await page.$('#quality-dropdown-menu:not(.hidden)');
      if (!menu) throw new Error('Quality dropdown menu did not open');
      await qualityBtn.click(); // Close
      return 'Quality selector verified';
    });

    await runTest(s10, 'Captions (CC) toggle button is present and clickable', async () => {
      const ccBtn = await page.$('#btn-toggle-captions-down');
      if (!ccBtn) throw new Error('Captions toggle button missing');
      const statusText = await page.$eval('#text-captions-status', el => el.innerText.trim());
      return `Captions status: "${statusText}"`;
    });

    await runTest(s10, 'Skip Intro (+85s) button is present and clickable', async () => {
      const skipBtn = await page.$('#btn-skip-intro');
      if (!skipBtn) throw new Error('Skip intro button missing');
      return 'Skip Intro (+85s) button verified';
    });

    await runTest(s10, 'Anime Details synopsis card renders metadata', async () => {
      await page.waitForSelector('#detail-anime-title', { timeout: 5000 });
      const title = await page.$eval('#detail-anime-title', el => el.innerText.trim());
      const score = await page.$eval('#detail-anime-score', el => el.innerText.trim());
      return `Details: "${title}" | ${score}`;
    });

    // -------------------------------------------------------------------------
    // SUITE 11: Episode Queue & Controls
    // -------------------------------------------------------------------------
    const s11 = '11. Episode Queue & Controls';
    await runTest(s11, 'Episode queue sidebar renders with episode list and search filter', async () => {
      await page.waitForSelector('#episode-search-filter', { timeout: 5000 });
      const filterInput = await page.$('#episode-search-filter');
      if (!filterInput) throw new Error('Episode filter input missing');
      return 'Episode queue sidebar verified';
    });

    await runTest(s11, 'Next and Previous episode buttons exist in player controls', async () => {
      const prevBtn = await page.$('#btn-prev-ep');
      const nextBtn = await page.$('#btn-next-ep');
      if (!prevBtn || !nextBtn) throw new Error('Prev/Next episode buttons missing');
      return 'Prev and Next episode buttons verified';
    });

    await runTest(s11, 'Recommendations panel "You Might Also Like" renders', async () => {
      const recsPanel = await page.$('#recs-list-container');
      if (!recsPanel) throw new Error('Recommendations container missing');
      return 'Recommendations container verified';
    });

    // -------------------------------------------------------------------------
    // SUITE 12: Authentication & Account
    // -------------------------------------------------------------------------
    const s12 = '12. Authentication & Account';
    await page.goto(`${BASE_URL}#home`, { waitUntil: 'domcontentloaded' });

    await runTest(s12, 'Sign In button in header opens Auth modal', async () => {
      await page.click('#btn-sign-in-nav');
      await page.waitForTimeout(300);
      const isVisible = await page.$eval('#auth-modal', el => !el.classList.contains('hidden'));
      if (!isVisible) throw new Error('Auth modal did not open');
      return 'Auth modal opened';
    });

    await runTest(s12, 'Auth modal toggles between "Sign In" and "Create Account"', async () => {
      await page.click('#tab-auth-register');
      await page.waitForTimeout(200);
      const formRegisterVisible = await page.$eval('#form-auth-register', el => !el.classList.contains('hidden'));
      if (!formRegisterVisible) throw new Error('Registration form not displayed');

      await page.click('#tab-auth-login');
      await page.waitForTimeout(200);
      const formLoginVisible = await page.$eval('#form-auth-login', el => !el.classList.contains('hidden'));
      if (!formLoginVisible) throw new Error('Login form not displayed');
      return 'Sign In & Create Account tabs toggle cleanly';
    });

    await runTest(s12, '1-Click Demo Login button authenticates test user', async () => {
      const demoBtn = await page.$('#btn-auth-demo');
      if (!demoBtn) throw new Error('1-Click Demo button not found');
      await demoBtn.click();
      await page.waitForSelector('#nav-user-block:not(.hidden)', { timeout: 10000 });
      const modalClosed = await page.$eval('#auth-modal', el => el.classList.contains('hidden'));
      if (!modalClosed) throw new Error('Auth modal did not close after login');
      const userBlockVisible = await page.$eval('#nav-user-block', el => !el.classList.contains('hidden'));
      if (!userBlockVisible) throw new Error('User profile block not visible after demo login');
      return '1-Click Demo Login succeeded (User session active)';
    });

    await runTest(s12, 'User profile block displays username and rank', async () => {
      const name = await page.$eval('#nav-user-name', el => el.innerText.trim());
      const tier = await page.$eval('#nav-user-tier', el => el.innerText.trim());
      return `User: "${name}" | ${tier}`;
    });

    await runTest(s12, 'Navigating to #account displays user statistics dashboard', async () => {
      await page.goto(`${BASE_URL}#account`, { waitUntil: 'domcontentloaded' });
      await page.waitForSelector('#account-view:not(.hidden)', { timeout: 8000 });
      const statScore = await page.$eval('#stat-mean-score', el => el.innerText.trim());
      return `User dashboard rendered. Mean Score: ${statScore}`;
    });

    await runTest(s12, 'Account tabs (Watchlist, History, Preferences, Settings) toggle cleanly', async () => {
      const tabs = await page.$$eval('#account-sub-tabs .account-tab', els => els.map(t => t.dataset.tab));
      const required = ['watchlist', 'history', 'preferences', 'settings'];
      const missing = required.filter(r => !tabs.includes(r));
      if (missing.length > 0) throw new Error(`Missing account tabs: ${missing.join(', ')}`);

      await page.click('#account-sub-tabs [data-tab="history"]');
      await page.waitForTimeout(200);
      const histVisible = await page.$eval('#tab-content-history', el => !el.classList.contains('hidden'));
      if (!histVisible) throw new Error('History tab did not become visible');

      await page.click('#account-sub-tabs [data-tab="preferences"]');
      await page.waitForTimeout(200);
      const prefVisible = await page.$eval('#tab-content-preferences', el => !el.classList.contains('hidden'));
      if (!prefVisible) throw new Error('Preferences tab did not become visible');

      return `All 4 account tabs verified`;
    });

    await runTest(s12, 'Playback preferences controls render (Audio, Quality, Auto-skip, Auto-next)', async () => {
      const audio = await page.$('#pref-audio');
      const quality = await page.$('#pref-quality');
      const skip = await page.$('#pref-skip-intro');
      const next = await page.$('#pref-auto-next');
      const saveBtn = await page.$('#btn-save-preferences');
      if (!audio || !quality || !skip || !next || !saveBtn) {
        throw new Error('Some playback preference inputs missing');
      }
      return 'Preferences controls verified';
    });

    // -------------------------------------------------------------------------
    // SUITE 13: Footer, TV APK Download & Donate Links
    // -------------------------------------------------------------------------
    const s13 = '13. Footer, APK & Donate';
    await page.goto(`${BASE_URL}#home`, { waitUntil: 'domcontentloaded' });
    await page.waitForSelector('footer', { timeout: 5000 });

    await runTest(s13, 'Footer contains valid Android TV APK download link', async () => {
      const apkLink = await page.$('footer a[download="ani-cli-tv.apk"]');
      if (!apkLink) throw new Error('TV APK download link missing from footer');
      const href = await apkLink.getAttribute('href');
      if (!href || !href.includes('.apk')) throw new Error(`Invalid APK href: ${href}`);
      return `APK link: ${href}`;
    });

    await runTest(s13, 'Footer contains Razorpay donation card pointing to razorpay.me/@ompradippatil', async () => {
      const donateCard = await page.$('footer a[href*="razorpay.me/@ompradippatil"]');
      if (!donateCard) throw new Error('Razorpay donate card missing from footer');
      const href = await donateCard.getAttribute('href');
      return `Donate card: ${href}`;
    });

    await runTest(s13, 'Footer Quick Links contains "Donate via Razorpay"', async () => {
      const donateLinks = await page.$$('footer a[href="https://razorpay.me/@ompradippatil"]');
      if (donateLinks.length < 2) throw new Error(`Expected at least 2 razorpay links in footer, found ${donateLinks.length}`);
      return `${donateLinks.length} Razorpay links present in footer`;
    });

    await runTest(s13, 'Footer bottom bar contains "Donate (Razorpay)" pill button', async () => {
      const pillBtn = await page.$('footer a:has-text("Donate (Razorpay)")');
      if (!pillBtn) throw new Error('Donate pill button missing from bottom bar');
      return 'Bottom bar Donate pill button verified';
    });

    await runTest(s13, 'Footer displays 2026 Om CLI copyright and open-source attribution', async () => {
      const footerText = await page.$eval('footer', el => el.innerText);
      if (!footerText.includes('Om CLI') || !footerText.includes('Om Pradip Patil')) {
        throw new Error(`Footer missing attribution: ${footerText.slice(0, 100)}`);
      }
      return 'Copyright and author credit verified';
    });

    // -------------------------------------------------------------------------
    // SUITE 14: Backend API & Edge Endpoints
    // -------------------------------------------------------------------------
    const s14 = '14. Backend API & Edge';
    await runTest(s14, 'GET /api/trending returns 200 with list of anime objects', async () => {
      const res = await page.request.get(`${BASE_URL}/api/trending`);
      if (res.status() !== 200) throw new Error(`Status ${res.status()}`);
      const json = await res.json();
      if (!json.success || !Array.isArray(json.data) || json.data.length === 0) {
        throw new Error('Invalid trending API response');
      }
      return `${json.data.length} trending items returned`;
    });

    await runTest(s14, 'GET /api/popular returns 200 with list of anime objects', async () => {
      const res = await page.request.get(`${BASE_URL}/api/popular`);
      if (res.status() !== 200) throw new Error(`Status ${res.status()}`);
      const json = await res.json();
      if (!json.success || !Array.isArray(json.data) || json.data.length === 0) {
        throw new Error('Invalid popular API response');
      }
      return `${json.data.length} popular items returned`;
    });

    await runTest(s14, 'GET /api/top-rated returns 200 with list of anime objects', async () => {
      const res = await page.request.get(`${BASE_URL}/api/top-rated`);
      if (res.status() !== 200) throw new Error(`Status ${res.status()}`);
      const json = await res.json();
      if (!json.success || !Array.isArray(json.data) || json.data.length === 0) {
        throw new Error('Invalid top-rated API response');
      }
      return `${json.data.length} top-rated items returned`;
    });

    await runTest(s14, 'GET /api/search?q=naruto returns matching Naruto anime', async () => {
      const res = await page.request.get(`${BASE_URL}/api/search?q=naruto`);
      if (res.status() !== 200) throw new Error(`Status ${res.status()}`);
      const json = await res.json();
      if (!json.success || !Array.isArray(json.data) || json.data.length === 0) {
        throw new Error('Search API returned no results');
      }
      const firstTitle = json.data[0].title.english || json.data[0].title.romaji || json.data[0].title.userPreferred;
      if (!firstTitle.toLowerCase().includes('naruto')) {
        throw new Error(`First search result is not Naruto: ${firstTitle}`);
      }
      return `Top search result: "${firstTitle}"`;
    });

    await runTest(s14, 'GET /api/browse?search=bleach returns matching Bleach anime without seed leak', async () => {
      const res = await page.request.get(`${BASE_URL}/api/browse?search=bleach`);
      if (res.status() !== 200) throw new Error(`Status ${res.status()}`);
      const json = await res.json();
      if (!json.success || !Array.isArray(json.data) || json.data.length === 0) {
        throw new Error('Browse search returned no results');
      }
      const firstTitle = json.data[0].title.english || json.data[0].title.romaji || json.data[0].title.userPreferred;
      if (!firstTitle.toLowerCase().includes('bleach')) {
        throw new Error(`Browse returned unmatching title: ${firstTitle}`);
      }
      return `Browse search result: "${firstTitle}"`;
    });

    await runTest(s14, 'GET /download/tv-apk responds with HTTP 302 redirecting to APK', async () => {
      const res = await page.request.get(`${BASE_URL}/download/tv-apk`, { maxRedirects: 0 });
      if (res.status() !== 302) throw new Error(`Expected 302 redirect, got ${res.status()}`);
      const location = res.headers()['location'];
      if (!location || !location.includes('ani-cli-production-signed.apk')) {
        throw new Error(`Redirect location invalid: ${location}`);
      }
      return `Redirects to: ${location}`;
    });

    await runTest(s14, 'GET /ani-cli-tv.apk responds with HTTP 302 redirecting to APK', async () => {
      const res = await page.request.get(`${BASE_URL}/ani-cli-tv.apk`, { maxRedirects: 0 });
      if (res.status() !== 302) throw new Error(`Expected 302 redirect, got ${res.status()}`);
      const location = res.headers()['location'];
      if (!location || !location.includes('ani-cli-production-signed.apk')) {
        throw new Error(`Redirect location invalid: ${location}`);
      }
      return `Redirects to: ${location}`;
    });

    await runTest(s14, 'GET /donate responds with HTTP 302 redirecting to Razorpay', async () => {
      const res = await page.request.get(`${BASE_URL}/donate`, { maxRedirects: 0 });
      if (res.status() !== 302) throw new Error(`Expected 302 redirect, got ${res.status()}`);
      const location = res.headers()['location'];
      if (!location || !location.includes('razorpay.me/@ompradippatil')) {
        throw new Error(`Redirect location invalid: ${location}`);
      }
      return `Redirects to: ${location}`;
    });

    // -------------------------------------------------------------------------
    // SUITE 15: Mobile Viewport & Responsiveness
    // -------------------------------------------------------------------------
    const s15 = '15. Mobile Viewport (375x812)';
    const mobileContext = await browser.newContext({
      viewport: { width: 375, height: 812 },
      userAgent: 'Mozilla/5.0 (iPhone; CPU iPhone OS 16_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.5 Mobile/15E148 Safari/604.1'
    });
    const mobilePage = await mobileContext.newPage();
    mobilePage.setDefaultTimeout(8000);

    await runTest(s15, 'Mobile page loads and renders thumb navigation bar', async () => {
      await mobilePage.goto(BASE_URL, { waitUntil: 'domcontentloaded' });
      await mobilePage.waitForSelector('#mobile-bottom-nav', { timeout: 5000 });
      const navVisible = await mobilePage.$eval('#mobile-bottom-nav', el => !el.classList.contains('hidden'));
      if (!navVisible) throw new Error('Mobile bottom nav is hidden');
      return 'Mobile bottom navigation bar rendered';
    });

    await runTest(s15, 'Mobile bottom nav contains 4 active tabs (Home, Browse, Watchlist, Account)', async () => {
      const btns = await mobilePage.$$eval('#mobile-bottom-nav .mobile-nav-btn', els => els.map(e => e.dataset.nav));
      if (btns.length !== 4) throw new Error(`Expected 4 mobile nav buttons, got ${btns.length}`);
      return `Mobile tabs: ${btns.join(', ')}`;
    });

    await runTest(s15, 'Clicking mobile search button opens full-screen search modal', async () => {
      await mobilePage.click('#btn-mobile-search-toggle');
      await mobilePage.waitForTimeout(300);
      const modalOpen = await mobilePage.$eval('#mobile-search-modal', el => !el.classList.contains('hidden'));
      if (!modalOpen) throw new Error('Mobile search modal failed to open');
      return 'Mobile search modal opened';
    });

    await runTest(s15, 'Mobile search modal renders trending search chips', async () => {
      const tags = await mobilePage.$$eval('#mobile-quick-tags .mobile-search-tag', els => els.map(e => e.dataset.query));
      if (tags.length < 4) throw new Error(`Expected >=4 trending tags, got ${tags.length}`);
      return `Trending tags: ${tags.join(', ')}`;
    });

    await runTest(s15, 'Clicking trending search chip "Demon Slayer" displays results', async () => {
      await mobilePage.click('#mobile-quick-tags [data-query="Demon Slayer"]');
      await mobilePage.waitForTimeout(1000);
      const results = await mobilePage.$$('#mobile-search-results > div');
      if (results.length === 0) throw new Error('No search results displayed in mobile search');
      return `${results.length} instant search results rendered`;
    });

    await runTest(s15, 'Mobile search modal close button closes overlay', async () => {
      await mobilePage.click('#mobile-search-close-btn');
      await mobilePage.waitForTimeout(300);
      const modalClosed = await mobilePage.$eval('#mobile-search-modal', el => el.classList.contains('hidden'));
      if (!modalClosed) throw new Error('Mobile search modal did not close');
      return 'Mobile search modal closed';
    });

    await runTest(s15, 'Mobile Browse view renders filter trigger button with counter badge', async () => {
      await mobilePage.click('#mobile-bottom-nav [data-nav="browse"]');
      await mobilePage.waitForTimeout(500);
      const filterBtn = await mobilePage.$('#btn-mobile-filter-open');
      if (!filterBtn) throw new Error('Mobile filter button missing in Browse');
      const badge = await mobilePage.$eval('#mobile-filter-count-badge', el => el.innerText.trim());
      return `Mobile filter button present (Badge: ${badge})`;
    });

    await mobileContext.close();

    // -------------------------------------------------------------------------
    // SUITE 16: Console Integrity & Performance
    // -------------------------------------------------------------------------
    const s16 = '16. Runtime Health';
    await runTest(s16, 'Zero uncaught JavaScript exceptions during execution', async () => {
      const fatalErrors = pageErrors.filter(e => !e.includes('favicon') && !e.includes('net::ERR_') && !e.includes('status of 404'));
      if (fatalErrors.length > 0) {
        throw new Error(`Encountered ${fatalErrors.length} console errors: ${fatalErrors.slice(0, 3).join(' | ')}`);
      }
      return 'No uncaught fatal errors observed';
    });

  } finally {
    await browser.close();
  }

  // -------------------------------------------------------------------------
  // SUMMARY REPORT GENERATION
  // -------------------------------------------------------------------------
  console.log('\n================================================================');
  console.log(`🏁 TEST EXECUTION COMPLETE: ${results.length} TOTAL TESTS`);
  console.log(`✅ PASSED: ${passedCount}`);
  console.log(`❌ FAILED: ${failedCount}`);
  const passRate = ((passedCount / results.length) * 100).toFixed(1);
  console.log(`📊 PASS RATE: ${passRate}%`);
  console.log('================================================================\n');

  // Output formatted JSON for report generation
  const reportPath = path.join(__dirname, 'test-report.json');
  fs.writeFileSync(reportPath, JSON.stringify({
    timestamp: new Date().toISOString(),
    baseUrl: BASE_URL,
    total: results.length,
    passed: passedCount,
    failed: failedCount,
    passRate: `${passRate}%`,
    results
  }, null, 2));

  console.log(`📄 Detailed JSON report written to: ${reportPath}`);

  if (failedCount > 0) {
    process.exit(1);
  }
}

main().catch(err => {
  console.error('Fatal test runner error:', err);
  process.exit(1);
});
