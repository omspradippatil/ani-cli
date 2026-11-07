/**
 * HTML Structure & Nesting Audit for Om CLI
 *
 * Guards against malformed markup that silently breaks the SPA:
 *  - unbalanced / stray closing tags
 *  - account tab panels accidentally nested OUTSIDE #account-view
 *    (which makes Watch History / Preferences / Settings render
 *     below the footer instead of inside the account view)
 *
 * Dependency-free: runs with plain `node`.
 */

const fs = require('fs');
const path = require('path');

const VOID_ELEMENTS = new Set([
  'area', 'base', 'br', 'col', 'embed', 'hr', 'img', 'input',
  'link', 'meta', 'param', 'source', 'track', 'wbr'
]);

function lineOf(source, index) {
  return source.slice(0, index).split('\n').length;
}

/**
 * Walks the tag stream and reports nesting problems.
 * Returns { strayCloses, mismatches, unclosed }
 */
function analyzeNesting(html) {
  const tagRe = /<\/?([a-zA-Z][a-zA-Z0-9-]*)((?:"[^"]*"|'[^']*'|[^>"'])*?)(\/?)>/g;
  const stack = [];
  const strayCloses = [];
  const mismatches = [];
  let match;

  while ((match = tagRe.exec(html)) !== null) {
    const name = match[1].toLowerCase();
    const isClose = match[0][1] === '/';
    const isSelfClosing = match[3] === '/';

    if (VOID_ELEMENTS.has(name) || isSelfClosing) continue;

    if (!isClose) {
      stack.push({ name, line: lineOf(html, match.index) });
      continue;
    }

    const depth = stack.map(s => s.name).lastIndexOf(name);
    if (depth === -1) {
      strayCloses.push({ name, line: lineOf(html, match.index) });
      continue;
    }
    if (depth !== stack.length - 1) {
      const inner = stack[stack.length - 1];
      mismatches.push({
        name,
        line: lineOf(html, match.index),
        innerName: inner.name,
        innerLine: inner.line
      });
    }
    stack.length = depth;
  }

  return { strayCloses, mismatches, unclosed: stack };
}

/**
 * Returns the raw inner HTML of the first element carrying the given id.
 * Brace-matches on <div> since the shell is div-based.
 */
function innerHtmlOfId(html, id) {
  const openRe = new RegExp(`<div[^>]*\\bid="${id}"[^>]*>`);
  const openMatch = openRe.exec(html);
  if (!openMatch) return null;

  const start = openMatch.index + openMatch[0].length;
  const tagRe = /<div(?:"[^"]*"|'[^']*'|[^>"'])*>|<\/div>/g;
  tagRe.lastIndex = start;

  let depth = 1;
  let match;
  while ((match = tagRe.exec(html)) !== null) {
    if (match[0] === '</div>') {
      depth--;
      if (depth === 0) return html.slice(start, match.index);
    } else {
      depth++;
    }
  }
  return null;
}

function run() {
  console.log('============================================================');
  console.log('🧱 HTML STRUCTURE & NESTING AUDIT FOR OM CLI');
  console.log('============================================================\n');

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

  const html = fs.readFileSync(path.join(__dirname, '../public/index.html'), 'utf8');
  const { strayCloses, mismatches, unclosed } = analyzeNesting(html);

  console.log('--- 1. Tag Balance ---');
  assert(
    strayCloses.length === 0,
    `No stray closing tags${strayCloses.length ? ` (found ${strayCloses.length}: ` +
      strayCloses.map(s => `</${s.name}> line ${s.line}`).join(', ') + ')' : ''}`
  );
  assert(
    mismatches.length === 0,
    `No mis-nested tags${mismatches.length ? ` (found ${mismatches.length}: ` +
      mismatches.map(m => `</${m.name}> line ${m.line} closes over <${m.innerName}> from line ${m.innerLine}`).join('; ') + ')' : ''}`
  );
  assert(
    unclosed.length === 0,
    `No unclosed tags at EOF${unclosed.length ? ` (found ${unclosed.length}: ` +
      unclosed.map(s => `<${s.name}> line ${s.line}`).join(', ') + ')' : ''}`
  );

  console.log('\n--- 2. Account View Contains All Tab Panels ---');
  const accountView = innerHtmlOfId(html, 'account-view');
  assert(accountView !== null, '#account-view element exists');

  if (accountView !== null) {
    ['tab-content-watchlist', 'tab-content-history', 'tab-content-preferences', 'tab-content-settings']
      .forEach(tabId => {
        assert(
          accountView.includes(`id="${tabId}"`),
          `#${tabId} is nested inside #account-view (not leaked to <body>)`
        );
      });
  }

  console.log('\n--- 3. Single Top-Level Landmark Wrappers ---');
  const bodyOpen = (html.match(/<body[\s>]/g) || []).length;
  const bodyClose = (html.match(/<\/body>/g) || []).length;
  assert(bodyOpen === 1 && bodyClose === 1, `Exactly one <body> open/close pair (found ${bodyOpen}/${bodyClose})`);

  console.log('\n============================================================');
  console.log(`📊 STRUCTURE AUDIT SUMMARY: ${passed} PASSED, ${failed} FAILED`);
  console.log('============================================================\n');

  if (failed > 0) process.exit(1);
}

run();