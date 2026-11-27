package com.tiendatv.player.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.tiendatv.player.config.AppConfig
import com.tiendatv.player.data.network.NetworkModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

data class RemoteVersionInfo(
    @SerializedName("versionCode") val versionCode: Int = 0,
    @SerializedName("versionName") val versionName: String = "",
    @SerializedName("apkUrl") val apkUrl: String = AppConfig.LATEST_APK_URL
)

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(
        val versionName: String,
        val versionCode: Int,
        val apkUrl: String
    ) : UpdateState
    data class Downloading(val progress: Float) : UpdateState
    data class ReadyToInstall(val apkFile: File) : UpdateState
    data class PermissionRequired(val apkFile: File) : UpdateState
    data class Error(val message: String) : UpdateState
}

object AppUpdater {
    private val gson = Gson()

    suspend fun checkForUpdate(): UpdateState = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(AppConfig.VERSION_CHECK_URL)
                .header("Cache-Control", "no-cache")
                .build()
            NetworkModule.apiClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext UpdateState.Error("HTTP ${response.code}")
                }
                val body = response.body?.string().orEmpty()
                if (body.isBlank()) {
                    return@withContext UpdateState.Error("Empty response")
                }
                val info = gson.fromJson(body, RemoteVersionInfo::class.java)
                if (info.versionCode > AppConfig.VERSION_CODE) {
                    UpdateState.Available(
                        versionName = info.versionName.ifBlank { "latest" },
                        versionCode = info.versionCode,
                        apkUrl = info.apkUrl.ifBlank { AppConfig.LATEST_APK_URL }
                    )
                } else {
                    UpdateState.UpToDate
                }
            }
        } catch (t: Throwable) {
            UpdateState.Error(t.localizedMessage ?: "Network error")
        }
    }

    suspend fun downloadApk(
        context: Context,
        apkUrl: String,
        onProgress: (Float) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
        val targetFile = File(updatesDir, "TiendaTV-latest.apk")
        val tempFile = File(updatesDir, "TiendaTV-latest.apk.tmp")
        try {
            if (tempFile.exists()) tempFile.delete()
            val request = Request.Builder()
                .url(apkUrl)
                .build()
            NetworkModule.apiClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IllegalStateException("HTTP ${response.code}"))
                }
                val body = response.body
                    ?: return@withContext Result.failure(IllegalStateException("Empty APK body"))
                val totalBytes = body.contentLength()
                var downloaded = 0L
                body.byteStream().use { input ->
                    FileOutputStream(tempFile).use { output ->
                        val buffer = ByteArray(16 * 1024)
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            downloaded += read
                            if (totalBytes > 0) {
                                onProgress((downloaded.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f))
                            }
                        }
                        output.flush()
                    }
                }
            }
            if (targetFile.exists()) targetFile.delete()
            if (!tempFile.renameTo(targetFile)) {
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
            }
            Result.success(targetFile)
        } catch (t: Throwable) {
            if (tempFile.exists()) tempFile.delete()
            Result.failure(t)
        }
    }

    fun installApk(context: Context, apkFile: File): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            val permIntent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(permIntent)
            return false
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(installIntent)
        return true
    }
}
