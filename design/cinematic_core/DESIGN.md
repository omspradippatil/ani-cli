---
name: Cinematic Core
colors:
  surface: '#131313'
  surface-dim: '#131313'
  surface-bright: '#393939'
  surface-container-lowest: '#0e0e0e'
  surface-container-low: '#1c1b1b'
  surface-container: '#201f1f'
  surface-container-high: '#2a2a2a'
  surface-container-highest: '#353534'
  on-surface: '#e5e2e1'
  on-surface-variant: '#e9bcb6'
  inverse-surface: '#e5e2e1'
  inverse-on-surface: '#313030'
  outline: '#af8782'
  outline-variant: '#5e3f3b'
  surface-tint: '#ffb4aa'
  primary: '#ffb4aa'
  on-primary: '#690003'
  primary-container: '#e50914'
  on-primary-container: '#fff7f6'
  inverse-primary: '#c0000c'
  secondary: '#adc6ff'
  on-secondary: '#002e69'
  secondary-container: '#4b8eff'
  on-secondary-container: '#00285c'
  tertiary: '#a7c8ff'
  on-tertiary: '#003061'
  tertiary-container: '#0072d7'
  on-tertiary-container: '#f8f9ff'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#ffdad5'
  primary-fixed-dim: '#ffb4aa'
  on-primary-fixed: '#410001'
  on-primary-fixed-variant: '#930007'
  secondary-fixed: '#d8e2ff'
  secondary-fixed-dim: '#adc6ff'
  on-secondary-fixed: '#001a41'
  on-secondary-fixed-variant: '#004493'
  tertiary-fixed: '#d5e3ff'
  tertiary-fixed-dim: '#a7c8ff'
  on-tertiary-fixed: '#001b3c'
  on-tertiary-fixed-variant: '#004689'
  background: '#131313'
  on-background: '#e5e2e1'
  surface-variant: '#353534'
typography:
  display-lg:
    fontFamily: Inter
    fontSize: 72px
    fontWeight: '800'
    lineHeight: 80px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Inter
    fontSize: 48px
    fontWeight: '700'
    lineHeight: 56px
  headline-md:
    fontFamily: Inter
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
  body-lg:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: '400'
    lineHeight: 32px
  body-md:
    fontFamily: Inter
    fontSize: 20px
    fontWeight: '400'
    lineHeight: 28px
  label-lg:
    fontFamily: Inter
    fontSize: 18px
    fontWeight: '600'
    lineHeight: 24px
    letterSpacing: 0.05em
  label-sm:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '500'
    lineHeight: 20px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  safe-area-x: 80px
  safe-area-y: 48px
  gutter: 24px
  section-gap: 64px
  card-gap: 20px
---

## Brand & Style

The design system is engineered for high-end Android TV environments, prioritizing immersion, legibility, and a "lean-back" experience. The personality is cinematic, authoritative, and premium, evoking the feeling of a private theater. 

The style utilizes **Modern Minimalism** mixed with **Tonal Layering**. It avoids unnecessary ornamentation to keep the focus entirely on the content. High-contrast focus states and subtle motion are used to provide the user with a clear sense of orientation within a 10-foot UI context. The overall aesthetic is dark and expansive, using deep blacks to allow hardware-level OLED blacks to disappear into the room.

## Colors

The palette is strictly dark-mode to reduce eye strain in low-light viewing environments. 
- **Background**: A near-perfect black (#0A0A0A) to ensure depth and contrast for media posters.
- **Surface**: Subtle charcoals (#121212 and #1F1F1F) define secondary containers and navigation bars.
- **Primary**: A vibrant, high-energy Red (#E50914) serves as the primary "Action" color, used sparingly for critical call-outs, progress bars, and branding.
- **Neutral**: Grayscale tones are used for metadata and inactive states to maintain a clear visual hierarchy where content remains king.

## Typography

This design system uses **Inter** for its exceptional legibility and modern geometric construction. Because the interface is viewed from a distance, font sizes are significantly larger than standard web or mobile defaults.

- **Scale**: The scale is aggressive to ensure readability for metadata like "Year" and "Rating" from 10 feet away.
- **Weight**: Bold weights (700+) are used for titles to maintain presence against high-contrast imagery.
- **Hierarchy**: Use `display-lg` for featured content titles and `body-lg` for synopsis text. All labels use medium or semi-bold weights to prevent "shimmering" on lower-resolution TV panels.

## Layout & Spacing

The layout is strictly landscape and follows a **Fluid Grid** model that respects TV safe areas (Overscan).

- **Safe Areas**: A mandatory margin of 80px on the left/right and 48px on the top/bottom must be maintained to prevent UI elements from being cut off by physical TV bezels.
- **Horizontal Rows**: Content is organized in scrollable horizontal rows. The first item in a row must align with the left safe-area margin.
- **Rhythm**: Use a 4px base unit. Most spacing increments should be multiples of 8px (24px, 32px, 64px) to ensure clean alignment on 4K and 1080p displays.

## Elevation & Depth

Depth is used exclusively to signify the "Focused" state. This design system does not use traditional shadows for static elements.

- **Focus Elevation**: When an element (card, button) is focused, it scales to 1.1x its original size and gains a high-contrast white or primary-colored border (2px to 4px).
- **Z-Index Layering**: The navigation sidebar sits on a higher Z-index than the content grid, utilizing a 20px backdrop blur to maintain context while obscuring the background content.
- **Tonal Tiers**: Use #121212 for the base content area and #1F1F1F for persistent UI elements like the sidebar or header.

## Shapes

The design system uses a **Rounded** shape language to create a friendly and premium feel. 

- **Base Radius**: 24px (rounded-xl) for all content cards (Movies, TV Shows).
- **Secondary Radius**: 12px (rounded-md) for buttons and input fields.
- **Consistency**: All focus borders must follow the exact corner radius of the element they enclose to prevent visual gaps.

## Components

### Content Cards
The primary interaction unit.
- **Standard**: 16:9 or 2:3 aspect ratio with a 24px corner radius.
- **Focused State**: Scales to 1.1x. Add a 4px solid white border. Text metadata below the card may change color to the primary accent.

### Buttons
- **Default**: Semi-transparent grey (#FFFFFF1A) with white text.
- **Focused**: Solid white background with black text (#0A0A0A).
- **Icon Buttons**: Circular (50% radius) with a 60px x 60px hit area.

### Navigation Sidebar
- **State**: Collapsed by default showing only icons. Expands on focus to reveal text labels.
- **Visual**: 60% opacity background with a heavy backdrop blur (30px).

### Selection Controls
- **Checkboxes/Radios**: Large (40px) hit areas. Use the Primary Color (#E50914) for the "Checked" state.
- **Progress Bars**: Used for "Continue Watching." 4px height, using Primary Color for progress and #FFFFFF33 for the track.

### Input Fields
- **Search**: Large, full-width fields with a 12px radius. When focused, the background turns white and the cursor is the Primary Color.