package com.inspiredandroid.kai.sandbox

import com.inspiredandroid.kai.filereview.AndroidProjectFileReviewFileSystem
import com.inspiredandroid.kai.filereview.FileReviewController
import com.inspiredandroid.kai.filereview.FileReviewEngine
import com.inspiredandroid.kai.linux.LinuxPaths
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
