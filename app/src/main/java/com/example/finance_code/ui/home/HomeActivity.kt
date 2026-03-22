package com.example.finance_code.ui.home

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
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
import com.example.finance_code.databinding.ActivityHomeBinding
import com.example.finance_code.ui.login.AuthCheckActivity
import com.example.finance_code.utils.ThemeUtils
import android.util.TypedValue

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var navController: NavController

    companion object {
        var isSessionActive: Boolean = false
        var pendingTargetFragment: String? = null
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. APLICA EL TEMA GLOBAL ANTES DE CREAR LA VISTA (Tiñe progreso, bordes, botones, TODO)
        setTheme(ThemeUtils.getAuraTheme(this))

        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 2. Teñir la barra de notificaciones superior
        window.statusBarColor = ThemeUtils.getAuraColor(this)

        aplicarColoresGlobalesYAmoled()

        solicitarPermisos()

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_home) as NavHostFragment
        navController = navHostFragment.navController
        binding.navView.setupWithNavController(navController)

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

        val sharedPrefs = getSharedPreferences("AppPrefe", Context.MODE_PRIVATE)
        val isAmoled = sharedPrefs.getBoolean("amoled_mode", false)
        val isDark = sharedPrefs.getBoolean("modo_oscuro", false)

        if (isAmoled && isDark) {
            window.decorView.setBackgroundColor(Color.BLACK)
            binding.root.setBackgroundColor(Color.BLACK)
        }

        // MAGIA: Esto intercepta TODOS los fragmentos y les pone el AMOLED negro puro automáticamente
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

    override fun onResume() {
        super.onResume()
        isSessionActive = true
        aplicarColoresGlobalesYAmoled()

        if (pendingTargetFragment != null) {
            navigateDirectly(pendingTargetFragment!!)
            pendingTargetFragment = null
        }
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