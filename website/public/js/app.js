// website/public/js/app.js
// Client Application Controller & Cinema Watch View Router

document.addEventListener('DOMContentLoaded', () => {
  let player = null;
  let currentAnime = null;
  let currentEpisodeIndex = 0;
  let currentMode = 'sub'; // 'sub' or 'dub'
  let spotlightList = [];
  let spotlightIndex = 0;
  let spotlightTimer = null;
  let rawTrendingList = [];

  // DOM Views
  const homeView = document.getElementById('home-view');
  const watchView = document.getElementById('watch-view');
  const searchView = document.getElementById('search-view');

  // Nav Elements
  const navLogoBtn = document.getElementById('nav-logo-btn');
  const navHomeBtn = document.getElementById('nav-home-btn');
  const globalSearchInput = document.getElementById('global-search-input');
  const searchDropdown = document.getElementById('search-dropdown');

  // Hero Elements
  const heroSpotlight = document.getElementById('hero-spotlight');
  const heroBackdrop = document.getElementById('hero-backdrop');
  const heroTitle = document.getElementById('hero-title');
  const heroDesc = document.getElementById('hero-desc');
  const heroScore = document.getElementById('hero-score');
  const heroEpisodes = document.getElementById('hero-episodes');
  const heroRank = document.getElementById('hero-rank');
  const heroPlayBtn = document.getElementById('hero-play-btn');
  const heroInfoBtn = document.getElementById('hero-info-btn');
  const heroIndicators = document.getElementById('hero-indicators');

  // Grids
  const trendingGrid = document.getElementById('trending-grid');
  const popularGrid = document.getElementById('popular-grid');
  const topRatedGrid = document.getElementById('top-rated-grid');
  const continueGrid = document.getElementById('continue-grid');
  const continueSection = document.getElementById('continue-watching-section');
  const searchGrid = document.getElementById('search-grid');
  const searchHeading = document.getElementById('search-results-heading');

  // Watch View Elements
  const btnBackHome = document.getElementById('btn-back-home');
  const crumbAnimeTitle = document.getElementById('crumb-anime-title');
  const crumbEpisodeNum = document.getElementById('crumb-episode-num');
  const playerAmbientGlow = document.getElementById('player-ambient-glow');
  const currentEpTitle = document.getElementById('current-ep-title');
  const currentAnimeTitleSub = document.getElementById('current-anime-title-sub');
  const btnAudioSub = document.getElementById('btn-audio-sub');
  const btnAudioDub = document.getElementById('btn-audio-dub');
  const btnPrevEp = document.getElementById('btn-prev-ep');
  const btnNextEp = document.getElementById('btn-next-ep');

  const detailPosterImg = document.getElementById('detail-poster-img');
  const detailAnimeTitle = document.getElementById('detail-anime-title');
  const detailAnimeNative = document.getElementById('detail-anime-native');
  const detailTagsContainer = document.getElementById('detail-tags-container');
  const detailSynopsisText = document.getElementById('detail-synopsis-text');

  const episodesTotalCount = document.getElementById('episodes-total-count');
  const episodeSearchFilter = document.getElementById('episode-search-filter');
  const rangeTabsContainer = document.getElementById('range-tabs-container');
  const episodesScrollList = document.getElementById('episodes-scroll-list');
  const recsListContainer = document.getElementById('recs-list-container');
  const genreBar = document.getElementById('genre-bar');

  // Initialize AnimePlayer
  player = new window.AnimePlayer('#artplayer-mount');
  player.onNextEpisode = () => playNextEpisode();

  // Navigation handlers
  navLogoBtn.addEventListener('click', (e) => { e.preventDefault(); showHomeView(); });
  navHomeBtn.addEventListener('click', (e) => { e.preventDefault(); showHomeView(); });
  btnBackHome.addEventListener('click', () => showHomeView());

  // Audio Switcher
  btnAudioSub.addEventListener('click', () => switchAudioMode('sub'));
  btnAudioDub.addEventListener('click', () => switchAudioMode('dub'));

  // Prev / Next Buttons
  btnPrevEp.addEventListener('click', () => playPrevEpisode());
  btnNextEp.addEventListener('click', () => playNextEpisode());

  // Search Filter in Episodes Sidebar
  episodeSearchFilter.addEventListener('input', (e) => {
    const val = e.target.value.trim().toLowerCase();
    const tiles = episodesScrollList.querySelectorAll('.ep-tile');
    tiles.forEach(tile => {
      const num = tile.dataset.num || '';
      tile.style.display = (!val || num.includes(val)) ? 'flex' : 'none';
    });
  });

  // Genre Filter Bar
  genreBar.addEventListener('click', (e) => {
    const chip = e.target.closest('.genre-chip');
    if (!chip) return;
    genreBar.querySelectorAll('.genre-chip').forEach(c => c.classList.remove('active'));
    chip.classList.add('active');

    const genre = chip.dataset.genre;
    if (genre === 'All') {
      renderAnimeCards(trendingGrid, rawTrendingList);
    } else {
      const filtered = rawTrendingList.filter(a => (a.genres || []).includes(genre));
      renderAnimeCards(trendingGrid, filtered);
    }
  });

  // Live Floating Search Handlers
  let searchDebounce = null;
  globalSearchInput.addEventListener('input', (e) => {
    clearTimeout(searchDebounce);
    const query = e.target.value.trim();
    if (!query) {
      searchDropdown.classList.remove('open');
      searchDropdown.innerHTML = '';
      return;
    }
    searchDebounce = setTimeout(() => handleLiveSearch(query), 300);
  });

  globalSearchInput.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') {
      const query = globalSearchInput.value.trim();
      if (query) {
        searchDropdown.classList.remove('open');
        showFullSearchView(query);
      }
    }
  });

  // Close search dropdown on click outside
  document.addEventListener('click', (e) => {
    if (!e.target.closest('.search-wrapper')) {
      searchDropdown.classList.remove('open');
    }
  });

  // Initialize content
  loadHomeFeed();
  renderContinueWatching();
  checkUrlHash();

  window.addEventListener('hashchange', checkUrlHash);

  // ==========================================================================
  // Router & View Switcher
  // ==========================================================================
  function showHomeView() {
    window.location.hash = '';
    homeView.style.display = 'block';
    watchView.style.display = 'none';
    searchView.style.display = 'none';
    if (player) player.stop();
    renderContinueWatching();
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  function showWatchView() {
    homeView.style.display = 'none';
    watchView.style.display = 'block';
    searchView.style.display = 'none';
    searchDropdown.classList.remove('open');
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  function showFullSearchView(query) {
    homeView.style.display = 'none';
    watchView.style.display = 'none';
    searchView.style.display = 'block';
    if (player) player.stop();

    searchHeading.textContent = `Search Results for "${query}"`;
    searchGrid.innerHTML = '<div class="skeleton-card"></div><div class="skeleton-card"></div><div class="skeleton-card"></div>';

    fetch(`/api/search?q=${encodeURIComponent(query)}`)
      .then(r => r.json())
      .then(json => {
        searchGrid.innerHTML = '';
        if (json.success && json.data.anilist && json.data.anilist.length > 0) {
          renderAnimeCards(searchGrid, json.data.anilist);
        } else {
          searchGrid.innerHTML = `<p style="grid-column: 1/-1; text-align: center; color: var(--text-muted); padding: 4rem;">No anime found matching "${query}"</p>`;
        }
      })
      .catch(err => {
        searchGrid.innerHTML = `<p style="grid-column: 1/-1; text-align: center; color: var(--accent-red); padding: 4rem;">Search failed: ${err.message}</p>`;
      });
  }

  function checkUrlHash() {
    const hash = window.location.hash;
    if (hash.startsWith('#watch')) {
      const params = new URLSearchParams(hash.replace('#watch?', ''));
      const animeId = params.get('id');
      const epIndex = parseInt(params.get('ep') || '0', 10);
      if (animeId) {
        openAnimeWatchPage(animeId, epIndex);
      }
    }
  }

  // ==========================================================================
  // Data Fetching & Home Renderers
  // ==========================================================================
  async function loadHomeFeed() {
    try {
      const [trendRes, popRes, topRes] = await Promise.all([
        fetch('/api/trending?perPage=24').then(r => r.json()),
        fetch('/api/popular?perPage=18').then(r => r.json()),
        fetch('/api/top-rated?perPage=18').then(r => r.json())
      ]);

      if (trendRes.success && trendRes.data.length > 0) {
        rawTrendingList = trendRes.data;
        spotlightList = trendRes.data.slice(0, 5);
        initSpotlightCarousel();
        renderAnimeCards(trendingGrid, trendRes.data);
      }

      if (popRes.success) {
        renderAnimeCards(popularGrid, popRes.data);
      }

      if (topRes.success) {
        renderAnimeCards(topRatedGrid, topRes.data);
      }
    } catch (err) {
      console.error('Home feed error:', err);
    }
  }

  function initSpotlightCarousel() {
    renderSpotlightItem(0);
    renderHeroIndicators();

    clearInterval(spotlightTimer);
    spotlightTimer = setInterval(() => {
      spotlightIndex = (spotlightIndex + 1) % spotlightList.length;
      renderSpotlightItem(spotlightIndex);
      updateHeroIndicators(spotlightIndex);
    }, 7000);
  }

  function renderSpotlightItem(idx) {
    const anime = spotlightList[idx];
    if (!anime) return;

    heroBackdrop.style.opacity = '0';
    setTimeout(() => {
      heroBackdrop.style.backgroundImage = `url('${anime.bannerImage || anime.coverImage.extraLarge}')`;
      heroBackdrop.style.opacity = '1';
    }, 250);

    const title = anime.title.english || anime.title.romaji || anime.title.userPreferred;
    heroRank.textContent = `🔥 #${idx + 1} Spotlight`;
    heroTitle.textContent = title;
    heroDesc.textContent = anime.description ? anime.description.replace(/<[^>]*>/g, '') : 'No synopsis available.';
    heroScore.textContent = anime.averageScore ? `★ ${(anime.averageScore / 10).toFixed(1)}` : 'N/A';
    heroEpisodes.textContent = anime.episodes ? `${anime.episodes} Episodes` : 'Airing';

    heroPlayBtn.onclick = () => {
      window.location.hash = `#watch?id=${anime.id}&ep=0`;
    };
    heroInfoBtn.onclick = () => {
      window.location.hash = `#watch?id=${anime.id}&ep=0`;
    };
  }

  function renderHeroIndicators() {
    heroIndicators.innerHTML = '';
    spotlightList.forEach((_, i) => {
      const dot = document.createElement('div');
      dot.className = `hero-dot ${i === spotlightIndex ? 'active' : ''}`;
      dot.onclick = () => {
        spotlightIndex = i;
        renderSpotlightItem(i);
        updateHeroIndicators(i);
      };
      heroIndicators.appendChild(dot);
    });
  }

  function updateHeroIndicators(idx) {
    const dots = heroIndicators.querySelectorAll('.hero-dot');
    dots.forEach((d, i) => d.classList.toggle('active', i === idx));
  }

  function renderAnimeCards(container, list) {
    container.innerHTML = '';
    list.forEach(anime => {
      const card = document.createElement('div');
      card.className = 'anime-card';
      const title = anime.title.english || anime.title.romaji || anime.title.userPreferred;
      const score = anime.averageScore ? (anime.averageScore / 10).toFixed(1) : null;
      const poster = anime.coverImage.large || anime.coverImage.medium;
      const year = anime.seasonYear || '';
      const format = anime.format || 'TV';

      card.innerHTML = `
        <div class="card-poster-area">
          <img src="${poster}" alt="${title}" class="card-img" loading="lazy">
          <div class="card-play-overlay">
            <div class="card-play-btn">
              <svg width="22" height="22" viewBox="0 0 24 24" fill="currentColor"><polygon points="5 3 19 12 5 21 5 3"></polygon></svg>
            </div>
          </div>
          ${score ? `<div class="card-badge-top-left">★ ${score}</div>` : ''}
          <div class="card-badge-bottom-bar">
            <div class="card-sub-dub">
              <span class="badge-sub">SUB</span>
              <span class="badge-dub">DUB</span>
            </div>
            <span>${anime.episodes ? `${anime.episodes} EP` : 'Ongoing'}</span>
          </div>
        </div>
        <div class="card-details">
          <div class="card-title" title="${title}">${title}</div>
          <div class="card-subinfo">
            <span>${format}</span>
            <span>${year}</span>
          </div>
        </div>
      `;

      card.onclick = () => {
        window.location.hash = `#watch?id=${anime.id}&ep=0`;
      };
      container.appendChild(card);
    });
  }

  // ==========================================================================
  // Live Floating Search Dropdown
  // ==========================================================================
  async function handleLiveSearch(query) {
    try {
      const res = await fetch(`/api/search?q=${encodeURIComponent(query)}`);
      const json = await res.json();

      if (!json.success || !json.data.anilist || json.data.anilist.length === 0) {
        searchDropdown.innerHTML = '<div style="padding: 1.25rem; text-align: center; color: var(--text-muted); font-size: 0.88rem;">No results found</div>';
        searchDropdown.classList.add('open');
        return;
      }

      searchDropdown.innerHTML = '';
      json.data.anilist.slice(0, 6).forEach(anime => {
        const title = anime.title.english || anime.title.romaji || anime.title.userPreferred;
        const poster = anime.coverImage.medium || anime.coverImage.large;
        const score = anime.averageScore ? (anime.averageScore / 10).toFixed(1) : null;
        const year = anime.seasonYear || '';

        const item = document.createElement('div');
        item.className = 'search-item';
        item.innerHTML = `
          <img src="${poster}" class="search-item-poster" alt="${title}">
          <div class="search-item-info">
            <div class="search-item-title">${title}</div>
            <div class="search-item-meta">
              ${score ? `<span style="color: #ffb800;">★ ${score}</span> &bull;` : ''}
              <span>${anime.format || 'Anime'}</span> &bull;
              <span>${year}</span>
            </div>
          </div>
        `;

        item.onclick = () => {
          searchDropdown.classList.remove('open');
          globalSearchInput.value = '';
          window.location.hash = `#watch?id=${anime.id}&ep=0`;
        };

        searchDropdown.appendChild(item);
      });

      searchDropdown.classList.add('open');
    } catch (err) {
      console.error('Live search error:', err);
    }
  }

  // ==========================================================================
  // Cinema Watch Page Logic
  // ==========================================================================
  async function openAnimeWatchPage(animeId, initialEpIndex = 0) {
    showWatchView();

    // Show loading state
    currentEpTitle.textContent = 'Loading Episode...';
    episodesScrollList.innerHTML = '<div class="skeleton-card" style="height: 44px;"></div><div class="skeleton-card" style="height: 44px;"></div><div class="skeleton-card" style="height: 44px;"></div>';

    try {
      const res = await fetch(`/api/anime/${animeId}`);
      const json = await res.json();

      if (!json.success || !json.data) throw new Error(json.error || 'Anime not found');

      currentAnime = json.data;
      const title = currentAnime.title.english || currentAnime.title.romaji || currentAnime.title.userPreferred;

      // Update Breadcrumbs & Meta
      crumbAnimeTitle.textContent = title;
      currentAnimeTitleSub.textContent = title;
      detailAnimeTitle.textContent = title;
      detailAnimeNative.textContent = currentAnime.title.native || '';
      detailSynopsisText.textContent = currentAnime.description ? currentAnime.description.replace(/<[^>]*>/g, '') : 'No synopsis available.';
      detailPosterImg.src = currentAnime.coverImage.extraLarge || currentAnime.coverImage.large;

      // Ambient Glow behind player
      playerAmbientGlow.style.backgroundImage = `url('${currentAnime.bannerImage || currentAnime.coverImage.extraLarge}')`;

      // Render Tags
      detailTagsContainer.innerHTML = (currentAnime.genres || []).map(g => `<span class="detail-tag">${g}</span>`).join('');

      // Render Episodes
      const episodes = currentAnime.streamEpisodes || [];
      episodesTotalCount.textContent = `${episodes.length} EP`;

      if (episodes.length === 0) {
        episodesScrollList.innerHTML = '<p style="grid-column: 1/-1; color: var(--text-muted); font-size: 0.88rem; padding: 1rem;">No streamable episodes found yet.</p>';
        return;
      }

      // Render Range Pagination Tabs (1-50, 51-100, etc.)
      renderEpisodeRangeTabs(episodes, initialEpIndex);

      // Render Recommendations
      renderSidebarRecommendations(currentAnime.recommendations?.nodes || []);

      // Start requested episode
      currentEpisodeIndex = Math.min(initialEpIndex, episodes.length - 1);
      playEpisodeAt(currentEpisodeIndex);

    } catch (err) {
      console.error('Watch page error:', err);
      alert(`Could not load anime: ${err.message}`);
    }
  }

  function renderEpisodeRangeTabs(episodes, activeIndex) {
    rangeTabsContainer.innerHTML = '';
    const chunkSize = 50;
    const totalChunks = Math.ceil(episodes.length / chunkSize);

    if (totalChunks <= 1) {
      rangeTabsContainer.style.display = 'none';
      renderEpisodeTiles(episodes, 0, episodes.length);
      return;
    }

    rangeTabsContainer.style.display = 'flex';
    const activeChunk = Math.floor(activeIndex / chunkSize);

    for (let c = 0; c < totalChunks; c++) {
      const start = c * chunkSize + 1;
      const end = Math.min((c + 1) * chunkSize, episodes.length);
      const tab = document.createElement('button');
      tab.className = `range-tab ${c === activeChunk ? 'active' : ''}`;
      tab.textContent = `${start}-${end}`;

      tab.onclick = () => {
        rangeTabsContainer.querySelectorAll('.range-tab').forEach(t => t.classList.remove('active'));
        tab.classList.add('active');
        renderEpisodeTiles(episodes, c * chunkSize, end);
      };

      rangeTabsContainer.appendChild(tab);
    }

    renderEpisodeTiles(episodes, activeChunk * chunkSize, Math.min((activeChunk + 1) * chunkSize, episodes.length));
  }

  function renderEpisodeTiles(episodes, startIndex, endIndex) {
    episodesScrollList.innerHTML = '';
    const slice = episodes.slice(startIndex, endIndex);

    slice.forEach((ep, sliceIdx) => {
      const globalIdx = startIndex + sliceIdx;
      const tile = document.createElement('button');
      tile.className = `ep-tile ${globalIdx === currentEpisodeIndex ? 'active' : ''}`;
      tile.textContent = `${ep.number}`;
      tile.dataset.num = `${ep.number}`;
      tile.title = ep.title || `Episode ${ep.number}`;

      tile.onclick = () => {
        playEpisodeAt(globalIdx);
      };

      episodesScrollList.appendChild(tile);
    });
  }

  function renderSidebarRecommendations(recs) {
    recsListContainer.innerHTML = '';
    const filtered = recs.filter(r => r.mediaRecommendation).slice(0, 5);

    if (filtered.length === 0) {
      recsListContainer.innerHTML = '<p style="color: var(--text-muted); font-size: 0.85rem;">No recommendations</p>';
      return;
    }

    filtered.forEach(item => {
      const rec = item.mediaRecommendation;
      const title = rec.title.userPreferred;
      const poster = rec.coverImage?.large;
      const score = rec.averageScore ? (rec.averageScore / 10).toFixed(1) : null;

      const el = document.createElement('div');
      el.className = 'rec-item';
      el.innerHTML = `
        <img src="${poster}" class="rec-poster" alt="${title}" loading="lazy">
        <div class="rec-info">
          <div class="rec-title">${title}</div>
          ${score ? `<div class="rec-score">★ ${score}</div>` : ''}
        </div>
      `;

      el.onclick = () => {
        window.location.hash = `#watch?id=${rec.id}&ep=0`;
      };

      recsListContainer.appendChild(el);
    });
  }

  async function playEpisodeAt(index) {
    if (!currentAnime || !currentAnime.streamEpisodes || !currentAnime.streamEpisodes[index]) return;

    currentEpisodeIndex = index;
    const ep = currentAnime.streamEpisodes[index];
    const poster = currentAnime.coverImage?.extraLarge || currentAnime.bannerImage;

    // Update UI headers
    crumbEpisodeNum.textContent = `Episode ${ep.number}`;
    currentEpTitle.textContent = ep.title || `Episode ${ep.number}`;

    // Update active state in grid
    episodesScrollList.querySelectorAll('.ep-tile').forEach(tile => {
      tile.classList.toggle('active', parseInt(tile.dataset.num, 10) === ep.number);
    });

    // Update Prev / Next button states
    btnPrevEp.disabled = index <= 0;
    btnNextEp.disabled = index >= currentAnime.streamEpisodes.length - 1;

    // Save to Continue Watching history
    saveContinueWatching(currentAnime, ep);

    // Update URL Hash without reload
    history.replaceState(null, '', `#watch?id=${currentAnime.id}&ep=${index}`);

    try {
      await player.loadStream(ep.id, currentMode, poster);
    } catch (err) {
      console.error('Play episode error:', err);
    }
  }

  function playNextEpisode() {
    if (!currentAnime || !currentAnime.streamEpisodes) return;
    if (currentEpisodeIndex + 1 < currentAnime.streamEpisodes.length) {
      playEpisodeAt(currentEpisodeIndex + 1);
    }
  }

  function playPrevEpisode() {
    if (!currentAnime || !currentAnime.streamEpisodes) return;
    if (currentEpisodeIndex - 1 >= 0) {
      playEpisodeAt(currentEpisodeIndex - 1);
    }
  }

  function switchAudioMode(mode) {
    if (currentMode === mode) return;
    currentMode = mode;
    btnAudioSub.classList.toggle('active', mode === 'sub');
    btnAudioDub.classList.toggle('active', mode === 'dub');

    if (currentAnime && currentAnime.streamEpisodes) {
      playEpisodeAt(currentEpisodeIndex);
    }
  }

  // ==========================================================================
  // Continue Watching Persistence
  // ==========================================================================
  function saveContinueWatching(anime, ep) {
    try {
      const historyKey = 'ani_continue_history';
      let list = JSON.parse(localStorage.getItem(historyKey)) || [];

      // Filter out existing entry for this anime
      list = list.filter(item => item.animeId !== anime.id);

      list.unshift({
        animeId: anime.id,
        animeTitle: anime.title.english || anime.title.romaji || anime.title.userPreferred,
        poster: anime.coverImage?.large || anime.coverImage?.medium,
        episodeNum: ep.number,
        episodeIndex: currentEpisodeIndex,
        updatedAt: Date.now()
      });

      // Keep only top 10
      localStorage.setItem(historyKey, JSON.stringify(list.slice(0, 10)));
    } catch {}
  }

  function renderContinueWatching() {
    try {
      const historyKey = 'ani_continue_history';
      const list = JSON.parse(localStorage.getItem(historyKey)) || [];

      if (!list || list.length === 0) {
        continueSection.style.display = 'none';
        return;
      }

      continueSection.style.display = 'block';
      continueGrid.innerHTML = '';

      list.slice(0, 6).forEach(item => {
        const card = document.createElement('div');
        card.className = 'anime-card';
        card.innerHTML = `
          <div class="card-poster-area">
            <img src="${item.poster}" alt="${item.animeTitle}" class="card-img" loading="lazy">
            <div class="card-play-overlay">
              <div class="card-play-btn">
                <svg width="22" height="22" viewBox="0 0 24 24" fill="currentColor"><polygon points="5 3 19 12 5 21 5 3"></polygon></svg>
              </div>
            </div>
            <div class="card-badge-bottom-bar">
              <span class="badge-sub">Resume EP ${item.episodeNum}</span>
            </div>
          </div>
          <div class="card-details">
            <div class="card-title">${item.animeTitle}</div>
            <div class="card-subinfo"><span>Continue</span></div>
          </div>
        `;

        card.onclick = () => {
          window.location.hash = `#watch?id=${item.animeId}&ep=${item.episodeIndex}`;
        };

        continueGrid.appendChild(card);
      });
    } catch {}
  }
});
