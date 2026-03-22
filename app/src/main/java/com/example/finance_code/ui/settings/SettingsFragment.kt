package com.example.finance_code.ui.settings

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.finance_code.R
import com.example.finance_code.data.AppDB
import com.example.finance_code.data.DriveService
import com.example.finance_code.ui.home.SplashActivity
import com.example.finance_code.ui.login.LoginActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.material.button.MaterialButton
import com.google.api.services.drive.DriveScopes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

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
        val root = inflater.inflate(R.layout.fragment_settings, container, false)
        auth = FirebaseAuth.getInstance()
        userUID = auth.currentUser?.uid
        userEmail = auth.currentUser?.email

        val switchModoOscuro = root.findViewById<SwitchCompat>(R.id.switchModoOscuro)
        val btnMenuTheme = root.findViewById<LinearLayout>(R.id.btnMenuTheme)
        val btnMenuPrivacy = root.findViewById<LinearLayout>(R.id.btnMenuPrivacy)
        val btnMenuCustomization = root.findViewById<LinearLayout>(R.id.btnMenuCustomization)
        val btnMenuBackup = root.findViewById<LinearLayout>(R.id.btnMenuBackup)
        val btnLogout = root.findViewById<MaterialButton>(R.id.btnLogout)

        val sharedPrefs = requireActivity().getSharedPreferences("AppPrefe", Context.MODE_PRIVATE)
        val isDarkModeOn = sharedPrefs.getBoolean("modo_oscuro", false)
        switchModoOscuro.isChecked = isDarkModeOn

        switchModoOscuro.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit().putBoolean("modo_oscuro", isChecked).apply()
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }

        btnMenuTheme.setOnClickListener {
            switchModoOscuro.isChecked = !switchModoOscuro.isChecked
        }

        btnMenuPrivacy.setOnClickListener {
            Toast.makeText(requireContext(), "Próximamente: Modo Discreto", Toast.LENGTH_SHORT).show()
        }

        btnMenuCustomization.setOnClickListener {
            Toast.makeText(requireContext(), "Próximamente: Selector de Aura", Toast.LENGTH_SHORT).show()
        }

        btnMenuBackup.setOnClickListener {
            val opciones = arrayOf("Hacer copia de seguridad", "Restaurar datos")
            AlertDialog.Builder(requireContext())
                .setTitle("Gestión de Respaldo")
                .setItems(opciones) { _, which ->
                    when (which) {
                        0 -> iniciarProcesoBackup()
                        1 -> iniciarProcesoRestauracion()
                    }
                }
                .show()
        }

        btnLogout.setOnClickListener {
            cerrarSesion()
        }

        return root
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

    private fun proceedWithBackup(googleAccount: GoogleSignInAccount) {
        if (userEmail == null || userUID == null) {
            Toast.makeText(requireContext(), "Error: Datos de usuario incompletos", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_backup, null)
        val progressBar = dialogView.findViewById<ProgressBar>(R.id.progressBarBackup)
        val txtProgress = dialogView.findViewById<TextView>(R.id.txtProgressBackup)

        val builder = AlertDialog.Builder(requireContext())
        builder.setView(dialogView).setCancelable(false)
        val customProgressDialog = builder.create()
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

    private fun mostrarDialogoConfirmarRestauracion(googleAccount: GoogleSignInAccount) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_confirm_restore, null)
        val btnRestaurar = dialogView.findViewById<Button>(R.id.btnConfirmarRestaurar)
        val btnCancelar = dialogView.findViewById<Button>(R.id.btnCancelarRestaurar)

        val dialog = AlertDialog.Builder(requireContext()).setView(dialogView).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnCancelar.setOnClickListener { dialog.dismiss() }
        btnRestaurar.setOnClickListener {
            dialog.dismiss()
            proceedWithRestore(googleAccount)
        }
        dialog.show()
    }

    private fun proceedWithRestore(googleAccount: GoogleSignInAccount) {
        if (userEmail == null || userUID == null) {
            Toast.makeText(requireContext(), "Error: Datos de usuario incompletos", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_restore, null)
        val progressBar = dialogView.findViewById<ProgressBar>(R.id.progressBarRestore)
        val txtProgress = dialogView.findViewById<TextView>(R.id.txtProgressRestore)

        val builder = AlertDialog.Builder(requireContext())
        builder.setView(dialogView).setCancelable(false)
        val customProgressDialog = builder.create()
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

    private fun cerrarSesion() {
        auth.signOut()

        val googleConf = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
        val googleClient = GoogleSignIn.getClient(requireActivity(), googleConf)
        googleClient.signOut()

        AppDB.closeInstance()

        val intent = Intent(requireActivity(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
        Runtime.getRuntime().exit(0)
    }
}
