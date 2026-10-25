// website/services/anilist.js
// Free public GraphQL API from AniList (No API key needed)

const ANILIST_URL = 'https://graphql.anilist.co';

let trendingSeed = [];
let popularSeed = [];
let topRatedSeed = [];

try {
  trendingSeed = require('../data/trending_seed.json');
} catch (_) {}
try {
  popularSeed = require('../data/popular_seed.json');
} catch (_) {}
try {
  topRatedSeed = require('../data/top_rated_seed.json');
} catch (_) {}

async function fetchAniList(query, variables = {}) {
  try {
    const res = await fetch(ANILIST_URL, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36'
      },
      body: JSON.stringify({ query, variables })
    });
    if (!res.ok) throw new Error(`AniList HTTP ${res.status}`);
    const json = await res.json();
    return json.data;
  } catch (err) {
    console.error('AniList fetch error:', err.message);
    throw err;
  }
}

const MEDIA_FIELDS = `
  id
  idMal
  title {
    romaji
    english
    native
    userPreferred
  }
  coverImage {
    extraLarge
    large
    medium
    color
  }
  bannerImage
  format
  status
  episodes
  duration
  seasonYear
  season
  averageScore
  genres
  description(asHtml: false)
  nextAiringEpisode {
    episode
    airingAt
    timeUntilAiring
  }
`;

/**
 * Get trending anime
 */
async function getTrending(page = 1, perPage = 20) {
  try {
    const query = `
      query ($page: Int, $perPage: Int) {
        Page(page: $page, perPage: $perPage) {
          pageInfo {
            hasNextPage
            currentPage
          }
          media(type: ANIME, sort: TRENDING_DESC, isAdult: false) {
            ${MEDIA_FIELDS}
          }
        }
      }
    `;
    const data = await fetchAniList(query, { page, perPage });
    const media = data?.Page?.media;
    if (media && media.length > 0) return media;
  } catch (err) {
    console.warn('[AniList] getTrending fallback to seed:', err.message);
  }
  return trendingSeed.slice(0, perPage);
}

/**
 * Get popular anime this season or all time
 */
async function getPopular(page = 1, perPage = 20) {
  try {
    const query = `
      query ($page: Int, $perPage: Int) {
        Page(page: $page, perPage: $perPage) {
          media(type: ANIME, sort: POPULARITY_DESC, isAdult: false) {
            ${MEDIA_FIELDS}
          }
        }
      }
    `;
    const data = await fetchAniList(query, { page, perPage });
    const media = data?.Page?.media;
    if (media && media.length > 0) return media;
  } catch (err) {
    console.warn('[AniList] getPopular fallback to seed:', err.message);
  }
  return popularSeed.slice(0, perPage);
}

/**
 * Get top rated anime
 */
async function getTopRated(page = 1, perPage = 20) {
  try {
    const query = `
      query ($page: Int, $perPage: Int) {
        Page(page: $page, perPage: $perPage) {
          media(type: ANIME, sort: SCORE_DESC, isAdult: false) {
            ${MEDIA_FIELDS}
          }
        }
      }
    `;
    const data = await fetchAniList(query, { page, perPage });
    const media = data?.Page?.media;
    if (media && media.length > 0) return media;
  } catch (err) {
    console.warn('[AniList] getTopRated fallback to seed:', err.message);
  }
  return topRatedSeed.slice(0, perPage);
}

/**
 * Search anime by title keyword
 */
async function searchAnime(keyword, page = 1, perPage = 20) {
  const query = `
    query ($search: String, $page: Int, $perPage: Int) {
      Page(page: $page, perPage: $perPage) {
        media(type: ANIME, search: $search, sort: SEARCH_MATCH, isAdult: false) {
          ${MEDIA_FIELDS}
        }
      }
    }
  `;
  const data = await fetchAniList(query, { search: keyword, page, perPage });
  return data?.Page?.media || [];
}

/**
 * Get detailed info for a single anime by ID
 */
async function getAnimeDetails(id) {
  try {
    const query = `
      query ($id: Int) {
        Media(id: $id, type: ANIME) {
          ${MEDIA_FIELDS}
          studios(isMain: true) {
            nodes {
              name
            }
          }
          relations {
            edges {
              relationType
              node {
                id
                title {
                  romaji
                  english
                  userPreferred
                }
                coverImage {
                  medium
                  large
                }
                format
                type
              }
            }
          }
          recommendations(perPage: 10) {
            nodes {
              mediaRecommendation {
                id
                title {
                  userPreferred
                }
                coverImage {
                  large
                }
                averageScore
              }
            }
          }
        }
      }
    `;
    const data = await fetchAniList(query, { id: Number(id) });
    if (data?.Media) return data.Media;
  } catch (err) {
    console.warn(`[AniList] getAnimeDetails(${id}) fallback:`, err.message);
  }
  const allSeeds = [...trendingSeed, ...popularSeed, ...topRatedSeed];
  return allSeeds.find(a => a.id == id) || null;
}

/**
 * Advanced browse with filtering & sorting
 */
async function browseAnime({ page = 1, perPage = 24, genre, sort = 'POPULARITY_DESC', search, format, status }) {
  const sortMap = {
    'score': 'SCORE_DESC',
    'popularity': 'POPULARITY_DESC',
    'updated': 'UPDATED_AT_DESC',
    'release': 'START_DATE_DESC',
    'episodes': 'EPISODES_DESC',
    'trending': 'TRENDING_DESC'
  };
  const resolvedSort = sortMap[sort] || sort || 'POPULARITY_DESC';

  const variables = { page: Number(page) || 1, perPage: Number(perPage) || 24, sort: resolvedSort };
  const filterArgs = ['$page: Int', '$perPage: Int', '$sort: [MediaSort]'];
  const mediaArgs = ['type: ANIME', 'sort: $sort', 'isAdult: false'];

  if (search && search.trim()) {
    variables.search = search.trim();
    filterArgs.push('$search: String');
    mediaArgs.push('search: $search');
  }

  if (genre && genre !== 'All') {
    variables.genre = genre;
    filterArgs.push('$genre: String');
    mediaArgs.push('genre: $genre');
  }

  if (format && format !== 'All') {
    variables.format = format;
    filterArgs.push('$format: MediaFormat');
    mediaArgs.push('format: $format');
  }

  if (status && status !== 'All') {
    variables.status = status;
    filterArgs.push('$status: MediaStatus');
    mediaArgs.push('status: $status');
  }

  const query = `
    query (${filterArgs.join(', ')}) {
      Page(page: $page, perPage: $perPage) {
        pageInfo {
          hasNextPage
          currentPage
          total
        }
        media(${mediaArgs.join(', ')}) {
          ${MEDIA_FIELDS}
        }
      }
    }
  `;

  const data = await fetchAniList(query, variables);
  return {
    items: data?.Page?.media || [],
    pageInfo: data?.Page?.pageInfo || { hasNextPage: false, currentPage: 1 }
  };
}

module.exports = {
  getTrending,
  getPopular,
  getTopRated,
  searchAnime,
  getAnimeDetails,
  browseAnime
};
