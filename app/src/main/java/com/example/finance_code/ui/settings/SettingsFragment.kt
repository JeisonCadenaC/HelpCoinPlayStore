package com.example.finance_code.ui.settings

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import com.example.finance_code.R
import com.example.finance_code.data.AppDB
import com.example.finance_code.ui.login.LoginActivity
import com.example.finance_code.utils.ThemeUtils
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth

class SettingsFragment : Fragment() {

    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_settings, container, false)
        auth = FirebaseAuth.getInstance()

        val switchModoOscuro = root.findViewById<SwitchCompat>(R.id.switchModoOscuro)
        val cardMenuTheme = root.findViewById<MaterialCardView>(R.id.cardMenuTheme)
        val cardMenuPrivacy = root.findViewById<MaterialCardView>(R.id.cardMenuPrivacy)
        val cardMenuCustomization = root.findViewById<MaterialCardView>(R.id.cardMenuCustomization)
        val cardMenuBackup = root.findViewById<MaterialCardView>(R.id.cardMenuBackup)
        val btnLogout = root.findViewById<MaterialButton>(R.id.btnLogout)

        val iconPrivacy = root.findViewById<ImageView>(R.id.iconPrivacy)
        val iconTheme = root.findViewById<ImageView>(R.id.iconTheme)
        val iconCustomization = root.findViewById<ImageView>(R.id.iconCustomization)
        val iconBackup = root.findViewById<ImageView>(R.id.iconBackup)

        val sharedPrefs = requireActivity().getSharedPreferences("AppPrefe", Context.MODE_PRIVATE)
        val isDarkModeOn = sharedPrefs.getBoolean("modo_oscuro", false)

        val auraColor = ThemeUtils.getAuraColor(requireContext())
        val auraColorStateList = ColorStateList.valueOf(auraColor)

        iconPrivacy.imageTintList = auraColorStateList
        iconTheme.imageTintList = auraColorStateList
        iconCustomization.imageTintList = auraColorStateList
        iconBackup.imageTintList = auraColorStateList
        switchModoOscuro.thumbTintList = auraColorStateList
        switchModoOscuro.trackTintList = ThemeUtils.getBottomNavColorStateList(auraColor)

        switchModoOscuro.isChecked = isDarkModeOn
        iconTheme.setImageResource(if (isDarkModeOn) R.drawable.ic_moon else R.drawable.ic_sun)

        switchModoOscuro.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit().putBoolean("modo_oscuro", isChecked).apply()
            iconTheme.setImageResource(if (isChecked) R.drawable.ic_moon else R.drawable.ic_sun)

            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }

            val isAmoled = sharedPrefs.getBoolean("amoled_mode", false)
            if (isAmoled && isChecked) {
                requireActivity().window.decorView.setBackgroundColor(Color.BLACK)
            } else {
                requireActivity().recreate()
            }
        }

        cardMenuTheme.setOnClickListener {
            switchModoOscuro.isChecked = !switchModoOscuro.isChecked
        }

        cardMenuPrivacy.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .add(android.R.id.content, PrivacyFragment())
                .commit()
        }

        cardMenuCustomization.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .add(android.R.id.content, CustomizationFragment())
                .commit()
        }

        cardMenuBackup.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .add(android.R.id.content, BackupFragment())
                .commit()
        }

        btnLogout.setOnClickListener {
            cerrarSesion()
        }

        return root
    }

    override fun onResume() {
        super.onResume()
        val sharedPrefs = requireActivity().getSharedPreferences("AppPrefe", Context.MODE_PRIVATE)
        val isAmoled = sharedPrefs.getBoolean("amoled_mode", false)
        val isDark = sharedPrefs.getBoolean("modo_oscuro", false)
        if (isAmoled && isDark) {
            requireActivity().window.decorView.setBackgroundColor(Color.BLACK)
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
        // Eliminado: Runtime.getRuntime().exit(0)
    }
}