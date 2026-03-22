package com.example.finance_code.ui.settings

import android.content.Context
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.SwitchCompat
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.example.finance_code.R
import com.example.finance_code.utils.ThemeUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class CustomizationFragment : Fragment() {

    private lateinit var sharedPrefs: SharedPreferences
    private var selectedColorHex: String = "#8000FF"
    private var isAmoledSelected: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_customization, container, false)
        sharedPrefs = requireActivity().getSharedPreferences("AppPrefe", Context.MODE_PRIVATE)

        val auraPrefs = requireContext().getSharedPreferences("HelpCoinAuraPrefs", Context.MODE_PRIVATE)
        selectedColorHex = auraPrefs.getString("aura_color_hex", "#8000FF") ?: "#8000FF"

        val btnBackCustom = root.findViewById<ImageView>(R.id.btnBackCustom)
        val switchAmoled = root.findViewById<SwitchCompat>(R.id.switchAmoled)
        val btnSaveCustomization = root.findViewById<MaterialButton>(R.id.btnSaveCustomization)

        val cardColorContainer = root.findViewById<MaterialCardView>(R.id.cardColorContainer)
        val cardAmoledContainer = root.findViewById<MaterialCardView>(R.id.cardAmoledContainer)

        val colorPurple = root.findViewById<CardView>(R.id.colorPurple)
        val colorIndigo = root.findViewById<CardView>(R.id.colorIndigo)
        val colorBlue = root.findViewById<CardView>(R.id.colorBlue)
        val colorCyan = root.findViewById<CardView>(R.id.colorCyan)
        val colorTeal = root.findViewById<CardView>(R.id.colorTeal)
        val colorGreen = root.findViewById<CardView>(R.id.colorGreen)
        val colorAmber = root.findViewById<CardView>(R.id.colorAmber)
        val colorOrange = root.findViewById<CardView>(R.id.colorOrange)
        val colorRed = root.findViewById<CardView>(R.id.colorRed)
        val colorPink = root.findViewById<CardView>(R.id.colorPink)

        val auraColor = ThemeUtils.getAuraColor(requireContext())
        btnSaveCustomization.backgroundTintList = ColorStateList.valueOf(auraColor)

        aplicarColorSwitchYTarjetas(switchAmoled, cardColorContainer, cardAmoledContainer, auraColor)

        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                requireActivity().supportFragmentManager.beginTransaction().remove(this@CustomizationFragment).commit()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)

        btnBackCustom.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction().remove(this@CustomizationFragment).commit()
        }

        isAmoledSelected = sharedPrefs.getBoolean("amoled_mode", false)
        switchAmoled.isChecked = isAmoledSelected

        switchAmoled.setOnCheckedChangeListener { _, isChecked ->
            isAmoledSelected = isChecked
        }

        val colorClickListener = View.OnClickListener { view ->
            selectedColorHex = when (view.id) {
                R.id.colorPurple -> "#8000FF"
                R.id.colorIndigo -> "#4000FF"
                R.id.colorBlue -> "#0000FF"
                R.id.colorCyan -> "#00FFFF"
                R.id.colorTeal -> "#00FF80"
                R.id.colorGreen -> "#00FF00"
                R.id.colorAmber -> "#FBC02D"
                R.id.colorOrange -> "#FF8000"
                R.id.colorRed -> "#FF0000"
                R.id.colorPink -> "#FF00FF"
                else -> "#8000FF"
            }

            val colorInt = Color.parseColor(selectedColorHex)
            btnSaveCustomization.backgroundTintList = ColorStateList.valueOf(colorInt)
            aplicarColorSwitchYTarjetas(switchAmoled, cardColorContainer, cardAmoledContainer, colorInt)
        }

        colorPurple.setOnClickListener(colorClickListener)
        colorIndigo.setOnClickListener(colorClickListener)
        colorBlue.setOnClickListener(colorClickListener)
        colorCyan.setOnClickListener(colorClickListener)
        colorTeal.setOnClickListener(colorClickListener)
        colorGreen.setOnClickListener(colorClickListener)
        colorAmber.setOnClickListener(colorClickListener)
        colorOrange.setOnClickListener(colorClickListener)
        colorRed.setOnClickListener(colorClickListener)
        colorPink.setOnClickListener(colorClickListener)

        btnSaveCustomization.setOnClickListener {
            ThemeUtils.saveAuraColor(requireContext(), selectedColorHex)
            sharedPrefs.edit().putBoolean("amoled_mode", isAmoledSelected).apply()
            Toast.makeText(requireContext(), "¡Personalización aplicada exitosamente!", Toast.LENGTH_SHORT).show()
            requireActivity().recreate()
        }

        return root
    }

    private fun aplicarColorSwitchYTarjetas(switch: SwitchCompat, card1: View, card2: View, color: Int) {
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

        actualizarBorde(card1, color)
        actualizarBorde(card2, color)
    }

    private fun actualizarBorde(view: View, color: Int) {
        if (view is MaterialCardView) {
            view.strokeColor = color
        }

        val bg = view.background?.mutate()
        val density = resources.displayMetrics.density
        val strokeWidth = (2 * density).toInt()

        if (bg is LayerDrawable) {
            val lastLayerIndex = bg.numberOfLayers - 1
            if (lastLayerIndex >= 0) {
                val strokeItem = bg.getDrawable(lastLayerIndex) as? GradientDrawable
                strokeItem?.setStroke(strokeWidth, color)
            }
        } else if (bg is GradientDrawable) {
            bg.setStroke(strokeWidth, color)
        }
    }

    override fun onResume() {
        super.onResume()
        val bottomNav = requireActivity().findViewById<View>(R.id.nav_view)
        bottomNav?.visibility = View.GONE

        val isAmoled = sharedPrefs.getBoolean("amoled_mode", false)
        val isDark = sharedPrefs.getBoolean("modo_oscuro", false)
        if (isAmoled && isDark) {
            requireView().setBackgroundColor(Color.BLACK)
        }
    }

    override fun onStop() {
        super.onStop()
        val bottomNav = requireActivity().findViewById<View>(R.id.nav_view)
        bottomNav?.visibility = View.VISIBLE
    }
}