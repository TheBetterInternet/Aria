package org.thebetterinternet.aria

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Process
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings
import kotlin.system.exitProcess

class App : Application() {
    companion object {
        private var runtime: GeckoRuntime? = null

        fun getRuntime(context: Context): GeckoRuntime {
            if (runtime == null) {
                val settings = GeckoRuntimeSettings.Builder()
                    .build()
                runtime = GeckoRuntime.create(context.applicationContext, settings)
            }
            return runtime!!
        }
    }

    override fun onCreate() {
        super.onCreate()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val intent = Intent(applicationContext, CrashActivity::class.java).apply {
                putExtra("crash_error", throwable.stackTraceToString())
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            startActivity(intent)
            Thread.sleep(2000)
            Process.killProcess(Process.myPid())
            exitProcess(10)
        }
    }
}
