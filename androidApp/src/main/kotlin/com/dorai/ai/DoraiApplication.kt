package com.dorai.ai

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.dorai.ai.data.TaskScheduler
import com.dorai.ai.inference.LocalRuntimeRegistry
import com.dorai.ai.inference.registerFlavorRuntimes
import com.dorai.ai.sandbox.sandboxModule
import com.dorai.ai.tools.androidToolsModule
import com.google.firebase.appcheck.FirebaseAppCheck
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class DoraiApplication : Application() {

    private val taskScheduler: TaskScheduler by inject()
    private val localRuntimeRegistry: LocalRuntimeRegistry by inject()

    override fun onCreate() {
        com.dorai.ai.AppContextHolder.context = this
        super.onCreate()
        initFirebaseAppCheck()
        startKoin {
            androidContext(this@DoraiApplication)
            modules(appModule, sandboxModule, androidToolsModule)
        }
        registerFlavorRuntimes(this, localRuntimeRegistry)
        // Track app foreground state so the scheduler only pushes a heartbeat notification
        // when the in-app banner isn't visible. ViewModel lifecycle is the wrong signal —
        // it survives backgrounding and only clears on Activity destruction.
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                taskScheduler.appInForeground = true
            }
            override fun onStop(owner: LifecycleOwner) {
                taskScheduler.appInForeground = false
            }
        })
    }

    private fun initFirebaseAppCheck() {
        runCatching {
            val appCheck = FirebaseAppCheck.getInstance()
            val providerClassName = if (BuildConfig.DEBUG) {
                "com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory"
            } else {
                "com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory"
            }
            val cls = Class.forName(providerClassName)
            val factory = cls.getMethod("getInstance").invoke(null)
            val install = appCheck.javaClass.getMethod(
                "installAppCheckProviderFactory",
                Class.forName("com.google.firebase.appcheck.AppCheckProviderFactory"),
            )
            install.invoke(appCheck, factory)
        }
    }
}
