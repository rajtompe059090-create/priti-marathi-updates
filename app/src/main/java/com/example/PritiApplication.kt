package com.example

import android.app.Application
import android.os.Build
import android.util.Log
import android.webkit.WebView
import java.io.File

class PritiApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initWebViewSafely()
    }

    private fun initWebViewSafely() {
        try {
            // Configure process-isolated WebView data directory if running in secondary process
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val currentProcess = getProcessName()
                if (packageName != currentProcess) {
                    WebView.setDataDirectorySuffix(currentProcess)
                }
            }

            // Clean up any corrupt or unsigned Chromium variations seed files
            // that trigger [ERROR:variations_seed_loader.cc(39)] Seed missing signature
            val candidateDirs = listOfNotNull(
                applicationContext.dataDir,
                applicationContext.filesDir,
                applicationContext.cacheDir
            )
            for (dir in candidateDirs) {
                val webviewDir = File(dir, "app_webview")
                if (webviewDir.exists() && webviewDir.isDirectory) {
                    webviewDir.listFiles()?.forEach { file ->
                        if (file.name.contains("variations_seed", ignoreCase = true)) {
                            file.delete()
                        }
                    }
                }
            }
        } catch (t: Throwable) {
            Log.w("PritiApplication", "Safe WebView init handled: ${t.message}")
        }
    }
}

