package com.example.finance_code.ui.home

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.example.finance_code.R
import com.example.finance_code.ui.login.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.switchmaterial.SwitchMaterial

class PerfilFragment : Fragment() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var auth: FirebaseAuth
    private lateinit var themeSwitch: SwitchMaterial
    private lateinit var themeIcon: ImageView
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var txtEmail: TextView

    companion object {
        private const val PREFS_NAME = "theme_prefs"
        private const val KEY_THEME = "theme_key"
        private const val NIGHT_MODE = AppCompatDelegate.MODE_NIGHT_YES
        private const val LIGHT_MODE = AppCompatDelegate.MODE_NIGHT_NO
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_perfil, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        sharedPreferences = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        txtEmail = view.findViewById(R.id.txtUserEmail)
        themeSwitch = view.findViewById(R.id.themeSwitch)
        themeIcon = view.findViewById(R.id.imgThemeIcon)
        val btnLogout = view.findViewById<Button>(R.id.btnLogout)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(requireContext(), gso)

        displayUserEmail()
        loadThemePreference()
        setupListeners()

        btnLogout.setOnClickListener {
            cerrarSesion()
        }
    }

    private fun displayUserEmail() {
        val user = auth.currentUser
        txtEmail.text = user?.email ?: "Invitado"
    }

    private fun cerrarSesion() {
        googleSignInClient.signOut().addOnCompleteListener {
            auth.signOut()

            Toast.makeText(requireContext(), "Sesión cerrada correctamente", Toast.LENGTH_SHORT).show()

            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)

            requireActivity().finish()
        }
    }

    private fun loadThemePreference() {
        val currentTheme = sharedPreferences.getInt(KEY_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        themeSwitch.isChecked = currentTheme == NIGHT_MODE
        updateThemeIcon(themeSwitch.isChecked)
    }

    private fun setupListeners() {
        themeSwitch.setOnCheckedChangeListener { _, isChecked ->
            val mode = if (isChecked) NIGHT_MODE else LIGHT_MODE
            setTheme(mode)
            updateThemeIcon(isChecked)
        }
    }

    private fun setTheme(mode: Int) {
        sharedPreferences.edit().putInt(KEY_THEME, mode).apply()
        AppCompatDelegate.setDefaultNightMode(mode)

       requireActivity().recreate()
    }

    private fun updateThemeIcon(isDark: Boolean) {
        if (isDark) {
            themeIcon.setImageResource(R.drawable.ic_delete)
        } else {
            themeIcon.setImageResource(R.drawable.ic_add)
        }
    }
}