# Visual Regression Matrix

This matrix defines the required visual regression coverage for DORAI AI. Paparazzi screenshot tests are the baseline for deterministic Compose rendering; manual/runtime accessibility checks remain separate verification items.

## Theme and surface coverage

| Surface | Light | Dark | Midnight | OLED Black |
|---|---:|---:|---:|---:|
| Chat with messages | Yes | Yes | Yes | Yes |
| Chat empty state | Yes | Existing baseline | — | — |
| Chat dynamic UI | Yes | — | — | — |
| Chat with Sandbox open | — | Yes | — | — |
| Settings Agent | — | Yes | Yes | Yes |
| Settings Free | — | Yes | — | — |
| Settings Tools | Yes | — | — | — |

System mode follows the host platform appearance and is verified through runtime/manual checks rather than a deterministic screenshot fixture.

## Device-size coverage

| Target | Existing coverage | Required review |
|---|---|---|
| Phone portrait | ScreenshotTest | Chat, Settings, long localized labels |
| Tablet | TabletStoreScreenshotTest | Chat and Settings in Light/Dark/Midnight/OLED |
| Desktop compact (840–900 dp) | Runtime/UI test required | Conversation rail and Sandbox width constraints |
| Desktop medium (1024 dp) | Runtime/UI test required | Main chat minimum width and composer |
| Desktop wide (1280 dp+) | Runtime/UI test required | Rail, chat, and side panel coexistence |

## Accessibility and localization review

- Settings desktop navigation exposes Role.Tab and selected state.
- Settings mobile selector exposes Role.Tab, selected state, keyboard focus, and left/right arrow navigation.
- Keyboard Tab and Shift+Tab retain normal focus traversal.
- Review default font scale plus 1.3x, 1.5x, and 2.0x on phone/tablet.
- Review English and Bengali labels; add German/French long-string fixtures when those locales are introduced.
- Review focus, border, selected-state, and body-text contrast on Dark, Midnight, and OLED.

## CI expectations

- Run :composeApp:allTests for shared logic and Compose common tests.
- Run the screenshot test module's test task and verify no unexpected snapshot changes.
- Treat intentional visual changes as reviewed snapshots, not silently accepted golden updates.
- Do not claim desktop runtime or accessibility coverage based solely on Paparazzi snapshots.