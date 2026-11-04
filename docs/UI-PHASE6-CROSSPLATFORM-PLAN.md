# DORAI AI — UI Phase 6: Cross-Platform Polish Plan

## Overview
Cross-platform UI consistency and responsive design improvements.
Requires testing on actual devices/emulators for each platform.

## Platform-Specific Areas

### Android
- [ ] Material You dynamic colors integration
- [ ] Edge-to-edge display handling
- [ ] Status bar / navigation bar theming
- [ ] Foldable device support (if applicable)
- [ ] Tablet layout (two-pane for settings/chat)

### Desktop (JVM)
- [ ] Window minimum size handling
- [ ] Menu bar integration (File, Edit, View, Help)
- [ ] Keyboard shortcuts (Ctrl+N new chat, Ctrl+, settings, etc.)
- [ ] Resizable panels (chat + sandbox side-by-side)
- [ ] High-DPI display support

### iOS (if applicable)
- [ ] Native navigation patterns
- [ ] Safe area handling
- [ ] Dynamic Type support
- [ ] Haptic feedback

### Web (WASM)
- [ ] Responsive layout (mobile/tablet/desktop breakpoints)
- [ ] PWA support (offline, install prompt)
- [ ] Browser back/forward navigation
- [ ] Touch-friendly controls on mobile web

## Responsive Design
- [ ] Breakpoints: compact (<600dp), medium (600-840dp), expanded (>840dp)
- [ ] Settings: two-column on expanded, single on compact
- [ ] Chat: sidebar on expanded, full-width on compact
- [ ] Navigation: bottom bar on compact, rail on medium, drawer on expanded

## Testing Checklist
- [ ] Android: Phone (portrait/landscape), Tablet
- [ ] Desktop: Windows, macOS, Linux (various window sizes)
- [ ] Web: Chrome, Firefox, Safari (mobile + desktop)
- [ ] iOS: iPhone, iPad (if applicable)

## Implementation Notes
- Use `BoxWithConstraints` for responsive layouts
- Use `WindowSizeClass` from Material3 adaptive
- Test with accessibility settings (large text, high contrast)
- Profile performance on low-end devices

## Status
- **Phase 6 NOT implemented** — requires platform testing infrastructure
- Plan document created for future reference
- Desktop + Android builds verified (Phase 1-5)