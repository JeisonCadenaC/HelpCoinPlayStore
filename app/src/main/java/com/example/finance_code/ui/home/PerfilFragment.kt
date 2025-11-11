package com.example.finance_code.ui.home

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
import com.bumptech.glide.Glide
import com.example.finance_code.R
import com.example.finance_code.databinding.FragmentPerfilBinding
import com.example.finance_code.ui.login.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import java.io.File
import java.io.FileOutputStream

class PerfilFragment : Fragment() {

    private var _binding: FragmentPerfilBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private var userUID: String? = null

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

        tvNombreUsuario.text = nombreGuardado

        if (rutaImagenGuardada != null) {
            val file = File(rutaImagenGuardada)
            if (file.exists()) {
                Glide.with(this)
                    .load(file)
                    .placeholder(R.drawable.ic_perfil)
                    .into(imgPerfil)
            }
        }
    }

    private fun cargarPreferenciasModoOscuro() {
        val sharedPreferences = requireActivity().getSharedPreferences("AppPrefe", Context.MODE_PRIVATE)
        val modoOscuroActivado = sharedPreferences.getBoolean("modo_oscuro", false)
        switchModoOscuro.isChecked = modoOscuroActivado
        aplicarModoOscuro(modoOscuroActivado)
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