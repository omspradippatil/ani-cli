/**
 * Shared Playwright helpers for the Om CLI test suite.
 *
 * The suites used to hardcode
 *   executablePath: '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome'
 * which meant none of them could run on Windows or Linux. Instead we ask
 * Playwright for an installed browser channel and fall back through the
 * candidates, so the same suite runs on macOS, Windows and CI.
 *
 * Override with BROWSER_CHANNEL=chrome (or msedge) when needed.
 */

const { chromium } = require('playwright');

const CHANNEL_CANDIDATES = [
  process.env.BROWSER_CHANNEL,
  'msedge',
  'chrome'
].filter(Boolean);

/**
 * Launches an installed Chromium-based browser.
 * @param {object} [options] extra Playwright launch options
 */
async function launchBrowser(options = {}) {
  const attempts = [];

  for (const channel of CHANNEL_CANDIDATES) {
    try {
      return await chromium.launch({
        channel,
        headless: true,
        args: ['--no-sandbox', '--disable-setuid-sandbox'],
        ...options
      });
    } catch (err) {
      attempts.push(`  ${channel}: ${String(err.message).split('\n')[0]}`);
    }
  }

  throw new Error(
    'No usable Chrome/Edge installation found. Tried:\n' +
      attempts.join('\n') +
      '\nInstall Chrome or Edge, or set BROWSER_CHANNEL to a Playwright channel name.'
  );
}

module.exports = { launchBrowser, CHANNEL_CANDIDATES };