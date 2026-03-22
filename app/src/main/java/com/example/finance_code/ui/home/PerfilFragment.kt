@file:Suppress("DEPRECATION")
package com.example.finance_code.ui.home

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.finance_code.R
import com.example.finance_code.data.AppDB
import com.example.finance_code.databinding.FragmentPerfilBinding
import com.example.finance_code.ui.login.LoginActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import java.io.File
import java.io.FileOutputStream

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

    private val KEY_USER_NAME = "user_name"
    private val KEY_IMAGE_PATH = "profile_image_path"

    private val photoPickerLauncher =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                cargarImagenSeleccionada(uri)
                guardarImagenLocalmente(uri)
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
                cerrarSesionForzada()
                return
            }
            userUID = currentUser.uid
            userEmail = currentUser.email

            if(userEmail == null) {
                Toast.makeText(requireContext(), "Error: Email de usuario nulo", Toast.LENGTH_SHORT).show()
                cerrarSesionForzada()
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

        // Acción del botón de retroceso superior
        binding.btnVolverAtras.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        try {
            val packageInfo = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
            binding.tvVersionApp.text = "Versión: ${packageInfo.versionName} (Evolution)"
        } catch (e: Exception) {
            binding.tvVersionApp.text = "Versión: Desconocida"
        }

        cargarDatosUsuario()

        binding.cardViewImagen.setOnClickListener {
            photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        imgEditarNombre.setOnClickListener {
            mostrarDialogoEditarNombre()
        }

        if (esUsuarioGoogle()) {
            binding.btnCambiarContrasena.visibility = View.GONE
        } else {
            binding.btnCambiarContrasena.visibility = View.VISIBLE
            binding.btnCambiarContrasena.setOnClickListener {
                mostrarDialogoCambiarContrasena()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Ocultar menú inferior como si fuera una ventana completa
        val bottomNav = requireActivity().findViewById<View>(R.id.nav_view)
        bottomNav?.visibility = View.GONE
    }

    override fun onStop() {
        super.onStop()
        // Restaurar menú inferior al salir
        val bottomNav = requireActivity().findViewById<View>(R.id.nav_view)
        bottomNav?.visibility = View.VISIBLE
    }

    private fun mostrarDialogoCambiarContrasena() {
        val user = auth.currentUser
        if (user == null || userEmail == null) {
            Toast.makeText(requireContext(), "Error de sesión", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_cambiar_contrasena, null)
        val etContrasenaActual = dialogView.findViewById<TextInputEditText>(R.id.etContrasenaActual)
        val etNuevaContrasena = dialogView.findViewById<TextInputEditText>(R.id.etNuevaContrasena)
        val etConfirmarContrasena = dialogView.findViewById<TextInputEditText>(R.id.etConfirmarContrasena)
        val btnGuardar = dialogView.findViewById<Button>(R.id.btnGuardarContrasena)
        val btnCancelar = dialogView.findViewById<Button>(R.id.btnCancelarCambioContrasena)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()

        btnCancelar.setOnClickListener { dialog.dismiss() }

        btnGuardar.setOnClickListener {
            val actualPass = etContrasenaActual.text.toString()
            val nuevaPass = etNuevaContrasena.text.toString()
            val confirmarPass = etConfirmarContrasena.text.toString()

            if (actualPass.isEmpty() || nuevaPass.isEmpty() || confirmarPass.isEmpty()) {
                Toast.makeText(requireContext(), "Por favor, completa todos los campos.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (nuevaPass.length < 6) {
                Toast.makeText(requireContext(), "La nueva contraseña debe tener al menos 6 caracteres.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (nuevaPass != confirmarPass) {
                Toast.makeText(requireContext(), "La nueva contraseña y la confirmación no coinciden.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            actualizarContrasenaEnFirebase(actualPass, nuevaPass, dialog)
        }
    }

    private fun actualizarContrasenaEnFirebase(actualPass: String, nuevaPass: String, dialog: AlertDialog) {
        val user = auth.currentUser
        val progressDialog = ProgressDialog(requireContext())
        progressDialog.setMessage("Verificando credenciales y actualizando contraseña...")
        progressDialog.setCancelable(false)
        progressDialog.show()

        val credential = EmailAuthProvider.getCredential(userEmail!!, actualPass)

        user?.reauthenticate(credential)
            ?.addOnCompleteListener { reauthTask ->
                progressDialog.dismiss()
                if (reauthTask.isSuccessful) {
                    actualizarContrasena(user, nuevaPass, progressDialog, dialog)
                } else {
                    Toast.makeText(requireContext(), "Error de autenticación: Contraseña actual incorrecta.", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun actualizarContrasena(user: FirebaseUser, nuevaPass: String, progressDialog: ProgressDialog, dialog: AlertDialog) {
        progressDialog.setMessage("Cambiando contraseña...")
        progressDialog.show()

        user.updatePassword(nuevaPass)
            .addOnCompleteListener { updateTask ->
                progressDialog.dismiss()
                if (updateTask.isSuccessful) {
                    Toast.makeText(requireContext(), "Contraseña actualizada exitosamente.", Toast.LENGTH_LONG).show()
                    dialog.dismiss()
                } else {
                    try {
                        throw updateTask.exception!!
                    } catch (e: FirebaseAuthRecentLoginRequiredException) {
                        Toast.makeText(requireContext(), "Error de sesión. Por favor, vuelve a iniciar sesión.", Toast.LENGTH_LONG).show()
                        cerrarSesionForzada()
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Error al actualizar: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
    }

    private fun esUsuarioGoogle(): Boolean {
        return auth.currentUser?.providerData?.any {
            it.providerId == GoogleAuthProvider.PROVIDER_ID
        } ?: false
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

    private fun cargarImagenSeleccionada(uri: Uri) {
        Glide.with(this).load(uri).into(imgPerfil)
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
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_name, null)
        val etNombre = dialogView.findViewById<TextInputEditText>(R.id.etNuevoNombre)
        val btnGuardar = dialogView.findViewById<Button>(R.id.btnGuardarNombre)
        val btnCancelar = dialogView.findViewById<Button>(R.id.btnCancelarNombre)

        etNombre.setText(tvNombreUsuario.text)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnCancelar.setOnClickListener { dialog.dismiss() }

        btnGuardar.setOnClickListener {
            val nuevoNombre = etNombre.text.toString().trim()
            if (nuevoNombre.isNotEmpty()) {
                getPrefs()?.edit()?.putString(KEY_USER_NAME, nuevoNombre)?.apply()
                tvNombreUsuario.text = nuevoNombre
            }
            dialog.dismiss()
        }
        dialog.show()
        etNombre.requestFocus()
        etNombre.selectAll()
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
                Glide.with(this).load(file).placeholder(R.drawable.ic_perfil).diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true).into(imgPerfil)
            } else if (googlePhotoUrl != null) {
                Glide.with(this).load(googlePhotoUrl).placeholder(R.drawable.ic_perfil).into(imgPerfil)
            } else {
                Glide.with(this).load(R.drawable.ic_perfil).into(imgPerfil)
            }
        } else if (googlePhotoUrl != null) {
            Glide.with(this).load(googlePhotoUrl).placeholder(R.drawable.ic_perfil).into(imgPerfil)
        } else {
            Glide.with(this).load(R.drawable.ic_perfil).into(imgPerfil)
        }
    }

    private fun cerrarSesionForzada() {
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}