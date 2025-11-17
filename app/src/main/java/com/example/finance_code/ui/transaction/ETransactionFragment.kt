package com.example.finance_code.ui.transaction

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.finance_code.R
import com.example.finance_code.data.AppDB
import com.example.finance_code.data.Movimiento
import com.example.finance_code.data.MovimientoRepository
import com.example.finance_code.viewmodel.MovimientoViewModel
import com.example.finance_code.viewmodel.MovimientoViewModelFactory
import com.google.firebase.auth.FirebaseAuth

class ETransactionFragment : Fragment(R.layout.fragment_e_transaction){

    private lateinit var viewModel: MovimientoViewModel
    private lateinit var movimiento: Movimiento

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        movimiento = requireArguments().getParcelable("movimiento", Movimiento::class.java)!!


        val etDescripcion = view.findViewById<EditText>(R.id.etDescripcion)
        val etMonto = view.findViewById<EditText>(R.id.etMonto)
        val btnGuardar = view.findViewById<Button>(R.id.btnGuardar)

        etDescripcion.setText(movimiento.descripcion)
        etMonto.setText(movimiento.cantidad.toString())

        val userEmail = FirebaseAuth.getInstance().currentUser?.email
        if (userEmail == null) {
            Toast.makeText(requireContext(), "Error: Usuario no autenticado", Toast.LENGTH_LONG).show()
            findNavController().popBackStack()
            return
        }

        val database = AppDB.getDatabase(requireContext(), userEmail)
        val repository = MovimientoRepository(database.movimientoDao())
        val factory = MovimientoViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[MovimientoViewModel::class.java]

        btnGuardar.setOnClickListener {
            val actualizado = movimiento.copy(
                descripcion = etDescripcion.text.toString(),
                cantidad = etMonto.text.toString().toDouble()
            )
            viewModel.actualizar(actualizado)
            Toast.makeText(requireContext(), "Movimiento actualizado", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
        }
    }
}