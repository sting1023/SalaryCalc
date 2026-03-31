package com.salarycalc

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AlertDialog
import java.net.HttpURLConnection
import java.net.URL

/**
 * 检查更新的工具类。
 * version.json 放在 NAS 上，格式：
 * {
 *   "versionCode": 5,
 *   "versionName": "1.0.5",
 *   "note": "修复了xxx问题",
 *   "apkUrl": "https://192.168.5.4/AI交流文件夹/娇素雅/salarycalc_v5.apk"
 * }
 */
object UpdateChecker {

    /** 当前版本码，和 version.json 里的一致就行 */
    const val CURRENT_VERSION_CODE = 5
    const val CURRENT_VERSION_NAME = "1.0.5"

    // NAS 在内网，用 HTTP（自签名证书），实际使用时请确保手机和 NAS 在同一局域网
    private const val VERSION_URL = "http://192.168.5.4/AI交流文件夹/娇素雅/version.json"

    /**
     * 弹更新提示对话框（异步，不阻塞UI）。
     * @param context Activity context
     * @param onNoUpdate 可选：没有更新时的回调（比如什么都不做）
     */
    fun check(context: Context, onNoUpdate: (() -> Unit)? = null) {
        val ctx = context.applicationContext
        Thread {
            try {
                val latest = fetchLatestVersion()
                if (latest != null && latest.versionCode > CURRENT_VERSION_CODE) {
                    val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
                    mainHandler.post { showUpdateDialog(ctx, latest) }
                } else {
                    onNoUpdate?.invoke()
                }
            } catch (e: Exception) {
                onNoUpdate?.invoke()
            }
        }.start()
    }

    private fun fetchLatestVersion(): LatestVersion? {
        val url = URL(VERSION_URL)
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 5000
        conn.readTimeout = 5000
        try {
            if (conn.responseCode != HttpURLConnection.HTTP_OK) return null
            val json = conn.inputStream.bufferedReader().readText()
            return parseVersion(json)
        } finally {
            conn.disconnect()
        }
    }

    private fun parseVersion(json: String): LatestVersion? {
        return try {
            val obj = org.json.JSONObject(json)
            LatestVersion(
                versionCode = obj.getInt("versionCode"),
                versionName = obj.optString("versionName", ""),
                note = obj.optString("note", "有新版本可以更新"),
                apkUrl = obj.optString("apkUrl", "")
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun showUpdateDialog(context: Context, latest: LatestVersion) {
        try {
            val dialog = AlertDialog.Builder(context)
                .setTitle("发现新版本 v${latest.versionName}")
                .setMessage(latest.note)
                .setPositiveButton("下载更新") { _, _ ->
                    openDownloadPage(context, latest.apkUrl)
                }
                .setNegativeButton("以后再说", null)
                .setCancelable(true)
                .create()
            dialog.show()
        } catch (e: Exception) {
            // Activity 已销毁，忽略
        }
    }

    /** 跳转到浏览器下载 APK（最简单可靠的方式） */
    private fun openDownloadPage(context: Context, apkUrl: String) {
        try {
            val uri = Uri.parse(apkUrl.ifEmpty { "https://192.168.5.4/AI交流文件夹/娇素雅/" })
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            // 跳转失败，忽略
        }
    }

    data class LatestVersion(
        val versionCode: Int,
        val versionName: String,
        val note: String,
        val apkUrl: String
    )
}
