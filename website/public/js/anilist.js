// website/public/js/anilist.js
// Direct Browser Client for AniList GraphQL API
// Provides seamless high-speed metadata querying directly from the browser (with open CORS)

(function () {
  const ANILIST_URL = 'https://graphql.anilist.co';

  async function fetchAniList(query, variables = {}) {
    const res = await fetch(ANILIST_URL, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json'
      },
      body: JSON.stringify({ query, variables })
    });
    if (!res.ok) throw new Error(`AniList HTTP ${res.status}`);
    const json = await res.json();
    if (json.errors && json.errors.length > 0) {
      throw new Error(json.errors[0].message);
    }
    return json.data;
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

  window.AniListClient = {
    async getTrending(page = 1, perPage = 20) {
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
      const data = await fetchAniList(query, { page: Number(page) || 1, perPage: Number(perPage) || 20 });
      return data?.Page?.media || [];
    },

    async getPopular(page = 1, perPage = 20) {
      const query = `
        query ($page: Int, $perPage: Int) {
          Page(page: $page, perPage: $perPage) {
            media(type: ANIME, sort: POPULARITY_DESC, isAdult: false) {
              ${MEDIA_FIELDS}
            }
          }
        }
      `;
      const data = await fetchAniList(query, { page: Number(page) || 1, perPage: Number(perPage) || 20 });
      return data?.Page?.media || [];
    },

    async getTopRated(page = 1, perPage = 20) {
      const query = `
        query ($page: Int, $perPage: Int) {
          Page(page: $page, perPage: $perPage) {
            media(type: ANIME, sort: SCORE_DESC, isAdult: false) {
              ${MEDIA_FIELDS}
            }
          }
        }
      `;
      const data = await fetchAniList(query, { page: Number(page) || 1, perPage: Number(perPage) || 20 });
      return data?.Page?.media || [];
    },

    async searchAnime(keyword, page = 1, perPage = 20) {
      const query = `
        query ($search: String, $page: Int, $perPage: Int) {
          Page(page: $page, perPage: $perPage) {
            media(type: ANIME, search: $search, sort: SEARCH_MATCH, isAdult: false) {
              ${MEDIA_FIELDS}
            }
          }
        }
      `;
      const data = await fetchAniList(query, { search: keyword, page: Number(page) || 1, perPage: Number(perPage) || 20 });
      return data?.Page?.media || [];
    },

    async getAnimeDetails(id) {
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
      return data?.Media || null;
    },

    async browseAnime({ page = 1, perPage = 24, genre, sort = 'popularity', search, format, status }) {
      const sortMap = {
        'score': 'SCORE_DESC',
        'popularity': 'POPULARITY_DESC',
        'updated': 'UPDATED_AT_DESC',
        'release': 'START_DATE_DESC',
        'episodes': 'EPISODES_DESC',
        'trending': 'TRENDING_DESC'
      };

      const ANILIST_GENRES = new Set([
        'action', 'adventure', 'comedy', 'drama', 'ecchi', 'fantasy', 'horror',
        'mahou shoujo', 'mecha', 'music', 'mystery', 'psychological', 'romance',
        'sci-fi', 'slice of life', 'sports', 'supernatural', 'thriller'
      ]);

      const variables = { page: Number(page) || 1, perPage: Number(perPage) || 24 };
      const filterArgs = ['$page: Int', '$perPage: Int'];
      const mediaArgs = ['type: ANIME', 'isAdult: false'];

      if (search && search.trim()) {
        variables.search = search.trim();
        filterArgs.push('$search: String');
        mediaArgs.push('search: $search');
        if (!sort || sort === 'popularity' || sort === 'score') {
          variables.sort = 'SEARCH_MATCH';
          filterArgs.push('$sort: [MediaSort]');
          mediaArgs.push('sort: $sort');
        } else {
          variables.sort = sortMap[sort] || sort;
          filterArgs.push('$sort: [MediaSort]');
          mediaArgs.push('sort: $sort');
        }
      } else {
        const resolvedSort = sortMap[sort] || sort || 'POPULARITY_DESC';
        variables.sort = resolvedSort;
        filterArgs.push('$sort: [MediaSort]');
        mediaArgs.push('sort: $sort');
      }

      // Handle genres and tags (like Shounen, Isekai, etc.)
      const rawGenres = Array.isArray(genre) ? genre : (genre && genre !== 'All' ? String(genre).split(',') : []);
      const matchedGenres = [];
      const matchedTags = [];

      for (const item of rawGenres) {
        if (!item || item === 'All') continue;
        const norm = item.trim();
        if (ANILIST_GENRES.has(norm.toLowerCase())) {
          matchedGenres.push(norm);
        } else {
          matchedTags.push(norm);
        }
      }

      if (matchedGenres.length === 1) {
        variables.genre = matchedGenres[0];
        filterArgs.push('$genre: String');
        mediaArgs.push('genre: $genre');
      } else if (matchedGenres.length > 1) {
        variables.genres = matchedGenres;
        filterArgs.push('$genres: [String]');
        mediaArgs.push('genre_in: $genres');
      }

      if (matchedTags.length === 1) {
        variables.tag = matchedTags[0];
        filterArgs.push('$tag: String');
        mediaArgs.push('tag: $tag');
      } else if (matchedTags.length > 1) {
        variables.tags = matchedTags;
        filterArgs.push('$tags: [String]');
        mediaArgs.push('tag_in: $tags');
      }

      if (format && format !== 'All' && format !== 'all') {
        variables.format = format;
        filterArgs.push('$format: MediaFormat');
        mediaArgs.push('format: $format');
      }

      if (status && status !== 'All' && status !== 'all') {
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
  };
})();
