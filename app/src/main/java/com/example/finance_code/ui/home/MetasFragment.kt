package com.example.finance_code.ui.home

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.RecognizerIntent
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.finance_code.R
import com.example.finance_code.data.MetaDB
import com.example.finance_code.databinding.FragmentMetasBinding
import com.example.finance_code.viewmodel.MetaViewModel
import com.example.finance_code.viewmodel.MetaViewModelFactory
import com.example.finance_code.DiscreetModeManager
import com.example.finance_code.ShakeDetector
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Locale
import java.util.UUID

class MetasFragment : Fragment() {

    private var _binding: FragmentMetasBinding? = null
    private val binding get() = _binding!!

    private val metaViewModel: MetaViewModel by viewModels {
        MetaViewModelFactory(requireActivity().application)
    }

    private lateinit var metasAdapter: MetasAdapter

    private var currentNombreInput: EditText? = null
    private var currentMontoInput: EditText? = null
    private var isUpdating = false

    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private lateinit var shakeDetector: ShakeDetector

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

        DiscreetModeManager.initialize(requireContext().applicationContext)

        val myEmail = metaViewModel.userEmail

        metasAdapter = MetasAdapter(
            currentUserEmail = myEmail,
            onMetaClick = { meta ->
                mostrarDialogoMeta(meta)
            },
            onAceptarClick = { meta ->
                metaViewModel.aceptarInvitacion(meta)
                Toast.makeText(context, "¡Bienvenido a la meta!", Toast.LENGTH_SHORT).show()
            },
            onRechazarClick = { meta ->
                AlertDialog.Builder(requireContext())
                    .setTitle("Rechazar invitación")
                    .setMessage("¿Seguro que deseas rechazar esta meta?")
                    .setPositiveButton("Sí, rechazar") { _, _ ->
                        metaViewModel.rechazarInvitacion(meta)
                        Toast.makeText(context, "Invitación eliminada", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
        )

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

        binding.btnDiscreetModeManual.setOnClickListener {
            DiscreetModeManager.toggleMode()
            val message = if (DiscreetModeManager.isDiscreetModeActive) "Modo Discreto Activado" else "Modo Visible Activado"
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }

        setupSensors()

        DiscreetModeManager.modeChangeListener = {
            metasAdapter.updateDiscreetMode()
            updateDiscreetModeButtonIcon(binding.btnDiscreetModeManual)
        }
        metasAdapter.updateDiscreetMode()
        updateDiscreetModeButtonIcon(binding.btnDiscreetModeManual)

        return binding.root
    }

    private fun setupSensors() {
        sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val vibrator = ContextCompat.getSystemService(requireContext(), Vibrator::class.java)

        shakeDetector = ShakeDetector {
            DiscreetModeManager.toggleMode()

            if (vibrator != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    vibrator.vibrate(100)
                }
            }

            val mensaje = if (DiscreetModeManager.isDiscreetModeActive) "Modo Discreto Activado" else "Modo Visible Activado"
            Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateDiscreetModeButtonIcon(btnIcon: ImageButton) {
        val drawableRes = if (DiscreetModeManager.isDiscreetModeActive)
            R.drawable.ic_visibility_off
        else
            R.drawable.ic_visibility
        btnIcon.setImageResource(drawableRes)
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.also { accel ->
            sensorManager?.registerListener(shakeDetector, accel, SensorManager.SENSOR_DELAY_UI)
        }

        DiscreetModeManager.modeChangeListener = {
            metasAdapter.updateDiscreetMode()
            updateDiscreetModeButtonIcon(binding.btnDiscreetModeManual)
        }
        updateDiscreetModeButtonIcon(binding.btnDiscreetModeManual)
    }

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(shakeDetector)
    }

    override fun onDestroyView() {
        DiscreetModeManager.modeChangeListener = null
        super.onDestroyView()
        _binding = null
        currentNombreInput = null
        currentMontoInput = null
    }

    private fun applyNumberFormatting(editText: EditText) {
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(editable: Editable) {
                if (isUpdating) return

                isUpdating = true

                val text = editable.toString()
                val cleanString = text.replace(".", "").replace(",", "")

                if (cleanString.isNotEmpty()) {
                    try {
                        val parsed = cleanString.toLong()

                        val symbols = DecimalFormatSymbols(Locale("es", "CO"))
                        symbols.groupingSeparator = '.'
                        symbols.decimalSeparator = ','

                        val localFormatter = DecimalFormat("#,##0", symbols)

                        val formatted = localFormatter.format(parsed)

                        editText.setText(formatted)
                        editText.setSelection(formatted.length)

                    } catch (e: NumberFormatException) {
                    }
                }

                isUpdating = false
            }
        })
    }

    private fun mostrarDialogoHistorial(metaDB: MetaDB) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_historial_metas, null)
        val rvHistorial = dialogView.findViewById<RecyclerView>(R.id.rvHistorialAportes)
        val tvHistorialTitle = dialogView.findViewById<TextView>(R.id.tvHistorialTitle)
        val btnCerrar = dialogView.findViewById<Button>(R.id.btnCerrarHistorial)

        tvHistorialTitle.text = "Historial de Aportes: ${metaDB.nombre}"

        val adapter = AporteAdapter()
        rvHistorial.layoutManager = LinearLayoutManager(context)
        rvHistorial.adapter = adapter

        metaViewModel.getAportesLog(metaDB.id).observe(viewLifecycleOwner) { aportes ->
            adapter.setData(aportes)
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        btnCerrar.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    private fun startVoiceInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Ej: 'Viaje a Europa 8 millones'")

        try {
            speechLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Tu dispositivo no soporta entrada de voz", Toast.LENGTH_SHORT).show()
        }
    }

    private fun procesarTextoVozMeta(textoOriginal: String) {
        var texto = textoOriginal.lowercase(Locale.getDefault()).trim()
        val numerosMap = mapOf(
            "cero" to "0", "un" to "1", "uno" to "1", "una" to "1",
            "dos" to "2", "tres" to "3", "cuatro" to "4", "cinco" to "5",
            "seis" to "6", "siete" to "7", "ocho" to "8", "nueve" to "9", "diez" to "10"
        )
        numerosMap.forEach { (palabra, digito) ->
            texto = texto.replace(Regex("\\b$palabra\\b"), digito)
        }
        var multiplicador = 1.0
        var textoLimpio = texto
        if (texto.contains("millones") || texto.contains("millón") || texto.contains("millon")) {
            multiplicador = 1000000.0
            textoLimpio = texto.replace(Regex("millon(es)?"), "").trim()
        } else if (texto.contains("mil")) {
            multiplicador = 1000.0
            textoLimpio = texto.replace(Regex("mil"), "").trim()
        }
        val regex = Regex("([0-9]+[.,]?[0-9]*[.,]?[0-9]*)")
        val matchResult = regex.findAll(textoLimpio).lastOrNull()
        if (matchResult != null) {
            var numeroString = matchResult.value.replace(",", "").replace(".", "")
            val valorNumerico = numeroString.toDoubleOrNull() ?: 0.0
            val valorFinal = valorNumerico * multiplicador
            val textoMonto = if (valorFinal % 1.0 == 0.0) valorFinal.toLong().toString() else valorFinal.toString()
            val descripcion = textoOriginal.substring(0, textoOriginal.indexOf(matchResult.value)).trim()
                .replace(Regex("millon(es)?|mil$"), "").trim()
            currentMontoInput?.setText(textoMonto)
            if (descripcion.isNotEmpty()) {
                if (currentNombreInput?.text.isNullOrEmpty() || currentNombreInput?.text.toString() == "Nueva Meta") {
                    currentNombreInput?.setText(descripcion.replaceFirstChar { it.uppercase() })
                }
            }
        } else {
            currentNombreInput?.setText(textoOriginal.replaceFirstChar { it.uppercase() })
            Toast.makeText(requireContext(), "No detecté un monto claro", Toast.LENGTH_SHORT).show()
        }
    }

    private fun mostrarFelicitaciones(nombreMeta: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("¡Excelente!")
            .setMessage("¡Felicidades! Has completado tu meta: $nombreMeta. \n¡Sigue así!")
            .setIcon(R.drawable.ic_rocket)
            .setPositiveButton("Gracias", null)
            .show()
    }

    private fun mostrarDialogoMeta(metaDBExistente: MetaDB?) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_meta, null)
        val contenedorFormulario = dialogView.findViewById<View>(R.id.contenedorFormulario)
        val contenedorConfirmacion = dialogView.findViewById<View>(R.id.contenedorConfirmacion)
        val etNombre = dialogView.findViewById<EditText>(R.id.etNombreMeta)
        val etMontoObjetivo = dialogView.findViewById<EditText>(R.id.etMontoObjetivo)
        val etMontoActual = dialogView.findViewById<EditText>(R.id.etMontoActual)
        val etMontoOperacion = dialogView.findViewById<EditText>(R.id.etMontoOperacion)
        val etEmailCompartido = dialogView.findViewById<EditText>(R.id.etEmailCompartido)
        val emailLayout = dialogView.findViewById<View>(R.id.emailCompartidoLayout)
        val btnSumar = dialogView.findViewById<View>(R.id.btnSumar)
        val btnRestar = dialogView.findViewById<View>(R.id.btnRestar)
        val layoutOperaciones = dialogView.findViewById<View>(R.id.layoutOperaciones)
        val btnEliminar = dialogView.findViewById<Button>(R.id.btnEliminarMeta)
        val dialogTitle = dialogView.findViewById<TextView>(R.id.dialog_title)
        val btnVoice = dialogView.findViewById<ImageButton>(R.id.btnVoiceInputMeta)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSave)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val tvMensajeConfirmacion = dialogView.findViewById<TextView>(R.id.tvMensajeConfirmacion)
        val btnCancelarDelete = dialogView.findViewById<Button>(R.id.btnCancelarDelete)
        val btnConfirmarDelete = dialogView.findViewById<Button>(R.id.btnConfirmarDelete)
        val btnVerHistorial = dialogView.findViewById<Button>(R.id.btnVerHistorial)

        currentNombreInput = etNombre
        currentMontoInput = etMontoObjetivo
        btnVoice.setOnClickListener { startVoiceInput() }

        applyNumberFormatting(etMontoObjetivo)
        applyNumberFormatting(etMontoActual)
        applyNumberFormatting(etMontoOperacion)

        val builder = AlertDialog.Builder(requireContext()).setView(dialogView)

        val localeCO = Locale.Builder().setLanguage("es").setRegion("CO").build()
        val formatoMoneda = NumberFormat.getCurrencyInstance(localeCO).apply {
            maximumFractionDigits = 0
        }

        val cleanAndParse = { editText: EditText ->
            editText.text.toString().replace(".", "").replace(",", ".").toDoubleOrNull() ?: 0.0
        }

        val originalMontoActual = metaDBExistente?.montoActual ?: 0.0

        if (metaDBExistente != null) {
            dialogTitle.text = "Gestionar meta"
            etNombre.setText(metaDBExistente.nombre)
            etMontoObjetivo.setText(metaDBExistente.montoObjetivo.toLong().toString())
            etMontoActual.setText(metaDBExistente.montoActual.toLong().toString())

            btnEliminar.visibility = View.VISIBLE
            layoutOperaciones.visibility = View.VISIBLE
            emailLayout.visibility = View.GONE
            tvMensajeConfirmacion.text = "¿Eliminar '${metaDBExistente.nombre}'?"
            btnVerHistorial.visibility = View.VISIBLE
            btnVerHistorial.setOnClickListener { mostrarDialogoHistorial(metaDBExistente) }
        } else {
            dialogTitle.text = "Nueva Meta"
            etMontoActual.setText("0")
            btnEliminar.visibility = View.GONE
            layoutOperaciones.visibility = View.GONE
            emailLayout.visibility = View.VISIBLE
            btnVerHistorial.visibility = View.GONE
        }

        val dialog = builder.create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val observer = androidx.lifecycle.Observer<List<MetaDB>> { metas ->
            val metaActualizada = metas.find { it.id == metaDBExistente?.id }
            if (metaActualizada != null && dialog.isShowing) {
                val monto = metaActualizada.montoActual
                if (!isUpdating) {
                    etMontoActual.setText(monto.toLong().toString())
                }
            }
        }

        if (metaDBExistente != null) {
            metaViewModel.allMetas.observe(viewLifecycleOwner, observer)
        }

        dialog.setOnDismissListener {
            if (metaDBExistente != null) {
                metaViewModel.allMetas.removeObserver(observer)
            }
        }

        val realizarOperacion = { sumar: Boolean ->
            val valorOperacion = cleanAndParse(etMontoOperacion)

            if (valorOperacion > 0) {

                val valorActualStr = cleanAndParse(etMontoActual)
                val montoCambio = if (sumar) valorOperacion else -valorOperacion

                val nuevoTotal = valorActualStr + montoCambio
                val totalFinal = if (nuevoTotal < 0) 0.0 else nuevoTotal

                etMontoActual.setText(totalFinal.toLong().toString())

                etMontoOperacion.setText("")

            } else if (metaDBExistente == null) {
                Toast.makeText(requireContext(), "Guarda la meta primero para registrar movimientos.", Toast.LENGTH_SHORT).show()
            }
        }

        btnSumar.setOnClickListener { realizarOperacion(true) }
        btnRestar.setOnClickListener { realizarOperacion(false) }

        dialog.show()

        btnEliminar.setOnClickListener {
            contenedorFormulario.visibility = View.GONE
            contenedorConfirmacion.visibility = View.VISIBLE
        }
        btnCancelarDelete.setOnClickListener {
            contenedorConfirmacion.visibility = View.GONE
            contenedorFormulario.visibility = View.VISIBLE
        }
        btnConfirmarDelete.setOnClickListener {
            if (metaDBExistente != null) {
                if (metaDBExistente.fechaCreacion != null) {
                    ReminderHelper.cancelWeeklyMetaNotification(requireContext(), metaDBExistente.nombre, metaDBExistente.fechaCreacion)
                }
                metaViewModel.delete(metaDBExistente)
                dialog.dismiss()
            }
        }

        btnSave.setOnClickListener {
            val nombre = etNombre.text.toString().trim()
            val montoObjetivo = cleanAndParse(etMontoObjetivo)
            val emailsInvitados = etEmailCompartido.text.toString()
            val userName = metaViewModel.userEmail.substringBefore('@')

            if (nombre.isNotEmpty() && montoObjetivo > 0) {
                if (metaDBExistente == null) {
                    val fechaCreacion = System.currentTimeMillis()
                    val montoActual = cleanAndParse(etMontoActual)

                    val nuevaMetaDB = MetaDB(id = UUID.randomUUID().toString(), nombre = nombre, montoObjetivo = montoObjetivo, montoActual = montoActual, fechaCreacion = fechaCreacion)

                    metaViewModel.insert(nuevaMetaDB, emailsInvitados)

                    if (activity != null) {
                        ReminderHelper.scheduleWeeklyMetaNotification(requireContext(), nuevaMetaDB.nombre, fechaCreacion)
                        ReminderHelper.showInstantMetaNotification(requireContext(), nuevaMetaDB.nombre)
                    }

                    val invitadosSeparados = emailsInvitados.split(",").map { it.trim() }.filter { it.isNotEmpty() }

                    val toastMessage = if (invitadosSeparados.isNotEmpty()) {
                        "Meta creada. Invitaciones enviadas a: ${invitadosSeparados.joinToString(", ")}"
                    } else {
                        "Meta creada exitosamente."
                    }
                    Toast.makeText(requireContext(), toastMessage, Toast.LENGTH_LONG).show()

                } else {
                    val nuevoMontoActual = cleanAndParse(etMontoActual)
                    val montoNetoCambio = nuevoMontoActual - originalMontoActual

                    if (Math.abs(montoNetoCambio) > 0.001) {
                        val tipo = if (montoNetoCambio > 0) "APORTE" else "RETIRO"
                        val montoAbsoluto = formatoMoneda.format(Math.abs(montoNetoCambio))
                        val accion = if (montoNetoCambio > 0) "agregó" else "restó"

                        metaViewModel.registrarAporte(metaDBExistente, montoNetoCambio, tipo)

                        Toast.makeText(requireContext(), "${userName} ${accion} ${montoAbsoluto}.", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(requireContext(), "Meta '${nombre}' actualizada.", Toast.LENGTH_SHORT).show()
                    }

                    val esCompletadaAhora = nuevoMontoActual >= montoObjetivo
                    val actualizada = metaDBExistente.copy(
                        nombre = nombre,
                        montoObjetivo = montoObjetivo,
                        completada = esCompletadaAhora,
                        montoActual = nuevoMontoActual
                    )

                    if (esCompletadaAhora && !metaDBExistente.completada) {
                        mostrarFelicitaciones(nombre)
                    }

                    metaViewModel.update(actualizada)
                }
            }
            dialog.dismiss()
        }

        btnCancel.setOnClickListener { dialog.dismiss() }
    }
}