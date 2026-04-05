package com.example.finance_code.ui.home

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.example.finance_code.R
import com.example.finance_code.ui.login.AuthCheckActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {

    // Candado de seguridad para evitar múltiples aperturas del Login
    private var yaSalto = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ReminderHelper.createNotificationChannels(this.applicationContext)

        val sharedPreferences = getSharedPreferences("AppPrefe", Context.MODE_PRIVATE)
        val modoOscuroActivado = sharedPreferences.getBoolean("modo_oscuro", false)
        aplicarModoOscuro(modoOscuroActivado)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        setContentView(R.layout.activity_splash)

        val videoView = findViewById<VideoView>(R.id.videoViewSplash)

        val path = "android.resource://$packageName/${R.raw.splash_screnhc}"
        val uri = path.toUri()
        videoView.setVideoURI(uri)

        videoView.setOnPreparedListener { mp ->
            mp.setVolume(0f, 0f)
            mp.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
        }

        videoView.setOnErrorListener { _, what, extra ->
            Log.e("SplashActivity", "Error al reproducir video. Codigo: $what, Extra: $extra")
            saltarAlLogin()
            true
        }

        videoView.setOnCompletionListener {
            saltarAlLogin()
        }

        videoView.start()

        // Failsafe Timeout: Si el motor de video falla silenciosamente (ej. se bloquea la pantalla
        // y al volver queda en negro), este temporizador asegura la entrada tras 6 segundos.
        lifecycleScope.launch {
            delay(6000) // Ajusta ligeramente
            saltarAlLogin()
        }
    }

    @Synchronized
    private fun saltarAlLogin() {
        if (yaSalto) return
        yaSalto = true

        val intent = Intent(this, AuthCheckActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun aplicarModoOscuro(activado: Boolean) {
        if (activado) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }
}