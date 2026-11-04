# DORAI AI — UI Upgrade Plan 5: Services and Sponsors

## Objective

Turn the Services tab into a clear place to connect, manage, prioritise, and inspect AI services. Rework the sponsor area as a responsive expand/collapse grid where the DORAI AI profile is always the first card.

## Current scope

- `ServicesContent` shows configured services in a reorderable list, an Add Service bottom sheet, and the Free tier card.
- `FreeSettings` merges fallback settings, sponsorship calls to action, current/past sponsors, and partnership copy in one card.
- `SponsorList` displays every remote sponsor immediately in a fixed-width `FlowRow`.
- Sponsor data is fetched separately as current and past GitHub sponsors. The project README identifies the DORAI AI fork profile as `habibbrox2`; it should be the initial owner-profile data source until the final public profile copy, avatar, and destination URL are approved.

## Target experience

- A user can see which services are active, connected, unavailable, or need attention before opening a card.
- Configured services remain reorderable, while their important details stay readable at a glance.
- Adding a provider is a guided choice with provider identity, required credentials, supported capabilities, and any local-network requirement.
- The Free tier and sponsor content are visually distinct from service configuration.
- The sponsor section starts with the DORAI AI profile card. It stays visible in both collapsed and expanded states.
- The sponsor grid shows a useful first set of cards, then reveals the remaining sponsors with a clear Show all / Collapse action only when more cards exist.

## Information architecture

```text
Services
├── Service overview
├── Configured services
│   ├── Service status and selected model
│   └── Expanded configuration
├── Add service
├── Free tier and fallback
└── Support DORAI AI
    ├── DORAI AI profile (first)
    ├── Sponsor grid
    └── Show all / Collapse sponsors
```

## Target structure

```text
ServicesContent
├── ServicesOverview
├── ConfiguredServicesList
│   └── ServiceCard
├── AddServiceSheet
├── FreeTierCard
└── SupportSection
    ├── OwnerProfileCard
    ├── SponsorGrid
    └── SponsorGridToggle
```

## Sponsor profile and ordering

Create a presentation model that can combine the static DORAI AI profile with remote sponsor entries without changing the sponsor API contract.

```kotlin
@Immutable
data class SupportProfile(
    val id: String,
    val name: String,
    val subtitle: String?,
    val avatarUrl: String?,
    val destinationUrl: String?,
    val isOwnerProfile: Boolean,
    val isCurrentSponsor: Boolean,
)
```

1. Build the profile list in the view-model or a dedicated mapper: owner profile first, current sponsors next, then past sponsors not already listed.
2. Give the owner profile a stable ID such as `brox-kai-owner`; do not treat it as a remote GitHub sponsor.
3. Start its content from the repository identity (`habibbrox2` / DORAI AI maintainer) and replace its public title, portrait, support link, and profile link with approved values before release.
4. Use a local branded avatar fallback when a network avatar is unavailable. Show a neutral initial/avatar fallback if no approved profile image exists.
5. Keep every destination optional in the model. A card with no approved link remains informational and must not appear tappable.

## Expand/collapse behaviour

| Layout width | Collapsed visible cards | Expanded state |
| --- | ---: | --- |
| Compact phone | 2 | All cards, one column |
| Medium tablet | 4 | All cards, two columns |
| Desktop / wide window | 6 | All cards, three columns |

- The owner profile is always index 0 and is never hidden by the collapsed limit.
- Show the toggle only when the number of profiles exceeds the applicable collapsed limit.
- The collapsed label reads `Show all sponsors` and includes the hidden count in its accessibility description. The expanded label reads `Collapse sponsors`.
- Keep the section in the existing settings scroll container. For this short collection, use `FlowRow` or a simple responsive row layout instead of nesting a scrolling lazy grid inside the page.
- Animate the grid height or content change with a short, reduced-motion-respecting transition. Preserve focus on the toggle after collapse.
- Keep the current/past sponsor distinction in the data model. It may be shown as a subtle “Supporter” detail in the expanded view, but it must not move the owner card from first position.

## Visual direction

- Use Midnight Navy surfaces with restrained Cyan and Electric Blue accents from the DORAI AI theme.
- Give the owner profile a modest featured treatment: avatar, `DORAI AI` name, maintainer subtitle, and one clear external-link action when a URL is approved.
- Render sponsor cards with equal visual weight. Avoid oversized logos, multiple competing buttons, or dense text.
- Keep connected/error service status text visible alongside the icon; color alone cannot carry status.
- Keep cards, fields, and buttons aligned to the existing adaptive card styling so the tab feels part of Settings.

## State ownership

- Preserve `SettingsUiState` and `SettingsActions` for service configuration, ordering, connection checks, and provider mutations.
- Continue fetching remote sponsors in `SettingsViewModel`; map the owner profile and remote entries to presentation items before the composable renders them.
- Keep `isSponsorGridExpanded` as UI-only state at the Services/Support section boundary. It should reset when leaving the screen unless product requirements call for persistent preference storage.
- Keep card click handling outside the item model: open only approved external destinations through the existing URI handler.
- Do not let sponsor UI fetch network data or mutate service settings directly.

## Implementation phases

### Phase 1 — Service hierarchy

1. Add a compact Services overview that explains configured-service count and any service requiring attention.
2. Improve each configured-service card header with provider name, selected model, connection status, and clear expand/reorder affordances.
3. Keep API keys and advanced fields inside the existing expanded detail area.

### Phase 2 — Add-service flow

1. Group providers in the bottom sheet by common hosted providers, local/on-device, and OpenAI-compatible custom endpoints.
2. Show a short setup requirement summary before a provider is added.
3. Keep the existing action pipeline and validation; the sheet only changes presentation and guidance.

### Phase 3 — Sponsor profile and grid

1. Add the owner-profile mapper and approved content configuration.
2. Replace `SponsorList` with `SupportSection`, `OwnerProfileCard`, `SponsorGrid`, and `SponsorGridToggle`.
3. Deduplicate current and past sponsors as today, then prepend the owner profile.
4. Apply the responsive collapsed limits and disclosure action.
5. Keep the sponsorship and business-partnership calls to action below the grid with their links owned by configuration.

### Phase 4 — Polish and accessibility

1. Add accessible names for service status, drag handles, owner profile links, sponsor links, and the disclosure control.
2. Ensure every card and toggle has a 48 dp minimum touch target and works with keyboard focus on desktop.
3. Verify large-font wrapping, missing avatars, no network response, empty sponsors, and one-card sponsor lists.
4. Respect reduced motion where platform settings expose it.

## Validation cases

- No configured services, one configured service, and multiple reordered services.
- Connected, checking, invalid key, quota exhausted, and local-network-permission service statuses.
- No remote sponsors: the DORAI AI profile card still renders first.
- Sponsor counts at, below, and above each collapsed threshold.
- Collapse after expansion returns focus to the toggle and never hides the owner profile.
- Compact, medium, and desktop width screenshots with default and large font scales.
- Avatar loading, avatar failure, no approved profile URL, and external-link interaction.

## Acceptance criteria

- Services can be configured and reordered without scrolling through sponsor content to find core controls.
- The owner profile appears as the first card in the sponsor area for every state.
- Sponsor lists larger than the viewport-friendly limit expand and collapse predictably; shorter lists have no redundant toggle.
- The sponsor grid stays responsive, accessible, and free of nested-scroll problems.
- Existing service actions, sponsor fetching, and Free tier fallback behaviour retain their current ownership and functionality.

## Risks and controls

- The existing feed is tied to the upstream sponsor account. Keep remote sponsor fetch failures non-blocking and preserve the static DORAI AI profile when the feed is unavailable.
- Profile image, wording, and public destination must be approved before release. Use a branded fallback asset until those values are available.
- A large remote sponsor list can make reflow expensive. Render only the collapsed subset until expanded and keep card content lightweight.
- External links are user-visible destinations. Keep them in configuration and do not hardcode unverified personal URLs in the composable.

## Progress status

All four phases are implemented, each compiled against the desktop target and committed separately.

### Phase 1 — Service hierarchy (done)

- Compact `ServicesOverview` card at the top of the Services tab: configured count plus a status badge (`N needs attention` / `Checking connections` / `All connected`); a dedicated empty state replaces the blank list when nothing is configured.
- Configured-service card headers show provider name, selected model, and now a **text** connection-status badge instead of the color-only dot — covering every `ConnectionStatus` (Not checked, Checking, Connected, Invalid API key, Quota exhausted, Rate limited, Connection failed, Network blocked, Error).
- API keys and advanced fields stay inside the existing expanded detail area; expand/reorder behavior untouched.

### Phase 2 — Add-service flow (done)

- The add-service sheet groups providers into **Hosted providers**, **On-device**, and **Custom endpoints** (headers hidden for empty groups), keeping first/last row corner rounding across groups.
- Each row shows a setup requirement summary derived from `Service` metadata: runs on this device / custom endpoint / API key from \<portal\> / key required / optional / none.
- Action pipeline and validation unchanged — presentation and guidance only.

### Phase 3 — Sponsor profile and grid (done)

- `SupportProfile` presentation model and `buildSupportProfiles` mapper: owner profile first (stable id `brox-kai-owner`, repository identity `habibbrox2`, local branded `logo` avatar — no network dependency), then current sponsors, then past (deduplicated as before). The GitHub sponsors API contract is unchanged and fetching stays in `SettingsViewModel`.
- `SponsorList` replaced by `SupportSection` + `OwnerProfileCard` + `SponsorGrid`: responsive collapse at 600/900dp shows 2/4/6 tiles, `Show all sponsors` / `Collapse sponsors` toggle with the hidden count in its accessibility description, short `animateContentSize` transition that keeps focus on the toggle. Lists at or below the limit render fully with **no** redundant toggle (acceptance criteria).
- Expanded view keeps the current/past distinction with section labels; the owner card is never hidden by the limit.
- Cards without a destination URL would render informational (no tap affordance); link actions carry accessible names; all support-area URLs are centralized in the `SupportLinks` configuration block (owner profile, sponsor base, become-sponsor, partnerships — the latter two keep their existing upstream destinations).
- Sponsorship and partnership calls to action now sit **below** the grid.

### Phase 4 — Polish and accessibility (done, with notes)

- Accessible names: service status badges (`Status: …`), drag handles (existing reorder description), owner/sponsor links (action + destination in the description), disclosure control (hidden count).
- Drag handle gains a 48dp minimum touch target around its 24dp glyph (`minimumInteractiveComponentSize`); cards and toggles are clickable/focusable through their existing Material components.
- Validation performed by review: missing avatars fall back to the branded logo (owner) or a neutral initial (sponsors), an empty feed still renders the owner card first, one-card and threshold-crossing lists behave per the toggle rules above.
- Deferred / out of scope: platform-specific reduced-motion detection (`animateContentSize` uses the default short duration), automated screenshot validation of every validation case, and persistent collapse-preference storage (state resets with the screen, per plan).
