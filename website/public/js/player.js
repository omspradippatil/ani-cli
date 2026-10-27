// website/public/js/player.js
// Cinema-Grade Artplayer.js Video Player Engine with Hls.js & Subtitles for Om CLI

function sanitizeVtt(vtt) {
  if (typeof vtt !== 'string') return vtt;
  return vtt
    // Unescape pre-escaped HTML formatting tags so they don't print as raw literal text
    .replace(/&lt;(\/?[a-zA-Z0-9]+)&gt;/gi, '<$1>')
    .replace(/&lt;(\/?[a-zA-Z0-9]+)\s+([^&]*?)&gt;/gi, '<$1 $2>')
    // Convert ASS style tags into standard WebVTT formatting
    .replace(/\{\\i1\}/g, '<i>')
    .replace(/\{\\i0\}/g, '</i>')
    .replace(/\{\\b1\}/g, '<b>')
    .replace(/\{\\b0\}/g, '</b>')
    // Remove other ASS position and override tags
    .replace(/\{[^\}]*\}/g, '');
}

class AnimePlayer {
  constructor(mountSelector) {
    this.mountSelector = mountSelector;
    this.art = null;
    this.currentEpisodeId = null;
    this.currentMode = 'sub';
    this.onNextEpisode = null;
    this.onPrevEpisode = null;
    this.onToggleMode = null;
    this.onTimeUpdate = null;
    this.onQualityList = null;
    this.onQualityChange = null;
    this.onCaptionsToggle = null;
    this.autoNext = true;
    this.captionsEnabled = true;
    this.levels = [];
    this.currentQualityLabel = 'Auto';
  }

  /**
   * Initializes or updates Artplayer with a stream
   */
  async loadStream(episodeId, mode = 'sub', posterUrl = '', resumeTime = 0) {
    this.currentEpisodeId = episodeId;
    this.currentMode = mode;

    const mount = document.querySelector(this.mountSelector);
    if (!mount) {
      console.error('[Player] Mount selector not found:', this.mountSelector);
      return;
    }

    // If a player instance already exists and is healthy, switch URL directly
    // This avoids unmounting DOM, prevents audio leaks, and keeps fullscreen state intact!
    if (this.art && !this.art.isDestroy) {
      if (this.art.notice) {
        this.art.notice.show = 'Loading episode stream...';
      }
      try {
        const res = await fetch(`/api/stream/${episodeId}?mode=${mode}`);
        const json = await res.json();
        if (!json.success || !json.data) {
          throw new Error(json.error || 'Failed to fetch episode stream');
        }
        const streamData = json.data;
        const streamUrl = streamData.proxyM3u8;

        // Prepare Subtitle
        let subUrl = '';
        if (streamData.subtitles && streamData.subtitles.length > 0) {
          const primarySub = streamData.subtitles.find(s => s.default) || streamData.subtitles[0];
          subUrl = primarySub.proxySrc || primarySub.src || '';
        }

        if (posterUrl) this.art.poster = posterUrl;
        if (subUrl && this.art.subtitle) {
          this.art.subtitle.switch(subUrl, {
            escape: false,
            onVttLoad: sanitizeVtt
          });
          this.art.subtitle.show = this.captionsEnabled;
        } else if (this.art.subtitle) {
          this.art.subtitle.show = false;
        }

        // Update in-player SUB / DUB toggle button
        const modeBtn = this.art.template.$controls?.querySelector('.art-btn-mode');
        if (modeBtn) {
          modeBtn.textContent = mode.toUpperCase();
        }

        // Switch stream URL
        await this.art.switchUrl(streamUrl);
        if (resumeTime > 0) {
          this.art.currentTime = resumeTime;
        }
        if (this.art.video) {
          this.art.video.muted = false;
          this.art.video.volume = 0.90;
        }
        if (this.art.notice) {
          this.art.notice.show = `Loaded Episode (${mode.toUpperCase()})`;
        }
        return streamData;
      } catch (err) {
        console.error('[Player] Switch stream error:', err);
        if (this.art.notice) {
          this.art.notice.show = `Error: ${err.message}`;
        }
        throw err;
      }
    }

    // FIRST: Stop all existing audio and clean up previous instance
    this.stop();

    // Show loading state in mount element
    mount.innerHTML = `
      <div class="w-full h-full flex flex-col items-center justify-center bg-black/95 text-white gap-3 select-none">
        <div class="w-10 h-10 rounded-full border-2 border-primary border-t-transparent animate-spin"></div>
        <span class="font-mono text-xs text-gray-400 tracking-wider">Connecting to Stream Node...</span>
      </div>
    `;

    try {
      const res = await fetch(`/api/stream/${episodeId}?mode=${mode}`);
      const json = await res.json();

      if (!json.success || !json.data) {
        throw new Error(json.error || 'Failed to fetch episode stream');
      }

      const streamData = json.data;
      const streamUrl = streamData.proxyM3u8;

      console.log(`[Player] Loaded stream for episode ${episodeId} (${mode}):`, streamUrl);

      // Check saved progress: priority to cross-device resumeTime, then local localStorage
      let savedTime = resumeTime || 0;
      if (!savedTime) {
        try {
          const saved = JSON.parse(localStorage.getItem(`ani_progress_${episodeId}`));
          if (saved && saved.time && saved.time < saved.duration - 15) {
            savedTime = saved.time;
          }
        } catch {}
      }

      // Prepare Subtitles
      let subUrl = '';
      if (streamData.subtitles && streamData.subtitles.length > 0) {
        const primarySub = streamData.subtitles.find(s => s.default) || streamData.subtitles[0];
        subUrl = primarySub.proxySrc || primarySub.src || '';
      }

      // Create a fresh dedicated stage element inside mount to guarantee NO DOM collision
      mount.innerHTML = '<div id="artplayer-inner-stage" class="w-full h-full" style="width:100%;height:100%;"></div>';

      const self = this;

      // Create new Artplayer instance
      this.art = new window.Artplayer({
        container: '#artplayer-inner-stage',
        url: streamUrl,
        type: 'm3u8',
        customType: {
          m3u8: function (video, url, art) {
            if (window.Hls && window.Hls.isSupported()) {
              if (art.hls) {
                try { art.hls.destroy(); } catch (e) {}
              }

              const hls = new window.Hls({
                enableWorker: true,
                maxBufferLength: 30,
                maxMaxBufferLength: 60
              });

              hls.loadSource(url);
              hls.attachMedia(video);
              art.hls = hls;

              art.on('destroy', () => {
                if (hls) {
                  try { hls.destroy(); } catch(e) {}
                }
              });

              hls.on(window.Hls.Events.MANIFEST_PARSED, () => {
                if (hls.levels && hls.levels.length > 0) {
                  const levels = hls.levels.map((lvl, idx) => ({
                    html: `${lvl.height}p`,
                    level: idx,
                    height: lvl.height,
                    default: idx === hls.currentLevel
                  }));
                  levels.unshift({ html: 'Auto', level: -1, height: 0, default: hls.currentLevel === -1 });
                  self.levels = levels;

                  // Update dynamic quality selector in Artplayer
                  art.setting.update({
                    name: 'quality',
                    html: 'Quality',
                    tooltip: self.currentQualityLabel,
                    selector: levels,
                    onSelect: function (item) {
                      return self.setQuality(item.level);
                    }
                  });

                  // Update in-player Quality control dropdown
                  if (art.controls && art.controls['quality-btn']) {
                    art.controls.update({
                      name: 'quality-btn',
                      selector: levels,
                      onSelect: function (item) {
                        return self.setQuality(item.level);
                      }
                    });
                  }

                  // Notify external listener (e.g. bottom toolbar dropdown)
                  if (typeof self.onQualityList === 'function') {
                    self.onQualityList(levels, hls.currentLevel);
                  }
                }
              });

              hls.on(window.Hls.Events.ERROR, (event, data) => {
                if (data.fatal) {
                  switch (data.type) {
                    case window.Hls.ErrorTypes.NETWORK_ERROR:
                      console.warn('[Player] Network error, recovering...');
                      hls.startLoad();
                      break;
                    case window.Hls.ErrorTypes.MEDIA_ERROR:
                      console.warn('[Player] Media error, recovering...');
                      hls.recoverMediaError();
                      break;
                    default:
                      console.error('[Player] Fatal unrecoverable error:', data);
                      try { hls.destroy(); } catch(e) {}
                      break;
                  }
                }
              });
            } else if (video.canPlayType('application/vnd.apple.mpegurl')) {
              video.src = url;
            }
          }
        },
        poster: posterUrl || '',
        volume: 0.90,
        isLive: false,
        muted: false,
        autoplay: true,
        storage: false,
        pip: false,
        autoSize: false,
        autoMini: false,
        screenshot: false,
        setting: true,
        loop: false,
        flip: false,
        playbackRate: true,
        aspectRatio: false,
        fullscreen: false,
        fullscreenWeb: true,
        autoOrientation: true,
        subtitleOffset: true,
        miniProgressBar: true,
        mutex: true,
        backdrop: true,
        playsInline: true,
        autoPlayback: true,
        airplay: true,
        theme: '#ff4f73',
        lang: 'en',
        moreVideoAttr: {
          crossOrigin: 'anonymous',
          playsinline: 'true',
          'webkit-playsinline': 'true'
        },
        subtitle: {
          url: subUrl,
          type: 'vtt',
          style: {
            color: '#ffffff',
            fontSize: 'clamp(13px, 3vw, 20px)',
            textShadow: '0 0 4px #000, 0 0 8px #000, 0 2px 4px #000',
            fontFamily: "'Plus Jakarta Sans', sans-serif",
            fontWeight: '600'
          },
          encoding: 'utf-8',
          escape: false,
          onVttLoad: sanitizeVtt,
        },
        controls: [
          // Previous Episode button (Left controls)
          {
            name: 'prev-ep-btn',
            position: 'left',
            index: 10,
            html: '<button class="art-icon-btn" title="Previous Episode"><svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><polygon points="19 20 9 12 19 4 19 20"></polygon><line x1="5" y1="19" x2="5" y2="5" stroke="currentColor" stroke-width="2.5"></line></svg></button>',
            click: function () {
              if (typeof self.onPrevEpisode === 'function') {
                self.onPrevEpisode();
              }
            },
          },
          // Next Episode button (Left controls)
          {
            name: 'next-ep-btn',
            position: 'left',
            index: 11,
            html: '<button class="art-icon-btn" title="Next Episode"><svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><polygon points="5 4 15 12 5 20 5 4"></polygon><line x1="19" y1="5" x2="19" y2="19" stroke="currentColor" stroke-width="2.5"></line></svg></button>',
            click: function () {
              if (typeof self.onNextEpisode === 'function') {
                self.onNextEpisode();
              }
            },
          },
          // Skip Intro (+85s) button (Left controls)
          {
            name: 'skip-intro-btn',
            position: 'left',
            index: 12,
            html: '<button class="art-btn-skip" title="Skip Opening (+85s)">+85s</button>',
            click: function () {
              if (self.art) {
                self.art.currentTime = Math.min(self.art.duration, self.art.currentTime + 85);
              }
            },
          },
          // Audio Mode SUB / DUB toggle button (Right controls)
          {
            name: 'audio-mode-btn',
            position: 'right',
            index: 10,
            html: `<button class="art-btn-mode" title="Switch Audio Track (SUB / DUB)">${mode.toUpperCase()}</button>`,
            click: function () {
              if (typeof self.onToggleMode === 'function') {
                self.onToggleMode();
              }
            },
          },
          // Captions Disable/Enable CC Button (Right controls)
          {
            name: 'cc-btn',
            position: 'right',
            index: 11,
            html: `<button class="art-btn-cc ${this.captionsEnabled ? '' : 'disabled'}" title="Toggle Captions (CC)">CC</button>`,
            click: function () {
              self.toggleCaptions();
            },
          },
          // Streaming Quality Button with popup selector (Right controls)
          {
            name: 'quality-btn',
            position: 'right',
            index: 12,
            html: `<button class="art-btn-quality" title="Change Streaming Quality">${this.currentQualityLabel}</button>`,
            selector: [{ html: 'Auto', level: -1, default: true }],
            onSelect: function (item) {
              return self.setQuality(item.level);
            },
          },
          // Unified Master Fullscreen Button with auto-rotation (Right controls)
          {
            name: 'custom-fullscreen-btn',
            position: 'right',
            index: 99,
            html: '<button class="art-icon-btn art-btn-fs" title="Toggle Fullscreen" style="display:inline-flex;align-items:center;justify-content:center;color:#00e3fd;"><svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M7 14H5v5h5v-2H7v-3zm-2-4h2V7h3V5H5v5zm12 7h-3v2h5v-5h-2v3zM14 5v2h3v3h2V5h-5z"/></svg></button>',
            click: function () {
              self.toggleFullscreen();
            },
          }
        ]
      });

      // Events
      this.art.on('ready', () => {
        if (savedTime > 0) {
          this.art.currentTime = savedTime;
        }
        // Sync initial captions state
        if (this.art.subtitle) {
          this.art.subtitle.show = this.captionsEnabled;
        }
        // Guarantee audio is unmuted and at full volume
        try {
          if (this.art.video) {
            this.art.video.muted = false;
            this.art.volume = 0.90;
          }
        } catch (e) {}
      });

      // Handle browser autoplay policy restrictions: unmute on first user click/touch/remote press
      const ensureAudioAudible = () => {
        try {
          if (self.art && self.art.video && self.art.video.muted) {
            self.art.video.muted = false;
            self.art.volume = 0.90;
          }
        } catch (e) {}
      };
      window.addEventListener('click', ensureAudioAudible, { once: true });
      window.addEventListener('keydown', ensureAudioAudible, { once: true });
      window.addEventListener('touchstart', ensureAudioAudible, { once: true });

      // Fullscreen and Orientation Event Sync
      this.art.on('fullscreen', (isFull) => {
        self.handleFullscreenChange(isFull);
      });

      this.art.on('fullscreenWeb', (isFull) => {
        self.handleFullscreenChange(isFull);
      });

      // Clean any raw or escaped HTML tags in subtitle lines so <i> or <b> never display as literal text
      this.art.on('subtitleAfterUpdate', () => {
        try {
          const subLines = self.art.template?.$subtitle?.querySelectorAll('.art-subtitle-line');
          if (subLines) {
            subLines.forEach(line => {
              if (line.innerHTML.includes('&lt;i&gt;') || line.innerHTML.includes('&lt;/i&gt;') || line.innerHTML.includes('&lt;b&gt;') || line.innerHTML.includes('&lt;/b&gt;')) {
                line.innerHTML = line.innerHTML
                  .replace(/&lt;(\/?[a-zA-Z0-9]+)&gt;/gi, '<$1>');
              }
            });
          }
        } catch (e) {}
      });

      this.art.on('video:timeupdate', () => {
        if (this.art && this.art.currentTime > 0) {
          try {
            localStorage.setItem(`ani_progress_${episodeId}`, JSON.stringify({
              time: this.art.currentTime,
              duration: this.art.duration,
              updatedAt: Date.now()
            }));
          } catch {}
          if (typeof this.onTimeUpdate === 'function') {
            this.onTimeUpdate(this.art.currentTime, this.art.duration);
          }
        }
      });

      this.art.on('video:ended', () => {
        console.log('[Player] Episode completed.');
        if (this.autoNext && typeof this.onNextEpisode === 'function') {
          this.onNextEpisode();
        }
      });

      return streamData;
    } catch (err) {
      console.error('[Player] Error loading stream:', err);
      if (mount) {
        mount.innerHTML = `
          <div class="w-full h-full flex flex-col items-center justify-center bg-black/95 text-white gap-3 p-6 text-center select-none">
            <span class="material-symbols-outlined text-[36px] text-red-400">error</span>
            <div class="text-sm font-bold text-white">Stream Currently Unavailable</div>
            <div class="text-xs text-gray-400 max-w-sm">${err.message || 'The video provider did not return stream for this episode.'}</div>
            <button class="mt-2 px-4 py-2 rounded-lg bg-surface-container-high border border-white/[0.1] text-xs font-mono font-bold text-secondary hover:text-white" onclick="window.location.reload()">Retry Stream</button>
          </div>
        `;
      }
      throw err;
    }
  }

  /**
   * Toggles captions (subtitles) on or off
   */
  toggleCaptions() {
    this.captionsEnabled = !this.captionsEnabled;
    if (this.art && this.art.subtitle) {
      this.art.subtitle.show = this.captionsEnabled;
    }
    if (this.art && this.art.notice) {
      this.art.notice.show = this.captionsEnabled ? 'Captions: Enabled' : 'Captions: Disabled';
    }

    // Update in-player CC button appearance
    const ccBtn = this.art?.template?.$controls?.querySelector('.art-btn-cc');
    if (ccBtn) {
      ccBtn.classList.toggle('disabled', !this.captionsEnabled);
    }

    if (typeof this.onCaptionsToggle === 'function') {
      this.onCaptionsToggle(this.captionsEnabled);
    }

    return this.captionsEnabled;
  }

  /**
   * Sets streaming quality level (-1 for Auto, or level index)
   */
  setQuality(levelIndex) {
    levelIndex = parseInt(levelIndex);
    if (this.art && this.art.hls) {
      this.art.hls.currentLevel = levelIndex;
      const match = this.levels.find(l => l.level === levelIndex);
      const label = match ? match.html : (levelIndex === -1 ? 'Auto' : `${this.art.hls.levels[levelIndex]?.height || ''}p`);
      this.currentQualityLabel = label;

      if (this.art.notice) {
        this.art.notice.show = `Quality: ${label}`;
      }

      // Update in-player quality button label
      const qBtn = this.art.template?.$controls?.querySelector('.art-btn-quality');
      if (qBtn) qBtn.textContent = label;

      if (typeof this.onQualityChange === 'function') {
        this.onQualityChange(levelIndex, label);
      }
      return label;
    }
    return null;
  }

  /**
   * Universal Mobile & Desktop Fullscreen with Automatic Orientation Rotation
   */
  async toggleFullscreen() {
    if (!this.art) return false;

    const isCurrentlyFull = !!(this.art.fullscreen || this.art.fullscreenWeb || document.fullscreenElement);

    if (isCurrentlyFull) {
      if (this.art.fullscreen) this.art.fullscreen = false;
      if (this.art.fullscreenWeb) this.art.fullscreenWeb = false;
      if (document.fullscreenElement) {
        try { await document.exitFullscreen(); } catch (e) {}
      }
      this.handleFullscreenChange(false);
      return false;
    }

    const isMobile = /Android|iPhone|iPad|iPod/i.test(navigator.userAgent) || (window.innerWidth < 768 && 'ontouchstart' in window);
    const isIOS = /iPhone|iPad|iPod/i.test(navigator.userAgent) || (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1);

    // Standard HTML5 Fullscreen (Desktop & Android Chrome/Firefox)
    const canNativeFs = !!(document.fullscreenEnabled || document.documentElement.requestFullscreen || document.documentElement.webkitRequestFullscreen);

    if (canNativeFs && !isIOS) {
      try {
        this.art.fullscreen = true;
        this.handleFullscreenChange(true);
        return true;
      } catch (err) {
        console.warn('[Player] Native fullscreen failed, attempting web fullscreen:', err);
      }
    }

    // Web Fullscreen Fallback (100% reliable on iOS Safari, PWA, and all mobile viewports)
    this.art.fullscreenWeb = true;
    this.handleFullscreenChange(true);
    return true;
  }

  /**
   * Synchronizes orientation, body classes, and UI buttons when fullscreen changes
   */
  async handleFullscreenChange(isFull) {
    if (isFull) {
      document.body.classList.add('player-in-fullscreen');

      // Attempt screen orientation lock to landscape on mobile devices
      if (screen.orientation && typeof screen.orientation.lock === 'function') {
        try {
          await screen.orientation.lock('landscape');
        } catch (e) {
          console.warn('[Player] screen.orientation.lock unavailable or denied:', e.message);
        }
      }
    } else {
      document.body.classList.remove('player-in-fullscreen');

      if (screen.orientation && typeof screen.orientation.unlock === 'function') {
        try {
          screen.orientation.unlock();
        } catch (e) {}
      }
    }

    this.updateFullscreenIcons(isFull);
    if (typeof this.onFullscreenChange === 'function') {
      this.onFullscreenChange(isFull);
    }
  }

  updateFullscreenIcons(isFull) {
    // In-player custom fullscreen button
    const fsBtn = this.art?.template?.$controls?.querySelector('.art-btn-fs');
    if (fsBtn) {
      fsBtn.innerHTML = isFull
        ? '<svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M5 16h3v3h2v-5H5v2zm3-8H5v2h5V5H8v3zm6 11h2v-3h3v-2h-5v5zm2-11V5h-2v5h5V8h-3z"/></svg>'
        : '<svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M7 14H5v5h5v-2H7v-3zm-2-4h2V7h3V5H5v5zm12 7h-3v2h5v-5h-2v3zM14 5v2h3v3h2V5h-5z"/></svg>';
      fsBtn.setAttribute('title', isFull ? 'Exit Fullscreen' : 'Enter Fullscreen');
    }

    // External Down Fullscreen button (#btn-fullscreen-down)
    const btnDown = document.getElementById('btn-fullscreen-down');
    if (btnDown) {
      btnDown.innerHTML = isFull
        ? '<span class="material-symbols-outlined text-[17px]">fullscreen_exit</span><span class="hidden sm:inline">Exit Fullscreen</span>'
        : '<span class="material-symbols-outlined text-[17px]">fullscreen</span><span class="hidden sm:inline">Fullscreen</span>';
      if (isFull) {
        btnDown.classList.add('border-primary/50', 'text-primary');
        btnDown.classList.remove('border-secondary/30', 'text-secondary');
      } else {
        btnDown.classList.remove('border-primary/50', 'text-primary');
        btnDown.classList.add('border-secondary/30', 'text-secondary');
      }
    }
  }

  /**
   * Completely stops, pauses, and tears down video/audio to guarantee no background sound
   */
  stop() {
    document.body.classList.remove('player-in-fullscreen');
    if (screen.orientation && typeof screen.orientation.unlock === 'function') {
      try { screen.orientation.unlock(); } catch (e) {}
    }

    if (this.art) {
      try {
        if (this.art.hls && typeof this.art.hls.destroy === 'function') {
          this.art.hls.destroy();
          this.art.hls = null;
        }
        if (this.art.video) {
          this.art.video.pause();
          this.art.video.muted = true;
          this.art.video.removeAttribute('src');
          this.art.video.load();
        }
        this.art.destroy(true);
      } catch (err) {
        console.warn('[Player] Error during stop:', err);
      }
      this.art = null;
    }

    // Stop and silence ALL audio & video elements anywhere in the mount or page
    try {
      const mount = document.querySelector(this.mountSelector);
      const mediaList = mount ? mount.querySelectorAll('video, audio') : document.querySelectorAll('video, audio');
      mediaList.forEach(media => {
        try {
          media.pause();
          media.muted = true;
          media.removeAttribute('src');
          media.load();
        } catch (e) {}
      });
      if (mount) mount.innerHTML = '';
    } catch (e) {}

    // Clean any lingering Artplayer instances
    if (window.Artplayer && Array.isArray(window.Artplayer.instances)) {
      window.Artplayer.instances.slice().forEach(inst => {
        try { inst.destroy(true); } catch (e) {}
      });
    }
  }
}

window.AnimePlayer = AnimePlayer;
