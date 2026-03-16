package com.example.finance_code.ui.home

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.hardware.Sensor
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
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
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.finance_code.DiscreetModeManager
import com.example.finance_code.R
import com.example.finance_code.ShakeDetector
import com.example.finance_code.data.MetaDB
import com.example.finance_code.databinding.FragmentMetasBinding
import com.example.finance_code.viewmodel.MetaViewModel
import com.example.finance_code.viewmodel.MetaViewModelFactory
import java.io.File
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import com.example.finance_code.PDF.ExtractoBancarioHelper
import com.example.finance_code.PDF.MovimientoExtraido
class MetasFragment : Fragment() {

    private var _binding: FragmentMetasBinding? = null
    private val binding get() = _binding!!

    private val metaViewModel: MetaViewModel by viewModels {
        MetaViewModelFactory(requireActivity().application)
    }

    private lateinit var metasAdapter: MetasAdapter
    private lateinit var itemTouchHelper: ItemTouchHelper

    private var currentNombreInput: EditText? = null
    private var currentMontoInput: EditText? = null
    private var isUpdating = false
    private var currentImagePreview: ImageView? = null
    private var currentImagePreviewCard: View? = null
    private var selectedImageUri: Uri? = null
    private var cameraUri: Uri? = null

    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private lateinit var shakeDetector: ShakeDetector

    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            currentImagePreviewCard?.visibility = View.VISIBLE
            currentImagePreview?.let {
                Glide.with(this).load(uri).into(it)
            }
        }
    }

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && cameraUri != null) {
            selectedImageUri = cameraUri
            currentImagePreviewCard?.visibility = View.VISIBLE
            currentImagePreview?.let {
                Glide.with(this).load(cameraUri).into(it)
            }
        }
    }

    private val requestCameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            abrirCamara()
        } else {
            Toast.makeText(requireContext(), "⚠️ Permiso de cámara denegado.", Toast.LENGTH_LONG).show()
        }
    }

    private val requestGalleryPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            abrirGaleria()
        } else {
            Toast.makeText(requireContext(), "⚠️ Permiso de galería denegado.", Toast.LENGTH_LONG).show()
        }
    }

    private val speechLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val speechResult = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = speechResult?.get(0) ?: ""
            procesarTextoVozMeta(spokenText)
        }
    }

    private val pickPdfLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            procesarPDF(uri, "")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMetasBinding.inflate(inflater, container, false)

        DiscreetModeManager.initialize(requireContext().applicationContext)

        val myEmail = metaViewModel.userEmail

        binding.btnSubirExtracto.setOnClickListener {
            pickPdfLauncher.launch(arrayOf("application/pdf"))
        }

        val swipeHelper = object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
            override fun isLongPressDragEnabled(): Boolean = false

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPos = viewHolder.adapterPosition
                val toPos = target.adapterPosition
                metasAdapter.moveItem(fromPos, toPos)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                    viewHolder?.itemView?.alpha = 0.8f
                    viewHolder?.itemView?.scaleX = 1.02f
                    viewHolder?.itemView?.scaleY = 1.02f
                }
            }

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                viewHolder.itemView.alpha = 1.0f
                viewHolder.itemView.scaleX = 1.0f
                viewHolder.itemView.scaleY = 1.0f
                viewHolder.itemView.findViewById<View>(R.id.ivDragHandle)?.visibility = View.GONE

                val listaOrdenada = metasAdapter.getActualList()
                metaViewModel.guardarNuevoOrden(listaOrdenada)
            }
        }

        itemTouchHelper = ItemTouchHelper(swipeHelper)

        metasAdapter = MetasAdapter(
            currentUserEmail = myEmail,
            onMetaClick = { meta -> mostrarDialogoMeta(meta) },
            onMetaLongClick = { meta -> mostrarDialogoMeta(meta) },
            onDragStart = { viewHolder -> itemTouchHelper.startDrag(viewHolder) },
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
        itemTouchHelper.attachToRecyclerView(binding.rvMetas)

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
        currentImagePreview = null
        currentImagePreviewCard = null
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

    private fun procesarPDF(uri: Uri, passwordIntento: String) {
        val helper = ExtractoBancarioHelper(requireContext())
        val (necesitaPassword, texto) = helper.extraerTextoDePDF(uri, passwordIntento)

        if (necesitaPassword) {
            mostrarDialogoPasswordPDF(uri)
        } else if (texto != null) {
            if (texto.contains("Bancolombia", ignoreCase = true) || texto.contains("Sucursal", ignoreCase = true)) {
                val movimientos = helper.analizarExtractoBancolombia(texto)
                mostrarResultadosPiloto(movimientos)
            } else {
                Toast.makeText(requireContext(), "Por ahora solo soportamos Bancolombia", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(requireContext(), "Error al leer el archivo.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun mostrarDialogoPasswordPDF(uri: Uri) {
        val input = EditText(requireContext())
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        input.hint = "Ej: 1002345678"

        AlertDialog.Builder(requireContext())
            .setTitle("PDF Protegido")
            .setMessage("Este extracto tiene contraseña (usualmente tu cédula). Ingrésala para leer los datos:")
            .setView(input)
            .setPositiveButton("Desbloquear") { _, _ ->
                val password = input.text.toString()
                procesarPDF(uri, password)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarResultadosPiloto(movimientos: List<MovimientoExtraido>) {
        if (movimientos.isEmpty()) {
            Toast.makeText(requireContext(), "No se encontraron movimientos", Toast.LENGTH_SHORT).show()
            return
        }

        val mensaje = movimientos.take(5).joinToString("\n\n") {
            "${it.fecha} | ${if (it.esIngreso) "🟢" else "🔴"} $${it.monto}\n${it.descripcion}"
        }

        AlertDialog.Builder(requireContext())
            .setTitle("¡Lectura Exitosa! (${movimientos.size} movs)")
            .setMessage(mensaje)
            .setPositiveButton("Genial", null)
            .show()
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
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_congratulations, null)
        val tvNombreMeta = dialogView.findViewById<TextView>(R.id.tvNombreMetaDestacado)
        val btnGracias = dialogView.findViewById<Button>(R.id.btnGracias)
        tvNombreMeta.text = nombreMeta

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        btnGracias.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
    }

    private fun abrirCamara() {
        val photoFile = createImageFile()
        cameraUri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.provider", photoFile)
        takePictureLauncher.launch(cameraUri)
    }

    private fun abrirGaleria() {
        pickMedia.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
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
        val btnSeleccionarImagen = dialogView.findViewById<Button>(R.id.btnSeleccionarImagen)
        val cardImagePreview = dialogView.findViewById<View>(R.id.cardImagePreview)
        val ivMetaImagePreview = dialogView.findViewById<ImageView>(R.id.ivMetaImagePreview)

        currentNombreInput = etNombre
        currentMontoInput = etMontoObjetivo
        currentImagePreview = ivMetaImagePreview
        currentImagePreviewCard = cardImagePreview
        selectedImageUri = null

        btnVoice.setOnClickListener { startVoiceInput() }

        applyNumberFormatting(etMontoObjetivo)
        applyNumberFormatting(etMontoActual)
        applyNumberFormatting(etMontoOperacion)

        btnSeleccionarImagen.setOnClickListener {
            val bottomSheetDialog = com.google.android.material.bottomsheet.BottomSheetDialog(requireContext())
            val sheetView = layoutInflater.inflate(R.layout.dialog_seleccionar_imagen, null)
            bottomSheetDialog.setContentView(sheetView)

            val btnTomarFoto = sheetView.findViewById<View>(R.id.btnTomarFoto)
            val btnElegirGaleria = sheetView.findViewById<View>(R.id.btnElegirGaleria)

            btnTomarFoto.setOnClickListener {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    abrirCamara()
                } else {
                    requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
                bottomSheetDialog.dismiss()
            }

            btnElegirGaleria.setOnClickListener {
                val permiso = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.READ_MEDIA_IMAGES
                } else {
                    Manifest.permission.READ_EXTERNAL_STORAGE
                }

                if (ContextCompat.checkSelfPermission(requireContext(), permiso) == PackageManager.PERMISSION_GRANTED) {
                    abrirGaleria()
                } else {
                    requestGalleryPermissionLauncher.launch(permiso)
                }
                bottomSheetDialog.dismiss()
            }

            bottomSheetDialog.show()
        }

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

            if (metaDBExistente.imagenUrl != null) {
                cardImagePreview.visibility = View.VISIBLE
                Glide.with(this).load(metaDBExistente.imagenUrl).into(ivMetaImagePreview)
            }
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

                    metaViewModel.insert(nuevaMetaDB, emailsInvitados, selectedImageUri)

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

                    metaViewModel.update(actualizada, selectedImageUri)
                }
            }
            dialog.dismiss()
        }

        btnCancel.setOnClickListener { dialog.dismiss() }
    }
}