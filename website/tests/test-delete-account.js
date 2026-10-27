// website/tests/test-delete-account.js
// Automated End-to-End Verification for Account Deletion Feature

const { chromium } = require('playwright');

const BASE_URL = 'https://om-cli-anime.omspradippatil.workers.dev';
const CHROME_PATH = '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome';

async function main() {
  console.log('================================================================');
  console.log('🗑️  STARTING DELETE ACCOUNT END-TO-END VERIFICATION');
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

  const testId = Date.now();
  const testUsername = `user_del_${testId}`;
  const testEmail = `del_${testId}@test.com`;
  const testPassword = 'Password123!';

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

    // 1. Register fresh test account
    await step('Register a fresh dedicated user account', async () => {
      await page.goto(`${BASE_URL}#home`, { waitUntil: 'domcontentloaded' });
      await page.click('#btn-sign-in-nav');
      await page.waitForSelector('#tab-auth-register', { timeout: 5000 });
      await page.click('#tab-auth-register');
      await page.waitForSelector('#reg-input-username', { timeout: 5000 });

      await page.fill('#reg-input-username', testUsername);
      await page.fill('#reg-input-email', testEmail);
      await page.fill('#reg-input-password', testPassword);
      await page.click('#form-auth-register button[type="submit"]');

      await page.waitForSelector('#nav-user-block:not(.hidden)', { timeout: 10000 });
      token = await page.evaluate(() => localStorage.getItem('omcli_token'));
      if (!token) throw new Error('No auth token stored after register');
      return `Created: ${testUsername} | Token length: ${token.length}`;
    });

    // 2. Add sample watch progress to D1 SQLite
    await step('Populate watch history for the user', async () => {
      const res = await page.evaluate(async (tok) => {
        const r = await fetch('/api/user/history', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${tok}` },
          body: JSON.stringify({
            animeId: '20',
            animeTitle: 'Naruto',
            poster: 'https://via.placeholder.com/300',
            episodeNum: 1,
            episodeId: 'naruto-ep-1',
            progressSeconds: 300,
            durationSeconds: 1420
          })
        });
        return r.json();
      }, token);
      if (!res.success) throw new Error(`History add failed: ${JSON.stringify(res)}`);
      return 'Watch progress recorded in D1';
    });

    // 3. Add sample watchlist entry to D1 SQLite
    await step('Populate watchlist for the user', async () => {
      const res = await page.evaluate(async (tok) => {
        const r = await fetch('/api/user/watchlist', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${tok}` },
          body: JSON.stringify({
            animeId: '269',
            animeTitle: 'Bleach',
            poster: 'https://via.placeholder.com/300',
            format: 'TV',
            score: 8.2,
            status: 'Watching'
          })
        });
        return r.json();
      }, token);
      if (!res.success) throw new Error(`Watchlist add failed: ${JSON.stringify(res)}`);
      return 'Watchlist item recorded in D1';
    });

    // 4. Verify data presence in D1
    await step('Verify D1 records active history and watchlist', async () => {
      const hist = await page.evaluate(async (tok) => {
        const r = await fetch('/api/user/history', { headers: { 'Authorization': `Bearer ${tok}` } });
        return r.json();
      }, token);
      const wl = await page.evaluate(async (tok) => {
        const r = await fetch('/api/user/watchlist', { headers: { 'Authorization': `Bearer ${tok}` } });
        return r.json();
      }, token);
      if (!hist.data?.length || !wl.data?.length) {
        throw new Error(`Data missing: history=${hist.data?.length}, watchlist=${wl.data?.length}`);
      }
      return `Confirmed ${hist.data.length} history + ${wl.data.length} watchlist item`;
    });

    // 5. Navigate to Account Settings and verify Danger Zone UI
    await step('Navigate to Account & Security and verify Danger Zone card', async () => {
      await page.goto(`${BASE_URL}#account`, { waitUntil: 'domcontentloaded' });
      await page.waitForSelector('#account-sub-tabs', { timeout: 6000 });
      await page.click('#account-sub-tabs [data-tab="settings"]');
      await page.waitForSelector('#btn-delete-account', { timeout: 6000 });

      const btnText = await page.$eval('#btn-delete-account', el => el.innerText.trim());
      if (!btnText.toLowerCase().includes('delete account')) throw new Error(`Unexpected button text: ${btnText}`);
      return `Danger Zone UI verified: "${btnText}"`;
    });

    // 6. Trigger Delete Account with confirmation dialog accept
    await step('Click Delete Account and accept confirmation prompt', async () => {
      page.once('dialog', async dialog => {
        await dialog.accept();
      });

      await page.click('#btn-delete-account');

      // Wait for token to be cleared from localStorage
      await page.waitForFunction(() => !localStorage.getItem('omcli_token'), { timeout: 8000 });
      await page.waitForTimeout(500);
      return 'Account deletion executed and session terminated';
    });

    // 7. Verify previous token is invalidated on Cloudflare edge
    await step('Verify deleted account token returns 401 Unauthorized', async () => {
      const checkRes = await page.evaluate(async (oldToken) => {
        const r = await fetch('/api/user/profile', {
          headers: { 'Authorization': `Bearer ${oldToken}` }
        });
        return { status: r.status, body: await r.json() };
      }, token);

      if (checkRes.status !== 401) {
        throw new Error(`Expected 401 Unauthorized, got ${checkRes.status}`);
      }
      return 'Session permanently invalidated (401)';
    });

    // 8. Attempt login with deleted user credentials (must fail)
    await step('Attempt login with deleted user credentials (must fail)', async () => {
      await page.reload({ waitUntil: 'domcontentloaded' });
      await page.waitForSelector('#btn-sign-in-nav', { timeout: 8000 });
      await page.click('#btn-sign-in-nav');
      await page.waitForSelector('#login-input-identity', { timeout: 5000 });

      await page.fill('#login-input-identity', testUsername);
      await page.fill('#login-input-password', testPassword);
      await page.click('#form-auth-login button[type="submit"]');

      await page.waitForSelector('#auth-alert-box:not(.hidden)', { timeout: 8000 });
      const alertMsg = await page.$eval('#auth-alert-box', el => el.innerText.trim());
      if (!alertMsg.toLowerCase().includes('invalid')) {
        throw new Error(`Expected invalid credentials alert, got: "${alertMsg}"`);
      }
      return `Login rejected: "${alertMsg}"`;
    });

    // 9. Re-verify Demo Otaku account is unaffected
    await step('Verify Demo Otaku user remains operational and intact', async () => {
      await page.click('#btn-auth-demo');
      await page.waitForSelector('#nav-user-block:not(.hidden)', { timeout: 10000 });
      const user = await page.$eval('#nav-user-name', el => el.innerText.trim());
      if (user !== 'Alex Mercer') throw new Error(`Expected Alex Mercer, got: ${user}`);
      return `Demo account healthy: "${user}"`;
    });

  } finally {
    await browser.close();
  }

  console.log('\n================================================================');
  console.log(`🏁 DELETE ACCOUNT TESTS COMPLETE`);
  console.log(`✅ PASSED: ${passed}`);
  console.log(`❌ FAILED: ${failed}`);
  console.log('================================================================\n');

  if (failed > 0) {
    process.exit(1);
  }
}

main().catch(err => {
  console.error('Test error:', err);
  process.exit(1);
});
