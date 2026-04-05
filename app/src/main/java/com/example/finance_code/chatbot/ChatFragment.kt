package com.example.finance_code.chatbot

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.finance_code.R
import com.example.finance_code.utils.ThemeUtils
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ChatFragment : Fragment() {

    private lateinit var viewModel: ChatViewModel
    private lateinit var rvChat: RecyclerView
    private lateinit var etInput: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var btnBack: ImageView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_chat, container, false)

        viewModel = ViewModelProvider(this)[ChatViewModel::class.java]

        rvChat = view.findViewById(R.id.rvChat)
        etInput = view.findViewById(R.id.etChatInput)
        btnSend = view.findViewById(R.id.btnSendChat)
        btnBack = view.findViewById(R.id.btnVolverAtrasChat)

        // Aplicar el color del Aura a los botones
        val auraColor = ThemeUtils.getAuraColor(requireContext())
        btnSend.backgroundTintList = ColorStateList.valueOf(auraColor)
        btnBack.imageTintList = ColorStateList.valueOf(auraColor)

        // Configurar RecyclerView para que apile desde abajo como un chat real
        val layoutManager = LinearLayoutManager(requireContext())
        layoutManager.stackFromEnd = true
        rvChat.layoutManager = layoutManager

        btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        btnSend.setOnClickListener {
            val text = etInput.text.toString().trim()
            if (text.isNotEmpty()) {
                viewModel.sendMessage(text)
                etInput.text.clear()
            }
        }

        // Observar mensajes
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.chatMessages.collectLatest { messages ->
                val adapter = ChatAdapter(messages)
                rvChat.adapter = adapter
                if (messages.isNotEmpty()) {
                    rvChat.smoothScrollToPosition(messages.size - 1)
                }
            }
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        // Ocultar menú inferior para dar inmersión al chat
        val bottomNav = requireActivity().findViewById<View>(R.id.nav_view)
        bottomNav?.visibility = View.GONE
    }

    override fun onStop() {
        super.onStop()
        // Restaurar menú inferior al salir
        val bottomNav = requireActivity().findViewById<View>(R.id.nav_view)
        bottomNav?.visibility = View.VISIBLE
    }
}