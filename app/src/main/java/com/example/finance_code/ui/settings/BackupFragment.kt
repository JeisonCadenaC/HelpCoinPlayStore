package com.example.finance_code.ui.settings

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
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
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.finance_code.R
import com.example.finance_code.data.AppDB
import com.example.finance_code.data.DriveService
import com.example.finance_code.ui.home.SplashActivity
import com.example.finance_code.utils.ThemeUtils
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.material.card.MaterialCardView
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.api.services.drive.DriveScopes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class BackupFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private var userUID: String? = null
    private var userEmail: String? = null

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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_backup, container, false)
        auth = FirebaseAuth.getInstance()
        userUID = auth.currentUser?.uid
        userEmail = auth.currentUser?.email

        val btnBackBackup = root.findViewById<ImageView>(R.id.btnBackBackup)
        val cardRealizarBackup = root.findViewById<MaterialCardView>(R.id.cardRealizarBackup)
        val cardRestaurarBackup = root.findViewById<MaterialCardView>(R.id.cardRestaurarBackup)

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

        // 🛑 AHORA LANZA EL DIÁLOGO DE CONFIRMACIÓN 🛑
        cardRealizarBackup.setOnClickListener {
            mostrarDialogoConfirmarBackup()
        }

        cardRestaurarBackup.setOnClickListener {
            iniciarProcesoRestauracion()
        }

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
    // LÓGICA DE CONFIRMACIÓN Y CREACIÓN DE BACKUP
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