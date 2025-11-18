package com.example.finance_code.ui.login

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.InputType
import android.text.method.LinkMovementMethod
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.finance_code.R
import com.example.finance_code.databinding.ActivityLoginBinding
import com.example.finance_code.ui.home.HomeActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.GoogleAuthProvider

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var loginButton: Button
    private lateinit var registerButton: Button
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var googleButton: ImageButton
    private lateinit var termsAndConditionsCheckbox: CheckBox
    private lateinit var rememberEmailCheckbox: CheckBox
    private var toggleDarkModeButton: ImageButton? = null

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var themePreferences: SharedPreferences
    private lateinit var binding: ActivityLoginBinding

    companion object {
        private const val PREFS_NAME = "LoginPrefs"
        private const val PREF_KEY_EMAIL = "saved_email"
        private const val PREF_KEY_REMEMBER = "remember_email"
        private const val KEY_USER_NAME = "user_name"
    }

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val cuenta = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(cuenta)
            } catch (e: ApiException) {
                showAlert("Error", "Error en Google Sign-In: ${e.message}")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)

        themePreferences = getSharedPreferences("AppPrefe", Context.MODE_PRIVATE)
        val modoOscuroActivado = themePreferences.getBoolean("modo_oscuro", false)
        aplicarModoOscuro(modoOscuroActivado)

        enableEdgeToEdge()

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setup()
    }

    private fun setup() {
        title = "Autenticación del usuario"
        auth = FirebaseAuth.getInstance()

        loginButton = binding.btnsingUpButton
        registerButton = binding.btnregisterButton
        emailEditText = binding.emailEditText
        passwordEditText = binding.passwordEditText
        googleButton = binding.googleButton
        termsAndConditionsCheckbox = binding.termsAndConditionsCheckbox
        rememberEmailCheckbox = binding.rememberEmailCheckbox

        toggleDarkModeButton = binding.themeToggleButton

        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        termsAndConditionsCheckbox.movementMethod = LinkMovementMethod.getInstance()

        loginButton.setOnClickListener { login() }
        registerButton.setOnClickListener { register() }

        binding.tvForgotPassword.setOnClickListener {
            showRecoverPasswordDialog()
        }

        googleButton.setOnClickListener {
            if (termsAndConditionsCheckbox.isChecked) {
                val googleConf = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                    .requestEmail()
                    .requestScopes(Scope(DriveScopes.DRIVE_APPDATA))
                    .build()

                val googleClient = GoogleSignIn.getClient(this, googleConf)
                googleSignInLauncher.launch(googleClient.signInIntent)
            } else {
                showAlert("Términos y Condiciones", getString(R.string.error_accept_terms))
            }
        }

        loadPreferences()
        setupDarkModeToggle()
    }

    private fun setupDarkModeToggle() {
        toggleDarkModeButton?.let { button ->
            val modoOscuroActivado = themePreferences.getBoolean("modo_oscuro", false)
            actualizarIconoModoOscuro(modoOscuroActivado)

            button.setOnClickListener {
                val nuevoEstado = !themePreferences.getBoolean("modo_oscuro", false)
                themePreferences.edit {
                    putBoolean("modo_oscuro", nuevoEstado)
                }
                aplicarModoOscuro(nuevoEstado)
                actualizarIconoModoOscuro(nuevoEstado)
            }
        }
    }

    private fun actualizarIconoModoOscuro(isDark: Boolean) {
        toggleDarkModeButton?.setImageResource(if (isDark) R.drawable.ic_sun else R.drawable.ic_moon)
    }

    private fun aplicarModoOscuro(activado: Boolean) {
        AppCompatDelegate.setDefaultNightMode(
            if (activado) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    private fun loadPreferences() {
        val shouldRemember = sharedPreferences.getBoolean(PREF_KEY_REMEMBER, false)
        rememberEmailCheckbox.isChecked = shouldRemember
        if (shouldRemember) {
            emailEditText.setText(sharedPreferences.getString(PREF_KEY_EMAIL, ""))
        }
    }

    private fun login() {
        if (!termsAndConditionsCheckbox.isChecked) {
            showAlert("Términos y Condiciones", getString(R.string.error_accept_terms))
            return
        }

        val email = emailEditText.text.toString()
        val password = passwordEditText.text.toString()

        sharedPreferences.edit {
            if (rememberEmailCheckbox.isChecked) {
                putString(PREF_KEY_EMAIL, email)
                putBoolean(PREF_KEY_REMEMBER, true)
            } else {
                remove(PREF_KEY_EMAIL)
                putBoolean(PREF_KEY_REMEMBER, false)
            }
        }

        if (email.isNotEmpty() && password.isNotEmpty()) {
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        showPrincipalView()
                    } else {
                        showAlert(
                            "Error de autenticación",
                            "No se pudo iniciar sesión. Verifique sus credenciales."
                        )
                    }
                }
        } else {
            showAlert("Campos obligatorios", "Por favor, ingrese su correo y contraseña.")
        }
    }

    private fun register() {
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
                    val errorMessage = task.exception?.message
                        ?: "Error desconocido al registrar el usuario."
                    showAlert("Error en el registro", errorMessage)
                }
            }
        } else {
            showAlert("Campos obligatorios", "Por favor, complete todos los campos para registrarse.")
        }
    }

    private fun showRecoverPasswordDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Recuperar Contraseña")
        builder.setMessage("Ingresa tu correo para enviarte un enlace de restablecimiento.")

        val input = EditText(this)
        input.hint = "Correo electrónico"
        input.inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        input.setText(binding.emailEditText.text.toString())

        val container = FrameLayout(this)
        val params = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(50, 20, 50, 20)
        input.layoutParams = params
        container.addView(input)
        builder.setView(container)

        builder.setPositiveButton("Enviar") { _, _ ->
            val email = input.text.toString().trim()
            if (email.isNotEmpty()) {
                enviarCorreoRecuperacion(email)
            } else {
                Toast.makeText(this, "Por favor ingresa un correo", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }

    private fun enviarCorreoRecuperacion(email: String) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    showAlert("Correo enviado", "Revisa tu bandeja de entrada para restablecer tu contraseña.")
                } else {
                    showAlert("Error", "No se pudo enviar el correo: ${task.exception?.message}")
                }
            }
    }

    private fun firebaseAuthWithGoogle(cuenta: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(cuenta.idToken, null)
        auth.signInWithCredential(credential).addOnCompleteListener(this) { firebaseTask ->
            if (firebaseTask.isSuccessful) {

                val user = auth.currentUser
                val userUID = user?.uid
                val googlePhotoUrl = cuenta.photoUrl
                val googleDisplayName = cuenta.displayName

                if (userUID != null) {
                    val prefsName = "${userUID}_UserProfilePrefs"
                    val userPrefs = getSharedPreferences(prefsName, Context.MODE_PRIVATE)

                    userPrefs.edit {
                        if (googlePhotoUrl != null) {
                            putString("google_photo_url", googlePhotoUrl.toString())
                        }
                        if (googleDisplayName != null) {
                            putString(KEY_USER_NAME, googleDisplayName)
                        }
                    }
                }

                showPrincipalView()
                val usuario = auth.currentUser
                Toast.makeText(
                    this,
                    "Bienvenido ${usuario?.displayName ?: "Usuario"}",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                val exception = firebaseTask.exception
                if (exception is FirebaseAuthUserCollisionException) {
                    showAlert(
                        "Error de Inicio",
                        "Ya existe una cuenta con este correo electrónico. Por favor, inicie sesión con su método original."
                    )
                } else {
                    showAlert("Error", "No se pudo autenticar con Google: ${exception?.message}")
                }
            }
        }
    }

    private fun showAlert(title: String, message: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(title)
        builder.setMessage(message)
        builder.setPositiveButton("Aceptar", null)
        builder.create().show()
    }

    private fun showPrincipalView() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }
}