package com.example.finance_code.ui.login

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import android.widget.CheckBox
import android.text.method.LinkMovementMethod
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.appcompat.app.AppCompatDelegate
import com.example.finance_code.R
import com.example.finance_code.ui.home.HomeActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import android.content.SharedPreferences

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var loginButton: Button
    private lateinit var registerButton: Button
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var googleButton: ImageButton
    private lateinit var termsAndConditionsCheckbox: CheckBox
    private lateinit var themeButton: ImageButton
    private lateinit var sharedPreferences: SharedPreferences

    companion object {
        private const val PREFS_NAME = "theme_prefs"
        private const val KEY_THEME = "theme_key"
        private const val NIGHT_MODE = AppCompatDelegate.MODE_NIGHT_YES
        private const val LIGHT_MODE = AppCompatDelegate.MODE_NIGHT_NO
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setup()
    }

    private fun setup() {
        title = "Autenticación del usuario"
        auth = FirebaseAuth.getInstance()

        loginButton = findViewById(R.id.btnsingUpButton)
        registerButton = findViewById(R.id.btnregisterButton)
        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        googleButton = findViewById(R.id.googleButton)
        termsAndConditionsCheckbox = findViewById(R.id.termsAndConditionsCheckbox)
        themeButton = findViewById(R.id.themeToggleButton)

        termsAndConditionsCheckbox.movementMethod = LinkMovementMethod.getInstance()

        updateThemeButtonIcon()

        loginButton.setOnClickListener { Login() }
        registerButton.setOnClickListener { Register() }

        themeButton.setOnClickListener {
            toggleTheme()
        }

        googleButton.setOnClickListener {
            if (!termsAndConditionsCheckbox.isChecked) {
                showAlert("Términos y Condiciones", getString(R.string.error_accept_terms))
                return@setOnClickListener
            }

            val googleConf = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()

            val googleClient = GoogleSignIn.getClient(this, googleConf)
            startActivityForResult(googleClient.signInIntent, 100)
        }
    }

    private fun toggleTheme() {
        val currentTheme = sharedPreferences.getInt(KEY_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        val newMode = if (currentTheme == NIGHT_MODE) LIGHT_MODE else NIGHT_MODE

        sharedPreferences.edit().putInt(KEY_THEME, newMode).apply()
        AppCompatDelegate.setDefaultNightMode(newMode)
        recreate()
    }

    private fun updateThemeButtonIcon() {
        val currentNightMode = sharedPreferences.getInt(KEY_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        val isDark = currentNightMode == NIGHT_MODE

        if (isDark) {
            themeButton.setImageResource(R.drawable.ic_theme_moon)
        } else {
            themeButton.setImageResource(R.drawable.ic_theme_sun)
        }
    }

    private fun Login() {
        if (!termsAndConditionsCheckbox.isChecked) {
            showAlert("Términos y Condiciones", getString(R.string.error_accept_terms))
            return
        }

        if (emailEditText.text.isNotEmpty() && passwordEditText.text.isNotEmpty()) {
            auth.signInWithEmailAndPassword(
                emailEditText.text.toString(),
                passwordEditText.text.toString()
            ).addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    showPrincipalView()
                } else {
                    showAlert("Error de autenticación", "No se pudo iniciar sesión. Verifique sus credenciales.")
                }
            }
        } else {
            showAlert("Campos obligatorios", "Por favor, ingrese su correo y contraseña.")
        }
    }

    private fun Register() {
        if (!termsAndConditionsCheckbox.isChecked) {
            showAlert("Términos y Condiciones", getString(R.string.error_accept_terms))
            return
        }

        if (emailEditText.text.isNotEmpty() && passwordEditText.text.isNotEmpty()) {
            auth.createUserWithEmailAndPassword(
                emailEditText.text.toString(),
                passwordEditText.text.toString()
            ).addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    showPrincipalView()
                } else {
                    val errorMessage = task.exception?.message ?: "Error desconocido al registrar el usuario."
                    showAlert("Error en el registro", errorMessage)
                }
            }
        } else {
            showAlert("Campos obligatorios", "Por favor, complete todos los campos para registrarse.")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 100) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val cuenta = task.getResult(ApiException::class.java)

                val credential = GoogleAuthProvider.getCredential(cuenta.idToken, null)
                auth.signInWithCredential(credential).addOnCompleteListener(this) { firebaseTask ->
                    if (firebaseTask.isSuccessful) {
                        showPrincipalView()
                        val usuario = auth.currentUser
                        Toast.makeText(this, "Bienvenido ${usuario?.displayName ?: "Usuario"}", Toast.LENGTH_SHORT).show()
                    } else {
                        showAlert("Error", "No se pudo autenticar con Google.")
                    }
                }
            } catch (e: ApiException) {
                showAlert("Error", "Error en Google Sign-In: ${e.message}")
            }
        }
    }

    private fun showAlert(title: String, message: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(title)
        builder.setMessage(message)
        builder.setPositiveButton("Aceptar", null)
        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

    private fun showPrincipalView() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish()
    }
}