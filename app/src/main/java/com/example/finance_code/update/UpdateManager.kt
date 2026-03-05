package com.example.finance_code

import android.animation.ObjectAnimator
import android.app.AlertDialog
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

    // URL directa al archivo version.json en tu nuevo repositorio público (rama main)
    // NOTA: Si tu repositorio por defecto se creó con la rama "master", cambia la palabra "main" por "master" en el enlace.
    private val UPDATE_JSON_URL = "https://raw.githubusercontent.com/JeisonCadenaC/HelpCoinUpdater/main/version.json"

    private var updateDialog: AlertDialog? = null
    private var txtProgress: TextView? = null
    private var progressBar: ProgressBar? = null
    private var layoutButtons: LinearLayout? = null
    private var layoutProgress: LinearLayout? = null

    fun checkForUpdates() {
        val urlFresca = "$UPDATE_JSON_URL?t=${System.currentTimeMillis()}"

        val request = Request.Builder()
            .url(urlFresca)
            .cacheControl(CacheControl.FORCE_NETWORK) // Petición pública
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

                        if (appVersion.versionCode > currentVersionCode) {
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
        btnLater.setOnClickListener { updateDialog?.dismiss() }

        updateDialog?.show()
    }

    private fun startDownload(url: String) {
        layoutButtons?.visibility = View.GONE
        layoutProgress?.visibility = View.VISIBLE
        txtProgress?.text = "Preparando actualización..."

        // Usamos la URL de descarga directa (el link del release)
        val request = Request.Builder()
            .url(url)
            .addHeader("Accept-Encoding", "identity")
            .build()

        // Forzamos a OkHttp a seguir redirecciones (esencial para los Releases de GitHub)
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

                        // Dependiendo del servidor, GitHub podría no informar el tamaño total (contentLength = -1)
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
                            // Si no se sabe el peso total, mostramos los MB descargados
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