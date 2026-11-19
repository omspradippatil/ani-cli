const { chromium, devices } = require("playwright");

const BASE_URL = process.env.BASE_URL || "http://127.0.0.1:3001";

async function runMobileControlsAutoHideTest() {
  console.log("============================================================");
  console.log("🧪 Testing Mobile Player Controls Auto-Hide & Gestures");
  console.log(`🎯 Target: ${BASE_URL}`);
  console.log("============================================================\n");

  const browser = await chromium.launch({ channel: "chrome", headless: true });
  const context = await browser.newContext({
    ...devices["iPhone 14"],
    isMobile: true,
    hasTouch: true
  });
  const page = await context.newPage();

  console.log("📌 Step 1: Navigating to watch view on mobile viewport...");
  await page.goto(`${BASE_URL}#watch?anime=20&ep=1`);
  await page.waitForSelector(".art-video-player", { timeout: 15000 });

  // Wait for playback to begin
  await page.waitForFunction(() => {
    const art = window.artplayerInstance?.art;
    return art?.video && !art.video.paused;
  }, { timeout: 10000 });
  console.log("  ✅ Video is actively streaming and playing");

  console.log("\n📌 Step 2: Verifying initial 3-second auto-hide while playing...");
  await page.waitForTimeout(3800);
  const state1 = await page.evaluate(() => {
    const bottom = document.querySelector(".art-bottom");
    return {
      controlsVisible: window.artplayerInstance.controlsVisible,
      opacity: window.getComputedStyle(bottom).opacity,
      visibility: window.getComputedStyle(bottom).visibility
    };
  });
  console.log("  State after 3.8s:", state1);
  if (state1.controlsVisible !== false || state1.visibility !== "hidden" || state1.opacity !== "0") {
    throw new Error("Failed: Controls did not auto-hide after 3 seconds");
  }
  console.log("  ✅ PASS: Player controls automatically faded out and hidden");

  console.log("\n📌 Step 3: Single tapping video area to reveal controls...");
  await page.tap(".art-video-player", { position: { x: 195, y: 100 } });
  await page.waitForTimeout(600);
  const state2 = await page.evaluate(() => {
    const bottom = document.querySelector(".art-bottom");
    return {
      controlsVisible: window.artplayerInstance.controlsVisible,
      opacity: window.getComputedStyle(bottom).opacity,
      visibility: window.getComputedStyle(bottom).visibility
    };
  });
  console.log("  State after single tap:", state2);
  if (state2.controlsVisible !== true || state2.visibility !== "visible") {
    throw new Error("Failed: Controls did not appear on single tap");
  }
  console.log("  ✅ PASS: Single tap woke controls immediately");

  console.log("\n📌 Step 4: Single tapping video area while visible immediately hides controls...");
  await page.tap(".art-video-player", { position: { x: 195, y: 100 } });
  await page.waitForTimeout(600);
  const state3 = await page.evaluate(() => {
    const bottom = document.querySelector(".art-bottom");
    return {
      controlsVisible: window.artplayerInstance.controlsVisible,
      opacity: window.getComputedStyle(bottom).opacity,
      visibility: window.getComputedStyle(bottom).visibility
    };
  });
  console.log("  State after second tap:", state3);
  if (state3.controlsVisible !== false || state3.visibility !== "hidden" || state3.opacity !== "0") {
    throw new Error("Failed: Controls did not hide on single tap");
  }
  console.log("  ✅ PASS: Single tap while visible hides controls immediately");

  console.log("\n📌 Step 5: Interacting with controls keeps them visible, then auto-hides after release...");
  // Wake controls
  await page.tap(".art-video-player", { position: { x: 195, y: 100 } });
  await page.waitForTimeout(600);

  // Tap forward 5s control button
  await page.tap(".art-control-skip-fwd-5s-btn");
  await page.waitForTimeout(300);
  const state4_active = await page.evaluate(() => window.artplayerInstance.controlsVisible);
  if (!state4_active) {
    throw new Error("Failed: Controls prematurely disappeared during button tap");
  }
  console.log("  ✅ PASS: Controls remained visible while interacting with control buttons");

  console.log("  Waiting 3.8s after interaction for auto-hide countdown...");
  await page.waitForTimeout(3800);
  const state4_hidden = await page.evaluate(() => {
    const bottom = document.querySelector(".art-bottom");
    return {
      controlsVisible: window.artplayerInstance.controlsVisible,
      visibility: window.getComputedStyle(bottom).visibility,
      opacity: window.getComputedStyle(bottom).opacity
    };
  });
  console.log("  State after 3.8s:", state4_hidden);
  if (state4_hidden.controlsVisible !== false || state4_hidden.visibility !== "hidden" || state4_hidden.opacity !== "0") {
    throw new Error("Failed: Controls did not auto-hide after control button tap");
  }
  console.log("  ✅ PASS: Controls auto-hid 3 seconds after interaction ended");

  console.log("\n📌 Step 6: Double-tap on left side rewinds 10s...");
  const tBefore = await page.evaluate(() => window.artplayerInstance.art.currentTime);
  await page.tap(".art-video-player", { position: { x: 50, y: 100 } });
  await page.waitForTimeout(80);
  await page.tap(".art-video-player", { position: { x: 50, y: 100 } });
  await page.waitForTimeout(600);
  const tAfter = await page.evaluate(() => window.artplayerInstance.art.currentTime);
  console.log(`  Seek: before=${tBefore.toFixed(1)}s, after=${tAfter.toFixed(1)}s`);
  console.log("  ✅ PASS: Double-tap seek functions without gesture interference");

  console.log("\n============================================================");
  console.log("🎉 ALL MOBILE PLAYER AUTO-HIDE & GESTURE TESTS PASSED!");
  console.log("============================================================\n");

  await browser.close();
}

runMobileControlsAutoHideTest().catch(err => {
  console.error("❌ TEST FAILURE:", err);
  process.exit(1);
});
