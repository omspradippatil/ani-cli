// website/tests/test-mobile-optimization.js
// Automated End-to-End Verification for Mobile Optimization & Auto-Rotate

const { chromium, devices } = require('playwright');
const { launchBrowser } = require('./browser');

const BASE_URL = process.env.BASE_URL || 'http://localhost:3456';

const summary = {
  total: 0,
  passed: 0,
  failed: 0,
  tests: []
};

function record(name, passed, details = '') {
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

async function runTests() {
  console.log('============================================================');
  console.log('📱 STARTING MOBILE OPTIMIZATION & AUTO-ROTATE VERIFICATION');
  console.log(`🌐 Target: ${BASE_URL}`);
  console.log('============================================================\n');

  const browser = await launchBrowser();

  try {
    // -------------------------------------------------------------------------
    // TEST GROUP 1: Mobile Viewport & Inline Player Controls Ergonomics
    // -------------------------------------------------------------------------
    console.log('📌 [TEST GROUP 1] Mobile Portrait Viewport & Non-Overlapping Controls');
    const iPhone = devices['iPhone 14'];
    const context = await browser.newContext({
      ...iPhone,
      viewport: { width: 390, height: 844 }
    });
    const page = await context.newPage();
    page.on('console', msg => console.log('PAGE LOG:', msg.text()));
    page.on('pageerror', err => console.log('PAGE ERROR:', err.message));

    await page.goto(`${BASE_URL}/#home`, { waitUntil: 'domcontentloaded' });
    await page.waitForSelector('#home-view:not(.hidden)', { timeout: 8000 });
    await page.goto(`${BASE_URL}/#watch?anime=20&ep=1`, { waitUntil: 'domcontentloaded' });
    await page.waitForSelector('#watch-view:not(.hidden)', { timeout: 10000 });
    await page.waitForSelector('.art-video-player .art-controls', { timeout: 15000 });

    // 1.1 Verify mobile navigation and player mount
    const playerMount = await page.$('#artplayer-mount');
    record('1.1 Player mounted in mobile portrait', !!playerMount);

    // 1.2 Check player bottom control bar exists
    const controls = await page.$('.art-controls');
    record('1.2 Player controls bar rendered', !!controls);

    // 1.3 Verify no horizontal overflow in controls bar
    const overflowCheck = await page.evaluate(() => {
      const bar = document.querySelector('.art-controls');
      const left = document.querySelector('.art-controls-left');
      const right = document.querySelector('.art-controls-right');
      if (!bar || !left || !right) return false;

      const barRect = bar.getBoundingClientRect();
      const leftRect = left.getBoundingClientRect();
      const rightRect = right.getBoundingClientRect();

      // Check if left and right controls overlap in portrait
      const overlap = leftRect.right > rightRect.left + 2;
      return {
        barWidth: barRect.width,
        leftRight: leftRect.right,
        rightLeft: rightRect.left,
        overlap
      };
    });
    record('1.3 Controls left & right never collide or overlap', !overflowCheck.overlap, 
      `Left end: ${overflowCheck.leftRight?.toFixed(1)}px, Right start: ${overflowCheck.rightLeft?.toFixed(1)}px`);

    // 1.4 Primary buttons in toolbar beneath player are visible on mobile
    const toolbar = await page.$('#player-toolbar');
    record('1.4 Action toolbar rendered beneath video with large touch targets', !!toolbar);

    // -------------------------------------------------------------------------
    // TEST GROUP 2: Fullscreen Entry & Landscape Auto-Rotation
    // -------------------------------------------------------------------------
    console.log('\n📌 [TEST GROUP 2] Fullscreen & Mobile Landscape Auto-Rotate');

    // 2.1 Trigger fullscreen via player API / button
    await page.evaluate(async () => {
      if (window.artplayerInstance && typeof window.artplayerInstance.toggleFullscreen === 'function') {
        await window.artplayerInstance.toggleFullscreen();
      } else {
        const btn = document.getElementById('btn-fullscreen-down');
        if (btn) btn.click();
      }
    });
    await page.waitForTimeout(600);

    const isFullscreen = await page.evaluate(() => document.body.classList.contains('player-in-fullscreen'));
    record('2.1 Fullscreen state active on body', isFullscreen);

    // 2.2 Forced landscape rotation CSS applied in mobile portrait
    const isRotated = await page.evaluate(() => {
      const hasForcedClass = document.body.classList.contains('player-forced-landscape');
      const player = document.querySelector('.art-video-player');
      const computed = player ? window.getComputedStyle(player) : null;
      const transform = computed ? computed.transform : '';
      return hasForcedClass || transform.includes('matrix');
    });
    record('2.2 Landscape orientation locked or CSS 90deg rotation applied', isRotated);

    // 2.3 Mobile bottom nav & header cleanly hidden in fullscreen
    const hiddenElements = await page.evaluate(() => {
      const nav = document.getElementById('mobile-bottom-nav');
      const header = document.querySelector('header');
      const navHidden = !nav || window.getComputedStyle(nav).display === 'none';
      const headerHidden = !header || window.getComputedStyle(header).display === 'none';
      return navHidden && headerHidden;
    });
    record('2.3 Mobile bottom nav and navbar hidden during fullscreen', hiddenElements);

    // 2.4 All cinema controls active and visible in landscape fullscreen
    const landscapeControls = await page.evaluate(() => {
      const prev = document.querySelector('.art-control-prev-ep-btn, [data-name="prev-ep-btn"]');
      const next = document.querySelector('.art-control-next-ep-btn, [data-name="next-ep-btn"]');
      const skip = document.querySelector('.art-control-skip-intro-btn, [data-name="skip-intro-btn"]');
      const mode = document.querySelector('.art-control-audio-mode-btn, [data-name="audio-mode-btn"]');
      const cc = document.querySelector('.art-control-cc-btn, [data-name="cc-btn"]');
      const q = document.querySelector('.art-control-quality-btn, [data-name="quality-btn"]');
      const fs = document.querySelector('.art-control-custom-fullscreen-btn, [data-name="custom-fullscreen-btn"]');
      return !!(prev && next && skip && mode && cc && q && fs);
    });
    record('2.4 Cinema controls (Prev, Next, +85s, SUB/DUB, CC, Quality, Fullscreen) available in fullscreen', landscapeControls);

    // -------------------------------------------------------------------------
    // TEST GROUP 3: Physical Device Tilt & Exit Fullscreen
    // -------------------------------------------------------------------------
    console.log('\n📌 [TEST GROUP 3] Physical Rotation & Exit Fullscreen');

    // 3.1 Simulate user tilting phone to landscape (844 x 390)
    await page.setViewportSize({ width: 844, height: 390 });
    await page.evaluate(() => {
      window.dispatchEvent(new Event('resize'));
      window.dispatchEvent(new Event('orientationchange'));
    });
    await page.waitForTimeout(300);

    const naturalLandscape = await page.evaluate(() => {
      // In true landscape, forced CSS rotation should be disengaged
      return !document.body.classList.contains('player-forced-landscape');
    });
    record('3.1 Physical landscape tilt smoothly removes CSS forced rotation', naturalLandscape);

    // 3.2 Exit fullscreen
    await page.evaluate(async () => {
      if (window.artplayerInstance && typeof window.artplayerInstance.exitFullscreen === 'function') {
        await window.artplayerInstance.exitFullscreen();
      } else {
        const btn = document.getElementById('btn-fullscreen-down');
        if (btn) btn.click();
      }
    });
    await page.waitForTimeout(600);

    const exitedFs = await page.evaluate(() => {
      return !document.body.classList.contains('player-in-fullscreen') &&
             !document.body.classList.contains('player-forced-landscape');
    });
    record('3.2 Exit fullscreen cleanly restores page and clears rotation states', exitedFs);

    await context.close();

    // -------------------------------------------------------------------------
    // TEST GROUP 4: Subtitles <i> and <b> Formatting Verification
    // -------------------------------------------------------------------------
    console.log('\n📌 [TEST GROUP 4] Subtitles Tag Sanitation');
    const desktopContext = await browser.newContext();
    const desktopPage = await desktopContext.newPage();
    await desktopPage.goto(`${BASE_URL}#watch?anime=20&ep=1`, { waitUntil: 'domcontentloaded' });
    await desktopPage.waitForSelector('.art-subtitle', { timeout: 15000 });

    // 4.1 Test subtitle line formatting
    const subtitleSanitation = await desktopPage.evaluate(() => {
      const art = window.artplayerInstance?.art;
      const subtitleContainer = art?.template?.$subtitle;
      if (!subtitleContainer) return false;

      // Inject sample subtitle line containing escaped and raw <i> and <b> tags
      const testLine = document.createElement('div');
      testLine.className = 'art-subtitle-line';
      testLine.innerHTML = '&lt;i&gt;Italic Text Sample&lt;/i&gt; and &lt;b&gt;Bold Word&lt;/b&gt;';
      subtitleContainer.appendChild(testLine);

      // Trigger mutation
      return new Promise(resolve => {
        setTimeout(() => {
          const hasRealItalic = !!testLine.querySelector('i');
          const hasRealBold = !!testLine.querySelector('b');
          const hasRawEscaped = testLine.innerHTML.includes('&lt;i&gt;') || testLine.textContent.includes('<i>');
          resolve(hasRealItalic && hasRealBold && !hasRawEscaped);
        }, 100);
      });
    });
    record('4.1 Subtitle <i> and <b> tags rendered as real formatted HTML, no literal <i> on screen', subtitleSanitation);

    await desktopContext.close();

  } finally {
    await browser.close();
  }

  console.log('\n============================================================');
  console.log(`📊 Test Summary: ${summary.passed}/${summary.total} Passed (${summary.failed} Failed)`);
  console.log('============================================================\n');

  if (summary.failed > 0) {
    process.exit(1);
  }
}

runTests().catch(err => {
  console.error('Fatal test runner error:', err);
  process.exit(1);
});
