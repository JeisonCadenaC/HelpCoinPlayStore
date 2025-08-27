package com.example.finance_code.ui.transaction
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.RadioGroup
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatEditText
import com.example.finance_code.R
import com.example.finance_code.ui.home.HomeActivity


class addTransaction : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_transaction)
        val btnGuardarT = findViewById<AppCompatButton>(R.id.guardarT)
        val btnregistrarOpcion = findViewById<RadioGroup>(R.id.registrarOpcion)
        val cdodescripcionT = findViewById<EditText>(R.id.descripcionT)
        val cdoValorT = findViewById<AppCompatEditText>(R.id.valorT)

        // funcion para navegar entre menus
        fun navegarBntGuardarT(){
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
        }
        btnGuardarT.setOnClickListener {navegarBntGuardarT()}
    }
}


