/**
 * Static Codebase Audit for Om CLI frontend.
 *
 * Catches bugs that only show up at runtime, without needing a browser:
 *  1. Duplicate element ids in index.html
 *  2. getElementById() calls whose id does not exist in index.html
 *  3. Responsive Tailwind variants that get destroyed by JS className rewrites
 *  4. Render-blocking duplicate font loads (HTML <link> + CSS @import)
 *  5. Sitemap entries that are fragment-only (Google ignores these as pages)
 *
 * Dependency-free: runs with plain `node`.
 */

const fs = require('fs');
const path = require('path');

const root = path.join(__dirname, '..');
const read = p => fs.readFileSync(path.join(root, p), 'utf8');

const html = read('public/index.html');
const css = read('public/css/app.css');
const jsFiles = ['public/js/app.js', 'public/js/player.js', 'public/js/anilist.js'];
const js = jsFiles.map(f => ({ file: f, src: read(f) }));

let passed = 0;
let failed = 0;

function assert(condition, message) {
  if (condition) {
    console.log(`  ✅ PASS: ${message}`);
    passed++;
  } else {
    console.error(`  ❌ FAIL: ${message}`);
    failed++;
  }
}

console.log('============================================================');
console.log('🔬 STATIC CODEBASE AUDIT FOR OM CLI');
console.log('============================================================\n');

// ---------------------------------------------------------------------------
// 1. Duplicate ids
// ---------------------------------------------------------------------------
console.log('--- 1. Unique Element IDs ---');
const idMatches = [...html.matchAll(/\bid="([^"]+)"/g)].map(m => m[1]);
const counts = idMatches.reduce((acc, id) => {
  acc[id] = (acc[id] || 0) + 1;
  return acc;
}, {});
const duplicates = Object.entries(counts).filter(([, n]) => n > 1);
assert(
  duplicates.length === 0,
  `No duplicate ids${duplicates.length ? ` (found: ${duplicates.map(([id, n]) => `#${id} x${n}`).join(', ')})` : ''}`
);

// ---------------------------------------------------------------------------
// 2. getElementById references resolve
// ---------------------------------------------------------------------------
console.log('\n--- 2. DOM References Resolve ---');
const htmlIds = new Set(idMatches);

js.forEach(({ file, src }) => {
  const refs = new Set(
    [...src.matchAll(/getElementById\(\s*['"`]([^'"`]+)['"`]\s*\)/g)]
      // Template-literal ids are built at runtime from data, not static markup
      .map(m => m[1])
      .filter(id => !id.includes('${'))
  );
  const missing = [...refs].filter(id => !htmlIds.has(id));
  assert(
    missing.length === 0,
    `${file}: all ${refs.size} static getElementById() ids exist in index.html` +
      (missing.length ? ` — MISSING: ${missing.join(', ')}` : '')
  );
});

// ---------------------------------------------------------------------------
// 3. Responsive variants survive JS className rewrites
// ---------------------------------------------------------------------------
console.log('\n--- 3. JS Preserves Responsive Variants ---');

const RESPONSIVE = /^(sm|md|lg|xl|2xl):/;

// Class tokens that exist only in JS-rendered templates (innerHTML). Elements
// such as ".range-tab" are never in the static markup, so a rewrite targeting
// them cannot be matched against a markup element and must be skipped.
const runtimeClasses = new Set();
js.forEach(({ src }) => {
  [...src.matchAll(/class=\\?["']([^"']*)["']/g)].forEach(m => {
    m[1].split(/\s+/).filter(Boolean).forEach(t => {
      if (!t.includes('$') && !t.includes(':')) runtimeClasses.add(t);
    });
  });
});

// Collect every class list declared in the markup, so a rewrite can be matched
// back to the element it actually targets (by class-set overlap). Anchoring on
// single utility tokens does not work: "py-2" or "rounded-lg" appear on dozens
// of unrelated elements.
const markupClassSets = [...html.matchAll(/class="([^"]+)"/g)]
  .map(m => new Set(m[1].split(/\s+/).filter(Boolean)));

// Does any static markup element carry this class?
const markupHasClass = token => markupClassSets.some(set => set.has(token));

function bestMarkupMatch(written) {
  let best = null;
  let bestScore = 0;
  let bestIntersection = 0;

  for (const set of markupClassSets) {
    let intersection = 0;
    written.forEach(t => { if (set.has(t)) intersection++; });
    // Score by coverage of the rewrite (intersection / |written|), NOT Jaccard:
    // the whole point is that the rewrite is MISSING some of the markup's
    // classes, and Jaccard penalises exactly those missing tokens.
    const score = intersection / written.size;
    if (score > bestScore) {
      bestScore = score;
      bestIntersection = intersection;
      best = set;
    }
  }

  return { best, bestScore, bestIntersection };
}

const destructive = [];
const MATCH_THRESHOLD = 0.6;

js.forEach(({ file, src }) => {
  src.split('\n').forEach((line, i) => {
    // Only literal rewrites can silently drop classes; template literals are dynamic.
    const m = line.match(/\.className\s*=\s*'([^']*)'/) || line.match(/\.className\s*=\s*"([^"]*)"/);
    if (!m) return;

    const written = new Set(m[1].split(/\s+/).filter(Boolean));
    if (written.size === 0) return;

    // Skip rewrites that target a JS-rendered component (no static markup source)
    if ([...written].some(t => runtimeClasses.has(t) && !markupHasClass(t))) return;

    const { best, bestScore, bestIntersection } = bestMarkupMatch(written);
    // Require a real overlap so JS-created nodes (hero dots, range tabs) are skipped
    if (!best || bestScore < MATCH_THRESHOLD || bestIntersection < 4) return;

    const dropped = [...best].filter(t => RESPONSIVE.test(t) && !written.has(t));
    if (dropped.length > 0) {
      destructive.push({ file, line: i + 1, dropped });
    }
  });
});

assert(
  destructive.length === 0,
  `No className rewrite drops a responsive variant declared in the markup` +
    (destructive.length
      ? ` — found ${destructive.length}:\n` +
        destructive.slice(0, 8).map(d => `        ${d.file}:${d.line}  drops: ${d.dropped.join(', ')}`).join('\n')
      : '')
);

// ---------------------------------------------------------------------------
// 4. No duplicate / render-blocking font loading
// ---------------------------------------------------------------------------
console.log('\n--- 4. Font Loading Strategy ---');
const cssImportsFonts = /@import\s+url\(['"]https:\/\/fonts\.googleapis\.com/.test(css);
assert(
  !cssImportsFonts,
  'app.css does not @import Google Fonts (would double-load + block render)'
);

// ---------------------------------------------------------------------------
// 5. Sitemap: no fragment-only URLs
// ---------------------------------------------------------------------------
console.log('\n--- 5. Sitemap URL Hygiene ---');
const sitemap = read('public/sitemap.xml');
const locs = [...sitemap.matchAll(/<loc>([^<]+)<\/loc>/g)].map(m => m[1]);
const fragmentLocs = locs.filter(l => l.includes('#'));
assert(
  fragmentLocs.length === 0,
  `No fragment-only URLs in sitemap (Google treats them as the base URL)` +
    (fragmentLocs.length ? ` — found ${fragmentLocs.length}` : '')
);

console.log('\n============================================================');
console.log(`📊 CODEBASE AUDIT SUMMARY: ${passed} PASSED, ${failed} FAILED`);
console.log('============================================================\n');

if (failed > 0) process.exit(1);