package com.dorai.ai.workspace

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * The coding agents Workspace can install on a fresh Debian. Each one ships a
 * self-contained installer, so the base system plus curl is all they need.
 *
 * Trust model — please read before "hardening" further. The install runs INSIDE the
 * guest over HTTPS only (the app forces `--proto '=https'`), and every vendor already
 * self-verifies its payload:
 *
 * - Claude (`claude.ai/install.sh`): downloads the signed release manifest
 *   (`manifest.json`) and refuses to execute unless the downloaded binary matches its
 *   SHA-256 checksum.
 * - Grok (`x.ai/cli/install.sh`): installs the `stable` channel by default, accepts an
 *   explicit version (`bash -s X.Y.Z`), and sanity-runs `--version` before installing.
 * - OpenCode (`opencode.ai/install`): pulls a versioned release from GitHub releases
 *   (preflights a 404 so a half-published version never installs).
 *
 * Pinning the *installer scripts* by hash would break every legitimate vendor update
 * (the script changes when the payload does), so the app instead logs the resolved
 * SHA-256 of each downloaded installer into the guest audit trail
 * (`/root/.local/state/dorai-agent-install.log`) — tamper-EVIDENT, not tamper-proof,
 * but forensic without staleness.
 *
 * Install locations (guest paths under `/root`):
 * - Claude → `~/.local/bin/claude` (versioned under `~/.local/share/claude/versions/`)
 * - Grok → `~/.grok/bin/grok`
 * - OpenCode → `~/.opencode/bin/opencode`
 *
 * Proot injects all three bin dirs into PATH for probes/installers. Login
 * shells rebuild PATH from profile files, so Workspace also writes a
 * profile.d snippet and launches agents by absolute path.
 */
@Immutable
data class WorkspaceAgent(
    val id: String,
    /** Product name — shown as-is, not translated. */
    val title: String,
    val binary: String,
    val installCommand: String,
)

private fun httpsInstaller(url: String): String = "tmp=/tmp/dorai-install.\$\$.sh && mkdir -p /root/.local/state" +
    " && curl -fsSL --proto '=https' -o \"\$tmp\" $url" +
    " && { date -u +%Y-%m-%dT%H:%M:%SZ; sha256sum \"\$tmp\"; } >> /root/.local/state/dorai-agent-install.log" +
    " && bash \"\$tmp\"; rc=\$?; rm -f \"\$tmp\"; exit \$rc"

object WorkspaceAgents {

    val all: ImmutableList<WorkspaceAgent> = persistentListOf(
        WorkspaceAgent(
            id = "claude-code",
            title = "Claude Code",
            binary = "claude",
            installCommand = httpsInstaller("https://claude.ai/install.sh"),
        ),
        WorkspaceAgent(
            id = "grok",
            title = "Grok",
            binary = "grok",
            installCommand = httpsInstaller("https://x.ai/cli/install.sh"),
        ),
        WorkspaceAgent(
            id = "opencode",
            title = "OpenCode",
            binary = "opencode",
            installCommand = httpsInstaller("https://opencode.ai/install"),
        ),
    )

    fun get(id: String?): WorkspaceAgent? = all.firstOrNull { it.id == id }
}
