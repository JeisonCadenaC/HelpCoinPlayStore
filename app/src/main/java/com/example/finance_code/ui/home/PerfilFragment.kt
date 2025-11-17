package com.example.finance_code.ui.home

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.finance_code.R
import com.example.finance_code.data.AppDB
import com.example.finance_code.data.DriveService
import com.example.finance_code.databinding.FragmentPerfilBinding
import com.example.finance_code.ui.login.LoginActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.launch

class PerfilFragment : Fragment() {

    private var _binding: FragmentPerfilBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private var userUID: String? = null
    private var userEmail: String? = null

    private lateinit var imgPerfil: ImageView
    private lateinit var tvNombreUsuario: TextView
    private lateinit var tvEmailUsuario: TextView
    private lateinit var imgEditarNombre: ImageView
    private lateinit var switchModoOscuro: SwitchCompat

    private val KEY_USER_NAME = "user_name"
    private val KEY_IMAGE_PATH = "profile_image_path"

    private val photoPickerLauncher =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                cargarImagenSeleccionada(uri)
                guardarImagenLocalmente(uri)
            }
        }

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
                    Toast.makeText(requireContext(), "Error al seleccionar cuenta", Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(requireContext(), "Error al seleccionar cuenta", Toast.LENGTH_SHORT).show()
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPerfilBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        try {
            auth = FirebaseAuth.getInstance()
            val currentUser = auth.currentUser
            if (currentUser == null) {
                cerrarSesion()
                return
            }
            userUID = currentUser.uid
            userEmail = currentUser.email

            if(userEmail == null) {
                Toast.makeText(requireContext(), "Error: Email de usuario nulo", Toast.LENGTH_SHORT).show()
                cerrarSesion()
                return
            }

        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error al inicializar", Toast.LENGTH_SHORT).show()
            return
        }

        imgPerfil = binding.imgPerfil
        tvNombreUsuario = binding.tvNombreUsuario
        tvEmailUsuario = binding.tvEmailUsuario
        imgEditarNombre = binding.imgEditarNombre
        switchModoOscuro = binding.switchModoOscuro

        cargarDatosUsuario()
        cargarPreferenciasModoOscuro()

        binding.btnCerrarSesion.setOnClickListener {
            cerrarSesion()
        }

        binding.cardViewImagen.setOnClickListener {
            lanzarSelectorFoto()
        }

        imgEditarNombre.setOnClickListener {
            mostrarDialogoEditarNombre()
        }

        switchModoOscuro.setOnCheckedChangeListener { _, isChecked ->
            configurarModoOscuro(isChecked)
        }

        binding.btnHacerBackup.setOnClickListener {
            iniciarProcesoBackup()
        }

        binding.btnRestaurar.setOnClickListener {
            iniciarProcesoRestauracion()
        }
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
            Toast.makeText(requireContext(), "Selecciona una cuenta de Google para guardar", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(requireContext(), "Selecciona la cuenta de Google de la que quieres restaurar", Toast.LENGTH_SHORT).show()
            googleSignInRestoreLauncher.launch(getGoogleSignInIntent())
        }
    }

    private fun proceedWithBackup(googleAccount: GoogleSignInAccount) {
        if (userEmail == null) {
            Toast.makeText(requireContext(), "Error: No se encontró Email de usuario", Toast.LENGTH_SHORT).show()
            return
        }

        val dbIdentifier = getDbIdentifier(userEmail!!)
        val dbName = "finance_db_$dbIdentifier"
        val dbFile = requireContext().getDatabasePath(dbName)

        if (!dbFile.exists()) {
            Toast.makeText(requireContext(), "Error: No se encontró la base de datos local", Toast.LENGTH_SHORT).show()
            return
        }

        val driveService = DriveService(requireContext(), googleAccount, dbIdentifier)
        Toast.makeText(requireContext(), "Iniciando copia de seguridad...", Toast.LENGTH_SHORT).show()

        viewLifecycleOwner.lifecycleScope.launch {
            val fileId = driveService.uploadBackup(dbFile)
            if (fileId != null) {
                Toast.makeText(requireContext(), "Copia de seguridad completada", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(requireContext(), "Error al realizar la copia", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun mostrarDialogoConfirmarRestauracion(googleAccount: GoogleSignInAccount) {
        AlertDialog.Builder(requireContext())
            .setTitle("Restaurar Copia")
            .setMessage("Esto sobrescribirá tus datos locales. ¿Estás seguro?\nLa app se reiniciará.")
            .setPositiveButton("Restaurar") { _, _ ->
                proceedWithRestore(googleAccount)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun proceedWithRestore(googleAccount: GoogleSignInAccount) {
        if (userEmail == null) {
            Toast.makeText(requireContext(), "Error: No se encontró Email de usuario", Toast.LENGTH_SHORT).show()
            return
        }

        val dbIdentifier = getDbIdentifier(userEmail!!)
        val dbName = "finance_db_$dbIdentifier"
        val dbFile = requireContext().getDatabasePath(dbName)

        AppDB.closeInstance()

        val driveService = DriveService(requireContext(), googleAccount, dbIdentifier)
        Toast.makeText(requireContext(), "Iniciando restauración...", Toast.LENGTH_SHORT).show()

        viewLifecycleOwner.lifecycleScope.launch {
            val exito = driveService.downloadRestore(dbFile)
            if (exito) {
                Toast.makeText(requireContext(), "Restauración completada. Reinicia la app.", Toast.LENGTH_LONG).show()
                activity?.finish()
            } else {
                Toast.makeText(requireContext(), "Error al restaurar o no se encontró copia", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun getPrefs(): SharedPreferences? {
        val uid = userUID ?: return null
        val prefsName = "${uid}_UserProfilePrefs"
        return requireContext().getSharedPreferences(prefsName, Context.MODE_PRIVATE)
    }

    private fun getProfileImageName(): String {
        val uid = userUID ?: "default_user"
        return "${uid}_profile_image.jpg"
    }

    private fun lanzarSelectorFoto() {
        photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private fun cargarImagenSeleccionada(uri: Uri) {
        Glide.with(this)
            .load(uri)
            .into(imgPerfil)
    }

    private fun guardarImagenLocalmente(uri: Uri) {
        try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val file = File(requireContext().filesDir, getProfileImageName())
            val outputStream = FileOutputStream(file)

            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()

            val prefs = getPrefs()
            prefs?.edit()?.putString(KEY_IMAGE_PATH, file.absolutePath)?.apply()

        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error al guardar imagen local", Toast.LENGTH_SHORT).show()
        }
    }

    private fun mostrarDialogoEditarNombre() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Cambiar nombre de usuario")

        val input = EditText(requireContext())
        input.setText(tvNombreUsuario.text)
        builder.setView(input)

        builder.setPositiveButton("Guardar") { dialog, _ ->
            val nuevoNombre = input.text.toString().trim()
            if (nuevoNombre.isNotEmpty()) {
                val prefs = getPrefs()
                prefs?.edit()?.putString(KEY_USER_NAME, nuevoNombre)?.apply()
                tvNombreUsuario.text = nuevoNombre
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    private fun cargarDatosUsuario() {
        tvEmailUsuario.text = auth.currentUser?.email

        val prefs = getPrefs()
        val nombreGuardado = prefs?.getString(KEY_USER_NAME, "Usuario") ?: "Usuario"
        val rutaImagenGuardada = prefs?.getString(KEY_IMAGE_PATH, null)
        val googlePhotoUrl = prefs?.getString("google_photo_url", null)

        tvNombreUsuario.text = nombreGuardado

        if (rutaImagenGuardada != null) {
            val file = File(rutaImagenGuardada)
            if (file.exists()) {
                Glide.with(this)
                    .load(file)
                    .placeholder(R.drawable.ic_perfil)
                    .into(imgPerfil)
            }
        } else if (googlePhotoUrl != null) {
            Glide.with(this)
                .load(googlePhotoUrl)
                .placeholder(R.drawable.ic_perfil)
                .into(imgPerfil)
        } else {
            Glide.with(this)
                .load(R.drawable.ic_perfil)
                .into(imgPerfil)
        }
    }

    private fun cargarPreferenciasModoOscuro() {
        val sharedPreferences = requireActivity().getSharedPreferences("AppPrefe", Context.MODE_PRIVATE)
        val modoOscuroActivado = sharedPreferences.getBoolean("modo_oscuro", false)
        switchModoOscuro.isChecked = modoOscuroActivado
    }
    private fun configurarModoOscuro(activado: Boolean) {
        val sharedPreferences = requireActivity().getSharedPreferences("AppPrefe", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putBoolean("modo_oscuro", activado)
            apply()
        }
        aplicarModoOscuro(activado)
    }

    private fun aplicarModoOscuro(activado: Boolean) {
        if (activado) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}