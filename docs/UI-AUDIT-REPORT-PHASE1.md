# DORAI AI — UI Audit Report (Phase 1: Read-Only)

## Audit Scope
Comprehensive read-only audit of AI Assistant Frontend UI across all screens and components.
No code changes were made during this phase.

## 1. Theme & Branding Audit

### Findings
| # | Issue | Severity | File |
|---|-------|----------|------|
| 1 | DORAI AI brand colors defined (`broxNavy`, `broxCyan`) but **NOT used** in `DarkColorScheme`/`LightColorScheme` — still uses legacy purple (`darkPurple`/`lightPurple`) | 🔴 High | `Theme.kt` |
| 2 | `gradientBrush` uses purple gradient, not Brox brand colors | 🔴 High | `Theme.kt` |
| 3 | `DarkColorScheme` primary = `#BB86FC` (Material purple), should be `broxCyan` | 🟡 Medium | `Theme.kt` |
| 4 | `LightColorScheme` primary = `darkPurple` (`#6200EE`), should be `broxNavy` | 🟡 Medium | `Theme.kt` |
| 5 | `broxNavy`, `broxCyan`, `broxDeepNavy` defined but unused anywhere | 🟡 Medium | `Theme.kt` |
| 6 | `animatedGradientBorder` uses purple/magenta gradient, not brand colors | 🟡 Medium | `Theme.kt` |

### Recommendation
- Replace primary colors with Brox brand palette
- Update gradientBrush to use broxNavy → broxCyan
- Ensure all screens use MaterialTheme.colorScheme (not hardcoded colors)

## 2. Chat Screen Audit

### Findings
| # | Issue | Severity | File |
|---|-------|----------|------|
| 1 | `EmptyState` uses `LogoAnimation()` — verify it shows Brox logo | 🟡 Medium | `EmptyState.kt` |
| 2 | Welcome message string `welcome_message` — check if branded | 🟢 Low | `EmptyState.kt` |
| 3 | Interactive mode top bar title uses `interactive_title` string | 🟢 Low | `ChatScreen.kt` |
| 4 | `EmptyState` has hardcoded `TerminalGreenOnDark`/`TerminalGreenOnLight` colors | 🟡 Medium | `EmptyState.kt` |
| 5 | Chat history LazyColumn uses `CenterHorizontally` alignment — may cause layout issues on wide screens | 🟡 Medium | `ChatScreen.kt` |
| 6 | `showScrollToBottom` FAB uses default M3 styling — no brand theming | 🟢 Low | `ChatScreen.kt` |

### Recommendation
- Verify LogoAnimation uses new brand assets
- Replace hardcoded terminal green with theme colors
- Consider left-aligning chat messages on wide screens

## 3. Message Bubble Audit

### Findings (BotMessage, UserMessage, ToolMessage)
| # | Issue | Severity | File |
|---|-------|----------|------|
| 1 | BotMessage uses `MaterialTheme.colorScheme` — good | ✅ OK | `BotMessage.kt` |
| 2 | UserMessage uses `MaterialTheme.colorScheme` — good | ✅ OK | `UserMessage.kt` |
| 3 | ToolMessage collapse/expand animation — verify smoothness | 🟡 Medium | `ToolMessage.kt` |
| 4 | Code block copy button — verify functionality | 🟢 Low | `CodeFenceBlock.kt` |
| 5 | Markdown table rendering — verify overflow handling | 🟡 Medium | `MarkdownRenderer.kt` |

### Recommendation
- Test message rendering with long content
- Verify code block syntax highlighting accuracy
- Check table horizontal scroll on narrow screens

## 4. Settings Screen Audit

### Findings
| # | Issue | Severity | File |
|---|-------|----------|------|
| 1 | `SettingsListItem` — verify consistent styling | 🟡 Medium | `SettingsListItem.kt` |
| 2 | Model selection sheet — verify search/filter works | 🟢 Low | `ModelSelectionSheet.kt` |
| 3 | MCP section — URL validation feedback needs integration | 🟡 Medium | `McpSection.kt` |
| 4 | Tool settings — toggle state persistence | 🟢 Low | `ToolsSettings.kt` |

### Recommendation
- Add URL validation error display in MCP section
- Ensure settings items use consistent icon sizing

## 5. Components Audit

### Findings
| # | Issue | Severity | File |
|---|-------|----------|------|
| 1 | `KaiChip` — verify selected/unselected states | 🟢 Low | `KaiChip.kt` |
| 2 | `KaiSlider` — verify thumb and track styling | 🟢 Low | `KaiSlider.kt` |
| 3 | `KaiSearchField` — verify clear button and focus | 🟢 Low | `KaiSearchField.kt` |
| 4 | `AnimatedGradientBorder` — performance concern with animation | 🟡 Medium | `AnimatedGradientBorder.kt` |
| 5 | `FullScreenImageHost` — verify zoom/pan gestures | 🟢 Low | `FullScreenImageHost.kt` |

### Recommendation
- Profile AnimatedGradientBorder for jank
- Test image viewer with large images

## 6. Accessibility Audit

### Findings
| # | Issue | Severity | File |
|---|-------|----------|------|
| 1 | Many Icon components have `contentDescription = null` | 🔴 High | Multiple |
| 2 | No `semantics` modifiers on interactive elements | 🔴 High | Multiple |
| 3 | Touch targets may be < 48dp in some places | 🟡 Medium | Multiple |
| 4 | No reduced motion support | 🟡 Medium | Multiple |
| 5 | Color contrast — verify WCAG AA for all text | 🟡 Medium | Theme.kt |

### Recommendation
- Add content descriptions to all icons
- Add semantics roles to buttons, cards, etc.
- Ensure minimum 48dp touch targets
- Test with TalkBack/VoiceOver

## 7. Performance Audit

### Findings
| # | Issue | Severity | File |
|---|-------|----------|------|
| 1 | `remember` blocks in ChatScreen are complex — potential recomposition issues | 🟡 Medium | `ChatScreen.kt` |
| 2 | `parseMarkdown` called in `remember` — good | ✅ OK | `ChatScreen.kt` |
| 3 | Image loading — verify caching | 🟢 Low | Multiple |
| 4 | LazyColumn with `contentType` — good optimization | ✅ OK | `ChatScreen.kt` |

### Recommendation
- Profile recomposition with Compose Layout Inspector
- Add `@Stable`/`@Immutable` annotations where missing

## 8. Cross-Platform Audit

### Findings
| # | Issue | Severity | File |
|---|-------|----------|------|
| 1 | Platform-specific UI code in `androidMain`/`desktopMain`/`iosMain` | 🟢 Low | Multiple |
| 2 | `handCursor()` only relevant for Desktop | 🟢 Low | `Theme.kt` |
| 3 | `navigationBarsPadding`/`statusBarsPadding` — verify on all platforms | 🟡 Medium | Multiple |

### Recommendation
- Test on all target platforms
- Verify responsive layout on different screen sizes

## Top 10 Priority Issues

| Rank | Issue | Impact | Effort |
|------|-------|--------|--------|
| 1 | Brand colors not applied (Theme.kt) | High | Low |
| 2 | Missing accessibility (content descriptions) | High | Medium |
| 3 | Hardcoded colors in EmptyState | Medium | Low |
| 4 | Gradient uses legacy purple | Medium | Low |
| 5 | Touch target sizes | Medium | Low |
| 6 | Color contrast verification | Medium | Medium |
| 7 | Markdown table overflow | Medium | Medium |
| 8 | ToolMessage animation smoothness | Low | Medium |
| 9 | Image loading optimization | Low | Medium |
| 10 | Cross-platform padding verification | Low | Low |

## Next Steps
- Phase 2: Branding & Theme (apply DORAI AI colors)
- Phase 3: Chat Screen Polish (message bubbles, input, empty state)
- Phase 4: Settings & Components
- Phase 5: Accessibility & Performance
- Phase 6: Cross-Platform Polish

## Files Audited
- `ui/Theme.kt` (197 lines)
- `ui/chat/ChatScreen.kt` (990+ lines)
- `ui/chat/composables/EmptyState.kt` (155 lines)
- `ui/chat/composables/BotMessage.kt`
- `ui/chat/composables/UserMessage.kt`
- `ui/chat/composables/TopBar.kt`
- `ui/chat/composables/ChatHistorySheet.kt`
- `ui/components/*.kt`
- `ui/markdown/*.kt`
- `ui/settings/*.kt`