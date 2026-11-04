package com.dorai.ai.sandbox

import com.dorai.ai.filereview.AndroidProjectFileReviewFileSystem
import com.dorai.ai.filereview.FileReviewController
import com.dorai.ai.filereview.FileReviewEngine
import com.dorai.ai.linux.LinuxPaths
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import java.io.File

val sandboxModule = module {
    single<LinuxSandboxManager> { LinuxSandboxManager(androidContext(), get(), get()) }
    single<FileReviewController> {
        FileReviewEngine(
            stagingRoot = File(androidContext().filesDir, "file-review"),
            fileSystem = AndroidProjectFileReviewFileSystem(LinuxPaths.forBuild(androidContext()).projectsDir),
        )
    }
}
