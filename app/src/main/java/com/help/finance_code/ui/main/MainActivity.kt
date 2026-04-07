package com.help.finance_code.ui.main

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.help.finance_code.R
import com.help.finance_code.UpdateManager
import com.google.android.play.core.review.ReviewManagerFactory

class MainActivity : AppCompatActivity() {

    private lateinit var updateManager: UpdateManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)

        // Inicializamos y verificamos si hay actualizaciones de forma silenciosa
        updateManager = UpdateManager(this)
        updateManager.checkForUpdates()
    }

    /**
     * Llama a este método cuando quieras pedirle al usuario que califique la app.
     * Ejemplo: Después de completar una meta exitosa o usarla por 5ta vez.
     */
    fun requestInAppReview() {
        val reviewManager = ReviewManagerFactory.create(this)
        val request = reviewManager.requestReviewFlow()

        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val reviewInfo = task.result
                val flow = reviewManager.launchReviewFlow(this, reviewInfo)
                flow.addOnCompleteListener { _ ->
                    // El flujo terminó (el usuario pudo calificar o cancelar).
                    Log.d("PlayStore", "Flujo de reseña finalizado.")
                }
            } else {
                Log.e("PlayStore", "Error al solicitar reseña", task.exception)
            }
        }
    }
}