package com.example.finance_code.ui.home

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.finance_code.R
import com.example.finance_code.databinding.ActivityHomeBinding
import com.example.finance_code.ui.login.AuthCheckActivity

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var navController: NavController
    companion object {
        var isSessionActive: Boolean = false
        var pendingTargetFragment: String? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
    }

    override fun onResume() {
        super.onResume()
        isSessionActive = true

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