package com.dorai.ai

import com.dorai.ai.workspace.DesktopWorkspaceController

actual fun createWorkspaceController(): WorkspaceController = DesktopWorkspaceController()
