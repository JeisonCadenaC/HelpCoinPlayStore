package com.example.finance_code.ui.transaction
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatEditText
import com.example.finance_code.R

class addTransaction : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_transaction)
        val btnGuardarT = findViewById<AppCompatButton>(R.id.guardarT)
        val btnregistrarOpcion = findViewById<RadioGroup>(R.id.registrarOpcion)
        val cdodescripcionT = findViewById<EditText>(R.id.descripcionT)
        val cdoValorT = findViewById<AppCompatEditText>(R.id.valorT)
        btnGuardarT.setOnClickListener {
            Log.i("Hola", "Me pulsaste y guardaste esto ${cdodescripcionT.text.toString()}")
        }

        //la condicion "grupo" hace referencia <RadioGroup>
        btnregistrarOpcion.setOnCheckedChangeListener { grupo, VerifcarID ->
            val oSelecccionada = findViewById<RadioButton>(VerifcarID)
            Log.i("opcion", " Seleccionate: ${oSelecccionada.text.toString()}")
        }

        cdodescripcionT.setOnClickListener {
            Log.i("Dice: ", " ${cdodescripcionT.text.toString()}")
        }

        cdoValorT.setOnClickListener {
            Log.i("Valor: ", "${cdoValorT.text}")
        }
    }

}
