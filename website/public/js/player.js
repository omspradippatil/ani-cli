// website/public/js/player.js
// Cinema-Grade Artplayer.js Video Player Engine with Hls.js & Subtitles for Om CLI

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
    this.autoNext = true;
  }

  /**
   * Initializes or updates Artplayer with a stream
   */
  async loadStream(episodeId, mode = 'sub', posterUrl = '') {
    this.currentEpisodeId = episodeId;
    this.currentMode = mode;

    try {
      const res = await fetch(`/api/stream/${episodeId}?mode=${mode}`);
      const json = await res.json();

      if (!json.success || !json.data) {
        throw new Error(json.error || 'Failed to fetch episode stream');
      }

      const streamData = json.data;
      const streamUrl = streamData.proxyM3u8;

      console.log(`[Player] Loaded stream for episode ${episodeId} (${mode}):`, streamUrl);

      // Check saved progress
      let savedTime = 0;
      try {
        const saved = JSON.parse(localStorage.getItem(`ani_progress_${episodeId}`));
        if (saved && saved.time && saved.time < saved.duration - 15) {
          savedTime = saved.time;
        }
      } catch {}

      // Prepare Subtitles
      let subUrl = '';
      if (streamData.subtitles && streamData.subtitles.length > 0) {
        const primarySub = streamData.subtitles.find(s => s.default) || streamData.subtitles[0];
        subUrl = primarySub.proxySrc || primarySub.src || '';
      }

      // Destroy previous player instance if exists
      this.stop();

      const self = this;

      // Create new Artplayer instance
      this.art = new window.Artplayer({
        container: this.mountSelector,
        url: streamUrl,
        type: 'm3u8',
        customType: {
          m3u8: function (video, url, art) {
            if (window.Hls && window.Hls.isSupported()) {
              if (art.hls) art.hls.destroy();

              const hls = new window.Hls({
                enableWorker: true,
                maxBufferLength: 30,
                maxMaxBufferLength: 60
              });

              hls.loadSource(url);
              hls.attachMedia(video);
              art.hls = hls;

              art.on('destroy', () => {
                if (hls) hls.destroy();
              });

              hls.on(window.Hls.Events.MANIFEST_PARSED, () => {
                // Populate dynamic quality selector in Artplayer
                if (hls.levels && hls.levels.length > 1) {
                  const levels = hls.levels.map((lvl, idx) => ({
                    html: `${lvl.height}p`,
                    level: idx,
                    default: idx === hls.currentLevel
                  }));
                  levels.unshift({ html: 'Auto', level: -1, default: true });

                  art.setting.update({
                    name: 'quality',
                    html: 'Quality',
                    tooltip: 'Auto',
                    selector: levels,
                    onSelect: function (item) {
                      hls.currentLevel = item.level;
                      return item.html;
                    }
                  });
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
                      hls.destroy();
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
        volume: 0.85,
        isLive: false,
        muted: false,
        autoplay: true,
        pip: true,
        autoSize: false,
        autoMini: false,
        screenshot: false, // Streamlined to prevent controls bar overflow
        setting: true,
        loop: false,
        flip: false,
        playbackRate: true,
        aspectRatio: false,
        fullscreen: true,
        fullscreenWeb: true,
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
        },
        subtitle: {
          url: subUrl,
          type: 'vtt',
          style: {
            color: '#ffffff',
            fontSize: '22px',
            textShadow: '0 0 5px #000, 0 0 8px #000',
            fontFamily: "'Plus Jakarta Sans', sans-serif",
            fontWeight: '600'
          },
          encoding: 'utf-8',
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
          // Audio Mode SUB / DUB toggle button (Right controls, accessible in fullscreen)
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
          }
        ]
      });

      // Events
      this.art.on('ready', () => {
        if (savedTime > 0) {
          this.art.currentTime = savedTime;
        }
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
      alert(`Streaming Error: ${err.message}`);
      throw err;
    }
  }

  /**
   * Completely stops, pauses, and tears down video/audio to guarantee no background sound
   */
  stop() {
    if (this.art) {
      try {
        if (this.art.hls) {
          this.art.hls.destroy();
          this.art.hls = null;
        }
        if (this.art.video) {
          this.art.video.pause();
          this.art.video.removeAttribute('src');
          this.art.video.load();
        }
        this.art.destroy(false);
      } catch (err) {
        console.warn('[Player] Error during stop:', err);
      }
      this.art = null;
    }
    const mount = document.querySelector(this.mountSelector);
    if (mount) mount.innerHTML = '';
  }
}

window.AnimePlayer = AnimePlayer;
