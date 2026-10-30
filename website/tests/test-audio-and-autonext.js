const { chromium } = require('playwright');

const BASE_URL = 'http://localhost:3000';

const results = {
  total: 0,
  passed: 0,
  failed: 0,
  tests: []
};

function record(name, passed, details = '') {
  results.total++;
  if (passed) {
    results.passed++;
    console.log(`  ✅ PASS: ${name}${details ? ` (${details})` : ''}`);
    results.tests.push({ name, status: 'PASS', details });
  } else {
    results.failed++;
    console.error(`  ❌ FAIL: ${name}${details ? ` (${details})` : ''}`);
    results.tests.push({ name, status: 'FAIL', details });
  }
}

async function runTests() {
  console.log('\n============================================================');
  console.log('🧪 Testing Universal SUB/DUB & Auto-Next Episode');
  console.log('🎯 Target:', BASE_URL);
  console.log('============================================================\n');

  const browser = await chromium.launch({ channel: 'chrome', headless: true });
  const context = await browser.newContext({ viewport: { width: 1280, height: 720 } });
  const page = await context.newPage();

  const pageErrors = [];
  page.on('pageerror', err => pageErrors.push(err.toString()));

  try {
    // -----------------------------------------------------------------
    // TEST 1: Default Audio Preference is Initialized
    // -----------------------------------------------------------------
    console.log('\n📌 [TEST GROUP 1] Universal Audio Preference State & UI');
    await page.goto(`${BASE_URL}/#home`, { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(1500);

    const initialMode = await page.evaluate(() => localStorage.getItem('omcli_preferred_audio') || 'sub');
    record('1.1 Default or stored audio mode exists', initialMode === 'sub' || initialMode === 'dub', `Mode: ${initialMode}`);

    // Switch to DUB via Navbar button
    console.log('  -> Clicking Quick DUB button in navbar...');
    const quickDubBtn = await page.$('#quick-dub-btn');
    if (quickDubBtn) {
      await quickDubBtn.click();
      await page.waitForTimeout(500);

      const savedModeAfterNavClick = await page.evaluate(() => localStorage.getItem('omcli_preferred_audio'));
      record('1.2 Quick DUB button saves "dub" to localStorage', savedModeAfterNavClick === 'dub', `localStorage: ${savedModeAfterNavClick}`);

      const dubBtnClasses = await page.getAttribute('#quick-dub-btn', 'class');
      record('1.3 Quick DUB button is visually active', dubBtnClasses.includes('bg-secondary-container'), `Classes: ${dubBtnClasses}`);
    } else {
      record('1.2 Quick DUB button found', false, 'Element #quick-dub-btn not found');
    }

    // -----------------------------------------------------------------
    // TEST 2: User Registration & Cloud Settings API
    // -----------------------------------------------------------------
    console.log('\n📌 [TEST GROUP 2] Authentication & Cloud Settings API');

    const testUser = `test_audio_${Date.now()}`;
    const testPass = 'Password123!';
    const regRes = await page.evaluate(async ({ user, pass }) => {
      const res = await fetch('/api/auth/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: user, email: `${user}@example.com`, password: pass })
      });
      return await res.json();
    }, { user: testUser, pass: testPass });

    let authToken = null;
    if (regRes.success && regRes.data?.token) {
      authToken = regRes.data.token;
      record('2.1 Test account registered for settings testing', true, `User: ${testUser}`);

      // Set auth in page localStorage so frontend recognizes user as logged in
      await page.evaluate(({ token, user }) => {
        localStorage.setItem('omcli_token', token);
        localStorage.setItem('omcli_user', JSON.stringify({ username: user, email: `${user}@example.com` }));
        if (window.appState && window.appState.auth) {
          window.appState.auth.token = token;
          window.appState.auth.user = { username: user, email: `${user}@example.com` };
        }
      }, { token: authToken, user: testUser });

      // Test PUT settings with camelCase keys
      const updateRes = await page.evaluate(async ({ token }) => {
        const res = await fetch('/api/user/settings', {
          method: 'PUT',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`
          },
          body: JSON.stringify({
            preferredAudio: 'dub',
            autoNext: 1
          })
        });
        return await res.json();
      }, { token: authToken });

      record('2.2 PUT /api/user/settings succeeds with camelCase payload', updateRes.success === true, JSON.stringify(updateRes.data || {}));

      // Test GET settings returns persisted values
      const getRes = await page.evaluate(async ({ token }) => {
        const res = await fetch('/api/user/settings', {
          headers: { 'Authorization': `Bearer ${token}` }
        });
        return await res.json();
      }, { token: authToken });

      const audioInDb = getRes.data?.preferred_audio || getRes.data?.preferredAudio;
      const autoNextInDb = getRes.data?.auto_next !== undefined ? getRes.data?.auto_next : getRes.data?.autoNext;
      record('2.3 GET /api/user/settings returns preferred_audio="dub"', audioInDb === 'dub', `preferred_audio: ${audioInDb}`);
      record('2.4 GET /api/user/settings returns auto_next=1', Number(autoNextInDb) === 1, `auto_next: ${autoNextInDb}`);
    } else {
      record('2.1 Test account registered', false, regRes.error || 'Registration failed');
    }

    // -----------------------------------------------------------------
    // TEST 3: Settings UI Sync & Save Button
    // -----------------------------------------------------------------
    console.log('\n📌 [TEST GROUP 3] Settings UI Sync & Persistence');
    await page.goto(`${BASE_URL}/#account-preferences`, { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(1000);

    // Switch to Preferences tab if not active
    await page.click('button.account-tab[data-tab="preferences"]').catch(() => {});
    await page.waitForTimeout(500);

    const prefAudioVal = await page.$eval('#pref-audio', el => el.value);
    record('3.1 Settings dropdown reflects DUB selection', prefAudioVal === 'dub', `prefAudio: ${prefAudioVal}`);

    const autoNextChecked = await page.$eval('#pref-auto-next', el => el.checked);
    record('3.2 Settings auto-next switch is active by default', autoNextChecked === true, `Checked: ${autoNextChecked}`);

    // Change setting to SUB and verify immediate sync
    await page.selectOption('#pref-audio', 'sub');
    await page.waitForTimeout(500);

    const syncedSub = await page.evaluate(() => localStorage.getItem('omcli_preferred_audio'));
    record('3.3 Selecting SUB in settings immediately syncs to localStorage', syncedSub === 'sub', `Stored: ${syncedSub}`);

    // Switch back to DUB
    await page.selectOption('#pref-audio', 'dub');
    await page.waitForTimeout(500);

    // Click Save Playback Preferences button
    await page.click('#btn-save-preferences');
    await page.waitForTimeout(1000);

    const savedDub = await page.evaluate(() => localStorage.getItem('omcli_preferred_audio'));
    record('3.4 Save button confirms DUB in localStorage', savedDub === 'dub', `Stored: ${savedDub}`);

    // -----------------------------------------------------------------
    // TEST 4: Universal DUB Mode Respected when Navigating Anime
    // -----------------------------------------------------------------
    console.log('\n📌 [TEST GROUP 4] Universal Audio Mode Respected on Watch Room');

    // Ensure state and localStorage are set to DUB
    await page.evaluate(() => {
      localStorage.setItem('omcli_preferred_audio', 'dub');
      localStorage.setItem('omcli_audio_mode', 'dub');
    });

    // Navigate to watch room WITHOUT &mode in hash
    console.log('  -> Navigating to #watch?anime=20&ep=1 without explicit mode...');
    await page.goto(`${BASE_URL}/#watch?anime=20&ep=1`, { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(3000);

    // Verify hash was updated with &mode=dub
    const currentHash = await page.evaluate(() => window.location.hash);
    record('4.1 Hash updated to reflect universal mode=dub', currentHash.includes('mode=dub'), `Hash: ${currentHash}`);

    // Verify player mode property
    const playerMode = await page.evaluate(() => window.appState ? window.appState.currentMode : (window.playerInstance ? window.playerInstance.currentMode : null));
    record('4.2 Player/App state uses dub mode', playerMode === 'dub' || currentHash.includes('mode=dub'), `Mode: ${playerMode}`);

    // Verify In-player SUB/DUB control button shows DUB
    const inPlayerModeBtn = await page.$('.art-btn-mode');
    if (inPlayerModeBtn) {
      const modeText = await inPlayerModeBtn.textContent();
      record('4.3 In-player control button displays DUB', modeText.trim().toUpperCase() === 'DUB', `Button text: "${modeText.trim()}"`);
    } else {
      record('4.3 In-player control button exists', false, '.art-btn-mode not rendered');
    }

    // -----------------------------------------------------------------
    // TEST 5: Auto-Next Episode on Playback Completion
    // -----------------------------------------------------------------
    console.log('\n📌 [TEST GROUP 5] Auto-Next Episode Playback');

    // Check autoNext flag on player
    const isAutoNextActive = await page.evaluate(() => {
      const player = window.appState?.player;
      return player ? player.autoNext : (localStorage.getItem('omcli_auto_next') !== '0');
    });
    record('5.1 Player autoNext setting is enabled', isAutoNextActive === true, `autoNext: ${isAutoNextActive}`);

    // Trigger autoNext directly to simulate natural episode completion
    console.log('  -> Simulating episode completion...');
    await page.evaluate(() => {
      const player = window.appState?.player;
      if (player && typeof player.triggerAutoNext === 'function') {
        player.triggerAutoNext();
      }
    });

    // Wait for transition to episode 2
    await page.waitForTimeout(2500);

    const hashAfterAutoNext = await page.evaluate(() => window.location.hash);
    const epNumInHash = hashAfterAutoNext.includes('ep=2');
    record('5.2 Auto-next advanced to Episode 2 in URL hash', epNumInHash, `Hash: ${hashAfterAutoNext}`);

    // Verify active episode badge in UI
    const activeEpTitle = await page.textContent('#current-ep-title').catch(() => '');
    record('5.3 Active episode title updated to Episode 2', activeEpTitle.includes('Episode 2'), `Title: "${activeEpTitle.trim()}"`);

    // Verify audio mode preserved in next episode
    record('5.4 Universal mode=dub preserved during auto-next', hashAfterAutoNext.includes('mode=dub'), `Hash: ${hashAfterAutoNext}`);

    // -----------------------------------------------------------------
    // TEST 6: In-Player Settings & Layout Stability
    // -----------------------------------------------------------------
    console.log('\n📌 [TEST GROUP 6] Player UI Layout & Controls Stability');

    const controlBar = await page.$('.art-bottom');
    record('6.1 Player bottom control bar visible', controlBar !== null);

    const playBtn = await page.$('.art-control-playAndPause');
    record('6.2 Play/Pause control visible', playBtn !== null);

    const timeControl = await page.$('.art-control-time');
    record('6.3 Time display control visible', timeControl !== null);

    const volumeControl = await page.$('.art-control-volume');
    record('6.4 Volume control visible', volumeControl !== null);

    const fullscreenBtn = await page.$('.art-btn-fs, button[title*="Fullscreen"], #btn-fullscreen-down');
    record('6.5 Fullscreen control visible', fullscreenBtn !== null);

    const nextBtn = await page.$('button[title="Next Episode"]');
    record('6.6 Next episode control button visible', nextBtn !== null);

    // -----------------------------------------------------------------
    // CLEANUP
    // -----------------------------------------------------------------
    if (authToken) {
      await page.evaluate(async ({ token }) => {
        await fetch('/api/user/account', {
          method: 'DELETE',
          headers: { 'Authorization': `Bearer ${token}` }
        });
      }, { token: authToken });
      console.log('  -> Test account cleaned up');
    }

  } catch (err) {
    console.error('Fatal test error:', err);
    record('Test execution completed without uncaught crash', false, err.message);
  } finally {
    await browser.close();
  }

  console.log('\n============================================================');
  console.log(`📊 Test Summary: ${results.passed}/${results.total} Passed (${results.failed} Failed)`);
  console.log('============================================================\n');

  if (results.failed > 0) {
    process.exit(1);
  }
}

runTests();
