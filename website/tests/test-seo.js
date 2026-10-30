const fs = require('fs');
const path = require('path');

function runSeoAudit() {
  console.log('============================================================');
  console.log('🔍 RUNNING COMPREHENSIVE SEO AUDIT FOR OM CLI');
  console.log('🌐 Target Domain: https://ani-cli-steel.vercel.app');
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

  // 1. Audit robots.txt
  console.log('--- 1. Robots.txt Validation ---');
  const robotsPublicPath = path.join(__dirname, '../public/robots.txt');
  assert(fs.existsSync(robotsPublicPath), 'public/robots.txt exists');

  const robotsContent = fs.readFileSync(robotsPublicPath, 'utf8');
  assert(robotsContent.includes('User-agent: *'), 'robots.txt allows all user agents');
  assert(robotsContent.includes('Disallow: /api/'), 'robots.txt disallows /api/ crawler waste');
  assert(robotsContent.includes('Sitemap: https://ani-cli-steel.vercel.app/sitemap.xml'), 'robots.txt points to absolute HTTPS sitemap URL');

  // 2. Audit sitemap.xml
  console.log('\n--- 2. Sitemap.xml Validation ---');
  const sitemapPublicPath = path.join(__dirname, '../public/sitemap.xml');
  assert(fs.existsSync(sitemapPublicPath), 'public/sitemap.xml exists');

  const sitemapContent = fs.readFileSync(sitemapPublicPath, 'utf8');
  assert(sitemapContent.includes('xmlns="http://www.sitemaps.org/schemas/sitemap/0.9"'), 'Valid sitemap 0.9 schema');
  assert(sitemapContent.includes('<loc>https://ani-cli-steel.vercel.app/</loc>'), 'Contains root canonical URL');
  assert(sitemapContent.includes('<image:image>'), 'Contains Google image sitemap extensions');
  assert(sitemapContent.includes('https://ani-cli-steel.vercel.app/assets/hero_bg.jpg'), 'Contains hero background image in sitemap');
  assert(sitemapContent.includes('<loc>https://ani-cli-steel.vercel.app/#browse?genre=Action</loc>'), 'Contains crawlable genre entries');

  // 3. Audit index.html Head Metadata
  console.log('\n--- 3. index.html Head Metadata & Social Previews ---');
  const indexPath = path.join(__dirname, '../public/index.html');
  const html = fs.readFileSync(indexPath, 'utf8');

  // Title & Canonical
  assert(html.includes('<title>Om CLI — Watch Anime Free in HD | Dual-Audio HLS &amp; Android TV Sync</title>'), 'Optimized primary title tag');
  assert(html.includes('<link rel="canonical" href="https://ani-cli-steel.vercel.app/">'), 'Self-referencing canonical tag with absolute HTTPS domain');

  // Meta Description & Keywords
  assert(html.includes('name="description"'), 'Meta description tag present');
  assert(html.includes('name="keywords"'), 'Meta keywords tag present');
  assert(html.includes('name="robots" content="index, follow'), 'Robots index, follow directive present');
  assert(html.includes('name="googlebot"'), 'Googlebot specific directives present');

  // Open Graph
  assert(html.includes('property="og:type" content="website"'), 'og:type website present');
  assert(html.includes('property="og:url" content="https://ani-cli-steel.vercel.app/"'), 'og:url present');
  assert(html.includes('property="og:title"'), 'og:title present');
  assert(html.includes('property="og:description"'), 'og:description present');
  assert(html.includes('property="og:image" content="https://ani-cli-steel.vercel.app/assets/hero_bg.jpg"'), 'og:image present with absolute URL');
  assert(html.includes('property="og:image:width" content="1376"'), 'og:image:width specified');
  assert(html.includes('property="og:image:height" content="768"'), 'og:image:height specified');

  // Twitter Cards
  assert(html.includes('name="twitter:card" content="summary_large_image"'), 'Twitter summary_large_image card present');
  assert(html.includes('name="twitter:title"'), 'twitter:title present');
  assert(html.includes('name="twitter:description"'), 'twitter:description present');
  assert(html.includes('name="twitter:image" content="https://ani-cli-steel.vercel.app/assets/hero_bg.jpg"'), 'twitter:image present');

  // 4. Structured Data (JSON-LD) Validation
  console.log('\n--- 4. Schema.org JSON-LD Structured Data Validation ---');
  const ldJsonRegex = /<script type="application\/ld\+json">([\s\S]*?)<\/script>/gi;
  let match;
  const schemas = [];
  while ((match = ldJsonRegex.exec(html)) !== null) {
    try {
      const parsed = JSON.parse(match[1]);
      schemas.push(parsed);
    } catch (e) {
      assert(false, `JSON-LD parsing failed: ${e.message}`);
    }
  }

  assert(schemas.length === 4, `Found exactly 4 JSON-LD schemas (found ${schemas.length})`);
  const types = schemas.map(s => s['@type']);
  assert(types.includes('WebSite'), 'WebSite schema present (with Sitelinks SearchBox)');
  assert(types.includes('Organization'), 'Organization schema present');
  assert(types.includes('WebApplication'), 'WebApplication schema present');
  assert(types.includes('FAQPage'), 'FAQPage schema present (Google Rich Results)');

  // 5. Heading Hierarchy Validation
  console.log('\n--- 5. Semantic Heading Hierarchy ---');
  const h1Matches = html.match(/<h1[\s>]/gi) || [];
  assert(h1Matches.length === 1, `Exactly 1 <h1> tag on the page (found ${h1Matches.length})`);

  // 6. Core Web Vitals & Image Shift Protection
  console.log('\n--- 6. Core Web Vitals & Performance ---');
  assert(html.includes('rel="preload" as="image" href="/assets/hero_bg.jpg"'), 'Preload LCP hero background image');
  assert(html.includes('rel="preconnect" href="https://fonts.googleapis.com"'), 'Preconnect to Google Fonts');
  assert(html.includes('width="32" height="32"'), 'Explicit dimensions on nav avatar image (prevents CLS)');
  assert(html.includes('width="144" height="208"'), 'Explicit dimensions on detail poster image (prevents CLS)');
  assert(html.includes('width="64" height="64"'), 'Explicit dimensions on account avatar image (prevents CLS)');

  // 7. Crawlable FAQ, Taxonomy & NoScript
  console.log('\n--- 7. Accessible Crawlable Content & Fallback ---');
  assert(html.includes('id="seo-knowledge-base"'), 'Semantic crawlable knowledge base section present');
  assert(html.includes('<noscript>'), '<noscript> fallback present for non-JS crawlers');

  // 8. Dynamic Client-Side Title Updates in app.js
  console.log('\n--- 8. SPA Route Title Synchronization ---');
  const appJsPath = path.join(__dirname, '../public/js/app.js');
  const appJs = fs.readFileSync(appJsPath, 'utf8');
  assert(appJs.includes("document.title = 'Om CLI — Watch Anime Free in HD"), 'Dynamic document.title on home route');
  assert(appJs.includes("document.title = `${genreParam} Anime — Stream Free in HD | Om CLI`"), 'Dynamic document.title on genre browse route');
  assert(appJs.includes("document.title = `Watch Episode ${epNum} — ${epAnimeTitle}"), 'Dynamic document.title on episode playback');

  console.log('\n============================================================');
  console.log(`📊 SEO AUDIT SUMMARY: ${passed} PASSED, ${failed} FAILED`);
  console.log('============================================================\n');

  if (failed > 0) {
    process.exit(1);
  }
}

runSeoAudit();
