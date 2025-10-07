package com.example.finance_code.ui.home

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.finance_code.R
import com.google.android.material.bottomnavigation.BottomNavigationView

class HomeActivity : AppCompatActivity() {

    private lateinit var NavController: NavController



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)

        // Ajustar padding para que no se superponga con las barras del sistema
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Inicializamos la referencia a la clase Application (MovimientoAPP)
        app = application as MovimientoAPP

        // Configurar navegación con NavController
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.mainContainer) as NavHostFragment
        navController = navHostFragment.navController

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        setupWithNavController(bottomNavigationView, NavController)
      //  val app = applicationContext as MovimientoAPP
     //   val Movimiento =  app.room.movimientoDao().obtenerTodos()
        bottomNavigationView.setupWithNavController(navController)

        // ✅ Si quieres probar la BD de inmediato:
        // CoroutineScope(Dispatchers.IO).launch {
        //     val movimientos = app.room.movimientoDao().obtenerTodos().first()
        //     Log.d("HomeActivity", "Movimientos: $movimientos")
        // }
    }
}