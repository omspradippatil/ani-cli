// website/public/js/app.js
// Om CLI Core Application Controller & Router

document.addEventListener('DOMContentLoaded', () => {
  // Global Application State
  const state = {
    auth: {
      token: localStorage.getItem('omcli_token') || localStorage.getItem('kuronami_token') || null,
      user: null,
      settings: {
        preferred_audio: 'sub',
        auto_next: 1,
        auto_skip_intro: 1,
        auto_play: 1,
        preferred_quality: 'auto'
      }
    },
    player: null,
    currentAnime: null,
    currentEpisodeIndex: 0,
    currentMode: 'sub',
    activeView: 'home',
    browseFilters: {
      search: '',
      sort: 'score',
      format: 'all',
      status: 'all',
      genre: 'All',
      viewMode: 'grid', // 'grid' | 'detailed' | 'compact'
      preset: 'trending'
    },
    spotlightList: [],
    spotlightIndex: 0,
    spotlightTimer: null,
    rawTrendingList: [],
    userWatchlist: [],
    userHistory: [],
    genresList: [
      'Action', 'Adventure', 'Comedy', 'Drama', 'Fantasy', 
      'Horror', 'Isekai', 'Mecha', 'Music', 'Mystery', 
      'Psychological', 'Romance', 'Sci-Fi', 'Shounen', 
      'Slice of Life', 'Sports', 'Supernatural', 'Thriller'
    ]
  };

  // ============================================================================
  // DOM ELEMENT SELECTORS
  // ============================================================================

  // Views
  const views = {
    home: document.getElementById('home-view'),
    browse: document.getElementById('browse-view'),
    watch: document.getElementById('watch-view'),
    account: document.getElementById('account-view')
  };

  // Nav Elements
  const navTabs = document.querySelectorAll('.nav-tab');
  const navLogoBtn = document.getElementById('nav-logo-btn');
  const globalSearchInput = document.getElementById('global-search-input');
  const searchDropdown = document.getElementById('search-dropdown');
  const quickSubBtn = document.getElementById('quick-sub-btn');
  const quickDubBtn = document.getElementById('quick-dub-btn');
  const navGuestBlock = document.getElementById('nav-guest-block');
  const navUserBlock = document.getElementById('nav-user-block');
  const btnSignInNav = document.getElementById('btn-sign-in-nav');
  const btnLogout = document.getElementById('btn-logout');
  const navAvatarImg = document.getElementById('nav-avatar-img');
  const navUserName = document.getElementById('nav-user-name');
  const navUserTier = document.getElementById('nav-user-tier');
  const flyoutUserName = document.getElementById('flyout-user-name');
  const flyoutUserBadge = document.getElementById('flyout-user-badge');
  const flyoutUserEmail = document.getElementById('flyout-user-email');
  const flyoutWatchlistCount = document.getElementById('flyout-watchlist-count');
  const flyoutHistoryCount = document.getElementById('flyout-history-count');

  // Auth Modal Elements
  const authModal = document.getElementById('auth-modal');
  const authModalClose = document.getElementById('auth-modal-close');
  const btnAuthDemo = document.getElementById('btn-auth-demo');
  const tabAuthLogin = document.getElementById('tab-auth-login');
  const tabAuthRegister = document.getElementById('tab-auth-register');
  const formAuthLogin = document.getElementById('form-auth-login');
  const formAuthRegister = document.getElementById('form-auth-register');
  const authAlertBox = document.getElementById('auth-alert-box');
  const loginInputIdentity = document.getElementById('login-input-identity');
  const loginInputPassword = document.getElementById('login-input-password');
  const regInputUsername = document.getElementById('reg-input-username');
  const regInputEmail = document.getElementById('reg-input-email');
  const regInputPassword = document.getElementById('reg-input-password');

  // Home View Elements
  const heroBackdrop = document.getElementById('hero-backdrop');
  const heroRank = document.getElementById('hero-rank');
  const heroTitle = document.getElementById('hero-title');
  const heroStudioMeta = document.getElementById('hero-studio-meta');
  const heroScore = document.getElementById('hero-score');
  const heroYear = document.getElementById('hero-year');
  const heroFormat = document.getElementById('hero-format');
  const heroEpisodes = document.getElementById('hero-episodes');
  const heroGenres = document.getElementById('hero-genres');
  const heroDesc = document.getElementById('hero-desc');
  const heroPlayBtn = document.getElementById('hero-play-btn');
  const heroWatchlistBtn = document.getElementById('hero-watchlist-btn');
  const heroInfoBtn = document.getElementById('hero-info-btn');
  const heroIndicators = document.getElementById('hero-indicators');
  const genreBar = document.getElementById('genre-bar');
  const continueSection = document.getElementById('continue-watching-section');
  const continueGrid = document.getElementById('continue-grid');
  const trendingGrid = document.getElementById('trending-grid');
  const popularGrid = document.getElementById('popular-grid');
  const topRatedGrid = document.getElementById('top-rated-grid');

  // Browse View Elements
  const browseSearchInput = document.getElementById('browse-search-input');
  const browseClearBtn = document.getElementById('browse-clear-btn');
  const browseSortSelect = document.getElementById('browse-sort-select');
  const viewModeGrid = document.getElementById('view-mode-grid');
  const viewModeDetailed = document.getElementById('view-mode-detailed');
  const viewModeCompact = document.getElementById('view-mode-compact');
  const browseQuickChips = document.getElementById('browse-quick-chips');
  const browseResetFilters = document.getElementById('browse-reset-filters');
  const filterFormatGroup = document.getElementById('filter-format-group');
  const filterStatusGroup = document.getElementById('filter-status-group');
  const filterGenresMatrix = document.getElementById('filter-genres-matrix');
  const browseResultsCount = document.getElementById('browse-results-count');
  const browseResultsContainer = document.getElementById('browse-results-container');
  const browseActiveTags = document.getElementById('browse-active-tags');

  // Mobile UI & Navigation Elements
  const mobileNavBtns = document.querySelectorAll('.mobile-nav-btn');
  const mobileWatchlistBadge = document.getElementById('mobile-watchlist-badge');
  const btnMobileSearchToggle = document.getElementById('btn-mobile-search-toggle');
  const mobileSearchModal = document.getElementById('mobile-search-modal');
  const mobileSearchCloseBtn = document.getElementById('mobile-search-close-btn');
  const mobileSearchInput = document.getElementById('mobile-search-input');
  const mobileSearchClearBtn = document.getElementById('mobile-search-clear-btn');
  const mobileSearchResults = document.getElementById('mobile-search-results');
  const mobileQuickTags = document.querySelectorAll('.mobile-search-tag');

  // Mobile Browse Filter Drawer Elements
  const btnMobileFilterOpen = document.getElementById('btn-mobile-filter-open');
  const btnMobileFilterClose = document.getElementById('btn-mobile-filter-close');
  const btnMobileFilterApply = document.getElementById('btn-mobile-filter-apply');
  const browseFilterSidebar = document.getElementById('browse-filter-sidebar');
  const browseFilterDrawerBackdrop = document.getElementById('browse-filter-drawer-backdrop');
  const mobileBrowseSortSelect = document.getElementById('mobile-browse-sort-select');
  const mobileFilterCountBadge = document.getElementById('mobile-filter-count-badge');

  // Cinema Watch Room Elements
  const crumbAnimeGenre = document.getElementById('crumb-anime-genre');
  const crumbAnimeTitle = document.getElementById('crumb-anime-title');
  const crumbEpisodeNum = document.getElementById('crumb-episode-num');
  const btnShareRoom = document.getElementById('btn-share-room');
  const btnToggleWatchlist = document.getElementById('btn-toggle-watchlist');
  const iconWatchlistStatus = document.getElementById('icon-watchlist-status');
  const textWatchlistStatus = document.getElementById('text-watchlist-status');
  const btnTheaterMode = document.getElementById('btn-theater-mode');
  const playerGridLayout = document.getElementById('player-grid-layout');
  const btnStreamSub = document.getElementById('btn-stream-sub');
  const btnStreamDub = document.getElementById('btn-stream-dub');
  const btnStreamSubDown = document.getElementById('btn-stream-sub-down');
  const btnStreamDubDown = document.getElementById('btn-stream-dub-down');
  const btnFullscreenDown = document.getElementById('btn-fullscreen-down');
  const btnToggleCaptionsDown = document.getElementById('btn-toggle-captions-down');
  const textCaptionsStatus = document.getElementById('text-captions-status');
  const btnQualityDown = document.getElementById('btn-quality-down');
  const textCurrentQuality = document.getElementById('text-current-quality');
  const qualitySelectorWrap = document.getElementById('quality-selector-wrap');
  const qualityDropdownMenu = document.getElementById('quality-dropdown-menu');
  const currentEpTitle = document.getElementById('current-ep-title');
  const currentAnimeTitleSub = document.getElementById('current-anime-title-sub');
  const btnSkipIntro = document.getElementById('btn-skip-intro');
  const btnPrevEp = document.getElementById('btn-prev-ep');
  const btnNextEp = document.getElementById('btn-next-ep');
  const detailPosterImg = document.getElementById('detail-poster-img');
  const detailAnimeTitle = document.getElementById('detail-anime-title');
  const detailAnimeScore = document.getElementById('detail-anime-score');
  const detailAnimeNative = document.getElementById('detail-anime-native');
  const detailTagsContainer = document.getElementById('detail-tags-container');
  const detailSynopsisText = document.getElementById('detail-synopsis-text');
  const episodesTotalCount = document.getElementById('episodes-total-count');
  const episodeSearchFilter = document.getElementById('episode-search-filter');
  const rangeTabsContainer = document.getElementById('range-tabs-container');
  const episodesScrollList = document.getElementById('episodes-scroll-list');
  const recsListContainer = document.getElementById('recs-list-container');

  // Account View Elements
  const accountAvatarImg = document.getElementById('account-avatar-img');
  const accountLevelBadge = document.getElementById('account-level-badge');
  const accountUserName = document.getElementById('account-user-name');
  const accountHandle = document.getElementById('account-handle');
  const accountTierBadge = document.getElementById('account-tier-badge');
  const accountEmail = document.getElementById('account-email');
  const btnAccountLogout = document.getElementById('btn-account-logout');
  const statTotalEpisodes = document.getElementById('stat-total-episodes');
  const statTotalDays = document.getElementById('stat-total-days');
  const statCompletedShows = document.getElementById('stat-completed-shows');
  const statWatchlistCount = document.getElementById('stat-watchlist-count');
  const accountSubTabs = document.getElementById('account-sub-tabs');
  const watchlistStatusChips = document.getElementById('watchlist-status-chips');
  const watchlistCountBadge = document.getElementById('watchlist-count-badge');
  const accountWatchlistGrid = document.getElementById('account-watchlist-grid');
  const btnClearAllHistory = document.getElementById('btn-clear-all-history');
  const accountHistoryList = document.getElementById('account-history-list');
  const prefAudio = document.getElementById('pref-audio');
  const prefQuality = document.getElementById('pref-quality');
  const prefSkipIntro = document.getElementById('pref-skip-intro');
  const prefAutoNext = document.getElementById('pref-auto-next');
  const btnSavePreferences = document.getElementById('btn-save-preferences');
  const accInputUsername = document.getElementById('acc-input-username');
  const accInputEmail = document.getElementById('acc-input-email');
  const btnAccLogoutBottom = document.getElementById('btn-acc-logout-bottom');

  // Toast Container
  const toastContainer = document.getElementById('toast-container');

  // ============================================================================
  // INITIALIZATION & PLAYER INSTANTIATION
  // ============================================================================

  state.player = new window.AnimePlayer('#artplayer-mount');
  state.player.onNextEpisode = () => playNextEpisode();
  state.player.onPrevEpisode = () => playPrevEpisode();
  state.player.onToggleMode = () => switchGlobalAudio(state.currentMode === 'sub' ? 'dub' : 'sub');

  // Captions Toggle Sync
  state.player.onCaptionsToggle = (isEnabled) => {
    if (textCaptionsStatus) {
      textCaptionsStatus.textContent = isEnabled ? 'CC ON' : 'CC OFF';
    }
    if (btnToggleCaptionsDown) {
      btnToggleCaptionsDown.classList.toggle('text-secondary', isEnabled);
      btnToggleCaptionsDown.classList.toggle('text-gray-400', !isEnabled);
      btnToggleCaptionsDown.classList.toggle('line-through', !isEnabled);
      btnToggleCaptionsDown.classList.toggle('opacity-60', !isEnabled);
    }
    showToast(isEnabled ? 'Captions Enabled' : 'Captions Disabled', 'info');
  };

  // Quality List & Change Sync
  state.player.onQualityList = (levels, currentLevel) => {
    if (!qualityDropdownMenu) return;
    qualityDropdownMenu.innerHTML = levels.map(lvl => {
      const isSelected = lvl.level === currentLevel;
      return `
        <button class="quality-opt-btn px-2.5 py-1.5 text-left rounded-lg text-xs font-mono font-bold transition-all ${isSelected ? 'bg-primary-container text-white' : 'text-gray-300 hover:bg-surface-container-high hover:text-white'}" data-level="${lvl.level}">
          ${lvl.html}
        </button>
      `;
    }).join('');

    qualityDropdownMenu.querySelectorAll('.quality-opt-btn').forEach(btn => {
      btn.addEventListener('click', (e) => {
        e.stopPropagation();
        const lvl = parseInt(btn.dataset.level);
        state.player.setQuality(lvl);
        qualityDropdownMenu.classList.add('hidden');
      });
    });
  };

  state.player.onQualityChange = (levelIndex, label) => {
    if (textCurrentQuality) {
      textCurrentQuality.textContent = `Quality: ${label}`;
    }
    if (qualityDropdownMenu) {
      qualityDropdownMenu.querySelectorAll('.quality-opt-btn').forEach(btn => {
        const isMatch = parseInt(btn.dataset.level) === levelIndex;
        btn.className = `quality-opt-btn px-2.5 py-1.5 text-left rounded-lg text-xs font-mono font-bold transition-all ${isMatch ? 'bg-primary-container text-white' : 'text-gray-300 hover:bg-surface-container-high hover:text-white'}`;
      });
    }
    showToast(`Streaming Quality: ${label}`, 'info');
  };

  // Stop video & audio when user unloads or leaves tab
  window.addEventListener('beforeunload', () => { if (state.player) state.player.stop(); });
  window.addEventListener('pagehide', () => { if (state.player) state.player.stop(); });

  // Progress sync debouncer
  let lastSyncTime = 0;
  state.player.onTimeUpdate = (currentTime, duration) => {
    const now = Date.now();
    if (now - lastSyncTime > 4000 && state.currentAnime) {
      lastSyncTime = now;
      syncProgressToBackend(currentTime, duration);
    }
  };

  // Build Genre Checkboxes in Browse Filter Sidebar
  buildGenreMatrix();

  // Safe initial default for hero play button
  if (heroPlayBtn) {
    heroPlayBtn.onclick = () => navigateTo('watch?anime=20&ep=1');
  }

  // Initialize Auth & Data
  initAuth().then(() => {
    initRouting();
    loadHomeFeed();
    loadContinueWatching();
  });

  // ============================================================================
  // AUTHENTICATION MODULE
  // ============================================================================

  async function initAuth() {
    if (!state.auth.token) {
      updateAuthUI(null);
      return;
    }

    try {
      const res = await fetch('/api/auth/me', {
        headers: { 'Authorization': `Bearer ${state.auth.token}` }
      });
      const data = await res.json();

      if (data.success && data.user) {
        state.auth.user = data.user;
        updateAuthUI(data.user);
        loadUserSettings();
      } else {
        // Token invalid or expired
        clearAuth();
      }
    } catch (err) {
      console.warn('[Auth] Check failed:', err);
      clearAuth();
    }
  }

  function clearAuth() {
    state.auth.token = null;
    state.auth.user = null;
    localStorage.removeItem('omcli_token');
    localStorage.removeItem('kuronami_token');
    updateAuthUI(null);
  }

  function setAuth(token, user) {
    state.auth.token = token;
    state.auth.user = user;
    localStorage.setItem('omcli_token', token);
    updateAuthUI(user);
    loadUserSettings();
    loadContinueWatching();
    showToast(`Welcome back, ${user.username}!`, 'success');
  }

  function updateAuthUI(user) {
    if (user) {
      navGuestBlock.classList.add('hidden');
      navUserBlock.classList.remove('hidden');

      const avatar = user.avatar || 'https://api.dicebear.com/7.x/bottts-neutral/svg?seed=Alex';
      navAvatarImg.src = avatar;
      navUserName.textContent = user.username;
      navUserTier.textContent = `LVL ${user.level || 42} OTAKU`;
      flyoutUserName.textContent = user.username;
      flyoutUserBadge.textContent = '100% FREE';
      flyoutUserEmail.textContent = user.email || `${user.username.toLowerCase()}@omcli.io`;

      // Update Account View Info
      accountAvatarImg.src = avatar;
      accountLevelBadge.innerHTML = `<span class="material-symbols-outlined text-[12px]">military_tech</span> LVL ${user.level || 42}`;
      accountUserName.textContent = user.username;
      accountHandle.textContent = `@${user.username.toLowerCase().replace(/\s+/g, '_')}`;
      accountTierBadge.innerHTML = `<span class="material-symbols-outlined text-[14px]">verified</span> Community Otaku`;
      accountEmail.textContent = user.email || `${user.username.toLowerCase()}@omcli.io`;
      const accountTokenId = document.getElementById('account-token-id');
      if (accountTokenId) accountTokenId.textContent = `#USER-${user.id || 'SYNC'}`;
      accInputUsername.value = user.username;
      accInputEmail.value = user.email || '';
    } else {
      navGuestBlock.classList.remove('hidden');
      navUserBlock.classList.add('hidden');
    }
  }

  async function loadUserSettings() {
    if (!state.auth.token) return;
    try {
      const res = await fetch('/api/user/settings', {
        headers: { 'Authorization': `Bearer ${state.auth.token}` }
      });
      const json = await res.json();
      if (json.success && json.data) {
        state.auth.settings = json.data;
        state.currentMode = json.data.preferred_audio || 'sub';
        updateAudioButtons(state.currentMode);

        if (prefAudio) prefAudio.value = json.data.preferred_audio || 'sub';
        if (prefQuality) prefQuality.value = json.data.preferred_quality || 'auto';
        if (prefSkipIntro) prefSkipIntro.checked = !!json.data.auto_skip_intro;
        if (prefAutoNext) prefAutoNext.checked = !!json.data.auto_next;
      }
    } catch (err) {
      console.warn('[Settings] Load failed:', err);
    }
  }

  // Auth Modal Listeners
  btnSignInNav.addEventListener('click', () => openAuthModal('login'));
  authModalClose.addEventListener('click', closeAuthModal);
  authModal.addEventListener('click', (e) => {
    if (e.target === authModal) closeAuthModal();
  });

  tabAuthLogin.addEventListener('click', () => switchAuthTab('login'));
  tabAuthRegister.addEventListener('click', () => switchAuthTab('register'));

  function openAuthModal(mode = 'login') {
    authAlertBox.classList.add('hidden');
    authAlertBox.textContent = '';
    switchAuthTab(mode);
    authModal.classList.remove('hidden');
  }

  function closeAuthModal() {
    authModal.classList.add('hidden');
  }

  function switchAuthTab(mode) {
    if (mode === 'login') {
      tabAuthLogin.className = 'flex-1 py-2 rounded-lg font-mono text-xs font-bold text-white bg-surface-container-high shadow-sm transition-all';
      tabAuthRegister.className = 'flex-1 py-2 rounded-lg font-mono text-xs font-bold text-gray-400 hover:text-white transition-all';
      formAuthLogin.classList.remove('hidden');
      formAuthRegister.classList.add('hidden');
    } else {
      tabAuthRegister.className = 'flex-1 py-2 rounded-lg font-mono text-xs font-bold text-white bg-surface-container-high shadow-sm transition-all';
      tabAuthLogin.className = 'flex-1 py-2 rounded-lg font-mono text-xs font-bold text-gray-400 hover:text-white transition-all';
      formAuthRegister.classList.remove('hidden');
      formAuthLogin.classList.add('hidden');
    }
  }

  // 1-Click Demo Login (Alex Mercer)
  btnAuthDemo.addEventListener('click', async () => {
    try {
      btnAuthDemo.disabled = true;
      btnAuthDemo.innerHTML = '<span class="material-symbols-outlined text-[16px] animate-spin">refresh</span> Logging in as Alex Mercer...';

      const res = await fetch('/api/auth/demo', { method: 'POST' });
      const json = await res.json();

      if (json.success && json.token) {
        setAuth(json.token, json.user);
        closeAuthModal();
      } else {
        throw new Error(json.error || 'Demo login failed');
      }
    } catch (err) {
      showAuthAlert(err.message);
    } finally {
      btnAuthDemo.disabled = false;
      btnAuthDemo.innerHTML = '<span class="material-symbols-outlined text-[16px]">bolt</span> 1-Click Demo Login (Alex Mercer - LVL 42)';
    }
  });

  // Login Form Submit
  formAuthLogin.addEventListener('submit', async (e) => {
    e.preventDefault();
    const identity = loginInputIdentity.value.trim();
    const password = loginInputPassword.value;

    try {
      const res = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ login: identity, password })
      });
      const json = await res.json();

      if (json.success && json.token) {
        setAuth(json.token, json.user);
        closeAuthModal();
      } else {
        throw new Error(json.error || 'Invalid credentials');
      }
    } catch (err) {
      showAuthAlert(err.message);
    }
  });

  // Register Form Submit
  formAuthRegister.addEventListener('submit', async (e) => {
    e.preventDefault();
    const username = regInputUsername.value.trim();
    const email = regInputEmail.value.trim();
    const password = regInputPassword.value;

    try {
      const res = await fetch('/api/auth/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, email, password })
      });
      const json = await res.json();

      if (json.success && json.token) {
        setAuth(json.token, json.user);
        closeAuthModal();
      } else {
        throw new Error(json.error || 'Registration failed');
      }
    } catch (err) {
      showAuthAlert(err.message);
    }
  });

  function showAuthAlert(msg) {
    authAlertBox.textContent = msg;
    authAlertBox.classList.remove('hidden');
  }

  // Logout Handlers
  btnLogout.addEventListener('click', handleLogout);
  btnAccountLogout.addEventListener('click', handleLogout);
  btnAccLogoutBottom.addEventListener('click', handleLogout);

  async function handleLogout() {
    if (state.auth.token) {
      try {
        await fetch('/api/auth/logout', {
          method: 'POST',
          headers: { 'Authorization': `Bearer ${state.auth.token}` }
        });
      } catch {}
    }
    clearAuth();
    showToast('Logged out successfully', 'info');
    navigateTo('home');
  }

  // ============================================================================
  // ROUTING & SPA VIEW MANAGEMENT
  // ============================================================================

  function initRouting() {
    window.addEventListener('hashchange', handleRoute);
    navLogoBtn.addEventListener('click', (e) => { e.preventDefault(); navigateTo('home'); });

    navTabs.forEach(tab => {
      tab.addEventListener('click', (e) => {
        e.preventDefault();
        const target = tab.dataset.nav;
        navigateTo(target);
      });
    });

    mobileNavBtns.forEach(btn => {
      btn.addEventListener('click', (e) => {
        e.preventDefault();
        const target = btn.dataset.nav;
        navigateTo(target);
      });
    });

    // Initial Route
    handleRoute();
  }

  function navigateTo(hashPath) {
    window.location.hash = hashPath;
  }

  function handleRoute() {
    const rawHash = window.location.hash.slice(1) || 'home';
    const [path, queryStr] = rawHash.split('?');
    const params = new URLSearchParams(queryStr || '');

    // STOP AUDIO / VIDEO PLAYBACK IMMEDIATELY IF LEAVING WATCH VIEW!
    if (path !== 'watch' && state.player) {
      state.player.stop();
      state.currentAnime = null;
    }

    // Auto-close any mobile overlays on route navigation
    if (mobileSearchModal) {
      mobileSearchModal.classList.add('hidden');
      mobileSearchModal.classList.remove('flex');
    }
    if (browseFilterSidebar) {
      browseFilterSidebar.classList.add('hidden');
      browseFilterSidebar.classList.remove('flex');
    }
    if (browseFilterDrawerBackdrop) {
      browseFilterDrawerBackdrop.classList.add('hidden');
    }
    document.body.classList.remove('overflow-hidden');

    // Hide all views
    Object.values(views).forEach(v => {
      if (v) v.classList.add('hidden');
    });

    // Update active desktop nav tabs
    navTabs.forEach(tab => {
      const isTarget = tab.dataset.nav === path || 
        (path.startsWith('account') && tab.dataset.nav === 'account') ||
        (path === 'watchlist' && tab.dataset.nav === 'watchlist');
      
      if (isTarget) {
        tab.className = 'nav-tab px-3.5 py-2 rounded-lg text-sm font-semibold text-white bg-surface-container-high transition-all';
      } else {
        tab.className = 'nav-tab px-3.5 py-2 rounded-lg text-sm font-medium text-gray-400 hover:text-white hover:bg-surface-container transition-all';
      }
    });

    // Update active mobile bottom nav buttons
    mobileNavBtns.forEach(btn => {
      const isTarget = btn.dataset.nav === path || 
        (path.startsWith('account') && btn.dataset.nav === 'account') ||
        (path === 'watchlist' && btn.dataset.nav === 'watchlist');
      
      if (isTarget) {
        btn.classList.add('active');
        btn.classList.remove('text-gray-400');
        btn.classList.add('text-primary');
      } else {
        btn.classList.remove('active');
        btn.classList.remove('text-primary');
        btn.classList.add('text-gray-400');
      }
    });

    state.activeView = path;
    window.scrollTo({ top: 0, behavior: 'smooth' });

    if (path === 'home') {
      views.home.classList.remove('hidden');
      loadContinueWatching();
    } else if (path === 'browse') {
      views.browse.classList.remove('hidden');
      const searchParam = params.get('search');
      const genreParam = params.get('genre');
      const sortParam = params.get('sort');
      const formatParam = params.get('format');
      const statusParam = params.get('status');

      if (searchParam) {
        browseSearchInput.value = searchParam;
        state.browseFilters.search = searchParam;
      } else {
        browseSearchInput.value = '';
        state.browseFilters.search = '';
      }

      if (genreParam) {
        state.browseFilters.genre = genreParam;
        filterGenresMatrix.querySelectorAll('.genre-matrix-chk').forEach(c => {
          c.checked = (c.value.toLowerCase() === genreParam.toLowerCase());
        });
      }

      if (sortParam) {
        state.browseFilters.sort = sortParam;
        browseSortSelect.value = sortParam;
        if (mobileBrowseSortSelect) mobileBrowseSortSelect.value = sortParam;
      }

      if (formatParam) {
        state.browseFilters.format = formatParam;
        filterFormatGroup.querySelectorAll('.filter-btn').forEach(b => {
          if (b.dataset.format && b.dataset.format.toLowerCase() === formatParam.toLowerCase()) {
            b.className = 'filter-btn active px-2.5 py-1.5 rounded-lg bg-surface-container-high border border-primary/30 text-white font-mono text-[11px] font-bold';
          } else {
            b.className = 'filter-btn px-2.5 py-1.5 rounded-lg bg-surface-container-low border border-white/[0.06] text-gray-400 hover:text-white font-mono text-[11px] font-bold';
          }
        });
      }

      if (statusParam) {
        state.browseFilters.status = statusParam;
        filterStatusGroup.querySelectorAll('.filter-btn').forEach(b => {
          if (b.dataset.status && b.dataset.status.toLowerCase() === statusParam.toLowerCase()) {
            b.className = 'filter-btn active px-2.5 py-1.5 rounded-lg bg-surface-container-high border border-primary/30 text-white font-mono text-[11px] font-bold';
          } else {
            b.className = 'filter-btn px-2.5 py-1.5 rounded-lg bg-surface-container-low border border-white/[0.06] text-gray-400 hover:text-white font-mono text-[11px] font-bold';
          }
        });
      }

      executeBrowse();
    } else if (path === 'watch') {
      views.watch.classList.remove('hidden');
      const animeId = params.get('anime') || params.get('id');
      const epNum = parseInt(params.get('ep')) || 1;
      const mode = params.get('mode') || state.currentMode || 'sub';
      const startTime = parseFloat(params.get('t')) || 0;
      if (animeId) {
        if (state.currentAnime && String(state.currentAnime.id) === String(animeId)) {
          // Anime already loaded in current watch room, simply switch episode
          if (state.currentEpisodeIndex !== epNum - 1 || state.currentMode !== mode) {
            state.currentMode = mode;
            window.playEpisodeAtIndex(epNum - 1, startTime);
          }
        } else {
          loadWatchRoom(animeId, epNum, mode, startTime);
        }
      } else {
        navigateTo('home');
      }
    } else if (path === 'watchlist') {
      views.account.classList.remove('hidden');
      switchAccountTab('watchlist');
      loadAccountData();
    } else if (path === 'account' || path.startsWith('account-')) {
      views.account.classList.remove('hidden');
      const sub = path.replace('account-', '') || 'watchlist';
      switchAccountTab(sub);
      loadAccountData();
    } else {
      views.home.classList.remove('hidden');
    }
  }

  // ============================================================================
  // AUDIO MODE CONTROLS (SUB / DUB) & FULLSCREEN
  // ============================================================================

  quickSubBtn.addEventListener('click', () => switchGlobalAudio('sub'));
  quickDubBtn.addEventListener('click', () => switchGlobalAudio('dub'));
  btnStreamSub.addEventListener('click', () => switchGlobalAudio('sub'));
  btnStreamDub.addEventListener('click', () => switchGlobalAudio('dub'));

  // Down Audio Controls
  if (btnStreamSubDown) btnStreamSubDown.addEventListener('click', () => switchGlobalAudio('sub'));
  if (btnStreamDubDown) btnStreamDubDown.addEventListener('click', () => switchGlobalAudio('dub'));

  // Down Captions Disable/Enable Button
  if (btnToggleCaptionsDown) {
    btnToggleCaptionsDown.addEventListener('click', () => {
      if (state.player) {
        state.player.toggleCaptions();
      }
    });
  }

  // Down Streaming Quality Selector Menu
  if (btnQualityDown && qualityDropdownMenu) {
    btnQualityDown.addEventListener('click', (e) => {
      e.stopPropagation();
      qualityDropdownMenu.classList.toggle('hidden');
    });

    document.addEventListener('click', (e) => {
      if (qualitySelectorWrap && !qualitySelectorWrap.contains(e.target)) {
        qualityDropdownMenu.classList.add('hidden');
      }
    });
  }

  // Down Fullscreen ("Make Again Full Big") Button
  if (btnFullscreenDown) {
    btnFullscreenDown.addEventListener('click', () => {
      if (state.player && typeof state.player.toggleFullscreen === 'function') {
        state.player.toggleFullscreen();
      } else if (state.player && state.player.art) {
        state.player.art.fullscreen = !state.player.art.fullscreen;
      }
    });
  }

  function switchGlobalAudio(mode) {
    state.currentMode = mode;
    updateAudioButtons(mode);

    // If currently playing in watch room, reload stream
    if (state.activeView === 'watch' && state.currentAnime) {
      const episodes = state.currentAnime.episodes || [];
      const ep = episodes[state.currentEpisodeIndex];
      if (ep) {
        state.player.loadStream(ep.id, mode, state.currentAnime.bannerImage || state.currentAnime.coverImage?.extraLarge);
      }
    }
  }

  function updateAudioButtons(mode) {
    const isSub = mode === 'sub';

    // Quick Navbar switch
    quickSubBtn.className = isSub 
      ? 'px-2.5 py-1 rounded text-xs font-mono font-bold transition-all bg-secondary-container text-on-secondary-container shadow-sm' 
      : 'px-2.5 py-1 rounded text-xs font-mono font-bold transition-all text-gray-400 hover:text-white';
    quickDubBtn.className = !isSub 
      ? 'px-2.5 py-1 rounded text-xs font-mono font-bold transition-all bg-secondary-container text-on-secondary-container shadow-sm' 
      : 'px-2.5 py-1 rounded text-xs font-mono font-bold transition-all text-gray-400 hover:text-white';

    // Top watch room switch
    btnStreamSub.className = isSub
      ? 'flex items-center gap-1.5 px-3 py-1.5 rounded-md bg-secondary text-on-secondary font-mono text-xs font-bold shadow-sm transition-all'
      : 'flex items-center gap-1.5 px-3 py-1.5 rounded-md text-gray-400 hover:text-white font-mono text-xs font-bold transition-all';
    btnStreamDub.className = !isSub
      ? 'flex items-center gap-1.5 px-3 py-1.5 rounded-md bg-secondary text-on-secondary font-mono text-xs font-bold shadow-sm transition-all'
      : 'flex items-center gap-1.5 px-3 py-1.5 rounded-md text-gray-400 hover:text-white font-mono text-xs font-bold transition-all';

    // Down watch room switch
    if (btnStreamSubDown && btnStreamDubDown) {
      btnStreamSubDown.className = isSub
        ? 'flex items-center gap-1 px-2.5 py-1 rounded-md bg-secondary text-on-secondary font-mono text-xs font-bold shadow-sm transition-all'
        : 'flex items-center gap-1 px-2.5 py-1 rounded-md text-gray-400 hover:text-white font-mono text-xs font-bold transition-all';
      btnStreamDubDown.className = !isSub
        ? 'flex items-center gap-1 px-2.5 py-1 rounded-md bg-secondary text-on-secondary font-mono text-xs font-bold shadow-sm transition-all'
        : 'flex items-center gap-1 px-2.5 py-1 rounded-md text-gray-400 hover:text-white font-mono text-xs font-bold transition-all';
    }
  }

  // ============================================================================
  // HOME DISCOVERY MODULE
  // ============================================================================

  async function loadHomeFeed() {
    // 1. Trending Feed & Spotlight
    try {
      let trendingItems = [];
      const res = await fetch('/api/trending').catch(() => null);
      if (res && res.ok) {
        const json = await res.json().catch(() => ({}));
        if (json.success && Array.isArray(json.data) && json.data.length > 0) {
          trendingItems = json.data;
        }
      }
      if (!trendingItems.length && window.AniListClient) {
        trendingItems = await window.AniListClient.getTrending(1, 20).catch(() => []);
      }

      if (trendingItems.length > 0) {
        state.rawTrendingList = trendingItems;
        state.spotlightList = trendingItems.slice(0, 5);
        initHeroSpotlight();
        renderAnimeCards(trendingGrid, trendingItems);
      }
    } catch (err) {
      console.error('[Feed] Trending error:', err);
    }

    // 2. Popular Feed
    (async () => {
      try {
        let popItems = [];
        const res = await fetch('/api/popular').catch(() => null);
        if (res && res.ok) {
          const json = await res.json().catch(() => ({}));
          if (json.success && Array.isArray(json.data) && json.data.length > 0) popItems = json.data;
        }
        if (!popItems.length && window.AniListClient) {
          popItems = await window.AniListClient.getPopular(1, 20).catch(() => []);
        }
        if (popItems.length > 0) renderAnimeCards(popularGrid, popItems);
      } catch (e) {
        console.error('[Feed] Popular error:', e);
      }
    })();

    // 3. Top Rated Feed
    (async () => {
      try {
        let topItems = [];
        const res = await fetch('/api/top-rated').catch(() => null);
        if (res && res.ok) {
          const json = await res.json().catch(() => ({}));
          if (json.success && Array.isArray(json.data) && json.data.length > 0) topItems = json.data;
        }
        if (!topItems.length && window.AniListClient) {
          topItems = await window.AniListClient.getTopRated(1, 20).catch(() => []);
        }
        if (topItems.length > 0) renderAnimeCards(topRatedGrid, topItems);
      } catch (e) {
        console.error('[Feed] Top rated error:', e);
      }
    })();
  }

  function initHeroSpotlight() {
    if (!state.spotlightList.length) return;
    renderHeroSpotlight(0);

    // Build indicator dots
    heroIndicators.innerHTML = state.spotlightList.map((_, idx) => `
      <button class="hero-dot w-6 h-1.5 rounded-full ${idx === 0 ? 'bg-primary-container w-10' : 'bg-surface-container-high'} transition-all cursor-pointer" data-index="${idx}"></button>
    `).join('');

    heroIndicators.querySelectorAll('.hero-dot').forEach(btn => {
      btn.addEventListener('click', () => {
        clearInterval(state.spotlightTimer);
        const idx = parseInt(btn.dataset.index);
        renderHeroSpotlight(idx);
        startSpotlightTimer();
      });
    });

    startSpotlightTimer();
  }

  function startSpotlightTimer() {
    clearInterval(state.spotlightTimer);
    state.spotlightTimer = setInterval(() => {
      state.spotlightIndex = (state.spotlightIndex + 1) % state.spotlightList.length;
      renderHeroSpotlight(state.spotlightIndex);
    }, 8000);
  }

  function renderHeroSpotlight(index) {
    const item = state.spotlightList[index];
    if (!item) return;

    state.spotlightIndex = index;

    // Update Indicators
    heroIndicators.querySelectorAll('.hero-dot').forEach((btn, idx) => {
      if (idx === index) {
        btn.className = 'hero-dot h-1.5 rounded-full bg-primary-container w-10 transition-all';
      } else {
        btn.className = 'hero-dot h-1.5 rounded-full bg-surface-container-high w-6 transition-all';
      }
    });

    const backdropUrl = item.bannerImage || item.coverImage?.extraLarge || '';
    if (heroBackdrop) heroBackdrop.style.backgroundImage = `url('${backdropUrl}')`;
    if (heroTitle) heroTitle.textContent = item.title?.english || item.title?.romaji || 'Anime Showcase';
    if (heroStudioMeta) heroStudioMeta.textContent = `${item.studios?.nodes?.[0]?.name || 'Studio Animation'} • Winter Arc`;
    if (heroScore) heroScore.innerHTML = `<span class="material-symbols-outlined text-[16px]" style="font-variation-settings: 'FILL' 1;">star</span> ${(item.averageScore ? (item.averageScore / 10).toFixed(1) : '8.8')} RATING`;
    if (heroYear) heroYear.textContent = item.seasonYear || item.startDate?.year || '2025';
    if (heroFormat) heroFormat.textContent = item.format || 'TV SERIES';
    if (heroEpisodes) heroEpisodes.textContent = `${item.episodes || 24} Episodes`;

    const genres = item.genres || ['Action', 'Fantasy'];
    if (heroGenres) {
      heroGenres.innerHTML = genres.slice(0, 3).map(g => `
        <span class="px-2 py-0.5 rounded bg-surface-container text-white text-xs">${g}</span>
      `).join('');
    }

    const cleanDesc = (item.description || 'Denji finds himself unexpectedly enchanted in a neon Tokyo underworld as deadly secrets ignite an epic clash.')
      .replace(/<[^>]*>/g, '');
    if (heroDesc) heroDesc.textContent = cleanDesc;

    // Watermark Japanese Title
    const watermarkEl = document.getElementById('hero-watermark');
    if (watermarkEl) {
      watermarkEl.textContent = item.title?.native || '黒波アニメ';
    }

    // Play CTA
    heroPlayBtn.onclick = () => {
      navigateTo(`watch?anime=${item.id}&ep=1`);
    };

    // Watchlist CTA
    heroWatchlistBtn.onclick = () => {
      toggleWatchlistAnime(item);
    };

    heroInfoBtn.onclick = () => {
      navigateTo(`watch?anime=${item.id}&ep=1`);
    };
  }

  // Genre Filter Bar on Home Feed
  genreBar.addEventListener('click', (e) => {
    const chip = e.target.closest('.genre-chip');
    if (!chip) return;

    const wasActive = chip.classList.contains('active');
    const genre = chip.dataset.genre || 'All';

    // If already active and clicked again on a specific genre, jump straight to full Browse catalog!
    if (wasActive && genre !== 'All') {
      navigateTo(`browse?genre=${encodeURIComponent(genre)}`);
      return;
    }

    genreBar.querySelectorAll('.genre-chip').forEach(c => {
      c.className = 'genre-chip px-4 py-1.5 rounded-full bg-surface-container-low border border-white/[0.06] text-gray-300 hover:bg-surface-container font-mono text-xs font-bold tracking-wide transition-all whitespace-nowrap';
    });
    chip.className = 'genre-chip active px-4 py-1.5 rounded-full bg-primary-container text-white font-mono text-xs font-bold tracking-wide transition-all whitespace-nowrap shadow-sm';

    if (genre === 'All') {
      renderAnimeCards(trendingGrid, state.rawTrendingList);
    } else {
      const gNorm = genre.toLowerCase();
      const filtered = state.rawTrendingList.filter(a => {
        const gList = (a.genres || []).map(g => g.toLowerCase());
        if (gList.includes(gNorm)) return true;
        if (gNorm === 'shounen') {
          const desc = (a.description || '').toLowerCase();
          return desc.includes('shounen') || desc.includes('shonen') || gList.includes('action') || gList.includes('adventure');
        }
        return false;
      });

      if (filtered.length > 0) {
        renderAnimeCards(trendingGrid, filtered);
      } else {
        // No items in memory trending feed, navigate directly to full catalog in browse
        navigateTo(`browse?genre=${encodeURIComponent(genre)}`);
      }
    }
  });

  // Continue Watching Rail Loading
  async function loadContinueWatching() {
    let history = [];

    if (state.auth.token) {
      try {
        const res = await fetch('/api/user/history', {
          headers: { 'Authorization': `Bearer ${state.auth.token}` }
        });
        const json = await res.json();
        if (json.success && Array.isArray(json.data)) {
          history = json.data;
          state.userHistory = history;
        }
      } catch (err) {
        console.warn('[History] Fetch failed:', err);
      }
    } else {
      // Guest mode fallback: check local progress keys only when not authenticated
      const localProgressKeys = Object.keys(localStorage).filter(k => k.startsWith('ani_progress_'));
      if (localProgressKeys.length) {
        history = localProgressKeys.map(k => {
          try {
            const data = JSON.parse(localStorage.getItem(k));
            const epId = k.replace('ani_progress_', '');
            return {
              anime_id: epId.split('-')[0] || epId,
              anime_title: 'Continue Episode',
              episode_num: 1,
              progress_seconds: data.time || 0,
              duration_seconds: data.duration || 1420
            };
          } catch {
            return null;
          }
        }).filter(Boolean);
      }
    }

    if (history.length > 0) {
      continueSection.classList.remove('hidden');
      if (flyoutHistoryCount) flyoutHistoryCount.textContent = history.length;
      renderContinueWatchingRail(history);
    } else {
      continueSection.classList.add('hidden');
      continueGrid.innerHTML = '';
      if (flyoutHistoryCount) flyoutHistoryCount.textContent = '0';
    }
  }

  function renderContinueWatchingRail(items) {
    continueGrid.innerHTML = items.slice(0, 6).map(item => {
      const progressPercent = item.duration_seconds > 0 
        ? Math.min(100, Math.round((item.progress_seconds / item.duration_seconds) * 100))
        : 0;

      return `
        <div class="relative group bg-surface-container-low border border-white/[0.08] hover:border-primary/50 rounded-2xl overflow-hidden shadow-lg transition-all cursor-pointer flex flex-col" onclick="window.location.hash='watch?anime=${item.anime_id}&ep=${item.episode_num}&t=${item.progress_seconds || 0}'">
          <div class="relative aspect-video w-full bg-surface-container-high overflow-hidden">
            <img src="${item.poster || 'https://via.placeholder.com/300x170'}" alt="${item.anime_title}" class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300">
            <div class="absolute inset-0 bg-gradient-to-t from-surface-container-lowest via-transparent to-transparent"></div>
            <div class="absolute bottom-2 left-2 right-2">
              <span class="px-2 py-0.5 rounded bg-primary-container text-white font-mono text-[10px] font-bold shadow-sm">
                EP ${item.episode_num}
              </span>
            </div>
            <!-- Play overlay -->
            <div class="absolute inset-0 flex items-center justify-center opacity-0 group-hover:opacity-100 bg-black/40 backdrop-blur-[2px] transition-opacity">
              <span class="w-10 h-10 rounded-full bg-primary-container text-white flex items-center justify-center shadow-lg">
                <span class="material-symbols-outlined text-[24px]">play_arrow</span>
              </span>
            </div>
          </div>
          <div class="p-3 flex flex-col gap-1.5 flex-1 justify-between">
            <h3 class="text-xs font-bold text-white line-clamp-1 group-hover:text-primary transition-colors">${item.anime_title}</h3>
            <!-- Progress Bar -->
            <div class="w-full bg-surface-container-high h-1.5 rounded-full overflow-hidden">
              <div class="bg-primary-container h-full rounded-full" style="width: ${progressPercent}%;"></div>
            </div>
            <div class="flex items-center justify-between text-[10px] font-mono text-gray-400">
              <span>${progressPercent}% Complete</span>
              <span class="text-secondary font-bold">Resume</span>
            </div>
          </div>
        </div>
      `;
    }).join('');
  }

  // Generic Card Renderer
  function renderAnimeCards(container, list) {
    if (!list || !list.length) {
      container.innerHTML = '<div class="col-span-full py-8 text-center text-gray-500 font-mono text-xs">No entries found.</div>';
      return;
    }

    container.innerHTML = list.map(item => {
      const title = item.title?.english || item.title?.romaji || 'Anime';
      const poster = item.coverImage?.extraLarge || item.coverImage?.large || '';
      const score = item.averageScore ? (item.averageScore / 10).toFixed(1) : '8.5';
      const format = item.format || 'TV';
      const epCount = item.episodes ? `${item.episodes} EP` : 'Simulcast';

      return `
        <div class="anime-card group relative bg-surface-container-low border border-white/[0.08] hover:border-primary/50 rounded-2xl overflow-hidden shadow-lg transition-all duration-300 flex flex-col cursor-pointer" onclick="window.location.hash='watch?anime=${item.id}&ep=1'">
          <div class="relative aspect-[2/3] w-full bg-surface-container-high overflow-hidden">
            <img src="${poster}" alt="${title}" loading="lazy" class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500">
            <div class="absolute inset-0 bg-gradient-to-t from-surface-container-lowest via-transparent to-transparent opacity-90"></div>
            
            <!-- Badges -->
            <div class="absolute top-2 left-2 flex items-center gap-1">
              <span class="px-2 py-0.5 rounded bg-black/60 backdrop-blur-md text-white font-mono text-[10px] font-bold border border-white/[0.1]">
                ${format}
              </span>
              <span class="px-1.5 py-0.5 rounded bg-secondary-container/90 text-on-secondary-container font-mono text-[9px] font-bold">
                SUB • DUB
              </span>
            </div>

            <div class="absolute top-2 right-2">
              <button class="w-8 h-8 rounded-full bg-black/70 backdrop-blur-md text-gray-300 hover:text-primary active:scale-90 flex items-center justify-center transition-all shadow-md cursor-pointer" title="Add to Watchlist" onclick="event.stopPropagation(); window.toggleWatchlistAnimeById('${item.id}', '${escapeQuotes(title)}', '${poster}')">
                <span class="material-symbols-outlined text-[17px]">bookmark_add</span>
              </button>
            </div>

            <div class="absolute bottom-2 left-2 right-2 flex items-center justify-between text-xs font-mono font-bold">
              <span class="text-primary flex items-center gap-0.5">
                <span class="material-symbols-outlined text-[14px]" style="font-variation-settings: 'FILL' 1;">star</span>
                ${score}
              </span>
              <span class="text-gray-400 text-[10px]">${epCount}</span>
            </div>
          </div>

          <div class="p-3 flex flex-col gap-1">
            <h3 class="text-xs font-bold text-white line-clamp-1 group-hover:text-primary transition-colors">${title}</h3>
            <span class="text-[10px] font-mono text-gray-500 line-clamp-1">${(item.genres || []).slice(0, 2).join(' • ')}</span>
          </div>
        </div>
      `;
    }).join('');
  }

  // ============================================================================
  // ADVANCED SEARCH & BROWSE MODULE
  // ============================================================================

  function buildGenreMatrix() {
    filterGenresMatrix.innerHTML = state.genresList.map(g => {
      const isChecked = state.browseFilters.genre && state.browseFilters.genre.toLowerCase() === g.toLowerCase();
      return `
        <label class="flex items-center gap-2 p-1.5 rounded-lg bg-surface-container hover:bg-surface-container-high transition-colors cursor-pointer text-xs text-gray-300">
          <input type="checkbox" value="${g}" ${isChecked ? 'checked' : ''} class="genre-matrix-chk rounded bg-surface-container-high border-white/[0.1] text-primary focus:ring-0">
          <span>${g}</span>
        </label>
      `;
    }).join('');

    filterGenresMatrix.querySelectorAll('.genre-matrix-chk').forEach(chk => {
      chk.addEventListener('change', () => {
        executeBrowse();
      });
    });
  }

  // Browse Search Handlers
  let browseDebounce = null;
  browseSearchInput.addEventListener('input', (e) => {
    clearTimeout(browseDebounce);
    state.browseFilters.search = e.target.value.trim();
    browseDebounce = setTimeout(() => executeBrowse(), 350);
  });

  browseClearBtn.addEventListener('click', () => {
    browseSearchInput.value = '';
    state.browseFilters.search = '';
    executeBrowse();
  });

  browseSearchInput.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') {
      clearTimeout(browseDebounce);
      executeBrowse();
    }
  });

  browseSortSelect.addEventListener('change', (e) => {
    state.browseFilters.sort = e.target.value;
    executeBrowse();
  });

  // View Mode Toggles
  viewModeGrid.addEventListener('click', () => setViewMode('grid'));
  viewModeDetailed.addEventListener('click', () => setViewMode('detailed'));
  viewModeCompact.addEventListener('click', () => setViewMode('compact'));

  function setViewMode(mode) {
    state.browseFilters.viewMode = mode;
    [viewModeGrid, viewModeDetailed, viewModeCompact].forEach(b => {
      b.className = 'p-1.5 rounded-lg text-gray-400 hover:text-white transition-all';
    });

    if (mode === 'grid') viewModeGrid.className = 'p-1.5 rounded-lg bg-surface-container-high text-secondary shadow-sm transition-all';
    if (mode === 'detailed') viewModeDetailed.className = 'p-1.5 rounded-lg bg-surface-container-high text-secondary shadow-sm transition-all';
    if (mode === 'compact') viewModeCompact.className = 'p-1.5 rounded-lg bg-surface-container-high text-secondary shadow-sm transition-all';

    executeBrowse();
  }

  // Format Filter
  filterFormatGroup.addEventListener('click', (e) => {
    const btn = e.target.closest('.filter-btn');
    if (!btn) return;
    filterFormatGroup.querySelectorAll('.filter-btn').forEach(b => {
      b.className = 'filter-btn px-2.5 py-1.5 rounded-lg bg-surface-container-low border border-white/[0.06] text-gray-400 hover:text-white font-mono text-[11px] font-bold';
    });
    btn.className = 'filter-btn active px-2.5 py-1.5 rounded-lg bg-surface-container-high border border-primary/30 text-white font-mono text-[11px] font-bold';
    state.browseFilters.format = btn.dataset.format;
    executeBrowse();
  });

  // Status Filter
  filterStatusGroup.addEventListener('click', (e) => {
    const btn = e.target.closest('.filter-btn');
    if (!btn) return;
    filterStatusGroup.querySelectorAll('.filter-btn').forEach(b => {
      b.className = 'filter-btn px-2.5 py-1.5 rounded-lg bg-surface-container-low border border-white/[0.06] text-gray-400 hover:text-white font-mono text-[11px] font-bold';
    });
    btn.className = 'filter-btn active px-2.5 py-1.5 rounded-lg bg-surface-container-high border border-primary/30 text-white font-mono text-[11px] font-bold';
    state.browseFilters.status = btn.dataset.status;
    executeBrowse();
  });

  // Quick Chips in Browse
  browseQuickChips.addEventListener('click', (e) => {
    const chip = e.target.closest('.browse-chip');
    if (!chip) return;
    browseQuickChips.querySelectorAll('.browse-chip').forEach(c => {
      c.className = 'browse-chip px-3 py-1 rounded-full bg-surface-container-low border border-white/[0.06] text-gray-300 hover:bg-surface-container font-mono text-[11px] font-bold whitespace-nowrap';
    });
    chip.className = 'browse-chip active px-3 py-1 rounded-full bg-primary-container text-white font-mono text-[11px] font-bold whitespace-nowrap';

    const preset = chip.dataset.preset;
    if (preset === 'top_rated') state.browseFilters.sort = 'score';
    if (preset === 'trending') state.browseFilters.sort = 'popularity';
    if (preset === 'movies') state.browseFilters.format = 'MOVIE';
    if (preset === 'completed') state.browseFilters.status = 'FINISHED';

    executeBrowse();
  });

  browseResetFilters.addEventListener('click', () => {
    state.browseFilters = {
      search: '',
      sort: 'score',
      format: 'all',
      status: 'all',
      genre: 'All',
      viewMode: state.browseFilters.viewMode,
      preset: 'trending'
    };
    browseSearchInput.value = '';
    browseSortSelect.value = 'score';
    filterGenresMatrix.querySelectorAll('.genre-matrix-chk').forEach(c => c.checked = false);
    executeBrowse();
  });

  async function executeBrowse() {
    browseResultsContainer.innerHTML = '<div class="col-span-full py-12 flex justify-center"><div class="w-8 h-8 rounded-full border-2 border-primary border-t-transparent animate-spin"></div></div>';

    // Gather checked genres
    const selectedGenres = Array.from(filterGenresMatrix.querySelectorAll('.genre-matrix-chk:checked')).map(c => c.value);

    // Update active filter counter badges
    let activeFilterCount = selectedGenres.length;
    if (state.browseFilters.format && state.browseFilters.format !== 'all') activeFilterCount++;
    if (state.browseFilters.status && state.browseFilters.status !== 'all') activeFilterCount++;
    if (mobileFilterCountBadge) mobileFilterCountBadge.textContent = activeFilterCount;
    if (browseResetFilters) browseResetFilters.textContent = `Reset (${activeFilterCount})`;
    if (mobileBrowseSortSelect) mobileBrowseSortSelect.value = state.browseFilters.sort;

    const queryParams = new URLSearchParams();
    if (state.browseFilters.search) queryParams.set('search', state.browseFilters.search);
    if (state.browseFilters.sort) queryParams.set('sort', state.browseFilters.sort);
    if (state.browseFilters.format && state.browseFilters.format !== 'all') queryParams.set('format', state.browseFilters.format);
    if (state.browseFilters.status && state.browseFilters.status !== 'all') queryParams.set('status', state.browseFilters.status);
    if (selectedGenres.length > 0) queryParams.set('genre', selectedGenres.join(','));

    try {
      let browseItems = null;

      // 1. Direct Browser Client (window.AniListClient) - High-speed, unblocked, complete 10,000+ catalog
      if (window.AniListClient) {
        const clientRes = await window.AniListClient.browseAnime({
          search: state.browseFilters.search,
          sort: state.browseFilters.sort,
          format: state.browseFilters.format,
          status: state.browseFilters.status,
          genre: selectedGenres.length > 0 ? selectedGenres : (state.browseFilters.genre || 'All')
        }).catch(err => {
          console.warn('[Browse] Direct AniListClient error, falling back:', err.message);
          return null;
        });

        if (clientRes && Array.isArray(clientRes.items) && clientRes.items.length > 0) {
          browseItems = clientRes.items;
        }
      }

      // 2. Server fallback if direct client failed or returned empty
      if (!browseItems || browseItems.length === 0) {
        const res = await fetch(`/api/browse?${queryParams.toString()}`).catch(() => null);
        if (res && res.ok) {
          const json = await res.json().catch(() => ({}));
          if (json.success && Array.isArray(json.data) && json.data.length > 0) {
            browseItems = json.data;
          }
        }
      }

      if (browseItems && browseItems.length > 0) {
        browseResultsCount.textContent = `SHOWING ${browseItems.length} ENTRIES`;
        renderBrowseResults(browseItems, state.browseFilters.viewMode);
      } else {
        browseResultsContainer.innerHTML = '<div class="col-span-full py-8 text-center text-gray-500 font-mono text-xs">No matching titles found.</div>';
      }
    } catch (err) {
      console.error('[Browse] Fetch error:', err);
      browseResultsContainer.innerHTML = '<div class="col-span-full py-8 text-center text-red-400 font-mono text-xs">Failed to load browse results.</div>';
    }
  }

  function renderBrowseResults(list, viewMode) {
    if (!list.length) {
      browseResultsContainer.innerHTML = '<div class="col-span-full py-12 text-center text-gray-500 font-mono text-sm">No titles match your filter matrix.</div>';
      return;
    }

    if (viewMode === 'grid') {
      browseResultsContainer.className = 'grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 xl:grid-cols-5 gap-3 sm:gap-4';
      renderAnimeCards(browseResultsContainer, list);
    } else if (viewMode === 'detailed') {
      browseResultsContainer.className = 'flex flex-col gap-3 sm:gap-4';
      browseResultsContainer.innerHTML = list.map(item => {
        const title = item.title?.english || item.title?.romaji || 'Anime';
        const poster = item.coverImage?.extraLarge || item.coverImage?.large || '';
        const score = item.averageScore ? (item.averageScore / 10).toFixed(1) : '8.5';
        const cleanDesc = (item.description || 'No synopsis provided.').replace(/<[^>]*>/g, '');

        return `
          <div class="p-3.5 sm:p-4 rounded-2xl bg-surface-container-low border border-white/[0.08] hover:border-primary/50 shadow-xl transition-all flex flex-row items-start gap-3 sm:gap-5 group cursor-pointer" onclick="window.location.hash='watch?anime=${item.id}&ep=1'">
            <img src="${poster}" alt="${title}" class="w-20 sm:w-28 md:w-32 h-28 sm:h-40 md:h-44 object-cover rounded-xl border border-white/[0.08] flex-shrink-0 group-hover:scale-102 transition-transform">
            <div class="flex flex-col justify-between flex-1 gap-2 min-w-0">
              <div class="flex flex-col gap-1">
                <div class="flex items-center justify-between flex-wrap gap-1 sm:gap-2">
                  <h3 class="text-sm sm:text-base md:text-lg font-bold text-white group-hover:text-primary transition-colors truncate">${title}</h3>
                  <span class="font-mono text-xs font-bold text-primary flex items-center gap-1 flex-shrink-0">★ ${score}</span>
                </div>
                <div class="flex items-center gap-1.5 sm:gap-2 font-mono text-[11px] sm:text-xs text-gray-400 flex-wrap">
                  <span class="px-1.5 py-0.5 rounded bg-surface-container-high text-white text-[9px] sm:text-[10px]">${item.format || 'TV'}</span>
                  <span>${item.seasonYear || '2025'}</span>
                  <span>•</span>
                  <span>${item.episodes ? `${item.episodes} Ep` : 'Airing'}</span>
                  <span class="hidden sm:inline">•</span>
                  <span class="hidden sm:inline text-secondary truncate max-w-xs">${(item.genres || []).slice(0, 3).join(', ')}</span>
                </div>
                <p class="text-xs text-gray-300 line-clamp-2 sm:line-clamp-3 leading-relaxed mt-0.5">${cleanDesc}</p>
              </div>

              <div class="flex items-center gap-2 pt-1 flex-wrap">
                <button class="px-3 sm:px-4 py-1.5 sm:py-2 rounded-xl bg-primary-container text-white text-xs font-bold uppercase tracking-wider shadow-md hover:brightness-110 active:scale-95 transition-all flex items-center gap-1 sm:gap-1.5 cursor-pointer" onclick="event.stopPropagation(); window.location.hash='watch?anime=${item.id}&ep=1'">
                  <span class="material-symbols-outlined text-[16px]">play_arrow</span>Play
                </button>
                <button class="px-3 sm:px-3.5 py-1.5 sm:py-2 rounded-xl bg-surface-container-high border border-white/[0.08] text-gray-300 hover:text-white text-xs font-semibold active:scale-95 transition-all flex items-center gap-1 sm:gap-1.5 cursor-pointer" onclick="event.stopPropagation(); window.toggleWatchlistAnimeById('${item.id}', '${escapeQuotes(title)}', '${poster}')">
                  <span class="material-symbols-outlined text-[16px]">bookmark_add</span>Watchlist
                </button>
              </div>
            </div>
          </div>
        `;
      }).join('');
    } else if (viewMode === 'compact') {
      browseResultsContainer.className = 'flex flex-col gap-2';
      browseResultsContainer.innerHTML = list.map((item, idx) => {
        const title = item.title?.english || item.title?.romaji || 'Anime';
        const poster = item.coverImage?.medium || item.coverImage?.large || '';
        const score = item.averageScore ? (item.averageScore / 10).toFixed(1) : '8.5';

        return `
          <div class="px-3.5 py-2 sm:px-4 sm:py-2.5 rounded-xl bg-surface-container-low border border-white/[0.06] hover:border-primary/50 flex items-center justify-between gap-3 sm:gap-4 transition-all cursor-pointer group" onclick="window.location.hash='watch?anime=${item.id}&ep=1'">
            <div class="flex items-center gap-2.5 sm:gap-3 min-w-0">
              <span class="font-mono text-xs text-gray-500 font-bold w-5 sm:w-6 text-center">${idx + 1}</span>
              <img src="${poster}" alt="${title}" class="w-8 sm:w-9 h-11 sm:h-12 object-cover rounded-md flex-shrink-0">
              <div class="flex flex-col min-w-0">
                <span class="text-xs font-bold text-white group-hover:text-primary transition-colors truncate">${title}</span>
                <span class="text-[10px] font-mono text-gray-500">${item.format || 'TV'} • ${item.episodes ? `${item.episodes} EP` : 'Simulcast'}</span>
              </div>
            </div>
            <div class="flex items-center gap-3 sm:gap-4 flex-shrink-0">
              <span class="font-mono text-xs font-bold text-primary">★ ${score}</span>
              <span class="material-symbols-outlined text-gray-400 group-hover:text-white text-[18px]">play_circle</span>
            </div>
          </div>
        `;
      }).join('');
    }
  }

  // Mobile Browse Filter Drawer Handlers
  if (btnMobileFilterOpen) {
    btnMobileFilterOpen.addEventListener('click', () => {
      if (browseFilterSidebar) {
        browseFilterSidebar.classList.remove('hidden');
        browseFilterSidebar.classList.add('flex');
      }
      if (browseFilterDrawerBackdrop) {
        browseFilterDrawerBackdrop.classList.remove('hidden');
      }
      document.body.classList.add('overflow-hidden');
    });
  }

  function closeMobileFilterDrawer() {
    if (browseFilterSidebar) {
      browseFilterSidebar.classList.add('hidden');
      browseFilterSidebar.classList.remove('flex');
    }
    if (browseFilterDrawerBackdrop) {
      browseFilterDrawerBackdrop.classList.add('hidden');
    }
    document.body.classList.remove('overflow-hidden');
  }

  if (btnMobileFilterClose) btnMobileFilterClose.addEventListener('click', closeMobileFilterDrawer);
  if (browseFilterDrawerBackdrop) browseFilterDrawerBackdrop.addEventListener('click', closeMobileFilterDrawer);
  if (btnMobileFilterApply) {
    btnMobileFilterApply.addEventListener('click', () => {
      closeMobileFilterDrawer();
      executeBrowse();
    });
  }

  if (mobileBrowseSortSelect) {
    mobileBrowseSortSelect.addEventListener('change', () => {
      state.browseFilters.sort = mobileBrowseSortSelect.value;
      browseSortSelect.value = mobileBrowseSortSelect.value;
      executeBrowse();
    });
  }

  // ============================================================================
  // CINEMA WATCH ROOM MODULE
  // ============================================================================

  async function loadWatchRoom(animeId, episodeNum = 1, mode = 'sub', startTime = 0) {
    state.currentEpisodeIndex = Math.max(0, parseInt(episodeNum) - 1);
    state.currentMode = mode;
    updateAudioButtons(mode);

    try {
      let anime = null;
      let episodes = [];

      // Check if we already have this anime cached in client memory
      const cached = (state.rawTrendingList || []).find(a => String(a.id) === String(animeId)) ||
                     (state.spotlightList || []).find(a => String(a.id) === String(animeId)) ||
                     (state.userWatchlist || []).find(a => String(a.anime_id) === String(animeId)) ||
                     (state.userHistory || []).find(a => String(a.anime_id) === String(animeId));
      const titleHint = cached?.title?.english || cached?.title?.romaji || cached?.title?.userPreferred || cached?.anime_title || '';

      const res = await fetch(`/api/anime/${animeId}${titleHint ? `?title=${encodeURIComponent(titleHint)}` : ''}`).catch(() => null);
      if (res && res.ok) {
        const json = await res.json().catch(() => ({}));
        if (json.success && json.data) {
          anime = json.data;
          episodes = Array.isArray(anime.episodes) ? anime.episodes : (anime.streamEpisodes || []);
        }
      }

      // Fallback: direct browser AniList query + backend HiAnime episode extraction
      if ((!anime || !episodes.length) && window.AniListClient) {
        const directDetails = await window.AniListClient.getAnimeDetails(animeId).catch(() => null);
        if (directDetails) {
          const directTitle = directDetails.title?.english || directDetails.title?.romaji || directDetails.title?.userPreferred || '';
          const epRes = await fetch(`/api/episodes?query=${encodeURIComponent(directTitle)}`).catch(() => null);
          let extractedEps = [];
          let match = null;
          if (epRes && epRes.ok) {
            const epJson = await epRes.json().catch(() => ({}));
            if (epJson.success && Array.isArray(epJson.data)) {
              extractedEps = epJson.data;
              match = epJson.hiAnimeMatch;
            }
          }
          anime = {
            ...directDetails,
            totalEpisodes: directDetails.episodes || extractedEps.length,
            episodes: extractedEps,
            streamEpisodes: extractedEps,
            hiAnimeMatch: match
          };
          episodes = extractedEps;
        }
      }

      if (!anime) {
        throw new Error('Failed to fetch anime metadata');
      }

      state.currentAnime = anime;
      state.currentAnime.episodes = episodes;

      // Update Breadcrumbs & Info safely
      const genre = anime.genres?.[0] || 'Anime';
      if (crumbAnimeGenre) crumbAnimeGenre.textContent = genre;
      if (crumbAnimeTitle) crumbAnimeTitle.textContent = anime.title?.english || anime.title?.romaji || 'Anime';
      if (crumbEpisodeNum) crumbEpisodeNum.textContent = `Episode ${episodeNum}`;

      if (detailPosterImg) detailPosterImg.src = anime.coverImage?.extraLarge || anime.coverImage?.large || '';
      if (detailAnimeTitle) detailAnimeTitle.textContent = anime.title?.english || anime.title?.romaji || 'Anime';
      if (detailAnimeNative) detailAnimeNative.textContent = anime.title?.native || '';
      if (detailAnimeScore) detailAnimeScore.textContent = `★ ${(anime.averageScore ? (anime.averageScore / 10).toFixed(1) : '9.0')}`;

      if (detailTagsContainer) {
        detailTagsContainer.innerHTML = (anime.genres || []).map(g => `
          <span class="px-2 py-0.5 rounded bg-surface-container-high border border-white/[0.08] text-white text-[11px] font-medium">${g}</span>
        `).join('');
      }

      if (detailSynopsisText) {
        detailSynopsisText.innerHTML = (anime.description || 'No synopsis provided.').replace(/<[^>]*>/g, '');
      }

      // Check Watchlist status
      updateWatchlistButtonState(anime.id);

      // Render Episode Queue
      if (episodesTotalCount) episodesTotalCount.textContent = `${episodes.length} EP`;
      renderEpisodeQueue(episodes, state.currentEpisodeIndex);

      // Load Current Episode Stream
      const currentEp = episodes[state.currentEpisodeIndex] || { id: `${animeId}-${episodeNum}`, number: episodeNum };
      if (currentEpTitle) currentEpTitle.textContent = `Episode ${currentEp.number || episodeNum}`;
      if (currentAnimeTitleSub) currentAnimeTitleSub.textContent = anime.title?.english || anime.title?.romaji || 'Anime';

      // Load stream into Artplayer with cross-device resume
      let resumeTime = Number(startTime) || 0;
      if (!resumeTime && state.userHistory && state.userHistory.length > 0) {
        const histMatch = state.userHistory.find(h => String(h.anime_id) === String(animeId));
        if (histMatch && Number(histMatch.episode_num) === Number(episodeNum) && Number(histMatch.progress_seconds) > 0) {
          resumeTime = Number(histMatch.progress_seconds);
        }
      }

      const posterUrl = anime.bannerImage || anime.coverImage?.extraLarge || '';
      state.player.loadStream(currentEp.id, mode, posterUrl, resumeTime).then(() => {
        if (state.player && state.player.art) {
          state.player.art.on('fullscreen', (isFull) => {
            if (btnFullscreenDown) {
              btnFullscreenDown.innerHTML = isFull
                ? '<span class="material-symbols-outlined text-[16px]">fullscreen_exit</span><span>Exit Fullscreen</span>'
                : '<span class="material-symbols-outlined text-[16px]">fullscreen</span><span>Fullscreen</span>';
            }
          });
        }
      }).catch(err => {
        console.error('[WatchRoom] Error loading stream into player:', err);
      });

      // Load recommendations
      loadRecommendations(anime.recommendations?.nodes || []);

    } catch (err) {
      console.error('[WatchRoom] Error loading watch room:', err);
      showToast(`Error: ${err.message}`, 'error');
    }
  }

  function renderEpisodeQueue(episodes, activeIndex) {
    if (!episodes || !episodes.length) {
      episodesScrollList.innerHTML = '<div class="py-6 text-center text-gray-500 font-mono text-xs">No episodes listed yet.</div>';
      return;
    }

    // Build Range Tabs if episodes > 50
    if (episodes.length > 50) {
      const ranges = [];
      for (let i = 0; i < episodes.length; i += 50) {
        const start = i + 1;
        const end = Math.min(episodes.length, i + 50);
        ranges.push({ start, end, index: Math.floor(i / 50) });
      }

      rangeTabsContainer.innerHTML = ranges.map((r, idx) => `
        <button class="range-tab px-2.5 py-1 rounded-md ${idx === 0 ? 'bg-primary-container text-white' : 'bg-surface-container text-gray-400'} font-mono text-[10px] font-bold whitespace-nowrap" data-start="${r.start}" data-end="${r.end}">
          ${r.start}-${r.end}
        </button>
      `).join('');

      rangeTabsContainer.querySelectorAll('.range-tab').forEach(tab => {
        tab.addEventListener('click', () => {
          rangeTabsContainer.querySelectorAll('.range-tab').forEach(t => t.className = 'range-tab px-2.5 py-1 rounded-md bg-surface-container text-gray-400 font-mono text-[10px] font-bold whitespace-nowrap');
          tab.className = 'range-tab px-2.5 py-1 rounded-md bg-primary-container text-white font-mono text-[10px] font-bold whitespace-nowrap';
          filterEpisodesByRange(parseInt(tab.dataset.start), parseInt(tab.dataset.end));
        });
      });
    } else {
      rangeTabsContainer.innerHTML = '';
    }

    renderEpisodeTiles(episodes, activeIndex);
  }

  function renderEpisodeTiles(episodes, activeIndex) {
    episodesScrollList.innerHTML = episodes.map((ep, idx) => {
      const isActive = idx === activeIndex;
      const isWatched = isEpisodeWatched(ep.number || idx + 1);

      return `
        <div class="ep-tile flex items-center justify-between p-2.5 rounded-xl ${isActive ? 'bg-primary-container/20 border border-primary/40' : 'bg-surface-container-low border border-white/[0.04] hover:bg-surface-container'} transition-all cursor-pointer group" data-idx="${idx}" data-num="${ep.number || idx + 1}" onclick="window.playEpisodeAtIndex(${idx})">
          <div class="flex items-center gap-3">
            <span class="w-7 h-7 rounded-lg ${isActive ? 'bg-primary-container text-white' : 'bg-surface-container-high text-gray-300'} font-mono text-xs font-bold flex items-center justify-center">
              ${ep.number || idx + 1}
            </span>
            <div class="flex flex-col">
              <span class="text-xs font-bold ${isActive ? 'text-primary' : 'text-white'} group-hover:text-primary transition-colors">
                Episode ${ep.number || idx + 1}
              </span>
              <span class="text-[10px] font-mono text-gray-500">${ep.title ? ep.title : 'Official Stream'}</span>
            </div>
          </div>

          <!-- Active Soundwave Equalizer or Watched Status -->
          <div class="flex items-center gap-2">
            ${isActive ? `
              <div class="wave-equalizer flex items-end gap-[3px] h-4">
                <span class="wave-bar w-[3px] h-full bg-primary rounded-full animate-bounce"></span>
                <span class="wave-bar w-[3px] h-3 bg-primary rounded-full animate-pulse"></span>
                <span class="wave-bar w-[3px] h-full bg-primary rounded-full animate-bounce"></span>
              </div>
            ` : isWatched ? `
              <span class="material-symbols-outlined text-[16px] text-emerald-400" title="Watched">check_circle</span>
            ` : ''}
          </div>
        </div>
      `;
    }).join('');

    // Scroll active into view
    const activeTile = episodesScrollList.querySelector(`.ep-tile[data-idx="${activeIndex}"]`);
    if (activeTile && typeof activeTile.scrollIntoView === 'function') {
      activeTile.scrollIntoView({ block: 'nearest', behavior: 'smooth' });
    }
  }

  function filterEpisodesByRange(start, end) {
    const tiles = episodesScrollList.querySelectorAll('.ep-tile');
    tiles.forEach(tile => {
      const num = parseInt(tile.dataset.num);
      tile.style.display = (num >= start && num <= end) ? 'flex' : 'none';
    });
  }

  function isEpisodeWatched(epNum) {
    if (!state.userHistory.length || !state.currentAnime) return false;
    const item = state.userHistory.find(h => String(h.anime_id) === String(state.currentAnime.id));
    return item && item.episode_num >= epNum;
  }

  window.playEpisodeAtIndex = function(index, resumeTime = 0) {
    if (!state.currentAnime || !state.currentAnime.episodes) return;
    const episodes = state.currentAnime.episodes;
    if (index < 0 || index >= episodes.length) return;

    state.currentEpisodeIndex = index;
    const ep = episodes[index];
    const epNum = ep.number || index + 1;

    if (crumbEpisodeNum) crumbEpisodeNum.textContent = `Episode ${epNum}`;
    if (currentEpTitle) currentEpTitle.textContent = `Episode ${epNum}`;

    renderEpisodeTiles(episodes, index);

    // Update URL without triggering recursive hashchange re-render
    try {
      history.replaceState(null, '', `#watch?anime=${state.currentAnime.id}&ep=${epNum}&mode=${state.currentMode}`);
    } catch {}

    const posterUrl = state.currentAnime.bannerImage || state.currentAnime.coverImage?.extraLarge || '';
    state.player.loadStream(ep.id, state.currentMode, posterUrl, resumeTime);
  };

  function playNextEpisode() {
    if (!state.currentAnime || !state.currentAnime.episodes) return;
    const nextIdx = state.currentEpisodeIndex + 1;
    if (nextIdx < state.currentAnime.episodes.length) {
      window.playEpisodeAtIndex(nextIdx);
    } else {
      showToast('You have reached the latest episode!', 'info');
    }
  }

  function playPrevEpisode() {
    if (!state.currentAnime || !state.currentAnime.episodes) return;
    const prevIdx = state.currentEpisodeIndex - 1;
    if (prevIdx >= 0) {
      window.playEpisodeAtIndex(prevIdx);
    }
  }

  btnNextEp.addEventListener('click', playNextEpisode);
  btnPrevEp.addEventListener('click', playPrevEpisode);

  btnSkipIntro.addEventListener('click', () => {
    if (state.player && state.player.art) {
      state.player.art.currentTime = Math.min(state.player.art.duration, state.player.art.currentTime + 85);
      showToast('Skipped intro (+85s)', 'info');
    }
  });

  // Theater Mode Toggle
  btnTheaterMode.addEventListener('click', () => {
    const isTheater = playerGridLayout.classList.toggle('theater-mode');
    btnTheaterMode.classList.toggle('bg-primary-container', isTheater);
    btnTheaterMode.classList.toggle('text-white', isTheater);
  });

  // Share Watch Room Link
  btnShareRoom.addEventListener('click', () => {
    navigator.clipboard.writeText(window.location.href).then(() => {
      showToast('Watch room link copied to clipboard!', 'success');
    }).catch(() => {
      showToast('Failed to copy link', 'error');
    });
  });

  // Watchlist Toggle
  btnToggleWatchlist.addEventListener('click', () => {
    if (!state.currentAnime) return;
    toggleWatchlistAnime(state.currentAnime);
  });

  function updateWatchlistBadges(count) {
    if (flyoutWatchlistCount) flyoutWatchlistCount.textContent = count;
    if (mobileWatchlistBadge) {
      if (count > 0) {
        mobileWatchlistBadge.textContent = count > 99 ? '99+' : count;
        mobileWatchlistBadge.classList.remove('hidden');
      } else {
        mobileWatchlistBadge.classList.add('hidden');
      }
    }
  }

  async function updateWatchlistButtonState(animeId) {
    if (!state.auth.token) {
      if (textWatchlistStatus) textWatchlistStatus.textContent = 'Add to Watchlist';
      if (iconWatchlistStatus) iconWatchlistStatus.textContent = 'bookmark_add';
      return;
    }

    try {
      const res = await fetch('/api/user/watchlist', {
        headers: { 'Authorization': `Bearer ${state.auth.token}` }
      });
      const json = await res.json();
      if (json.success && Array.isArray(json.data)) {
        state.userWatchlist = json.data;
        updateWatchlistBadges(json.data.length);

        const isListed = json.data.some(w => String(w.anime_id) === String(animeId));
        if (isListed) {
          if (textWatchlistStatus) textWatchlistStatus.textContent = 'In Watchlist';
          if (iconWatchlistStatus) iconWatchlistStatus.textContent = 'bookmark_added';
          if (btnToggleWatchlist) btnToggleWatchlist.className = 'flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-primary-container/20 border border-primary/40 text-primary text-xs font-semibold transition-all';
        } else {
          if (textWatchlistStatus) textWatchlistStatus.textContent = 'Add to Watchlist';
          if (iconWatchlistStatus) iconWatchlistStatus.textContent = 'bookmark_add';
          if (btnToggleWatchlist) btnToggleWatchlist.className = 'flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-surface-container-low border border-white/[0.08] text-gray-300 hover:text-primary text-xs font-semibold transition-all';
        }
      }
    } catch {}
  }

  async function toggleWatchlistAnime(anime) {
    if (!state.auth.token) {
      openAuthModal('login');
      return;
    }

    const animeId = anime.id;
    const title = anime.title?.english || anime.title?.romaji || 'Anime';
    const poster = anime.coverImage?.extraLarge || anime.coverImage?.large || '';
    const isListed = state.userWatchlist.some(w => String(w.anime_id) === String(animeId));

    try {
      if (isListed) {
        await fetch(`/api/user/watchlist/${animeId}`, {
          method: 'DELETE',
          headers: { 'Authorization': `Bearer ${state.auth.token}` }
        });
        showToast(`Removed "${title}" from Watchlist`, 'info');
      } else {
        await fetch('/api/user/watchlist', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${state.auth.token}`
          },
          body: JSON.stringify({
            animeId,
            animeTitle: title,
            poster,
            status: 'Watching'
          })
        });
        showToast(`Added "${title}" to Watchlist!`, 'success');
      }
      updateWatchlistButtonState(animeId);
    } catch (err) {
      showToast(err.message, 'error');
    }
  }

  window.toggleWatchlistAnimeById = function(id, title, poster) {
    toggleWatchlistAnime({
      id,
      title: { english: title },
      coverImage: { extraLarge: poster }
    });
  };

  // Debounced History Progress Sync
  async function syncProgressToBackend(progressSeconds, durationSeconds) {
    if (!state.auth.token || !state.currentAnime) return;
    const episodes = state.currentAnime.episodes || [];
    const currentEp = episodes[state.currentEpisodeIndex] || {};

    try {
      await fetch('/api/user/history', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${state.auth.token}`
        },
        body: JSON.stringify({
          animeId: state.currentAnime.id,
          animeTitle: state.currentAnime.title?.english || state.currentAnime.title?.romaji || 'Anime',
          poster: state.currentAnime.coverImage?.extraLarge || state.currentAnime.coverImage?.large || '',
          episodeNum: currentEp.number || state.currentEpisodeIndex + 1,
          episodeId: currentEp.id || '',
          progressSeconds,
          durationSeconds
        })
      });
    } catch {}
  }

  function loadRecommendations(recs) {
    if (!recs || !recs.length) {
      recsListContainer.innerHTML = '<div class="py-4 text-center text-gray-500 font-mono text-xs">No related titles.</div>';
      return;
    }

    recsListContainer.innerHTML = recs.slice(0, 4).map(node => {
      const media = node.mediaRecommendation;
      if (!media) return '';
      const title = media.title?.english || media.title?.romaji || 'Anime';
      const poster = media.coverImage?.medium || media.coverImage?.large || '';
      const score = media.averageScore ? (media.averageScore / 10).toFixed(1) : '8.5';

      return `
        <div class="p-2 rounded-xl bg-surface-container-low hover:bg-surface-container border border-white/[0.04] hover:border-primary/40 flex items-center gap-3 transition-all cursor-pointer group" onclick="window.location.hash='watch?anime=${media.id}&ep=1'">
          <img src="${poster}" alt="${title}" class="w-10 h-14 object-cover rounded-lg flex-shrink-0">
          <div class="flex flex-col min-w-0 flex-1">
            <span class="text-xs font-bold text-white group-hover:text-primary transition-colors truncate">${title}</span>
            <span class="text-[10px] font-mono text-gray-500">${media.format || 'TV'} • ★ ${score}</span>
          </div>
          <span class="material-symbols-outlined text-gray-500 group-hover:text-primary text-[18px]">play_circle</span>
        </div>
      `;
    }).join('');
  }

  // ============================================================================
  // USER ACCOUNT, WATCHLIST & SETTINGS MODULE
  // ============================================================================

  accountSubTabs.addEventListener('click', (e) => {
    const tab = e.target.closest('.account-tab');
    if (!tab) return;
    const tabName = tab.dataset.tab;
    switchAccountTab(tabName);
  });

  function switchAccountTab(tabName) {
    accountSubTabs.querySelectorAll('.account-tab').forEach(t => {
      if (t.dataset.tab === tabName) {
        t.className = 'account-tab active px-4 py-2.5 font-semibold text-xs text-white border-b-2 border-primary transition-all flex items-center gap-2';
      } else {
        t.className = 'account-tab px-4 py-2.5 font-semibold text-xs text-gray-400 hover:text-white border-b-2 border-transparent transition-all flex items-center gap-2';
      }
    });

    document.querySelectorAll('.account-tab-panel').forEach(p => p.classList.add('hidden'));
    const targetPanel = document.getElementById(`tab-content-${tabName}`);
    if (targetPanel) targetPanel.classList.remove('hidden');
  }

  async function loadAccountData() {
    if (!state.auth.token) {
      openAuthModal('login');
      return;
    }

    try {
      // 1. Fetch Profile & Stats
      const profRes = await fetch('/api/user/profile', {
        headers: { 'Authorization': `Bearer ${state.auth.token}` }
      });
      const profJson = await profRes.json();

      if (profJson.success && profJson.data) {
        const { stats, user } = profJson.data;
        if (user) updateAuthUI(user);
        if (stats) {
          if (statTotalEpisodes) statTotalEpisodes.innerHTML = `${stats.totalEpisodesWatched || 0} <span class="text-primary text-sm font-normal">Eps</span>`;
          if (statTotalDays) statTotalDays.textContent = `${stats.totalDaysWatched || '0.0'} Days`;
          if (statCompletedShows) statCompletedShows.innerHTML = `${stats.completedSeriesCount || 0} <span class="text-secondary text-sm font-normal">Shows</span>`;
          if (statWatchlistCount) statWatchlistCount.innerHTML = `${stats.watchlistCount || 0} <span class="text-tertiary text-sm font-normal">Titles</span>`;
          const statMeanScore = document.getElementById('stat-mean-score');
          if (statMeanScore) {
            statMeanScore.innerHTML = `${stats.meanScore || '--'} <span class="text-yellow-400 text-sm font-normal">★</span>`;
          }
          updateWatchlistBadges(stats.watchlistCount || 0);
          if (flyoutHistoryCount) flyoutHistoryCount.textContent = stats.totalEpisodesWatched || 0;
        }
      }

      // 2. Load Watchlist Tab
      loadAccountWatchlist('All');

      // 3. Load Watch History Tab
      loadAccountHistory();

    } catch (err) {
      console.error('[Account] Error loading data:', err);
    }
  }

  // Watchlist Filter Chips in Account
  watchlistStatusChips.addEventListener('click', (e) => {
    const chip = e.target.closest('.wl-chip');
    if (!chip) return;
    watchlistStatusChips.querySelectorAll('.wl-chip').forEach(c => {
      c.className = 'wl-chip px-3 py-1 rounded-full bg-surface-container-low border border-white/[0.06] text-gray-400 hover:text-white font-mono text-xs font-bold';
    });
    chip.className = 'wl-chip active px-3 py-1 rounded-full bg-primary-container text-white font-mono text-xs font-bold';
    loadAccountWatchlist(chip.dataset.status);
  });

  async function loadAccountWatchlist(statusFilter = 'All') {
    accountWatchlistGrid.innerHTML = '<div class="col-span-full py-8 flex justify-center"><div class="w-8 h-8 rounded-full border-2 border-primary border-t-transparent animate-spin"></div></div>';

    try {
      const url = statusFilter === 'All' ? '/api/user/watchlist' : `/api/user/watchlist?status=${encodeURIComponent(statusFilter)}`;
      const res = await fetch(url, {
        headers: { 'Authorization': `Bearer ${state.auth.token}` }
      });
      const json = await res.json();

      if (json.success && Array.isArray(json.data)) {
        watchlistCountBadge.textContent = `${json.data.length} TITLES`;
        renderAccountWatchlistGrid(json.data);
      } else {
        accountWatchlistGrid.innerHTML = '<div class="col-span-full py-8 text-center text-gray-500 font-mono text-xs">Your watchlist is currently empty.</div>';
      }
    } catch {
      accountWatchlistGrid.innerHTML = '<div class="col-span-full py-8 text-center text-red-400 font-mono text-xs">Failed to load watchlist.</div>';
    }
  }

  function renderAccountWatchlistGrid(list) {
    if (!list.length) {
      accountWatchlistGrid.innerHTML = '<div class="col-span-full py-8 text-center text-gray-500 font-mono text-xs">No entries match this filter.</div>';
      return;
    }

    accountWatchlistGrid.innerHTML = list.map(item => `
      <div class="group relative bg-surface-container-low border border-white/[0.08] hover:border-primary/50 rounded-2xl overflow-hidden shadow-lg transition-all duration-300 flex flex-col cursor-pointer" onclick="window.location.hash='watch?anime=${item.anime_id}&ep=1'">
        <div class="relative aspect-[2/3] w-full bg-surface-container-high overflow-hidden">
          <img src="${item.poster || 'https://via.placeholder.com/300x450'}" alt="${item.anime_title}" class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500">
          <div class="absolute inset-0 bg-gradient-to-t from-surface-container-lowest via-transparent to-transparent"></div>
          
          <div class="absolute top-2 left-2">
            <span class="px-2 py-0.5 rounded bg-black/60 backdrop-blur-md text-white font-mono text-[10px] font-bold border border-white/[0.1]">
              ${item.status || 'Watching'}
            </span>
          </div>

          <div class="absolute top-2 right-2">
            <button class="w-7 h-7 rounded-full bg-black/60 text-red-400 hover:bg-red-500 hover:text-white flex items-center justify-center transition-all" title="Remove" onclick="event.stopPropagation(); window.removeWatchlistItem('${item.anime_id}')">
              <span class="material-symbols-outlined text-[15px]">delete</span>
            </button>
          </div>
        </div>

        <div class="p-3 flex flex-col gap-1">
          <h3 class="text-xs font-bold text-white line-clamp-1 group-hover:text-primary transition-colors">${item.anime_title}</h3>
          <span class="text-[10px] font-mono text-secondary font-semibold">Resume Watching &rarr;</span>
        </div>
      </div>
    `).join('');
  }

  window.removeWatchlistItem = async function(animeId) {
    if (!state.auth.token) return;
    try {
      await fetch(`/api/user/watchlist/${animeId}`, {
        method: 'DELETE',
        headers: { 'Authorization': `Bearer ${state.auth.token}` }
      });
      showToast('Item removed from watchlist', 'info');
      loadAccountWatchlist();
    } catch {}
  };

  // History Tab
  async function loadAccountHistory() {
    accountHistoryList.innerHTML = '<div class="py-8 flex justify-center"><div class="w-8 h-8 rounded-full border-2 border-primary border-t-transparent animate-spin"></div></div>';

    try {
      const res = await fetch('/api/user/history', {
        headers: { 'Authorization': `Bearer ${state.auth.token}` }
      });
      const json = await res.json();

      if (json.success && Array.isArray(json.data) && json.data.length > 0) {
        accountHistoryList.innerHTML = json.data.map(item => {
          const progressPercent = item.duration_seconds > 0 
            ? Math.min(100, Math.round((item.progress_seconds / item.duration_seconds) * 100))
            : 50;
          const dateStr = new Date(item.updated_at).toLocaleDateString('en-US', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });

          return `
            <div class="p-3.5 rounded-2xl bg-surface-container-low border border-white/[0.08] hover:border-primary/50 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 transition-all cursor-pointer group" onclick="window.location.hash='watch?anime=${item.anime_id}&ep=${item.episode_num}'">
              <div class="flex items-center gap-3.5 min-w-0">
                <img src="${item.poster || 'https://via.placeholder.com/60x80'}" alt="${item.anime_title}" class="w-12 h-16 object-cover rounded-xl border border-white/[0.08] flex-shrink-0">
                <div class="flex flex-col min-w-0">
                  <h3 class="text-sm font-bold text-white group-hover:text-primary transition-colors truncate">${item.anime_title}</h3>
                  <div class="flex items-center gap-2 font-mono text-xs text-gray-400 mt-0.5">
                    <span class="text-secondary font-bold">Episode ${item.episode_num}</span>
                    <span>•</span>
                    <span>${progressPercent}% Watched</span>
                    <span>•</span>
                    <span class="text-[10px] text-gray-500">${dateStr}</span>
                  </div>
                  <!-- Progress bar -->
                  <div class="w-48 bg-surface-container-high h-1 rounded-full overflow-hidden mt-1.5">
                    <div class="bg-primary-container h-full rounded-full" style="width: ${progressPercent}%;"></div>
                  </div>
                </div>
              </div>

              <div class="flex items-center gap-3 flex-shrink-0 self-end sm:self-center">
                <button class="px-3.5 py-1.5 rounded-xl bg-primary-container text-white font-mono text-xs font-bold uppercase shadow-md hover:brightness-110 transition-all flex items-center gap-1">
                  <span class="material-symbols-outlined text-[15px]">play_arrow</span>Resume
                </button>
                <button class="p-1.5 rounded-lg text-gray-400 hover:text-red-400 hover:bg-red-500/10 transition-all" title="Remove from history" onclick="event.stopPropagation(); window.removeHistoryItem('${item.anime_id}')">
                  <span class="material-symbols-outlined text-[16px]">close</span>
                </button>
              </div>
            </div>
          `;
        }).join('');
      } else {
        accountHistoryList.innerHTML = '<div class="py-12 text-center text-gray-500 font-mono text-xs">No watch history recorded yet. Start watching anime to build your timeline!</div>';
      }
    } catch {
      accountHistoryList.innerHTML = '<div class="py-8 text-center text-red-400 font-mono text-xs">Failed to load history timeline.</div>';
    }
  }

  window.removeHistoryItem = async function(animeId) {
    if (!state.auth.token) return;
    try {
      await fetch(`/api/user/history/${animeId}`, {
        method: 'DELETE',
        headers: { 'Authorization': `Bearer ${state.auth.token}` }
      });
      showToast('Item removed from history', 'info');
      state.userHistory = (state.userHistory || []).filter(h => String(h.anime_id) !== String(animeId));
      loadAccountHistory();
      loadContinueWatching();
    } catch {}
  };

  btnClearAllHistory.addEventListener('click', async () => {
    if (!state.auth.token || !confirm('Are you sure you want to clear your entire watch history?')) return;
    try {
      await fetch('/api/user/history', {
        method: 'DELETE',
        headers: { 'Authorization': `Bearer ${state.auth.token}` }
      });
      showToast('Watch history cleared', 'info');
      state.userHistory = [];
      loadAccountHistory();
      loadContinueWatching();
    } catch {}
  });

  // Save Playback Preferences
  btnSavePreferences.addEventListener('click', async () => {
    if (!state.auth.token) return;

    const data = {
      preferredAudio: prefAudio.value,
      preferredQuality: prefQuality.value,
      autoSkipIntro: prefSkipIntro.checked,
      autoNext: prefAutoNext.checked
    };

    try {
      btnSavePreferences.disabled = true;
      btnSavePreferences.textContent = 'Saving...';

      const res = await fetch('/api/user/settings', {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${state.auth.token}`
        },
        body: JSON.stringify(data)
      });
      const json = await res.json();

      if (json.success) {
        state.auth.settings = json.data;
        state.currentMode = data.preferredAudio;
        updateAudioButtons(data.preferredAudio);
        showToast('Playback preferences saved successfully!', 'success');
      } else {
        throw new Error(json.error || 'Failed to update preferences');
      }
    } catch (err) {
      showToast(err.message, 'error');
    } finally {
      btnSavePreferences.disabled = false;
      btnSavePreferences.textContent = 'Save Playback Preferences';
    }
  });

  // ============================================================================
  // LIVE FLOATING SEARCH DROPDOWN
  // ============================================================================

  let liveSearchDebounce = null;
  globalSearchInput.addEventListener('input', (e) => {
    clearTimeout(liveSearchDebounce);
    const query = e.target.value.trim();
    if (!query) {
      searchDropdown.classList.add('hidden');
      searchDropdown.innerHTML = '';
      return;
    }
    liveSearchDebounce = setTimeout(() => handleLiveSearch(query), 300);
  });

  globalSearchInput.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') {
      const q = globalSearchInput.value.trim();
      if (q) {
        searchDropdown.classList.add('hidden');
        navigateTo(`browse?search=${encodeURIComponent(q)}`);
      }
    }
  });

  document.addEventListener('click', (e) => {
    if (!e.target.closest('#global-search-input') && !e.target.closest('#search-dropdown')) {
      searchDropdown.classList.add('hidden');
    }
  });

  async function handleLiveSearch(query) {
    try {
      let results = [];
      // 1. Direct Browser Client (window.AniListClient) - High-speed, unblocked, complete catalog
      if (window.AniListClient) {
        results = await window.AniListClient.searchAnime(query, 1, 10).catch(() => []);
      }
      // 2. Server fallback if direct client failed or returned empty
      if (!results.length) {
        const res = await fetch(`/api/search?q=${encodeURIComponent(query)}`).catch(() => null);
        if (res && res.ok) {
          const json = await res.json().catch(() => ({}));
          if (json.success && Array.isArray(json.data) && json.data.length > 0) {
            results = json.data;
          }
        }
      }

      if (results.length > 0) {
        searchDropdown.innerHTML = results.slice(0, 5).map(item => {
          const title = item.title?.english || item.title?.romaji || 'Anime';
          const poster = item.coverImage?.medium || item.coverImage?.large || '';
          const score = item.averageScore ? (item.averageScore / 10).toFixed(1) : '8.5';

          return `
            <div class="p-2 rounded-lg hover:bg-surface-container flex items-center gap-3 transition-colors cursor-pointer" onclick="window.location.hash='watch?anime=${item.id}&ep=1'; document.getElementById('search-dropdown').classList.add('hidden');">
              <img src="${poster}" alt="${title}" class="w-9 h-12 object-cover rounded-md flex-shrink-0">
              <div class="flex flex-col min-w-0 flex-1">
                <span class="text-xs font-bold text-white truncate">${title}</span>
                <span class="text-[10px] font-mono text-gray-500">${item.format || 'TV'} • ★ ${score}</span>
              </div>
            </div>
          `;
        }).join('') + `
          <div class="pt-2 mt-1 border-t border-white/[0.06] text-center">
            <button class="text-xs font-mono text-secondary hover:underline font-bold" onclick="window.location.hash='browse?search=${encodeURIComponent(query)}'; document.getElementById('search-dropdown').classList.add('hidden');">
              View All Results &rarr;
            </button>
          </div>
        `;
        searchDropdown.classList.remove('hidden');
      } else {
        searchDropdown.innerHTML = '<div class="p-3 text-center text-xs font-mono text-gray-500">No matching titles</div>';
        searchDropdown.classList.remove('hidden');
      }
    } catch {}
  }

  // ============================================================================
  // MOBILE SEARCH MODAL & INSTANT RESULTS
  // ============================================================================

  if (btnMobileSearchToggle) {
    btnMobileSearchToggle.addEventListener('click', () => {
      if (mobileSearchModal) {
        mobileSearchModal.classList.remove('hidden');
        mobileSearchModal.classList.add('flex');
        if (mobileSearchInput) {
          mobileSearchInput.value = '';
          setTimeout(() => mobileSearchInput.focus(), 80);
        }
        if (mobileSearchResults) mobileSearchResults.innerHTML = '';
      }
    });
  }

  if (mobileSearchCloseBtn) {
    mobileSearchCloseBtn.addEventListener('click', () => {
      if (mobileSearchModal) {
        mobileSearchModal.classList.add('hidden');
        mobileSearchModal.classList.remove('flex');
      }
    });
  }

  let mobileLiveSearchDebounce = null;
  if (mobileSearchInput) {
    mobileSearchInput.addEventListener('input', (e) => {
      clearTimeout(mobileLiveSearchDebounce);
      const query = e.target.value.trim();
      if (mobileSearchClearBtn) {
        if (query) mobileSearchClearBtn.classList.remove('hidden');
        else mobileSearchClearBtn.classList.add('hidden');
      }
      if (!query) {
        if (mobileSearchResults) mobileSearchResults.innerHTML = '';
        return;
      }
      mobileLiveSearchDebounce = setTimeout(() => handleMobileLiveSearch(query), 300);
    });

    mobileSearchInput.addEventListener('keydown', (e) => {
      if (e.key === 'Enter') {
        const q = mobileSearchInput.value.trim();
        if (q) {
          if (mobileSearchModal) {
            mobileSearchModal.classList.add('hidden');
            mobileSearchModal.classList.remove('flex');
          }
          navigateTo(`browse?search=${encodeURIComponent(q)}`);
        }
      }
    });
  }

  if (mobileSearchClearBtn) {
    mobileSearchClearBtn.addEventListener('click', () => {
      if (mobileSearchInput) {
        mobileSearchInput.value = '';
        mobileSearchInput.focus();
      }
      mobileSearchClearBtn.classList.add('hidden');
      if (mobileSearchResults) mobileSearchResults.innerHTML = '';
    });
  }

  mobileQuickTags.forEach(tag => {
    tag.addEventListener('click', () => {
      const q = tag.dataset.query;
      if (mobileSearchInput) {
        mobileSearchInput.value = q;
        if (mobileSearchClearBtn) mobileSearchClearBtn.classList.remove('hidden');
        handleMobileLiveSearch(q);
      }
    });
  });

  async function handleMobileLiveSearch(query) {
    if (!mobileSearchResults) return;
    try {
      mobileSearchResults.innerHTML = '<div class="py-8 flex justify-center"><div class="w-7 h-7 rounded-full border-2 border-primary border-t-transparent animate-spin"></div></div>';
      let results = [];
      // 1. Direct Browser Client (window.AniListClient) - High-speed, unblocked, complete catalog
      if (window.AniListClient) {
        results = await window.AniListClient.searchAnime(query, 1, 10).catch(() => []);
      }
      // 2. Server fallback if direct client failed or returned empty
      if (!results.length) {
        const res = await fetch(`/api/search?q=${encodeURIComponent(query)}`).catch(() => null);
        if (res && res.ok) {
          const json = await res.json().catch(() => ({}));
          if (json.success && Array.isArray(json.data) && json.data.length > 0) {
            results = json.data;
          }
        }
      }

      if (results.length > 0) {
        mobileSearchResults.innerHTML = results.slice(0, 8).map(item => {
          const title = item.title?.english || item.title?.romaji || 'Anime';
          const poster = item.coverImage?.medium || item.coverImage?.large || '';
          const score = item.averageScore ? (item.averageScore / 10).toFixed(1) : '8.5';

          return `
            <div class="p-2.5 rounded-xl bg-surface-container border border-white/[0.06] hover:bg-surface-container-high flex items-center gap-3 transition-colors cursor-pointer active:scale-98" onclick="window.location.hash='watch?anime=${item.id}&ep=1'; const m = document.getElementById('mobile-search-modal'); if (m) { m.classList.add('hidden'); m.classList.remove('flex'); }">
              <img src="${poster}" alt="${title}" class="w-10 h-14 object-cover rounded-lg flex-shrink-0">
              <div class="flex flex-col min-w-0 flex-1">
                <span class="text-xs font-bold text-white truncate">${title}</span>
                <span class="text-[10px] font-mono text-gray-400 mt-0.5">${item.format || 'TV'} • ★ ${score}</span>
              </div>
              <span class="material-symbols-outlined text-primary text-[20px] flex-shrink-0">play_circle</span>
            </div>
          `;
        }).join('') + `
          <div class="pt-2 text-center">
            <button class="w-full py-2.5 rounded-xl bg-surface-container-high text-xs font-mono font-bold text-secondary hover:text-white transition-colors cursor-pointer" onclick="window.location.hash='browse?search=${encodeURIComponent(query)}'; const m = document.getElementById('mobile-search-modal'); if (m) { m.classList.add('hidden'); m.classList.remove('flex'); }">
              Explore All Results for "${escapeQuotes(query)}" &rarr;
            </button>
          </div>
        `;
      } else {
        mobileSearchResults.innerHTML = '<div class="py-8 text-center text-xs font-mono text-gray-500">No matching anime titles found.</div>';
      }
    } catch {
      mobileSearchResults.innerHTML = '<div class="py-8 text-center text-xs font-mono text-gray-500">Error loading search results.</div>';
    }
  }

  // ============================================================================
  // TOAST NOTIFICATION UTILITY
  // ============================================================================

  function showToast(message, type = 'info') {
    const toast = document.createElement('div');
    const color = type === 'success' ? 'border-primary/50 text-white' : type === 'error' ? 'border-red-500/50 text-red-300' : 'border-secondary/50 text-white';
    const icon = type === 'success' ? 'check_circle' : type === 'error' ? 'error' : 'info';

    toast.className = `flex items-center gap-2.5 px-4 py-2.5 rounded-xl bg-surface-container-low border ${color} shadow-2xl backdrop-blur-md text-xs font-semibold animate-slide-up transition-all`;
    toast.innerHTML = `
      <span class="material-symbols-outlined text-[16px] text-primary">${icon}</span>
      <span>${message}</span>
    `;

    toastContainer.appendChild(toast);
    setTimeout(() => {
      toast.style.opacity = '0';
      toast.style.transform = 'translateY(10px)';
      setTimeout(() => toast.remove(), 300);
    }, 3500);
  }

  function escapeQuotes(str) {
    return (str || '').replace(/'/g, "\\'").replace(/"/g, '&quot;');
  }

});
