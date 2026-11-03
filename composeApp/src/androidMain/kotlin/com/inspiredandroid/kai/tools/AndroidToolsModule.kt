package com.inspiredandroid.kai.tools

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Android-only tool dependencies.
 *
 * The gallery/downloads controllers and repositories are Android implementations, so they are
 * registered here rather than in the shared [com.inspiredandroid.kai.appModule]. KaiApplication
 * loads this alongside the shared module; the tool implementations resolve them by injection.
 */
val androidToolsModule = module {
    single { GalleryPermissionController(androidContext()) }
    single { DownloadsPermissionController(androidContext()) }
    single<GalleryRepository> { GalleryRepositoryImpl(androidContext(), get()) }
    single<DownloadsRepository> { DownloadsRepositoryImpl(androidContext(), get()) }
}
