package com.example.finance_code



import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.VideoView
import androidx.core.net.toUri

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        @Suppress("DEPRECATION")
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        setContentView(R.layout.activity_splash)

        val videoView = findViewById<VideoView>(R.id.videoViewSplash)


        val path = "android.resource://$packageName/${R.raw.splash_screnhc}"
        val uri = path.toUri()
        videoView.setVideoURI(uri)

        videoView.setOnPreparedListener { mp ->
            mp.setVolume(0f, 0f)
        }


        videoView.setOnErrorListener { mp, what, extra ->

            Log.e("SplashActivity", "Error al reproducir video. Codigo: $what, Extra: $extra")


            saltarAlLogin()


            true
        }


        videoView.setOnCompletionListener {

            saltarAlLogin()
        }


        videoView.start()
    }

    private fun saltarAlLogin() {

        val intent = Intent(this, com.example.finance_code.ui.login.LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}
