package com.help.finance_code.ui.settings

import android.app.Activity
import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.help.finance_code.R
import com.help.finance_code.data.AppDB
import com.help.finance_code.data.BackupWorker
import com.help.finance_code.data.DriveService
import com.help.finance_code.ui.home.SplashActivity
import com.help.finance_code.utils.ThemeUtils
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.card.MaterialCardView
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.radiobutton.MaterialRadioButton
import com.google.api.services.drive.DriveScopes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

class BackupFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private var userUID: String? = null
    private var userEmail: String? = null // Email original de login (Solo para copias manuales y nombre del archivo)

    // Preferencias para el Backup Automático
    private val PREFS_NAME = "HelpCoinBackupPrefs"
    private val KEY_FREQ = "backup_frequency"
    private val KEY_HOUR = "backup_hour"
    private val KEY_MINUTE = "backup_minute"
    private val KEY_ACCOUNT = "backup_account" // Email exclusivo de Drive

    private var selectedHour = 2
    private var selectedMinute = 0

    // Vistas de los ajustes automáticos (estilo WhatsApp)
    private lateinit var layoutFrecuencia: LinearLayout
    private lateinit var layoutHora: LinearLayout
    private lateinit var layoutCuenta: LinearLayout
    private lateinit var tvFrecuenciaSeleccionada: TextView
    private lateinit var tvHoraSeleccionada: TextView
    private lateinit var tvCuentaSeleccionada: TextView
    private lateinit var dividerHora: View

    private val googleSignInBackupLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                try {
                    val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    val account = task.getResult(ApiException::class.java)
                    if (account != null) {
                        proceedWithBackup(account)
                    }
                } catch (e: ApiException) {
                    Toast.makeText(requireContext(), "Error al autenticar con Google", Toast.LENGTH_SHORT).show()
                }
            }
        }

    private val googleSignInRestoreLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                try {
                    val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    val account = task.getResult(ApiException::class.java)
                    if (account != null) {
                        mostrarDialogoConfirmarRestauracion(account)
                    }
                } catch (e: ApiException) {
                    Toast.makeText(requireContext(), "Error al autenticar con Google", Toast.LENGTH_SHORT).show()
                }
            }
        }

    // Launcher exclusivo para seleccionar la cuenta del backup automático
    private val googleSignInAutoBackupLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                try {
                    val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    val account = task.getResult(ApiException::class.java)
                    if (account != null && account.email != null) {
                        // Guardamos el correo seleccionado y actualizamos en tiempo real
                        saveAccountPreference(account.email!!)
                        updateAutoBackupUI()
                        Toast.makeText(requireContext(), "Cuenta enlazada: ${account.email}", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: ApiException) {
                    Toast.makeText(requireContext(), "Error al enlazar cuenta", Toast.LENGTH_SHORT).show()
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_backup, container, false)
        auth = FirebaseAuth.getInstance()
        userUID = auth.currentUser?.uid
        userEmail = auth.currentUser?.email

        // Inicializar vistas manuales
        val btnBackBackup = root.findViewById<ImageView>(R.id.btnBackBackup)
        val cardRealizarBackup = root.findViewById<MaterialCardView>(R.id.cardRealizarBackup)
        val cardRestaurarBackup = root.findViewById<MaterialCardView>(R.id.cardRestaurarBackup)

        // Inicializar vistas automáticas
        layoutFrecuencia = root.findViewById(R.id.layoutFrecuencia)
        layoutHora = root.findViewById(R.id.layoutHora)
        layoutCuenta = root.findViewById(R.id.layoutCuenta)
        tvFrecuenciaSeleccionada = root.findViewById(R.id.tvFrecuenciaSeleccionada)
        tvHoraSeleccionada = root.findViewById(R.id.tvHoraSeleccionada)
        tvCuentaSeleccionada = root.findViewById(R.id.tvCuentaSeleccionada)
        dividerHora = root.findViewById(R.id.dividerHora)

        val auraColor = ThemeUtils.getAuraColor(requireContext())
        root.findViewById<ImageView>(R.id.iconUpload).imageTintList = ColorStateList.valueOf(auraColor)
        root.findViewById<ImageView>(R.id.iconDownload).imageTintList = ColorStateList.valueOf(auraColor)

        actualizarBorde(cardRealizarBackup, auraColor)
        actualizarBorde(cardRestaurarBackup, auraColor)

        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                requireActivity().supportFragmentManager.beginTransaction().remove(this@BackupFragment).commit()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)

        btnBackBackup.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction().remove(this@BackupFragment).commit()
        }

        // Listeners para copias manuales
        cardRealizarBackup.setOnClickListener { mostrarDialogoConfirmarBackup() }
        cardRestaurarBackup.setOnClickListener { iniciarProcesoRestauracion() }

        // Listeners para copias automáticas
        layoutFrecuencia.setOnClickListener { mostrarDialogoFrecuencia() }
        layoutHora.setOnClickListener { mostrarSelectorHora() }
        layoutCuenta.setOnClickListener { seleccionarCuentaGoogleAutoBackup() }

        // Cargar preferencias previas de copias automáticas
        loadPreferences()
        updateAutoBackupUI()

        return root
    }

    private fun actualizarBorde(view: View, color: Int) {
        if (view is MaterialCardView) {
            view.strokeColor = color
        }

        val bg = view.background?.mutate()
        val density = resources.displayMetrics.density
        val strokeWidth = (2 * density).toInt()

        if (bg is LayerDrawable) {
            val lastLayerIndex = bg.numberOfLayers - 1
            if (lastLayerIndex >= 0) {
                val strokeItem = bg.getDrawable(lastLayerIndex) as? GradientDrawable
                strokeItem?.setStroke(strokeWidth, color)
            }
        } else if (bg is GradientDrawable) {
            bg.setStroke(strokeWidth, color)
        }
    }

    override fun onResume() {
        super.onResume()
        val bottomNav = requireActivity().findViewById<View>(R.id.nav_view)
        bottomNav?.visibility = View.GONE

        val sharedPrefs = requireActivity().getSharedPreferences("AppPrefe", Context.MODE_PRIVATE)
        val isAmoled = sharedPrefs.getBoolean("amoled_mode", false)
        val isDark = sharedPrefs.getBoolean("modo_oscuro", false)
        if (isAmoled && isDark) {
            requireActivity().window.decorView.setBackgroundColor(Color.BLACK)
        }
    }

    override fun onStop() {
        super.onStop()
        val bottomNav = requireActivity().findViewById<View>(R.id.nav_view)
        bottomNav?.visibility = View.VISIBLE
    }

    private fun getDbIdentifier(email: String): String {
        return email.replace(Regex("[^a-zA-Z0-9]"), "_")
    }

    private fun esUsuarioGoogle(): Boolean {
        return auth.currentUser?.providerData?.any {
            it.providerId == GoogleAuthProvider.PROVIDER_ID
        } ?: false
    }

    private fun getGoogleSignInIntent(): Intent {
        val googleConf = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_APPDATA))
            .build()
        val googleClient = GoogleSignIn.getClient(requireActivity(), googleConf)
        return googleClient.signInIntent
    }

    // =========================================================
    // LÓGICA DE BACKUP AUTOMÁTICO (ESTILO WHATSAPP Y MD3)
    // =========================================================

    private fun loadPreferences() {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        selectedHour = prefs.getInt(KEY_HOUR, 2) // Por defecto a las 2:00 AM
        selectedMinute = prefs.getInt(KEY_MINUTE, 0)
    }

    private fun updateAutoBackupUI() {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val freq = prefs.getString(KEY_FREQ, "Nunca") ?: "Nunca"
        val savedAccount = prefs.getString(KEY_ACCOUNT, "")

        tvFrecuenciaSeleccionada.text = freq

        // Mostrar cuenta elegida o mensaje por defecto
        if (savedAccount.isNullOrEmpty()) {
            tvCuentaSeleccionada.text = "Ninguna cuenta seleccionada"
        } else {
            tvCuentaSeleccionada.text = savedAccount
        }

        // Mostrar u ocultar la selección de hora si es "Nunca"
        if (freq == "Nunca") {
            layoutHora.visibility = View.GONE
            dividerHora.visibility = View.GONE
        } else {
            layoutHora.visibility = View.VISIBLE
            dividerHora.visibility = View.VISIBLE

            // Formatear hora (Ej: 02:00 AM)
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, selectedHour)
                set(Calendar.MINUTE, selectedMinute)
            }
            val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            tvHoraSeleccionada.text = timeFormat.format(cal.time)
        }
    }

    private fun mostrarDialogoFrecuencia() {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.layout_bottom_sheet_frecuencia, null)
        dialog.setContentView(view)

        val rgFrecuencia = view.findViewById<RadioGroup>(R.id.rgFrecuencia)
        val rbNunca = view.findViewById<MaterialRadioButton>(R.id.rbNunca)
        val rbDiariamente = view.findViewById<MaterialRadioButton>(R.id.rbDiariamente)
        val rbSemanalmente = view.findViewById<MaterialRadioButton>(R.id.rbSemanalmente)
        val rbMensualmente = view.findViewById<MaterialRadioButton>(R.id.rbMensualmente)

        // Configurar color Aura para los RadioButtons
        val auraColor = ThemeUtils.getAuraColor(requireContext())
        val colorStateList = ColorStateList(
            arrayOf(
                intArrayOf(-android.R.attr.state_checked),
                intArrayOf(android.R.attr.state_checked)
            ),
            intArrayOf(
                Color.GRAY, // Inactivo
                auraColor   // Activo
            )
        )

        rbNunca.buttonTintList = colorStateList
        rbDiariamente.buttonTintList = colorStateList
        rbSemanalmente.buttonTintList = colorStateList
        rbMensualmente.buttonTintList = colorStateList

        // Leer preferencia actual
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentFreq = prefs.getString(KEY_FREQ, "Nunca")

        when (currentFreq) {
            "Diariamente" -> rbDiariamente.isChecked = true
            "Semanalmente" -> rbSemanalmente.isChecked = true
            "Mensualmente" -> rbMensualmente.isChecked = true
            else -> rbNunca.isChecked = true
        }

        rgFrecuencia.setOnCheckedChangeListener { _, checkedId ->
            val seleccion = when (checkedId) {
                R.id.rbDiariamente -> "Diariamente"
                R.id.rbSemanalmente -> "Semanalmente"
                R.id.rbMensualmente -> "Mensualmente"
                else -> "Nunca"
            }

            prefs.edit().putString(KEY_FREQ, seleccion).apply()

            // Si selecciona algo diferente a nunca y no ha vinculado cuenta, lo forzamos
            if (seleccion != "Nunca" && prefs.getString(KEY_ACCOUNT, "") == "") {
                seleccionarCuentaGoogleAutoBackup()
            }

            updateAutoBackupUI()
            reprogramarWorkManager()

            // Pequeño delay para que se vea el efecto de selección
            view.postDelayed({ dialog.dismiss() }, 250)
        }

        dialog.show()
    }

    private fun mostrarSelectorHora() {
        val timePickerDialog = TimePickerDialog(
            requireContext(),
            { _, hourOfDay, minute ->
                selectedHour = hourOfDay
                selectedMinute = minute
                val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit()
                    .putInt(KEY_HOUR, selectedHour)
                    .putInt(KEY_MINUTE, selectedMinute)
                    .apply()

                updateAutoBackupUI()
                reprogramarWorkManager()
            },
            selectedHour, selectedMinute, false // Formato 12 horas (AM/PM)
        )
        timePickerDialog.show()
    }

    private fun seleccionarCuentaGoogleAutoBackup() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_APPDATA))
            .build()

        val googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)

        // Forzamos signOut para que el usuario pueda elegir otra cuenta en lugar de autologear la actual
        googleSignInClient.signOut().addOnCompleteListener {
            val signInIntent = googleSignInClient.signInIntent
            googleSignInAutoBackupLauncher.launch(signInIntent)
        }
    }

    private fun saveAccountPreference(email: String) {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_ACCOUNT, email).apply()
    }

    private fun reprogramarWorkManager() {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val freq = prefs.getString(KEY_FREQ, "Nunca") ?: "Nunca"
        val workManager = WorkManager.getInstance(requireContext())

        if (freq == "Nunca") {
            workManager.cancelUniqueWork("HelpCoinAutoBackup")
            return
        }

        // Intervalos de repetición en días
        val repeatIntervalDays = when (freq) {
            "Diariamente" -> 1L
            "Semanalmente" -> 7L
            "Mensualmente" -> 30L
            else -> return
        }

        // Calcular el retraso inicial para ejecutarlo a la hora elegida (Ej: 02:00 AM)
        val currentDate = Calendar.getInstance()
        val dueDate = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, selectedHour)
            set(Calendar.MINUTE, selectedMinute)
            set(Calendar.SECOND, 0)
        }

        // Si la hora ya pasó hoy, se programa para mañana
        if (dueDate.before(currentDate)) {
            dueDate.add(Calendar.HOUR_OF_DAY, 24)
        }
        val initialDelay = dueDate.timeInMillis - currentDate.timeInMillis

        // Solo requiere internet, así ahorramos batería
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val backupRequest = PeriodicWorkRequestBuilder<BackupWorker>(repeatIntervalDays, TimeUnit.DAYS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            "HelpCoinAutoBackup",
            ExistingPeriodicWorkPolicy.UPDATE,
            backupRequest
        )

        Toast.makeText(requireContext(), "Copia programada a las ${tvHoraSeleccionada.text}", Toast.LENGTH_SHORT).show()
    }

    // =========================================================
    // LÓGICA DE CONFIRMACIÓN Y CREACIÓN DE BACKUP MANUAL
    // =========================================================

    private fun mostrarDialogoConfirmarBackup() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_confirm_backup, null)
        val btnRespaldar = dialogView.findViewById<Button>(R.id.btnConfirmarBackup)
        val btnCancelar = dialogView.findViewById<Button>(R.id.btnCancelarBackup)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()

        btnCancelar.setOnClickListener { dialog.dismiss() }
        btnRespaldar.setOnClickListener {
            dialog.dismiss()
            iniciarProcesoBackup()
        }
    }

    private fun iniciarProcesoBackup() {
        if (esUsuarioGoogle()) {
            val account = GoogleSignIn.getLastSignedInAccount(requireContext())
            if (account != null && account.grantedScopes.contains(Scope(DriveScopes.DRIVE_APPDATA))) {
                proceedWithBackup(account)
            } else {
                googleSignInBackupLauncher.launch(getGoogleSignInIntent())
            }
        } else {
            Toast.makeText(requireContext(), "Inicia sesión con Google para respaldar", Toast.LENGTH_SHORT).show()
            googleSignInBackupLauncher.launch(getGoogleSignInIntent())
        }
    }

    private fun proceedWithBackup(googleAccount: GoogleSignInAccount) {
        if (userEmail == null || userUID == null) {
            Toast.makeText(requireContext(), "Error: Datos de usuario incompletos", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_backup, null)
        val progressBar = dialogView.findViewById<LinearProgressIndicator>(R.id.progressBarBackup)
        val txtProgress = dialogView.findViewById<TextView>(R.id.txtProgressBackup)

        // 🛑 COLOR DINÁMICO DEL TEXTO SEGÚN EL TEMA 🛑
        val isDarkTheme = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        txtProgress.setTextColor(if (isDarkTheme) Color.WHITE else Color.BLACK)

        val customProgressDialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()

        customProgressDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        customProgressDialog.show()

        val progressJob = viewLifecycleOwner.lifecycleScope.launch {
            var progress = 0
            while (progress < 95) {
                progress += (2..5).random()
                if (progress > 95) progress = 95
                progressBar.progress = progress
                txtProgress.text = "Subiendo datos... $progress%"
                delay(40)
            }
        }

        val dbIdentifier = getDbIdentifier(userEmail!!)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                AppDB.checkpointAndClose(requireContext(), userEmail!!)
                val driveService = DriveService(requireContext(), googleAccount, dbIdentifier)
                val fileId = driveService.uploadFullBackup(userEmail!!, userUID!!)

                progressJob.cancel()
                progressBar.progress = 100
                txtProgress.text = "¡Completado! 100%"
                delay(500)
                customProgressDialog.dismiss()

                if (fileId != null) {
                    Toast.makeText(requireContext(), "Copia de seguridad guardada exitosamente", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(requireContext(), "Error al subir el archivo a Drive", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                progressJob.cancel()
                customProgressDialog.dismiss()
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // =========================================================
    // LÓGICA DE CONFIRMACIÓN Y RESTAURACIÓN
    // =========================================================

    private fun iniciarProcesoRestauracion() {
        if (esUsuarioGoogle()) {
            val account = GoogleSignIn.getLastSignedInAccount(requireContext())
            if (account != null && account.grantedScopes.contains(Scope(DriveScopes.DRIVE_APPDATA))) {
                mostrarDialogoConfirmarRestauracion(account)
            } else {
                googleSignInRestoreLauncher.launch(getGoogleSignInIntent())
            }
        } else {
            Toast.makeText(requireContext(), "Inicia sesión con Google para restaurar", Toast.LENGTH_SHORT).show()
            googleSignInRestoreLauncher.launch(getGoogleSignInIntent())
        }
    }

    private fun mostrarDialogoConfirmarRestauracion(googleAccount: GoogleSignInAccount) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_confirm_restore, null)
        val btnRestaurar = dialogView.findViewById<Button>(R.id.btnConfirmarRestaurar)
        val btnCancelar = dialogView.findViewById<Button>(R.id.btnCancelarRestaurar)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()

        btnCancelar.setOnClickListener { dialog.dismiss() }
        btnRestaurar.setOnClickListener {
            dialog.dismiss()
            proceedWithRestore(googleAccount)
        }
    }

    private fun proceedWithRestore(googleAccount: GoogleSignInAccount) {
        if (userEmail == null || userUID == null) {
            Toast.makeText(requireContext(), "Error: Datos de usuario incompletos", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_restore, null)
        val progressBar = dialogView.findViewById<LinearProgressIndicator>(R.id.progressBarRestore)
        val txtProgress = dialogView.findViewById<TextView>(R.id.txtProgressRestore)

        // 🛑 COLOR DINÁMICO DEL TEXTO SEGÚN EL TEMA 🛑
        val isDarkTheme = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        txtProgress.setTextColor(if (isDarkTheme) Color.WHITE else Color.BLACK)

        val customProgressDialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()

        customProgressDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        customProgressDialog.show()

        val progressJob = viewLifecycleOwner.lifecycleScope.launch {
            var progress = 0
            while (progress < 95) {
                progress += (2..5).random()
                if (progress > 95) progress = 95
                progressBar.progress = progress
                txtProgress.text = "Descargando datos... $progress%"
                delay(40)
            }
        }

        val dbIdentifier = getDbIdentifier(userEmail!!)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                AppDB.closeInstance()
                val driveService = DriveService(requireContext(), googleAccount, dbIdentifier)
                val exito = driveService.restoreFullBackup(userEmail!!, userUID!!)

                progressJob.cancel()
                if (exito) {
                    progressBar.progress = 100
                    txtProgress.text = "¡Completado! 100%"
                    delay(500)
                    customProgressDialog.dismiss()
                    Toast.makeText(requireContext(), "Restauración completada. Reiniciando...", Toast.LENGTH_LONG).show()
                    reiniciarApp()
                } else {
                    customProgressDialog.dismiss()
                    Toast.makeText(requireContext(), "No se encontró copia completa para este usuario", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                progressJob.cancel()
                customProgressDialog.dismiss()
                Toast.makeText(requireContext(), "Error al restaurar: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun reiniciarApp() {
        val intent = Intent(requireContext(), SplashActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        requireActivity().finish()
        Runtime.getRuntime().exit(0)
    }
}