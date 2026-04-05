package com.example.finance_code

import android.animation.ObjectAnimator
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.gson.Gson
import okhttp3.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

data class AppVersion(
    val versionCode: Int,
    val versionName: String,
    val url: String,
    val changelog: String
)

class UpdateManager(private val activity: AppCompatActivity) {

    companion object {
        // Variable estática que sobrevive a la recreación de la Activity al cambiar de tema
        private var isSkippedThisSession = false
        private const val PLAY_UPDATE_REQUEST_CODE = 1001
    }

    private val UPDATE_JSON_URL = "https://raw.githubusercontent.com/JeisonCadenaC/HelpCoinUpdater/main/version.json"

    private var updateDialog: AlertDialog? = null
    private var txtProgress: TextView? = null
    private var progressBar: ProgressBar? = null
    private var layoutButtons: LinearLayout? = null
    private var layoutProgress: LinearLayout? = null

    init {
        deleteOldApk()
    }

    private fun deleteOldApk() {
        try {
            val file = File(activity.getExternalFilesDir(null), "HelpCoin_Update.apk")
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Método para detectar el origen de la instalación
    private fun getAppSource(context: Context): String {
        val pm = context.packageManager
        val installer = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            try {
                pm.getInstallSourceInfo(context.packageName).installingPackageName
            } catch (e: Exception) {
                null
            }
        } else {
            @Suppress("DEPRECATION")
            pm.getInstallerPackageName(context.packageName)
        }

        return when (installer) {
            "com.android.vending" -> "PLAY_STORE"
            else -> "OTHER" // Incluye sideload, GitHub, Debug, etc.
        }
    }

    fun checkForUpdates() {
        if (isSkippedThisSession) return

        val source = getAppSource(activity)
        if (source == "PLAY_STORE") {
            checkGooglePlayUpdate()
        } else {
            checkGitHubUpdate()
        }
    }

    private fun checkGooglePlayUpdate() {
        val appUpdateManager = AppUpdateManagerFactory.create(activity)
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo

        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
            ) {
                try {
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        AppUpdateType.IMMEDIATE,
                        activity,
                        PLAY_UPDATE_REQUEST_CODE
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun checkGitHubUpdate() {
        val urlFresca = "$UPDATE_JSON_URL?t=${System.currentTimeMillis()}"

        val request = Request.Builder()
            .url(urlFresca)
            .cacheControl(CacheControl.FORCE_NETWORK)
            .build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("UpdateManager", "Error de red: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) return

                    val json = response.body?.string()

                    try {
                        val appVersion = Gson().fromJson(json, AppVersion::class.java)
                        val currentVersionCode = activity.packageManager
                            .getPackageInfo(activity.packageName, 0).versionCode

                        // Verificamos de nuevo por si cambió la variable mientras se hacía la petición
                        if (appVersion.versionCode > currentVersionCode && !isSkippedThisSession) {
                            activity.runOnUiThread {
                                showCustomUpdateDialog(appVersion)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        })
    }

    private fun showCustomUpdateDialog(version: AppVersion) {
        val dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_update, null)

        val txtVersion = dialogView.findViewById<TextView>(R.id.txtVersion)
        val txtChangelog = dialogView.findViewById<TextView>(R.id.txtChangelog)
        val btnUpdate = dialogView.findViewById<Button>(R.id.btnUpdate)
        val btnLater = dialogView.findViewById<Button>(R.id.btnLater)

        txtProgress = dialogView.findViewById(R.id.txtProgress)
        progressBar = dialogView.findViewById(R.id.progressBar)
        layoutButtons = dialogView.findViewById(R.id.layoutButtons)
        layoutProgress = dialogView.findViewById(R.id.layoutProgress)

        txtVersion.text = "Versión ${version.versionName} disponible"
        txtChangelog.text = version.changelog

        val builder = AlertDialog.Builder(activity)
        builder.setView(dialogView)
        builder.setCancelable(false)
        updateDialog = builder.create()
        updateDialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        btnUpdate.setOnClickListener { startDownload(version.url) }

        btnLater.setOnClickListener {
            // Registramos que el usuario no quiere actualizar en esta sesión
            isSkippedThisSession = true
            updateDialog?.dismiss()
        }

        updateDialog?.show()
    }

    private fun startDownload(url: String) {
        layoutButtons?.visibility = View.GONE
        layoutProgress?.visibility = View.VISIBLE
        txtProgress?.text = "Preparando actualización..."

        val request = Request.Builder()
            .url(url)
            .addHeader("Accept-Encoding", "identity")
            .build()

        val client = OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                activity.runOnUiThread {
                    Toast.makeText(activity, "Error en descarga: ${e.message}", Toast.LENGTH_SHORT).show()
                    updateDialog?.dismiss()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    activity.runOnUiThread {
                        Toast.makeText(activity, "Error del servidor: ${response.code}", Toast.LENGTH_SHORT).show()
                        updateDialog?.dismiss()
                    }
                    return
                }

                val body = response.body ?: return
                val contentLength = body.contentLength()
                val source = body.source()

                val file = File(activity.getExternalFilesDir(null), "HelpCoin_Update.apk")

                try {
                    val sink = FileOutputStream(file)
                    val buffer = ByteArray(8192)
                    var totalBytesRead: Long = 0
                    var bytesRead: Int
                    var lastProgress = 0

                    while (source.read(buffer).also { bytesRead = it } != -1) {
                        sink.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead

                        if (contentLength > 0) {
                            val progress = ((totalBytesRead * 100) / contentLength).toInt()
                            if (progress > lastProgress) {
                                lastProgress = progress
                                activity.runOnUiThread {
                                    updateProgressBarSmoothly(progress)
                                    txtProgress?.text = "Descargando actualización... $progress%"
                                }
                            }
                        } else {
                            activity.runOnUiThread {
                                val megabytes = totalBytesRead / (1024 * 1024)
                                txtProgress?.text = "Descargando... ${megabytes}MB"
                                progressBar?.isIndeterminate = true
                            }
                        }
                    }

                    sink.flush()
                    sink.close()

                    activity.runOnUiThread {
                        progressBar?.isIndeterminate = false
                        progressBar?.progress = 100
                        txtProgress?.text = "¡Descarga completada!"
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            updateDialog?.dismiss()
                            installApk(file)
                        }, 500)
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    activity.runOnUiThread {
                        Toast.makeText(activity, "Error al guardar actualización", Toast.LENGTH_SHORT).show()
                        updateDialog?.dismiss()
                    }
                }
            }
        })
    }

    private fun updateProgressBarSmoothly(targetProgress: Int) {
        progressBar?.let { bar ->
            if (bar.isIndeterminate) return
            val animation = ObjectAnimator.ofInt(bar, "progress", bar.progress, targetProgress)
            animation.duration = 200
            animation.interpolator = DecelerateInterpolator()
            animation.start()
        }
    }

    private fun installApk(file: File) {
        if (file.exists()) {
            val uri = FileProvider.getUriForFile(
                activity,
                "${activity.packageName}.provider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                activity.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(activity, "Error abriendo instalador", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }
    }
}