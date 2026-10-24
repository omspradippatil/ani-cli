// website/services/hianime.js
// HiAnime (Zoro) scraper & ZokoAnime stream extractor (based on ani-cli v5.1.6)

const BASE_API = 'https://hianime.at';
const USER_AGENT = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36';

const HEADERS = {
  'User-Agent': USER_AGENT,
  'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
  'Accept-Language': 'en-US,en;q=0.9'
};

/**
 * Searches HiAnime for anime matching keyword
 */
async function search(keyword) {
  try {
    const url = `${BASE_API}/search?keyword=${encodeURIComponent(keyword)}`;
    const res = await fetch(url, { headers: HEADERS });
    if (!res.ok) throw new Error(`HiAnime search failed: HTTP ${res.status}`);
    const html = await res.text();

    const mainPart = html.split('id="main-sidebar"')[0] || html;
    const results = [];
    const titleRegex = /<h3 class="film-name">\s*<a href="[^"]*\/([^"/]+)"\s*title="([^"]+)"/g;
    let match;

    while ((match = titleRegex.exec(mainPart)) !== null) {
      const slug = match[1];
      const title = match[2]
        .replace(/&#039;/g, "'")
        .replace(/&quot;/g, '"')
        .replace(/&amp;/g, '&');
      const animeId = slug.split('-').pop();

      results.push({
        id: animeId,
        slug,
        title
      });
    }

    return results;
  } catch (err) {
    console.error('HiAnime search error:', err.message);
    return [];
  }
}

/**
 * Get episode list for an anime by ID
 */
async function getEpisodes(animeIdOrSlug) {
  try {
    // If slug provided, extract numeric ID at end
    const id = animeIdOrSlug.includes('-') ? animeIdOrSlug.split('-').pop() : animeIdOrSlug;
    const url = `${BASE_API}/api/theme/episode/list/${id}`;
    
    const res = await fetch(url, { headers: HEADERS });
    if (!res.ok) throw new Error(`HiAnime episodes HTTP ${res.status}`);
    const data = await res.json();
    if (!data.status || !data.html) return [];

    const episodes = [];
    const epRegex = /data-number="([^"]*)"[\s\S]*?data-id="([0-9]+)"/g;
    let match;

    while ((match = epRegex.exec(data.html)) !== null) {
      const epNum = match[1];
      const epId = match[2];

      episodes.push({
        id: epId,
        number: parseFloat(epNum) || 0,
        title: `Episode ${epNum}`
      });
    }

    // Sort by episode number ascending
    episodes.sort((a, b) => a.number - b.number);
    return episodes;
  } catch (err) {
    console.error('HiAnime getEpisodes error:', err.message);
    return [];
  }
}

/**
 * Get available servers (sub/dub) for an episode
 */
async function getServers(episodeId) {
  try {
    const url = `${BASE_API}/api/theme/episode/servers?episodeId=${episodeId}`;
    const res = await fetch(url, { headers: HEADERS });
    if (!res.ok) throw new Error(`HiAnime servers HTTP ${res.status}`);
    const data = await res.json();
    if (!data.status || !data.html) return [];

    const servers = [];
    const serverRegex = /<div class="item server-item"\s*data-type="([^"]+)"\s*data-server-name="([^"]+)"\s*data-hash="([^"]+)"/g;
    let match;

    while ((match = serverRegex.exec(data.html)) !== null) {
      servers.push({
        type: match[1], // 'sub' or 'dub'
        name: match[2], // 'ZokoAnime', 'HD-1', etc.
        hash: match[3]
      });
    }

    return servers;
  } catch (err) {
    console.error('HiAnime getServers error:', err.message);
    return [];
  }
}

/**
 * XOR decrypts base64 encoded blob with key 'otaku-embed-v1'
 */
function deobfuscateBlob(base64Blob) {
  const buf = Buffer.from(base64Blob, 'base64');
  const key = Buffer.from('otaku-embed-v1', 'utf-8');
  const out = Buffer.alloc(buf.length);

  for (let i = 0; i < buf.length; i++) {
    out[i] = buf[i] ^ key[i % key.length];
  }

  return JSON.parse(out.toString('utf-8'));
}

/**
 * Extract streaming source from server hash (ZokoAnime)
 */
async function extractStreamFromHash(hash) {
  try {
    const embedUrl = Buffer.from(hash, 'base64').toString('utf-8');
    if (!embedUrl.startsWith('http')) throw new Error('Invalid embed URL');

    const embedRes = await fetch(embedUrl, {
      headers: {
        'User-Agent': USER_AGENT,
        'Referer': BASE_API + '/'
      }
    });

    if (!embedRes.ok) throw new Error(`Embed fetch failed: HTTP ${embedRes.status}`);
    const embedHtml = await embedRes.text();

    const blobMatch = embedHtml.match(/window\.__P="([^"]+)"/);
    if (!blobMatch) throw new Error('window.__P payload not found in embed');

    const config = deobfuscateBlob(blobMatch[1]);
    const referer = embedUrl.substring(0, embedUrl.indexOf('/', 9) + 1); // e.g. "https://zokoanime.video/"

    return {
      success: true,
      m3u8: config.src,
      subtitles: config.subtitles || [],
      skip: config.skip || null,
      referer: referer
    };
  } catch (err) {
    console.error('Stream extraction error:', err.message);
    return {
      success: false,
      error: err.message
    };
  }
}

/**
 * Convenience method: resolve stream directly by episodeId and preferred mode (sub/dub)
 */
async function resolveEpisodeStream(episodeId, mode = 'sub') {
  const servers = await getServers(episodeId);
  if (!servers.length) throw new Error('No servers found for episode');

  // Try ZokoAnime in requested mode, then other mode, then any ZokoAnime, then first server
  let selected = servers.find(s => s.type === mode && s.name.toLowerCase() === 'zokoanime');
  if (!selected) {
    const fallbackMode = mode === 'sub' ? 'dub' : 'sub';
    selected = servers.find(s => s.type === fallbackMode && s.name.toLowerCase() === 'zokoanime');
  }
  if (!selected) {
    selected = servers.find(s => s.name.toLowerCase() === 'zokoanime');
  }

  if (!selected) {
    throw new Error('ZokoAnime stream server unavailable for this episode');
  }

  const result = await extractStreamFromHash(selected.hash);
  result.serverName = selected.name;
  result.type = selected.type;
  result.availableServers = servers.map(s => ({ type: s.type, name: s.name }));
  return result;
}

module.exports = {
  search,
  getEpisodes,
  getServers,
  extractStreamFromHash,
  resolveEpisodeStream
};
