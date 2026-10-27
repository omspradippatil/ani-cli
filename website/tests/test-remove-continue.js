// website/tests/test-remove-continue.js
// Verification for removing individual/all items from Continue Watching rail

const { chromium } = require('playwright');

const BASE_URL = 'https://om-cli-anime.omspradippatil.workers.dev';
const CHROME_PATH = '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome';

async function main() {
  console.log('================================================================');
  console.log('🎬 TESTING REMOVE FROM CONTINUE WATCHING FEATURE');
  console.log(`🌍 Target Base URL: ${BASE_URL}`);
  console.log('================================================================\n');

  const browser = await chromium.launch({
    executablePath: CHROME_PATH,
    headless: true,
    args: ['--no-sandbox', '--disable-setuid-sandbox']
  });

  const context = await browser.newContext({ viewport: { width: 1440, height: 900 } });
  const page = await context.newPage();
  page.setDefaultTimeout(10000);

  let passed = 0;
  let failed = 0;

  async function step(name, fn) {
    process.stdout.write(`⏳ ${name}... `);
    try {
      const res = await fn();
      console.log(`✅ PASS: ${res || 'OK'}`);
      passed++;
    } catch (err) {
      console.log(`❌ FAIL: ${err.message}`);
      failed++;
    }
  }

  try {
    let token = null;

    // 1. Authenticate with Demo Account
    await step('Authenticate with Demo Otaku account', async () => {
      await page.goto(`${BASE_URL}#home`, { waitUntil: 'domcontentloaded' });
      await page.click('#btn-sign-in-nav');
      await page.waitForSelector('#btn-auth-demo', { timeout: 5000 });
      await page.click('#btn-auth-demo');
      await page.waitForSelector('#nav-user-block:not(.hidden)', { timeout: 10000 });
      token = await page.evaluate(() => localStorage.getItem('omcli_token'));
      return `Logged in | Token length: ${token?.length}`;
    });

    // 2. Clear pre-existing history for deterministic test
    await step('Clear pre-existing history', async () => {
      await page.evaluate(async (tok) => {
        await fetch('/api/user/history', { method: 'DELETE', headers: { 'Authorization': `Bearer ${tok}` } });
      }, token);
      await page.reload({ waitUntil: 'domcontentloaded' });
      return 'History wiped';
    });

    // 3. Populate 2 anime in Continue Watching
    await step('Populate Naruto and Bleach in watch history', async () => {
      await page.evaluate(async (tok) => {
        await fetch('/api/user/history', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${tok}` },
          body: JSON.stringify({
            animeId: '20',
            animeTitle: 'Naruto',
            poster: 'https://via.placeholder.com/300',
            episodeNum: 1,
            episodeId: 'naruto-ep-1',
            progressSeconds: 400,
            durationSeconds: 1420
          })
        });
        await fetch('/api/user/history', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${tok}` },
          body: JSON.stringify({
            animeId: '269',
            animeTitle: 'Bleach',
            poster: 'https://via.placeholder.com/300',
            episodeNum: 1,
            episodeId: 'bleach-ep-1',
            progressSeconds: 700,
            durationSeconds: 1440
          })
        });
      }, token);

      await page.reload({ waitUntil: 'domcontentloaded' });
      await page.waitForSelector('#continue-grid > div', { timeout: 8000 });
      const cards = await page.$$('#continue-grid > div');
      if (cards.length !== 2) throw new Error(`Expected 2 cards, got ${cards.length}`);
      return `2 cards rendered: Bleach & Naruto`;
    });

    // 4. Verify quick remove button exists on cards
    await step('Verify .btn-remove-continue option exists on each card', async () => {
      const removeBtns = await page.$$('#continue-grid .btn-remove-continue');
      if (removeBtns.length !== 2) throw new Error(`Expected 2 remove buttons, got ${removeBtns.length}`);
      return `Found ${removeBtns.length} remove buttons`;
    });

    // 5. Click remove button on first card (Bleach)
    await step('Click remove button on first card (Bleach)', async () => {
      const firstRemoveBtn = (await page.$$('#continue-grid .btn-remove-continue'))[0];
      await firstRemoveBtn.click();

      // Wait for toast notification
      await page.waitForSelector('#toast-container > div', { timeout: 5000 });
      const toastText = await page.$eval('#toast-container', el => el.innerText.trim());

      // Wait for rail to update to 1 card
      await page.waitForFunction(() => document.querySelectorAll('#continue-grid > div').length === 1, { timeout: 8000 });

      // Ensure page did NOT navigate to #watch (stopPropagation verified)
      const currentUrl = page.url();
      if (currentUrl.includes('#watch')) throw new Error(`Incorrectly navigated to watch view: ${currentUrl}`);

      const remainingTitle = await page.$eval('#continue-grid > div h3', el => el.innerText.trim());
      if (!remainingTitle.includes('Naruto')) throw new Error(`Expected Naruto remaining, got ${remainingTitle}`);

      return `Removed Bleach cleanly! Toast: "${toastText}" | Remaining: "${remainingTitle}"`;
    });

    // 6. Test Header "Clear All" button
    await step('Click "Clear All" in rail header and verify rail hides', async () => {
      page.once('dialog', async dialog => {
        await dialog.accept();
      });

      await page.click('#btn-clear-continue-watching');

      // Rail section should immediately hide
      await page.waitForFunction(() => document.getElementById('continue-watching-section')?.classList.contains('hidden'), { timeout: 8000 });
      const isHidden = await page.$eval('#continue-watching-section', el => el.classList.contains('hidden'));
      if (!isHidden) throw new Error('Section did not get .hidden class');
      return 'Continue Watching rail cleanly cleared and hidden';
    });

  } finally {
    await browser.close();
  }

  console.log('\n================================================================');
  console.log(`🏁 REMOVE FROM CONTINUE WATCHING TESTS COMPLETE`);
  console.log(`✅ PASSED: ${passed}`);
  console.log(`❌ FAILED: ${failed}`);
  console.log('================================================================\n');

  if (failed > 0) process.exit(1);
}

main().catch(err => {
  console.error('Test error:', err);
  process.exit(1);
});
