# DORAI AI — AI Assistant Frontend UI Audit & Polish Plan

## Overview
This plan covers a comprehensive audit and polish of the AI Assistant frontend UI,
focusing on visual consistency, UX improvements, accessibility, performance, and branding.

## 1. Branding & Visual Identity

### 1.1 Logo & App Identity
- [ ] Ensure DORAI AI logo is used consistently across all screens (TopBar, EmptyState, Settings, About)
- [ ] Update app name references from "DORAI AI" to "DORAI AI" in UI strings
- [ ] Verify logo animation (LogoAnimation.kt) uses new brand assets
- [ ] Check splash screen / launcher icon consistency

### 1.2 Color Scheme & Theme
- [ ] Audit `Theme.kt` — ensure DORAI AI brand colors are applied
- [ ] Dark/light theme consistency across all screens
- [ ] Accent color usage (buttons, links, active states)
- [ ] Error/warning/success color semantics consistent

### 1.3 Typography
- [ ] Consistent font families and sizes across screens
- [ ] Heading hierarchy (H1-H6 equivalent in Compose)
- [ ] Code block font (monospace) consistency in markdown rendering
- [ ] Line height and letter spacing for readability

## 2. Chat Screen (Primary UI)

### 2.1 Empty State
- [ ] EmptyState.kt — welcome message, suggested prompts, logo display
- [ ] First-run experience (onboarding hints)
- [ ] Suggested action chips

### 2.2 Message Bubbles
- [ ] BotMessage.kt — markdown rendering, code blocks, tool results
- [ ] UserMessage.kt — input display, attachment previews
- [ ] ToolMessage.kt — tool call/result formatting, collapsible sections
- [ ] ErrorMessage.kt — error styling, retry button, dismiss
- [ ] Message spacing and alignment consistency

### 2.3 Input Area
- [ ] Text input field styling and placeholder
- [ ] Send button state (enabled/disabled/loading)
- [ ] Attachment button (image, file)
- [ ] Voice input button (if applicable)
- [ ] Skill autocomplete (SkillAutocomplete.kt) positioning and styling
- [ ] Multi-line input handling

### 2.4 Top Bar
- [ ] TopBar.kt — model selector, settings gear, new chat button
- [ ] Title display (conversation name or "New Chat")
- [ ] Action button spacing and touch targets

### 2.5 Chat History
- [ ] ChatHistorySheet.kt — conversation list, search, delete
- [ ] Empty history state
- [ ] Conversation preview (last message, timestamp)
- [ ] Swipe-to-delete or long-press menu

## 3. Settings Screen

### 3.1 Navigation & Layout
- [ ] SettingsScreen.kt — section list, scroll behavior
- [ ] SettingsListItem.kt — consistent item styling
- [ ] Section headers and dividers

### 3.2 Provider/Model Selection
- [ ] ModelSelectionSheet.kt — model list, search, filter
- [ ] Provider cards with logo and status
- [ ] Free provider suggestions (FreeProviderSuggestionsPanel.kt)

### 3.3 MCP Settings
- [ ] McpSection.kt — server list, add/edit/delete
- [ ] Connection status indicators
- [ ] URL validation feedback (new McpUrlValidator integration)

### 3.4 Tool Settings
- [ ] ToolsSettings.kt — tool enable/disable toggles
- [ ] Tool description and status

### 3.5 General Settings
- [ ] GeneralSettings.kt — theme, language, etc.
- [ ] Export/Import (ExportImportSection.kt)

## 4. Markdown & Rich Content

### 4.1 Markdown Renderer
- [ ] MarkdownRenderer.kt — heading, paragraph, list, table, code block
- [ ] CodeHighlighter.kt — syntax highlighting accuracy
- [ ] CodeFenceBlock.kt — copy button, language label
- [ ] MathRenderer.kt — LaTeX rendering quality
- [ ] InlineContent.kt — image, link, bold, italic, code

### 4.2 Dynamic UI (DORAI AI UI)
- [ ] KaiUiRenderer.kt — custom UI components from AI
- [ ] KaiUiParser.kt — parsing accuracy
- [ ] KaiUiNodeBuilders.kt — component builders
- [ ] TTS integration (KaiUiTts.kt)

## 5. Sandbox UI

### 5.1 Sandbox Screens
- [ ] SandboxTabsContent.kt — tab navigation
- [ ] SandboxFileBrowserScreen.kt — file tree, navigation
- [ ] SandboxPackagesScreen.kt — package list, install
- [ ] SandboxStatusText.kt — status indicators

### 5.2 Terminal
- [ ] TerminalSheet.kt — terminal emulator UI
- [ ] TerminalKeyRow.kt / TerminalKeyIcons.kt — virtual keyboard
- [ ] AnsiParser.kt — ANSI color rendering

## 6. Build Screen (DORAI Build)

### 6.1 Build UI
- [ ] KaiBuildScreen.kt — project list, build status
- [ ] BuildSetupContent.kt — setup wizard
- [ ] BuildTerminalContent.kt — terminal output
- [ ] BuildSessionBar.kt — session management
- [ ] BuildProjectsContent.kt — project cards

## 7. Components & Reusable UI

### 7.1 Shared Components
- [ ] KaiChip.kt — chip styling consistency
- [ ] KaiSlider.kt — slider theming
- [ ] KaiSearchField.kt — search input styling
- [ ] SettingsListItem.kt — list item consistency
- [ ] SmallIconButton.kt — icon button sizing
- [ ] RefreshIconButton.kt — refresh animation
- [ ] AnimatedGradientBorder.kt — gradient animation performance
- [ ] FullScreenImageHost.kt — image viewer

### 7.2 Icons
- [ ] KaiIcons.kt — icon set completeness
- [ ] Consistent icon sizing and tinting

## 8. Accessibility

### 8.1 Screen Reader Support
- [ ] Content descriptions on all interactive elements
- [ ] Semantic roles (button, heading, etc.)
- [ ] Focus management (keyboard navigation)

### 8.2 Visual Accessibility
- [ ] Color contrast ratios (WCAG AA minimum)
- [ ] Touch target sizes (minimum 48dp)
- [ ] Text scaling support

### 8.3 Motion & Animation
- [ ] Reduced motion support
- [ ] Animation performance (60fps target)
- [ ] Loading state indicators

## 9. Performance

### 9.1 Rendering
- [ ] Lazy loading for long chat histories
- [ ] Image loading optimization
- [ ] Markdown rendering performance for large messages

### 9.2 Memory
- [ ] Bitmap caching and cleanup
- [ ] Stream collector memory management

## 10. Cross-Platform Consistency

### 10.1 Platform-Specific
- [ ] Android: Material You dynamic colors
- [ ] Desktop: window management, menu bar
- [ ] iOS: native feel (if applicable)
- [ ] Web: responsive layout

### 10.2 Responsive Design
- [ ] Tablet/landscape layout
- [ ] Minimum window size handling

## Implementation Phases

### Phase 1: Audit & Inventory (Read-Only)
- Screenshot all screens on Desktop + Android
- Document current state vs. desired state
- Identify top 10 visual inconsistencies
- Create priority matrix (impact vs. effort)

### Phase 2: Branding & Theme (High Impact, Low Effort)
- Logo integration verification
- Color scheme update
- Typography standardization

### Phase 3: Chat Screen Polish (High Impact, Medium Effort)
- Message bubble refinements
- Input area improvements
- Empty state enhancement

### Phase 4: Settings & Components (Medium Impact, Medium Effort)
- Settings screen cleanup
- Component consistency
- Icon standardization

### Phase 5: Accessibility & Performance (Medium Impact, High Effort)
- Screen reader support
- Color contrast fixes
- Performance optimization

### Phase 6: Cross-Platform Polish (Low Impact, High Effort)
- Platform-specific refinements
- Responsive design

## Priority Matrix

| Priority | Area | Effort | Impact |
|----------|------|--------|--------|
| P1 | Branding (logo, colors, name) | Low | High |
| P1 | Chat message bubbles | Medium | High |
| P2 | Settings screen consistency | Medium | Medium |
| P2 | Markdown rendering quality | Medium | Medium |
| P3 | Accessibility (contrast, touch targets) | High | Medium |
| P3 | Performance optimization | High | Medium |
| P4 | Cross-platform polish | High | Low |

## Files to Audit (Key Files)

### Chat
- `ui/chat/ChatScreen.kt`
- `ui/chat/composables/BotMessage.kt`
- `ui/chat/composables/UserMessage.kt`
- `ui/chat/composables/TopBar.kt`
- `ui/chat/composables/EmptyState.kt`
- `ui/chat/composables/ChatHistorySheet.kt`

### Settings
- `ui/settings/SettingsScreen.kt`
- `ui/settings/SettingsListItem.kt`
- `ui/settings/GeneralSettings.kt`
- `ui/settings/ModelSelectionSheet.kt`

### Theme
- `ui/Theme.kt`

### Components
- `ui/components/*.kt`

### Markdown
- `ui/markdown/MarkdownRenderer.kt`
- `ui/markdown/MarkdownInlineRenderer.kt`
- `ui/markdown/CodeFenceBlock.kt`

## Notes
- This is a planning document only — no code changes until explicit approval per phase
- Each phase requires separate approval before implementation
- Screenshots should be captured before and after each phase
- User communicates in Bengali; keep responses bilingual