/**
 * Responsive Layout Audit for Om CLI
 *
 * Drives a real browser across every breakpoint the design targets and fails
 * when the page overflows horizontally or an element is wider than the viewport.
 *
 * Uses the locally installed Edge/Chrome channel so no browser download is
 * needed (the previous hardcoded macOS Chrome path could never run on Windows).
 *
 * Usage:  node tests/test-responsive.js
 *         BASE_URL=http://localhost:3000 node tests/test-responsive.js
 */

const { chromium } = require('playwright');

const BASE_URL = process.env.BASE_URL || 'http://localhost:3000';

// Widths that matter: small phones -> desktop. 320 is the narrowest realistic
// device (iPhone SE 1st gen / Galaxy Fold cover screen).
const VIEWPORTS = [
  { name: 'Fold cover  320x653', width: 320, height: 653 },
  { name: 'Phone S     360x800', width: 360, height: 800 },
  { name: 'iPhone 14   390x844', width: 390, height: 844 },
  { name: 'Phone L     414x896', width: 414, height: 896 },
  { name: 'Tablet      768x1024', width: 768, height: 1024 },
  { name: 'Laptop      1024x768', width: 1024, height: 768 },
  { name: 'Desktop     1280x800', width: 1280, height: 800 },
  { name: 'Wide        1600x900', width: 1600, height: 900 }
];

// Views to visit; each is a hash route in the SPA.
// Covers EVERY view the app can render, not just the three tabbed ones: the
// cinema/watch room and the standalone watchlist route were previously
// unmeasured, so "the whole site is responsive" was not actually verified.
const ROUTES = [
  { name: 'home', hash: '#home' },
  { name: 'browse', hash: '#browse' },
  { name: 'watch', hash: '#watch?anime=21&ep=1' },
  { name: 'account', hash: '#account' },
  { name: 'watchlist', hash: '#watchlist' }
];

/**
 * Stops external API calls (AniList GraphQL, the streaming provider) from
 * turning a layout audit into a network race. Tailwind, jsdelivr and the font
 * CDN are deliberately left alone: the layout under test depends on them.
 */
async function blockExternalApis(context) {
  await context.route('**graphql.anilist.co/**', r => r.abort());
  await context.route('**anilist.co/**', r => r.abort());
}

let passed = 0;
let failed = 0;
const failures = [];

function record(ok, message) {
  if (ok) {
    console.log(`  ✅ PASS: ${message}`);
    passed++;
  } else {
    console.error(`  ❌ FAIL: ${message}`);
    failed++;
    failures.push(message);
  }
}

/**
 * Measures document-level horizontal overflow plus the widest offending nodes.
 */
async function measureOverflow(page) {
  return page.evaluate(() => {
    const doc = document.documentElement;
    const viewportWidth = doc.clientWidth;
    const scrollWidth = Math.max(doc.scrollWidth, document.body.scrollWidth);
    const overflow = scrollWidth - viewportWidth;

    const offenders = [];
    if (overflow > 1) {
      document.querySelectorAll('body *').forEach(el => {
        const style = window.getComputedStyle(el);
        if (style.display === 'none' || style.visibility === 'hidden') return;
        if (style.position === 'fixed') return; // fixed overlays are intentional
        const rect = el.getBoundingClientRect();
        if (rect.width === 0) return;
        // Only report the element that actually sticks out to the right
        if (rect.right > viewportWidth + 1) {
          offenders.push({
            tag: el.tagName.toLowerCase(),
            id: el.id || '',
            cls: (el.className && typeof el.className === 'string'
              ? el.className.split(/\s+/).slice(0, 4).join(' ')
              : ''),
            right: Math.round(rect.right),
            width: Math.round(rect.width)
          });
        }
      });
    }

    // Keep only the outermost few, deepest duplicates are noise
    return {
      viewportWidth,
      scrollWidth,
      overflow,
      offenders: offenders.slice(0, 6)
    };
  });
}

/**
 * Font-independent pass.
 *
 * Material Symbols is a ligature font, so before it loads the browser paints the
 * raw ligature text ("account_circle") which is far wider than the glyph. On a
 * slow or blocked font CDN that used to push the header past the viewport edge
 * on narrow phones. Blocking the font requests makes that condition
 * deterministic instead of a race against the network.
 */
async function runFontBlockedPass(browser) {
  console.log('\n📌 FONT CDN BLOCKED (cold-load / offline condition)');

  for (const route of ROUTES) {
    for (const vp of VIEWPORTS.filter(v => v.width <= 414)) {
      const context = await browser.newContext({
        viewport: { width: vp.width, height: vp.height },
        isMobile: true,
        hasTouch: true
      });
      await blockExternalApis(context);

      // Refuse every webfont so the page runs on fallback fonts only.
      await context.route('**fonts.googleapis.com/**', r => r.abort());
      await context.route('**fonts.gstatic.com/**', r => r.abort());

      const page = await context.newPage();
      await page.goto(`${BASE_URL}/${route.hash}`, { waitUntil: 'domcontentloaded' });
      await page.waitForTimeout(900);

      const result = await measureOverflow(page);
      record(
        result.overflow <= 1,
        `${route.hash} ${vp.name} — no overflow without webfonts` +
          (result.overflow > 1
            ? ` (overflows by ${result.overflow}px, scrollWidth ${result.scrollWidth} > ${result.viewportWidth})` +
              result.offenders.map(o =>
                `\n           ↳ <${o.tag}${o.id ? ` id="${o.id}"` : ''}> right=${o.right}px width=${o.width}px class="${o.cls}"`
              ).join('')
            : '')
      );

      await context.close();
    }
  }
}

/**
 * Interaction pass: drives the account sub-tabs at phone width.
 *
 * switchAccountTab() used to rewrite className on every click, which dropped the
 * responsive variants (px-3 sm:px-4, py-2 sm:py-2.5, gap-1.5 sm:gap-2,
 * whitespace-nowrap) and made the tab strip misbehave after the first tap.
 */
async function runAccountTabPass(browser) {
  console.log('\n📌 ACCOUNT SUB-TABS AFTER INTERACTION (phone width)');

  const context = await browser.newContext({
    viewport: { width: 390, height: 844 },
    isMobile: true,
    hasTouch: true
  });
  const page = await context.newPage();
  await page.goto(`${BASE_URL}/#account`, { waitUntil: 'domcontentloaded' });
  await page.waitForTimeout(600);

  for (const tab of ['history', 'preferences', 'settings', 'watchlist']) {
    const info = await page.evaluate(async (tabName) => {
      const btn = document.querySelector(`#account-sub-tabs [data-tab="${tabName}"]`);
      if (!btn) return { error: `no tab button for ${tabName}` };
      btn.click();
      await new Promise(r => setTimeout(r, 120));

      const strip = document.getElementById('account-sub-tabs');
      const cs = getComputedStyle(btn);
      const panel = document.getElementById(`tab-content-${tabName}`);
      const stripRect = strip.getBoundingClientRect();

      return {
        // The panel must be visible AND inside the account view
        panelVisible: !!panel && !panel.classList.contains('hidden'),
        panelInAccountView: !!panel && !!panel.closest('#account-view'),
        // Responsive variants must survive the class toggle
        paddingLeft: cs.paddingLeft,
        paddingTop: cs.paddingTop,
        whiteSpace: cs.whiteSpace,
        // The strip itself must not spill out of the viewport
        stripOverflows: stripRect.right > document.documentElement.clientWidth + 1
      };
    }, tab);

    if (info.error) {
      record(false, `#account tab "${tab}" — ${info.error}`);
      continue;
    }

    record(info.panelVisible, `tab "${tab}" panel becomes visible after click`);
    record(info.panelInAccountView, `tab "${tab}" panel renders inside #account-view`);
    record(
      info.whiteSpace === 'nowrap',
      `tab "${tab}" keeps whitespace-nowrap after click (got "${info.whiteSpace}")`
    );
    record(
      parseFloat(info.paddingLeft) <= 12.1,
      `tab "${tab}" keeps mobile padding px-3 (got ${info.paddingLeft})`
    );
    record(!info.stripOverflows, `tab "${tab}" tab strip stays within viewport`);
  }

  await context.close();
}

async function run() {
  console.log('============================================================');
  console.log('📐 RESPONSIVE LAYOUT AUDIT FOR OM CLI');
  console.log(`🌐 Target: ${BASE_URL}`);
  console.log('============================================================\n');

  const browser = await chromium.launch({
    channel: 'msedge',
    headless: true,
    args: ['--no-sandbox', '--disable-setuid-sandbox']
  });

  try {
    for (const route of ROUTES) {
      console.log(`\n📌 ROUTE ${route.hash}`);

      for (const vp of VIEWPORTS) {
        const context = await browser.newContext({
          viewport: { width: vp.width, height: vp.height },
          deviceScaleFactor: 1,
          isMobile: vp.width < 768,
          hasTouch: vp.width < 768
        });
        await blockExternalApis(context);
        const page = await context.newPage();

        const pageErrors = [];
        page.on('pageerror', err => pageErrors.push(err.message));

        await page.goto(`${BASE_URL}/${route.hash}`, { waitUntil: 'domcontentloaded' });
        await page.waitForTimeout(700); // let the SPA render its rails

        const result = await measureOverflow(page);

        record(
          result.overflow <= 1,
          `${vp.name} — no horizontal overflow` +
            (result.overflow > 1
              ? ` (overflows by ${result.overflow}px, scrollWidth ${result.scrollWidth} > ${result.viewportWidth})` +
                result.offenders.map(o =>
                  `\n           ↳ <${o.tag}${o.id ? ` id="${o.id}"` : ''}> right=${o.right}px width=${o.width}px class="${o.cls}"`
                ).join('')
              : '')
        );

        record(
          pageErrors.length === 0,
          `${vp.name} — no uncaught JS errors` +
            (pageErrors.length ? ` (${pageErrors.slice(0, 2).join(' | ')})` : '')
        );

        await context.close();
      }
    }

    await runFontBlockedPass(browser);
    await runAccountTabPass(browser);
  } finally {
    await browser.close();
  }

  console.log('\n============================================================');
  console.log(`📊 RESPONSIVE AUDIT SUMMARY: ${passed} PASSED, ${failed} FAILED`);
  console.log('============================================================\n');

  if (failed > 0) process.exit(1);
}

run().catch(err => {
  console.error('Fatal responsive audit error:', err);
  process.exit(1);
});