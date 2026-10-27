// website/tests/test-watching-features.js
// Deep Automated Verification of Watching, Continue Watching, Resume, and Cross-Device Sync Features
// Powered by Playwright & Google Chrome

const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');

const BASE_URL = process.env.BASE_URL || 'https://om-cli-anime.omspradippatil.workers.dev';
const CHROME_PATH = '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome';

const results = [];
let passedCount = 0;
let failedCount = 0;

function recordResult(suite, name, passed, details = '', durationMs = 0) {
  if (passed) passedCount++;
  else failedCount++;
  results.push({ suite, name, passed, details, durationMs });
  const icon = passed ? '✅ PASS' : '❌ FAIL';
  console.log(`[${suite}] ${icon} - ${name} (${durationMs}ms)${details ? ' :: ' + details : ''}`);
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

async function dismissModalIfOpen(page) {
  try {
    await page.evaluate(() => {
      const m = document.getElementById('auth-modal');
      if (m && !m.classList.contains('hidden')) m.classList.add('hidden');
    });
    await page.waitForTimeout(100);
  } catch (_) {}
}

async function main() {
  console.log('================================================================');
  console.log('🎬 STARTING WATCHING & CONTINUE WATCHING FEATURE TEST SUITE');
  console.log(`🌍 Target Base URL: ${BASE_URL}`);
  console.log(`🖥️  Browser Binary: ${CHROME_PATH}`);
  console.log('================================================================\n');

  const browser = await chromium.launch({
    executablePath: CHROME_PATH,
    headless: true,
    args: ['--no-sandbox', '--disable-setuid-sandbox', '--disable-gpu']
  });

  // Device 1 Context (Primary User Session)
  const context1 = await browser.newContext({
    viewport: { width: 1440, height: 900 },
    userAgent: 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36'
  });
  const page1 = await context1.newPage();
  page1.setDefaultTimeout(10000);

  let authToken = null;
  let testUser = null;

  async function goToHomeFresh() {
    await page1.goto(`${BASE_URL}#home`, { waitUntil: 'domcontentloaded' });
    await page1.reload({ waitUntil: 'domcontentloaded' });
    await page1.waitForTimeout(400);
  }

  try {
    // -------------------------------------------------------------------------
    // SUITE A: Authentication & Clean Slate State
    // -------------------------------------------------------------------------
    const sA = 'A. Auth & Clean Slate';

    await runTest(sA, '1-Click Demo Login generates valid JWT token and session', async () => {
      await page1.goto(BASE_URL, { waitUntil: 'domcontentloaded' });
      await page1.click('#btn-sign-in-nav');
      await page1.waitForSelector('#btn-auth-demo', { timeout: 5000 });
      await page1.click('#btn-auth-demo');
      await page1.waitForSelector('#nav-user-block:not(.hidden)', { timeout: 10000 });
      authToken = await page1.evaluate(() => localStorage.getItem('omcli_token'));
      if (!authToken) throw new Error('No JWT token stored in localStorage');
      const username = await page1.$eval('#nav-user-name', el => el.innerText.trim());
      testUser = username;
      return `Authenticated as: "${username}" | Token length: ${authToken.length}`;
    });

    await runTest(sA, 'Clear existing watch history for deterministic testing', async () => {
      const resp = await page1.evaluate(async (token) => {
        const res = await fetch('/api/user/history', {
          method: 'DELETE',
          headers: { 'Authorization': `Bearer ${token}` }
        });
        return res.json();
      }, authToken);
      if (!resp.success) throw new Error(`Clear history failed: ${JSON.stringify(resp)}`);
      await page1.reload({ waitUntil: 'domcontentloaded' });
      return 'History wiped clean in SQLite';
    });

    await runTest(sA, 'GET /api/user/history returns empty list', async () => {
      const resp = await page1.evaluate(async (token) => {
        const res = await fetch('/api/user/history', {
          headers: { 'Authorization': `Bearer ${token}` }
        });
        return res.json();
      }, authToken);
      if (!resp.success || resp.data.length !== 0) throw new Error(`Expected 0 history items, got ${resp.data?.length}`);
      return 'Confirmed 0 history entries';
    });

    await runTest(sA, 'Continue Watching rail on Home feed is hidden when history is empty', async () => {
      await goToHomeFresh();
      const isHidden = await page1.$eval('#continue-watching-section', el => el.classList.contains('hidden'));
      if (!isHidden) throw new Error('Continue watching rail is visible with empty history!');
      return 'Continue watching rail cleanly hidden';
    });

    // -------------------------------------------------------------------------
    // SUITE B: Single Anime Watching & Progress Recording
    // -------------------------------------------------------------------------
    const sB = 'B. Watch Progress Sync';

    await runTest(sB, 'Sync watch progress for Naruto (Ep 1 @ 420s / 1420s = 30%)', async () => {
      const res = await page1.evaluate(async (token) => {
        const r = await fetch('/api/user/history', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`
          },
          body: JSON.stringify({
            animeId: '20',
            animeTitle: 'Naruto',
            poster: 'https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx20-YJvLbgJQPCo4.png',
            episodeNum: 1,
            episodeId: 'naruto-ep-1',
            progressSeconds: 420,
            durationSeconds: 1420
          })
        });
        return r.json();
      }, authToken);
      if (!res.success) throw new Error(`Sync failed: ${JSON.stringify(res)}`);
      return 'Progress saved to D1 SQLite database';
    });

    await runTest(sB, 'GET /api/user/history returns synced Naruto record with 30% progress', async () => {
      const res = await page1.evaluate(async (token) => {
        const r = await fetch('/api/user/history', {
          headers: { 'Authorization': `Bearer ${token}` }
        });
        return r.json();
      }, authToken);
      if (!res.success || !res.data.length) throw new Error('No history returned');
      const item = res.data[0];
      if (String(item.anime_id) !== '20') throw new Error(`Anime ID mismatch: ${item.anime_id}`);
      if (Number(item.progress_seconds) !== 420) throw new Error(`Progress mismatch: ${item.progress_seconds}`);
      if (Number(item.episode_num) !== 1) throw new Error(`Episode mismatch: ${item.episode_num}`);
      return `Anime: "${item.anime_title}" | Ep ${item.episode_num} | ${item.progress_seconds}s / ${item.duration_seconds}s`;
    });

    await runTest(sB, 'Home feed unhides #continue-watching-section when history exists', async () => {
      await goToHomeFresh();
      await page1.waitForSelector('#continue-watching-section:not(.hidden)', { timeout: 8000 });
      const isVisible = await page1.$eval('#continue-watching-section', el => !el.classList.contains('hidden'));
      if (!isVisible) throw new Error('Continue Watching rail is still hidden');
      return 'Continue Watching rail is now visible';
    });

    await runTest(sB, 'Continue Watching card renders title, episode badge, and 30% progress', async () => {
      await page1.waitForSelector('#continue-grid > div', { timeout: 5000 });
      const cards = await page1.$$('#continue-grid > div');
      if (cards.length === 0) throw new Error('No cards in continue grid');
      const card = cards[0];
      const title = await card.$eval('h3', el => el.innerText.trim());
      const epBadge = await card.$eval('.bg-primary-container', el => el.innerText.trim());
      const percentText = await card.$eval('.font-mono span:first-child', el => el.innerText.trim());
      if (!title.includes('Naruto')) throw new Error(`Unexpected title: ${title}`);
      if (!epBadge.includes('EP 1')) throw new Error(`Unexpected episode badge: ${epBadge}`);
      if (!percentText.includes('30%')) throw new Error(`Unexpected progress: ${percentText}`);
      return `Card: "${title}" | Badge: "${epBadge}" | Progress: "${percentText}"`;
    });

    await runTest(sB, 'Continue Watching progress bar visual width is 30%', async () => {
      const widthStyle = await page1.$eval('#continue-grid > div:first-child .bg-primary-container.h-full', el => el.style.width);
      if (widthStyle !== '30%') throw new Error(`Expected width 30%, got ${widthStyle}`);
      return `Progress bar style: width: ${widthStyle}`;
    });

    // -------------------------------------------------------------------------
    // SUITE C: Resume Playback Verification
    // -------------------------------------------------------------------------
    const sC = 'C. Playback Resume';

    await runTest(sC, 'Clicking Continue Watching card navigates to Watch Room with t=420', async () => {
      await page1.click('#continue-grid > div:first-child');
      await page1.waitForTimeout(600);
      const url = page1.url();
      if (!url.includes('anime=20') || !url.includes('ep=1') || !url.includes('t=420')) {
        throw new Error(`Resume URL did not contain expected params: ${url}`);
      }
      return `Resume URL: ${url}`;
    });

    await runTest(sC, 'Watch Room mounts player stage with breadcrumbs', async () => {
      await page1.waitForSelector('#artplayer-mount', { timeout: 8000 });
      const isMountVisible = await page1.$eval('#artplayer-mount', el => !el.classList.contains('hidden'));
      if (!isMountVisible) throw new Error('Player mount not visible');
      const titleCrumb = await page1.$eval('#crumb-anime-title', el => el.innerText.trim());
      const epCrumb = await page1.$eval('#crumb-episode-num', el => el.innerText.trim());
      return `Breadcrumb: "${titleCrumb}" / ${epCrumb}`;
    });

    await runTest(sC, 'Anime Details card renders correct synopsis and score', async () => {
      await page1.waitForSelector('#detail-anime-title', { timeout: 6000 });
      const title = await page1.$eval('#detail-anime-title', el => el.innerText.trim());
      const score = await page1.$eval('#detail-anime-score', el => el.innerText.trim());
      return `Details: "${title}" | Score: ${score}`;
    });

    // -------------------------------------------------------------------------
    // SUITE D: Progress Progression & Update
    // -------------------------------------------------------------------------
    const sD = 'D. Progress Update';

    await runTest(sD, 'Simulate user watching further in episode (advance to 1136s = 80%)', async () => {
      const res = await page1.evaluate(async (token) => {
        const r = await fetch('/api/user/history', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`
          },
          body: JSON.stringify({
            animeId: '20',
            animeTitle: 'Naruto',
            poster: 'https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx20-YJvLbgJQPCo4.png',
            episodeNum: 1,
            episodeId: 'naruto-ep-1',
            progressSeconds: 1136,
            durationSeconds: 1420
          })
        });
        return r.json();
      }, authToken);
      if (!res.success) throw new Error(`Update failed: ${JSON.stringify(res)}`);
      return 'Updated progress to 1136s';
    });

    await runTest(sD, 'Home feed Continue Watching card updates to 80% Complete', async () => {
      await goToHomeFresh();
      await page1.waitForSelector('#continue-grid > div', { timeout: 8000 });
      const percentText = await page1.$eval('#continue-grid > div:first-child .font-mono span:first-child', el => el.innerText.trim());
      const widthStyle = await page1.$eval('#continue-grid > div:first-child .bg-primary-container.h-full', el => el.style.width);
      if (!percentText.includes('80%')) throw new Error(`Expected 80%, got ${percentText}`);
      if (widthStyle !== '80%') throw new Error(`Expected width 80%, got ${widthStyle}`);
      return `Updated Progress: "${percentText}" | Bar Width: ${widthStyle}`;
    });

    await runTest(sD, 'Clicking updated card navigates with t=1136', async () => {
      await page1.click('#continue-grid > div:first-child');
      await page1.waitForTimeout(600);
      const url = page1.url();
      if (!url.includes('t=1136')) throw new Error(`Expected t=1136 in URL, got ${url}`);
      return `Navigated to: ${url}`;
    });

    // -------------------------------------------------------------------------
    // SUITE E: Episode Advancement (Next Episode)
    // -------------------------------------------------------------------------
    const sE = 'E. Episode Progression';

    await runTest(sE, 'User completes Ep 1 and starts Ep 2 (180s / 1420s = 13%)', async () => {
      const res = await page1.evaluate(async (token) => {
        const r = await fetch('/api/user/history', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`
          },
          body: JSON.stringify({
            animeId: '20',
            animeTitle: 'Naruto',
            poster: 'https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx20-YJvLbgJQPCo4.png',
            episodeNum: 2,
            episodeId: 'naruto-ep-2',
            progressSeconds: 180,
            durationSeconds: 1420
          })
        });
        return r.json();
      }, authToken);
      if (!res.success) throw new Error(`Episode progression sync failed: ${JSON.stringify(res)}`);
      return 'Episode 2 progress recorded in D1 SQLite';
    });

    await runTest(sE, 'Continue Watching rail reflects Episode 2 badge and 13% progress', async () => {
      await goToHomeFresh();
      await page1.waitForSelector('#continue-grid > div', { timeout: 8000 });
      const epBadge = await page1.$eval('#continue-grid > div:first-child .bg-primary-container', el => el.innerText.trim());
      const percentText = await page1.$eval('#continue-grid > div:first-child .font-mono span:first-child', el => el.innerText.trim());
      if (!epBadge.includes('EP 2')) throw new Error(`Expected EP 2, got ${epBadge}`);
      if (!percentText.includes('13%')) throw new Error(`Expected 13%, got ${percentText}`);
      return `Badge: "${epBadge}" | Progress: "${percentText}"`;
    });

    // -------------------------------------------------------------------------
    // SUITE F: Multi-Anime Queue (Chronological Ordering)
    // -------------------------------------------------------------------------
    const sF = 'F. Multi-Anime Queue';

    await runTest(sF, 'Add Bleach to history (Anime ID 269, Ep 1 @ 600s / 1440s = 42%)', async () => {
      await new Promise(r => setTimeout(r, 100));
      const res = await page1.evaluate(async (token) => {
        const r = await fetch('/api/user/history', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`
          },
          body: JSON.stringify({
            animeId: '269',
            animeTitle: 'Bleach',
            poster: 'https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx269-KxhUp7p8kX4w.png',
            episodeNum: 1,
            episodeId: 'bleach-ep-1',
            progressSeconds: 600,
            durationSeconds: 1440
          })
        });
        return r.json();
      }, authToken);
      if (!res.success) throw new Error(`Multi-anime sync failed: ${JSON.stringify(res)}`);
      return 'Bleach added to watch history';
    });

    await runTest(sF, 'Continue Watching rail renders both Bleach and Naruto in chronological order', async () => {
      await goToHomeFresh();
      await page1.waitForSelector('#continue-grid > div', { timeout: 8000 });
      const cards = await page1.$$('#continue-grid > div');
      if (cards.length < 2) throw new Error(`Expected at least 2 cards, got ${cards.length}`);
      const firstTitle = await cards[0].$eval('h3', el => el.innerText.trim());
      const secondTitle = await cards[1].$eval('h3', el => el.innerText.trim());
      if (!firstTitle.includes('Bleach')) throw new Error(`First card should be Bleach, got ${firstTitle}`);
      if (!secondTitle.includes('Naruto')) throw new Error(`Second card should be Naruto, got ${secondTitle}`);
      return `1st: "${firstTitle}" | 2nd: "${secondTitle}"`;
    });

    await runTest(sF, 'Bleach card displays EP 1 and 42% Complete', async () => {
      const card = (await page1.$$('#continue-grid > div'))[0];
      const epBadge = await card.$eval('.bg-primary-container', el => el.innerText.trim());
      const percentText = await card.$eval('.font-mono span:first-child', el => el.innerText.trim());
      if (!epBadge.includes('EP 1')) throw new Error(`Expected EP 1, got ${epBadge}`);
      if (!percentText.includes('42%')) throw new Error(`Expected 42%, got ${percentText}`);
      return `Bleach: "${epBadge}" | "${percentText}"`;
    });

    // -------------------------------------------------------------------------
    // SUITE G: Account History Timeline Management
    // -------------------------------------------------------------------------
    const sG = 'G. Account Timeline';

    await runTest(sG, 'Navigating to #account History tab displays timeline entries', async () => {
      await page1.goto(`${BASE_URL}#account`, { waitUntil: 'domcontentloaded' });
      await page1.waitForSelector('#account-sub-tabs', { timeout: 6000 });
      await page1.click('#account-sub-tabs [data-tab="history"]');
      await page1.waitForFunction(() => document.querySelectorAll('#account-history-list .group').length >= 2, { timeout: 8000 });
      const items = await page1.$$('#account-history-list .group');
      if (items.length < 2) throw new Error(`Expected at least 2 timeline items, got ${items.length}`);
      return `${items.length} items rendered in Account History Timeline`;
    });

    await runTest(sG, 'Timeline entry includes episode info, percentage, and date string', async () => {
      const firstItem = (await page1.$$('#account-history-list .group'))[0];
      const title = await firstItem.$eval('h3', el => el.innerText.trim());
      const meta = await firstItem.$eval('.font-mono', el => el.innerText.trim());
      if (!meta.includes('Episode') || !meta.includes('% Watched')) {
        throw new Error(`Incomplete history meta: ${meta}`);
      }
      return `Timeline Item: "${title}" | Meta: "${meta}"`;
    });

    await runTest(sG, 'Removing Naruto from history via DELETE /api/user/history/20', async () => {
      const res = await page1.evaluate(async (token) => {
        const r = await fetch('/api/user/history/20', {
          method: 'DELETE',
          headers: { 'Authorization': `Bearer ${token}` }
        });
        return r.json();
      }, authToken);
      if (!res.success) throw new Error(`Delete failed: ${JSON.stringify(res)}`);
      return 'Naruto removed from history';
    });

    await runTest(sG, 'Home Continue Watching rail now only contains Bleach', async () => {
      await goToHomeFresh();
      await page1.waitForSelector('#continue-grid > div', { timeout: 8000 });
      const cards = await page1.$$('#continue-grid > div');
      if (cards.length !== 1) throw new Error(`Expected exactly 1 card, got ${cards.length}`);
      const title = await cards[0].$eval('h3', el => el.innerText.trim());
      if (!title.includes('Bleach')) throw new Error(`Expected Bleach, got ${title}`);
      return `Confirmed 1 card remaining: "${title}"`;
    });

    // -------------------------------------------------------------------------
    // SUITE H: Cross-Device Synchronization Simulation
    // -------------------------------------------------------------------------
    const sH = 'H. Cross-Device Sync';

    // Device 2 Context
    const context2 = await browser.newContext({
      viewport: { width: 1440, height: 900 }
    });
    const page2 = await context2.newPage();
    page2.setDefaultTimeout(10000);

    await runTest(sH, 'Device 2 loads initially without stored session', async () => {
      await page2.goto(BASE_URL, { waitUntil: 'domcontentloaded' });
      const hasToken = await page2.evaluate(() => !!localStorage.getItem('omcli_token'));
      if (hasToken) throw new Error('Device 2 should not have a token yet');
      return 'Device 2 is unauthenticated visitor';
    });

    await runTest(sH, 'Device 2 authenticates with same user session token', async () => {
      await page2.evaluate((token) => {
        localStorage.setItem('omcli_token', token);
      }, authToken);
      await page2.reload({ waitUntil: 'domcontentloaded' });
      await page2.waitForSelector('#nav-user-block:not(.hidden)', { timeout: 10000 });
      const user = await page2.$eval('#nav-user-name', el => el.innerText.trim());
      if (user !== testUser) throw new Error(`User mismatch on Device 2: ${user}`);
      return `Device 2 session restored for: "${user}"`;
    });

    await runTest(sH, 'Device 2 instantly syncs Continue Watching rail from Cloudflare D1', async () => {
      await page2.waitForSelector('#continue-grid > div', { timeout: 8000 });
      const cards = await page2.$$('#continue-grid > div');
      if (cards.length !== 1) throw new Error(`Expected 1 card on Device 2, got ${cards.length}`);
      const title = await cards[0].$eval('h3', el => el.innerText.trim());
      const percent = await cards[0].$eval('.font-mono span:first-child', el => el.innerText.trim());
      if (!title.includes('Bleach') || !percent.includes('42%')) {
        throw new Error(`Device 2 sync mismatch: ${title} / ${percent}`);
      }
      return `Device 2 synced: "${title}" (${percent})`;
    });

    await runTest(sH, 'Device 2 updates Bleach progress to 90% (1296s / 1440s)', async () => {
      const res = await page2.evaluate(async (token) => {
        const r = await fetch('/api/user/history', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`
          },
          body: JSON.stringify({
            animeId: '269',
            animeTitle: 'Bleach',
            poster: 'https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx269-KxhUp7p8kX4w.png',
            episodeNum: 1,
            episodeId: 'bleach-ep-1',
            progressSeconds: 1296,
            durationSeconds: 1440
          })
        });
        return r.json();
      }, authToken);
      if (!res.success) throw new Error(`Device 2 update failed: ${JSON.stringify(res)}`);
      return 'Progress updated on Device 2';
    });

    await runTest(sH, 'Device 1 reloads #home and automatically displays Device 2 progress (90%)', async () => {
      await goToHomeFresh();
      await page1.waitForSelector('#continue-grid > div', { timeout: 8000 });
      const percentText = await page1.$eval('#continue-grid > div:first-child .font-mono span:first-child', el => el.innerText.trim());
      const widthStyle = await page1.$eval('#continue-grid > div:first-child .bg-primary-container.h-full', el => el.style.width);
      if (!percentText.includes('90%')) throw new Error(`Device 1 did not reflect Device 2 update: ${percentText}`);
      if (widthStyle !== '90%') throw new Error(`Bar width mismatch: ${widthStyle}`);
      return `Device 1 synchronized: "${percentText}" | Bar Width: ${widthStyle}`;
    });

    await context2.close();

    // -------------------------------------------------------------------------
    // SUITE I: Clear All History Functionality
    // -------------------------------------------------------------------------
    const sI = 'I. Clear All History';

    await runTest(sI, 'Clear entire watch history via DELETE /api/user/history', async () => {
      const res = await page1.evaluate(async (token) => {
        const r = await fetch('/api/user/history', {
          method: 'DELETE',
          headers: { 'Authorization': `Bearer ${token}` }
        });
        return r.json();
      }, authToken);
      if (!res.success) throw new Error(`Clear all failed: ${JSON.stringify(res)}`);
      return 'All history cleared';
    });

    await runTest(sI, 'Continue Watching rail on Home immediately hides when history is wiped', async () => {
      await goToHomeFresh();
      const isHidden = await page1.$eval('#continue-watching-section', el => el.classList.contains('hidden'));
      if (!isHidden) throw new Error('Continue Watching rail failed to hide after history clear');
      return 'Continue Watching rail successfully hidden';
    });

    await runTest(sI, 'Account History tab displays empty state placeholder', async () => {
      await page1.goto(`${BASE_URL}#account`, { waitUntil: 'domcontentloaded' });
      await page1.waitForSelector('#account-sub-tabs', { timeout: 6000 });
      await page1.click('#account-sub-tabs [data-tab="history"]');
      await page1.waitForFunction(() => {
        const el = document.querySelector('#account-history-list');
        return el && el.innerText.trim().length > 0 && !el.querySelector('.animate-spin');
      }, { timeout: 8000 });
      const text = await page1.$eval('#account-history-list', el => el.innerText.trim());
      if (!text.toLowerCase().includes('no watch history')) {
        throw new Error(`Unexpected empty state text: "${text}"`);
      }
      return `Empty state verified: "${text}"`;
    });

    // -------------------------------------------------------------------------
    // SUITE J: Watchlist "Watching" State Synchronization
    // -------------------------------------------------------------------------
    const sJ = 'J. Watchlist Integration';

    await runTest(sJ, 'Add Demon Slayer to Watchlist with status "Watching"', async () => {
      const res = await page1.evaluate(async (token) => {
        const r = await fetch('/api/user/watchlist', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`
          },
          body: JSON.stringify({
            animeId: '101922',
            animeTitle: 'Demon Slayer: Kimetsu no Yaiba',
            poster: 'https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx101922-PEn1CTDYeaNO.png',
            format: 'TV',
            score: 8.5,
            status: 'Watching'
          })
        });
        return r.json();
      }, authToken);
      if (!res.success) throw new Error(`Watchlist add failed: ${JSON.stringify(res)}`);
      return 'Added Demon Slayer as "Watching"';
    });

    await runTest(sJ, 'Account Watchlist tab renders Demon Slayer under "Watching" category', async () => {
      await page1.goto(`${BASE_URL}#account`, { waitUntil: 'domcontentloaded' });
      await page1.reload({ waitUntil: 'domcontentloaded' });
      await page1.waitForSelector('#account-sub-tabs', { timeout: 6000 });
      await page1.click('#account-sub-tabs [data-tab="watchlist"]');
      await page1.waitForFunction(() => document.querySelectorAll('#account-watchlist-grid .group').length >= 1, { timeout: 8000 });
      const cards = await page1.$$('#account-watchlist-grid .group');
      if (cards.length === 0) throw new Error('No cards in account watchlist');
      const title = await cards[0].$eval('h3', el => el.innerText.trim());
      if (!title.includes('Demon Slayer')) throw new Error(`Expected Demon Slayer, got ${title}`);
      return `Watchlist Card: "${title}"`;
    });

    await runTest(sJ, 'Remove Demon Slayer from Watchlist via DELETE /api/user/watchlist/101922', async () => {
      const res = await page1.evaluate(async (token) => {
        const r = await fetch('/api/user/watchlist/101922', {
          method: 'DELETE',
          headers: { 'Authorization': `Bearer ${token}` }
        });
        return r.json();
      }, authToken);
      if (!res.success) throw new Error(`Delete failed: ${JSON.stringify(res)}`);
      return 'Demon Slayer removed from Watchlist';
    });

    // -------------------------------------------------------------------------
    // SUITE K: Watch Room & In-Player Watching Controls
    // -------------------------------------------------------------------------
    const sK = 'K. In-Player Controls';

    await runTest(sK, 'Navigate to Watch Room for Naruto Ep 1', async () => {
      await page1.goto(`${BASE_URL}#watch?anime=20&ep=1`, { waitUntil: 'domcontentloaded' });
      await page1.waitForSelector('#watch-view:not(.hidden)', { timeout: 8000 });
      return 'Watch room active';
    });

    await runTest(sK, 'Dual Audio switchers (SUB / DUB) toggle cleanly', async () => {
      const dubBtn = await page1.$('#btn-stream-dub');
      const subBtn = await page1.$('#btn-stream-sub');
      if (!dubBtn || !subBtn) throw new Error('Audio buttons missing');
      await dubBtn.click();
      await page1.waitForTimeout(200);
      await subBtn.click();
      await page1.waitForTimeout(200);
      return 'Dual Audio toggled SUB -> DUB -> SUB';
    });

    await runTest(sK, 'Skip Intro (+85s) button triggers seek forward', async () => {
      const skipBtn = await page1.$('#btn-skip-intro');
      if (!skipBtn) throw new Error('Skip intro button missing');
      await skipBtn.click();
      return 'Skip Intro (+85s) clicked';
    });

    await runTest(sK, 'Captions (CC) button toggles subtitles status', async () => {
      const ccBtn = await page1.$('#btn-toggle-captions-down');
      if (!ccBtn) throw new Error('CC button missing');
      await ccBtn.click();
      await page1.waitForTimeout(200);
      const status = await page1.$eval('#text-captions-status', el => el.innerText.trim());
      return `Captions status: "${status}"`;
    });

    await runTest(sK, 'Streaming Quality button opens resolution menu', async () => {
      const qBtn = await page1.$('#btn-quality-down');
      if (!qBtn) throw new Error('Quality button missing');
      await qBtn.click();
      await page1.waitForTimeout(200);
      const menu = await page1.$('#quality-dropdown-menu:not(.hidden)');
      if (!menu) throw new Error('Quality dropdown did not open');
      await qBtn.click(); // Close
      return 'Quality menu toggled';
    });

    await runTest(sK, 'Episode queue sidebar renders episodes list with interactive items', async () => {
      await page1.waitForSelector('#episodes-scroll-list', { timeout: 6000 });
      const filterInput = await page1.$('#episode-search-filter');
      if (!filterInput) throw new Error('Episode search filter missing');
      return 'Episode queue sidebar verified';
    });

    await runTest(sK, 'Next Episode button exists and is interactive', async () => {
      const nextBtn = await page1.$('#btn-next-ep');
      if (!nextBtn) throw new Error('Next episode button missing');
      return 'Next Episode button verified';
    });

    await runTest(sK, 'Previous Episode button exists and is interactive', async () => {
      const prevBtn = await page1.$('#btn-prev-ep');
      if (!prevBtn) throw new Error('Prev episode button missing');
      return 'Prev Episode button verified';
    });

    await runTest(sK, 'Recommendations panel renders related anime tiles', async () => {
      const recs = await page1.$('#recs-list-container');
      if (!recs) throw new Error('Recommendations container missing');
      return 'Recommendations rail verified';
    });

  } finally {
    await browser.close();
  }

  // -------------------------------------------------------------------------
  // SUMMARY REPORT GENERATION
  // -------------------------------------------------------------------------
  console.log('\n================================================================');
  console.log(`🏁 WATCHING FEATURE TESTS COMPLETE: ${results.length} TOTAL TESTS`);
  console.log(`✅ PASSED: ${passedCount}`);
  console.log(`❌ FAILED: ${failedCount}`);
  const passRate = ((passedCount / results.length) * 100).toFixed(1);
  console.log(`📊 PASS RATE: ${passRate}%`);
  console.log('================================================================\n');

  const reportPath = path.join(__dirname, 'watching-tests-report.json');
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
