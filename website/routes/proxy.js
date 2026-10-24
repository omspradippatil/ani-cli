// website/routes/proxy.js
// High-performance video & playlist proxy that injects necessary Referer and eliminates CORS issues for browsers

const express = require('express');
const router = express.Router();

const USER_AGENT = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36';

/**
 * Helper to resolve relative URLs against a base URL
 */
function resolveUrl(relative, base) {
  try {
    return new URL(relative, base).href;
  } catch {
    return relative;
  }
}

/**
 * Proxies and rewrites M3U8 playlists (master and media variants)
 */
router.get('/m3u8', async (req, res) => {
  const targetUrl = req.query.url;
  const referer = req.query.ref || 'https://zokoanime.video/';

  if (!targetUrl) {
    return res.status(400).send('Missing "url" query parameter');
  }

  try {
    const upstreamRes = await fetch(targetUrl, {
      headers: {
        'User-Agent': USER_AGENT,
        'Referer': referer,
        'Origin': referer.replace(/\/$/, '')
      }
    });

    if (!upstreamRes.ok) {
      return res.status(upstreamRes.status).send(`Upstream playlist returned ${upstreamRes.status}`);
    }

    const playlistText = await upstreamRes.text();
    const lines = playlistText.split(/\r?\n/);
    const rewrittenLines = [];
    const proxyBase = process.env.STREAM_PROXY_URL ? process.env.STREAM_PROXY_URL.replace(/\/$/, '') : '';

    for (let i = 0; i < lines.length; i++) {
      let line = lines[i].trim();

      if (!line) {
        rewrittenLines.push(line);
        continue;
      }

      // Handle tags
      if (line.startsWith('#')) {
        // Rewrite URI="..." in EXT-X-KEY, EXT-X-MAP, EXT-X-MEDIA
        if (line.includes('URI="')) {
          line = line.replace(/URI="([^"]+)"/g, (match, uri) => {
            const absolute = resolveUrl(uri, targetUrl);
            const proxyEndpoint = uri.includes('.m3u8') ? `${proxyBase}/api/proxy/m3u8` : `${proxyBase}/api/proxy/ts`;
            return `URI="${proxyEndpoint}?url=${encodeURIComponent(absolute)}&ref=${encodeURIComponent(referer)}"`;
          });
        }
        rewrittenLines.push(line);
        continue;
      }

      // URI Line (Segment or Child Playlist)
      const absoluteUrl = resolveUrl(line, targetUrl);

      // Check if it's a child m3u8 playlist or a video segment (.ts / .jpg / .mp4)
      if (absoluteUrl.includes('.m3u8') || absoluteUrl.includes('playlist')) {
        rewrittenLines.push(`${proxyBase}/api/proxy/m3u8?url=${encodeURIComponent(absoluteUrl)}&ref=${encodeURIComponent(referer)}`);
      } else {
        rewrittenLines.push(`${proxyBase}/api/proxy/ts?url=${encodeURIComponent(absoluteUrl)}&ref=${encodeURIComponent(referer)}`);
      }
    }

    res.setHeader('Content-Type', 'application/vnd.apple.mpegurl');
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, OPTIONS');
    res.setHeader('Cache-Control', 'no-cache, no-store, must-revalidate');
    return res.send(rewrittenLines.join('\n'));
  } catch (err) {
    console.error('m3u8 proxy error:', err.message);
    res.status(500).send(`Proxy error: ${err.message}`);
  }
});

/**
 * Proxies video segments (.ts, .jpg, .m4s)
 */
router.get('/ts', async (req, res) => {
  const targetUrl = req.query.url;
  const referer = req.query.ref || 'https://zokoanime.video/';

  if (!targetUrl) {
    return res.status(400).send('Missing "url" query parameter');
  }

  try {
    const forwardHeaders = {
      'User-Agent': USER_AGENT,
      'Referer': referer,
      'Origin': referer.replace(/\/$/, '')
    };

    if (req.headers.range) {
      forwardHeaders['Range'] = req.headers.range;
    }

    const upstreamRes = await fetch(targetUrl, { headers: forwardHeaders });

    if (!upstreamRes.ok && upstreamRes.status !== 206) {
      return res.status(upstreamRes.status).send(`Upstream TS returned ${upstreamRes.status}`);
    }

    res.status(upstreamRes.status);
    res.setHeader('Content-Type', upstreamRes.headers.get('content-type') || 'video/mp2t');
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Headers', 'Range');
    res.setHeader('Access-Control-Expose-Headers', 'Content-Range, Content-Length, Accept-Ranges');
    res.setHeader('Cache-Control', 'public, max-age=86400, immutable');

    const contentRange = upstreamRes.headers.get('content-range');
    if (contentRange) res.setHeader('Content-Range', contentRange);

    const contentLength = upstreamRes.headers.get('content-length');
    if (contentLength) res.setHeader('Content-Length', contentLength);

    // Stream response
    const arrayBuffer = await upstreamRes.arrayBuffer();
    return res.send(Buffer.from(arrayBuffer));
  } catch (err) {
    console.error('TS proxy error:', err.message);
    res.status(500).send(`Segment proxy error: ${err.message}`);
  }
});

/**
 * Proxies WebVTT subtitles
 */
router.get('/vtt', async (req, res) => {
  const targetUrl = req.query.url;
  const referer = req.query.ref || 'https://zokoanime.video/';

  if (!targetUrl) return res.status(400).send('Missing "url" query parameter');

  try {
    const upstreamRes = await fetch(targetUrl, {
      headers: {
        'User-Agent': USER_AGENT,
        'Referer': referer
      }
    });

    if (!upstreamRes.ok) return res.status(upstreamRes.status).send('VTT fetch failed');

    const text = await upstreamRes.text();
    res.setHeader('Content-Type', 'text/vtt; charset=utf-8');
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Cache-Control', 'public, max-age=86400');
    return res.send(text);
  } catch (err) {
    res.status(500).send(`VTT proxy error: ${err.message}`);
  }
});

module.exports = router;
