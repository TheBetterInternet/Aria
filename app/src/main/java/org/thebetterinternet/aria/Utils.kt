package org.thebetterinternet.aria

import android.app.DownloadManager
import android.content.Context
import android.os.Environment
import androidx.core.net.toUri
import org.mozilla.geckoview.WebResponse
import android.content.pm.PackageManager
import android.os.Build

fun startDownload(context: Context, url: String, filename: String) {
    val request = DownloadManager.Request(url.toUri())
        .setTitle(filename)
        .setDescription("Downloading file...")
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
        .setAllowedOverMetered(true)
        .setAllowedOverRoaming(true)

    val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    downloadManager.enqueue(request)
}
fun extractFilename(response: WebResponse): String {
    response.headers["content-disposition"]?.let { contentDisposition ->
        val filenameRegex = "filename[*]?\\s*=\\s*[\"']?([^;\"'\\n\\r]+)".toRegex(RegexOption.IGNORE_CASE)
        filenameRegex.find(contentDisposition)?.groupValues?.get(1)?.let { filename ->
            return filename.trim()
        }
    }

    return extractFilenameFromUrl(response.uri)
}

fun extractFilenameFromUrl(url: String): String {
    return try {
        val uri = url.toUri()
        val path = uri.path ?: ""
        val filename = path.substringAfterLast('/')

        if (filename.isNotEmpty() && filename.contains('.')) {
            filename
        } else {
            "download_${System.currentTimeMillis()}"
        }
    } catch (e: Exception) {
        "download_${System.currentTimeMillis()}"
    }
}

fun getVersionName(context: Context): String {
    return try {
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(0)
            )
        } else {
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
        packageInfo.versionName ?: "Unknown"
    } catch (e: Exception) {
        "Unknown"
    }
}

@Suppress("DEPRECATION")
fun getVersionCode(context: Context): String {
    return try {
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(0)
            )
        } else {
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
        "${packageInfo.versionCode}" ?: "Unknown" // because API 21 cant do packageInfo.longVersionCode
    } catch (e: PackageManager.NameNotFoundException) {
        "Unknown"
    }
}

fun lerp(start: Float, stop: Float, fraction: Float): Float {
    return start + fraction * (stop - start)
}