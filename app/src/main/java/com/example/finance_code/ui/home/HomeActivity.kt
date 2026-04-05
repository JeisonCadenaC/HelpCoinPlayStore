package com.example.finance_code.ui.home

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.TypedValue
import android.view.View
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.finance_code.R
import com.example.finance_code.UpdateManager
import com.example.finance_code.DiscreetModeManager
import com.example.finance_code.ShakeDetector
import com.example.finance_code.chatbot.ChatFragment
import com.example.finance_code.databinding.ActivityHomeBinding
import com.example.finance_code.ui.login.AuthCheckActivity
import com.example.finance_code.utils.ThemeUtils

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var navController: NavController

    // Variables para el sensor de movimiento
    private var shakeDetector: ShakeDetector? = null
    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null

    companion object {
        var isSessionActive: Boolean = false
        var pendingTargetFragment: String? = null
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(ThemeUtils.getAuraTheme(this))
        super.onCreate(savedInstanceState)

        DiscreetModeManager.initialize(this)

        val sharedPrefs = getSharedPreferences("AppPrefe", Context.MODE_PRIVATE)

        if (sharedPrefs.getBoolean("secure_screen", false)) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }

        if (savedInstanceState == null && sharedPrefs.getBoolean("hide_balances_startup", false)) {
            if (!DiscreetModeManager.isDiscreetModeActive) {
                DiscreetModeManager.toggleMode()
            }
        }

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        shakeDetector = ShakeDetector {
            val prefs = getSharedPreferences("AppPrefe", Context.MODE_PRIVATE)
            val isShakeDisabled = prefs.getBoolean("disable_shake_gesture", false)

            if (!isShakeDisabled) {
                DiscreetModeManager.toggleMode()
                recreate()
            }
        }

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.statusBarColor = ThemeUtils.getAuraColor(this)
        aplicarColoresGlobalesYAmoled()
        solicitarPermisos()

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_home) as NavHostFragment
        navController = navHostFragment.navController
        binding.navView.setupWithNavController(navController)

        // Acción del Botón Flotante Estilo Meta AI
        binding.fabMetaAI.setOnClickListener {
            // Abrimos el chat de forma superpuesta con una animación de fade in
            supportFragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .add(android.R.id.content, ChatFragment())
                .addToBackStack(null)
                .commit()
        }

        if (!isSessionActive) {
            val target = intent.getStringExtra(ReminderHelper.EXTRA_TARGET_FRAGMENT)
            if (target != null) {
                pendingTargetFragment = target
                redirectToAuth()
                return
            }
        } else {
            handleIncomingNotificationIntent(intent)
        }

        UpdateManager(this).checkForUpdates()
    }

    private fun aplicarColoresGlobalesYAmoled() {
        val auraColor = ThemeUtils.getAuraColor(this)
        val navColorStateList = ThemeUtils.getBottomNavColorStateList(auraColor)

        binding.navView.itemIconTintList = navColorStateList
        binding.navView.itemTextColor = navColorStateList

        // Sincronizar color del botón flotante
        binding.fabMetaAI.backgroundTintList = android.content.res.ColorStateList.valueOf(auraColor)

        val sharedPrefs = getSharedPreferences("AppPrefe", Context.MODE_PRIVATE)
        val isAmoled = sharedPrefs.getBoolean("amoled_mode", false)
        val isDark = sharedPrefs.getBoolean("modo_oscuro", false)

        if (isAmoled && isDark) {
            window.decorView.setBackgroundColor(Color.BLACK)
            binding.root.setBackgroundColor(Color.BLACK)
            binding.navView.setBackgroundColor(Color.BLACK)
        } else {
            val typedValue = TypedValue()
            theme.resolveAttribute(com.google.android.material.R.attr.colorSurface, typedValue, true)
            binding.navView.setBackgroundColor(typedValue.data)
        }

        supportFragmentManager.registerFragmentLifecycleCallbacks(object : FragmentManager.FragmentLifecycleCallbacks() {
            override fun onFragmentViewCreated(fm: FragmentManager, f: Fragment, v: View, savedInstanceState: Bundle?) {
                super.onFragmentViewCreated(fm, f, v, savedInstanceState)
                if (isAmoled && isDark) {
                    v.setBackgroundColor(Color.BLACK)
                } else {
                    val typedValue = TypedValue()
                    theme.resolveAttribute(android.R.attr.colorBackground, typedValue, true)
                    v.setBackgroundColor(typedValue.data)
                }
            }
        }, true)
    }

    private fun solicitarPermisos() {
        val permisosNecesarios = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permisosNecesarios.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            permisosNecesarios.add(Manifest.permission.READ_CALENDAR)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            permisosNecesarios.add(Manifest.permission.WRITE_CALENDAR)
        }

        if (permisosNecesarios.isNotEmpty()) {
            requestPermissionLauncher.launch(permisosNecesarios.toTypedArray())
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                startActivity(intent)
            }
        }
    }

    fun updateShakeSensorRegistration() {
        val sharedPrefs = getSharedPreferences("AppPrefe", Context.MODE_PRIVATE)
        val isShakeEnabled = sharedPrefs.getBoolean("shake_mode_enabled", true)

        if (isShakeEnabled) {
            accelerometer?.let {
                sensorManager?.unregisterListener(shakeDetector)
                sensorManager?.registerListener(shakeDetector, it, SensorManager.SENSOR_DELAY_UI)
            }
        } else {
            sensorManager?.unregisterListener(shakeDetector)
        }
    }

    override fun onResume() {
        super.onResume()
        isSessionActive = true
        aplicarColoresGlobalesYAmoled()
        updateShakeSensorRegistration()

        if (pendingTargetFragment != null) {
            navigateDirectly(pendingTargetFragment!!)
            pendingTargetFragment = null
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(shakeDetector)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (isSessionActive) {
            handleIncomingNotificationIntent(intent)
        } else {
            val target = intent?.getStringExtra(ReminderHelper.EXTRA_TARGET_FRAGMENT)
            if (target != null) {
                pendingTargetFragment = target
                redirectToAuth()
            }
        }
    }

    private fun handleIncomingNotificationIntent(intent: Intent?) {
        val targetFragment = intent?.getStringExtra(ReminderHelper.EXTRA_TARGET_FRAGMENT)
        if (targetFragment != null) {
            navigateDirectly(targetFragment)
        }
    }

    private fun navigateDirectly(targetFragment: String) {
        val destinationId = when (targetFragment) {
            ReminderHelper.TARGET_CALENDARIO -> R.id.calendarioFragment
            ReminderHelper.TARGET_METAS -> R.id.metasFragment
            else -> null
        }
        if (destinationId != null) {
            navController.popBackStack(R.id.inicioFragment, false)
            navController.navigate(destinationId)
        }
    }

    private fun redirectToAuth() {
        val intent = Intent(this, AuthCheckActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            isSessionActive = false
        }
    }
}