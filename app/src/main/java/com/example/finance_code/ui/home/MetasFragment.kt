package com.example.finance_code.ui.home

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.finance_code.R
import com.example.finance_code.data.MetaDB
import com.example.finance_code.databinding.FragmentMetasBinding
import com.example.finance_code.viewmodel.MetaViewModel
import com.example.finance_code.viewmodel.MetaViewModelFactory
import java.util.Locale

class MetasFragment : Fragment() {

    private var _binding: FragmentMetasBinding? = null
    private val binding get() = _binding!!

    private val metaViewModel: MetaViewModel by viewModels {
        MetaViewModelFactory(requireActivity().application)
    }

    private lateinit var metasAdapter: MetasAdapter

    private var currentNombreInput: EditText? = null
    private var currentMontoInput: EditText? = null

    private val speechLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val speechResult = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = speechResult?.get(0) ?: ""
            procesarTextoVozMeta(spokenText)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMetasBinding.inflate(inflater, container, false)

        metasAdapter = MetasAdapter(onActualizarClick = { meta ->
            mostrarDialogoMeta(meta)
        })

        binding.rvMetas.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = metasAdapter
        }

        metaViewModel.allMetas.observe(viewLifecycleOwner) { metas ->
            metasAdapter.setData(metas)
        }

        binding.fabAddMeta.setOnClickListener {
            mostrarDialogoMeta(null)
        }

        return binding.root
    }

    private fun startVoiceInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Di algo como: 'Viaje a Europa 15 millones'")

        try {
            speechLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Tu dispositivo no soporta entrada de voz", Toast.LENGTH_SHORT).show()
        }
    }

    private fun procesarTextoVozMeta(textoOriginal: String) {
        val texto = textoOriginal.lowercase(Locale.getDefault()).trim()
        var multiplicador = 1.0
        var textoLimpio = texto

        if (texto.endsWith("millones") || texto.endsWith("millón")) {
            multiplicador = 1000000.0
            textoLimpio = texto.replace(Regex("millon(es)?$"), "").trim()
        } else if (texto.endsWith("mil")) {
            multiplicador = 1000.0
            textoLimpio = texto.replace(Regex("mil$"), "").trim()
        }

        val regex = Regex("([0-9]+[.,]?[0-9]*)$")
        val matchResult = regex.find(textoLimpio)

        if (matchResult != null) {
            val numeroEncontrado = matchResult.value.replace(",", ".")
            val valorNumerico = numeroEncontrado.toDoubleOrNull() ?: 0.0
            val valorFinal = valorNumerico * multiplicador

            val textoMonto = if (valorFinal % 1.0 == 0.0) {
                valorFinal.toLong().toString()
            } else {
                valorFinal.toString()
            }

            val descripcion = textoLimpio.substring(0, matchResult.range.first).trim()

            currentMontoInput?.setText(textoMonto)

            if (descripcion.isNotEmpty()) {
                currentNombreInput?.setText(descripcion.replaceFirstChar { it.uppercase() })
            } else {
                if (currentNombreInput?.text.isNullOrEmpty()) {
                    currentNombreInput?.setText("Nueva Meta")
                }
            }
        } else {
            currentNombreInput?.setText(textoOriginal.replaceFirstChar { it.uppercase() })
            Toast.makeText(requireContext(), "No detecté un monto claro", Toast.LENGTH_SHORT).show()
        }
    }

    private fun mostrarDialogoMeta(metaDBExistente: MetaDB?) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_meta, null)

        val etNombre = dialogView.findViewById<EditText>(R.id.etNombreMeta)
        val etMontoObjetivo = dialogView.findViewById<EditText>(R.id.etMontoObjetivo)
        val etMontoActual = dialogView.findViewById<EditText>(R.id.etMontoActual)
        val btnEliminar = dialogView.findViewById<Button>(R.id.btnEliminarMeta)
        val dialogTitle = dialogView.findViewById<TextView>(R.id.dialog_title)
        val btnVoice = dialogView.findViewById<ImageButton>(R.id.btnVoiceInputMeta)

        val btnSave = dialogView.findViewById<Button>(R.id.btnSave)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)

        currentNombreInput = etNombre
        currentMontoInput = etMontoObjetivo

        btnVoice.setOnClickListener {
            startVoiceInput()
        }

        val builder = AlertDialog.Builder(requireContext())
            .setView(dialogView)

        if (metaDBExistente != null) {
            dialogTitle.text = "Editar meta"
            etNombre.setText(metaDBExistente.nombre)
            etMontoObjetivo.setText(metaDBExistente.montoObjetivo.toLong().toString())
            etMontoActual.setText(metaDBExistente.montoActual.toLong().toString())
            btnEliminar.visibility = View.VISIBLE
        } else {
            dialogTitle.text = "Nueva Meta"
            btnEliminar.visibility = View.GONE
        }

        val dialog = builder.create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()

        btnSave.setOnClickListener {
            val nombre = etNombre.text.toString().trim()

            val montoObjetivo = etMontoObjetivo.text.toString()
                .replace(",", "")
                .replace(".", "")
                .toDoubleOrNull() ?: 0.0

            val montoActual = etMontoActual.text.toString()
                .replace(",", "")
                .replace(".", "")
                .toDoubleOrNull() ?: 0.0

            if (nombre.isNotEmpty() && montoObjetivo > 0) {
                if (metaDBExistente == null) {
                    val fechaCreacion = System.currentTimeMillis()

                    val nuevaMetaDB = MetaDB(
                        nombre = nombre,
                        montoObjetivo = montoObjetivo,
                        montoActual = montoActual,
                        fechaCreacion = fechaCreacion
                    )
                    metaViewModel.insert(nuevaMetaDB)

                    if (activity != null) {
                        ReminderHelper.scheduleWeeklyMetaNotification(
                            requireContext(),
                            nuevaMetaDB.nombre,
                            fechaCreacion
                        )
                    }

                    Toast.makeText(
                        requireContext(),
                        "Meta creada exitosamente",
                        Toast.LENGTH_SHORT
                    ).show()

                } else {
                    val eraCompletada = metaDBExistente.completada
                    val esCompletadaAhora = montoActual >= montoObjetivo

                    val actualizada = metaDBExistente.copy(
                        nombre = nombre,
                        montoObjetivo = montoObjetivo,
                        montoActual = montoActual,
                        completada = esCompletadaAhora
                    )
                    metaViewModel.update(actualizada)

                    if (esCompletadaAhora && !eraCompletada) {
                        if (actualizada.fechaCreacion != null) {
                            ReminderHelper.cancelWeeklyMetaNotification(
                                requireContext(),
                                actualizada.nombre,
                                actualizada.fechaCreacion
                            )
                        }
                        Toast.makeText(
                            requireContext(),
                            "¡Meta completada!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            dialog.dismiss()
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }


        if (metaDBExistente != null) {
            btnEliminar.setOnClickListener {
                AlertDialog.Builder(requireContext())
                    .setTitle("Confirmar eliminación")
                    .setMessage("¿Eliminar '${metaDBExistente.nombre}'?")
                    .setPositiveButton("Eliminar") { _, _ ->
                        if (metaDBExistente.fechaCreacion != null) {
                            ReminderHelper.cancelWeeklyMetaNotification(
                                requireContext(),
                                metaDBExistente.nombre,
                                metaDBExistente.fechaCreacion
                            )
                        }
                        metaViewModel.delete(metaDBExistente)
                        dialog.dismiss()
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        currentNombreInput = null
        currentMontoInput = null
    }
}