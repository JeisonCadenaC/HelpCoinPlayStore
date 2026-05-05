package com.help.finance_code.ui.settings

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import com.help.finance_code.R
import com.help.finance_code.ui.home.HomeActivity
import com.help.finance_code.utils.ThemeUtils
import com.google.android.material.card.MaterialCardView

class PrivacyFragment : Fragment() {

    private lateinit var sharedPrefs: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_privacy, container, false)
        sharedPrefs = requireActivity().getSharedPreferences("AppPrefe", Context.MODE_PRIVATE)

        val btnBackPrivacy = root.findViewById<ImageView>(R.id.btnBackPrivacy)

        val switchBiometric = root.findViewById<SwitchCompat>(R.id.switchBiometric)
        val switchSecureScreen = root.findViewById<SwitchCompat>(R.id.switchSecureScreen)
        val switchHideBalances = root.findViewById<SwitchCompat>(R.id.switchHideBalances)
        val switchShake = root.findViewById<SwitchCompat>(R.id.switchShake)

        val cardBiometric = root.findViewById<MaterialCardView>(R.id.cardBiometric)
        val cardSecureScreen = root.findViewById<MaterialCardView>(R.id.cardSecureScreen)
        val cardHideBalances = root.findViewById<MaterialCardView>(R.id.cardHideBalances)
        val cardShake = root.findViewById<MaterialCardView>(R.id.cardShake)

        val iconBiometric = root.findViewById<ImageView>(R.id.iconBiometric)
        val iconSecureScreen = root.findViewById<ImageView>(R.id.iconSecureScreen)
        val iconHideBalances = root.findViewById<ImageView>(R.id.iconHideBalances)
        val iconShake = root.findViewById<ImageView>(R.id.iconShake)

        val auraColor = ThemeUtils.getAuraColor(requireContext())
        val auraColorStateList = ColorStateList.valueOf(auraColor)

        iconBiometric?.imageTintList = auraColorStateList
        iconSecureScreen?.imageTintList = auraColorStateList
        iconHideBalances?.imageTintList = auraColorStateList
        iconShake?.imageTintList = auraColorStateList

        aplicarColorSwitchYTarjetas(switchBiometric, cardBiometric, auraColor)
        aplicarColorSwitchYTarjetas(switchSecureScreen, cardSecureScreen, auraColor)
        aplicarColorSwitchYTarjetas(switchHideBalances, cardHideBalances, auraColor)
        aplicarColorSwitchYTarjetas(switchShake, cardShake, auraColor)

        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                requireActivity().supportFragmentManager.beginTransaction().remove(this@PrivacyFragment).commit()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)

        btnBackPrivacy.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction().remove(this@PrivacyFragment).commit()
        }

        val isBiometricEnabled = sharedPrefs.getBoolean("biometric_auth", true)
        switchBiometric.isChecked = isBiometricEnabled
        iconBiometric?.setImageResource(if (isBiometricEnabled) R.drawable.ic_fingerprint else R.drawable.ic_fingerprint_off)

        switchSecureScreen.isChecked = sharedPrefs.getBoolean("secure_screen", false)
        switchHideBalances.isChecked = sharedPrefs.getBoolean("hide_balances_startup", false)

        // CORRECCIÓN: Como el switch dice "Desactivar", su estado es inverso al "enabled"
        // Si shake_mode_enabled es false, el switch debe verse ENCENDIDO.
        switchShake.isChecked = !sharedPrefs.getBoolean("shake_mode_enabled", true)

        switchBiometric.setOnCheckedChangeListener { _, isChecked ->
            if (!isChecked) {
                mostrarAdvertenciaBiometrica(switchBiometric, iconBiometric)
            } else {
                sharedPrefs.edit().putBoolean("biometric_auth", true).apply()
                iconBiometric?.setImageResource(R.drawable.ic_fingerprint)
            }
        }

        switchSecureScreen.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit().putBoolean("secure_screen", isChecked).apply()
            if (isChecked) {
                requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            } else {
                requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }

        switchHideBalances.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit().putBoolean("hide_balances_startup", isChecked).apply()
        }

        switchShake.setOnCheckedChangeListener { _, isChecked ->
            // CORRECCIÓN: Si el switch de "Desactivar" se enciende (true), guardamos "shake_mode_enabled" como false
            sharedPrefs.edit().putBoolean("shake_mode_enabled", !isChecked).apply()
            // AQUI APAGAMOS O PRENDEMOS EL SENSOR DIRECTAMENTE EN EL ACTIVITY AL INSTANTE
            (requireActivity() as? HomeActivity)?.updateShakeSensorRegistration()
        }

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.postDelayed({
            if (isAdded && context != null) {
                TutorialPrivacidad(requireActivity(), view).start()
            }
        }, 500)
    }

    private fun mostrarAdvertenciaBiometrica(switch: SwitchCompat, icon: ImageView?) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_warning_biometric, null)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val btnCancelar = dialogView.findViewById<Button>(R.id.btnCancelarWarning)
        val btnConfirmar = dialogView.findViewById<Button>(R.id.btnConfirmarWarning)

        btnCancelar.setOnClickListener {
            switch.isChecked = true
            dialog.dismiss()
        }

        btnConfirmar.setOnClickListener {
            sharedPrefs.edit().putBoolean("biometric_auth", false).apply()
            icon?.setImageResource(R.drawable.ic_fingerprint_off)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun aplicarColorSwitchYTarjetas(switch: SwitchCompat, card: MaterialCardView?, color: Int) {
        if (card == null) return

        switch.thumbTintList = ColorStateList.valueOf(color)
        val trackStates = arrayOf(
            intArrayOf(android.R.attr.state_checked),
            intArrayOf(-android.R.attr.state_checked)
        )
        val trackColors = intArrayOf(
            Color.argb(128, Color.red(color), Color.green(color), Color.blue(color)),
            ThemeUtils.getSwitchNeutralColor()
        )
        switch.trackTintList = ColorStateList(trackStates, trackColors)

        val density = resources.displayMetrics.density
        val strokeWidth = (1 * density).toInt()

        card.strokeColor = color
        card.strokeWidth = strokeWidth
    }

    override fun onResume() {
        super.onResume()
        requireActivity().findViewById<View>(R.id.nav_view)?.visibility = View.GONE
        val isAmoled = sharedPrefs.getBoolean("amoled_mode", false)
        val isDark = sharedPrefs.getBoolean("modo_oscuro", false)
        if (isAmoled && isDark) {
            requireView().setBackgroundColor(Color.BLACK)
        }
    }

    override fun onStop() {
        super.onStop()
        requireActivity().findViewById<View>(R.id.nav_view)?.visibility = View.VISIBLE
    }
}