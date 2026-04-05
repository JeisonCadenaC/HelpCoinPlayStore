package com.example.finance_code.chatbot

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.finance_code.data.AppDB
import com.example.finance_code.data.MovimientoDao
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    // 1. Obtenemos el email del usuario logueado en Firebase para abrir su DB
    private val userEmail = FirebaseAuth.getInstance().currentUser?.email ?: ""
    private val movimientoDao: MovimientoDao = AppDB.getDatabase(application, userEmail).movimientoDao()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(listOf(
        ChatMessage("¡Hola! Soy tu asistente de HelpCoin. ¿En qué te puedo ayudar con tus finanzas hoy?", false)
    ))
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages

    fun sendMessage(userText: String) {
        if (userText.isBlank()) return

        val currentList = _chatMessages.value.toMutableList()
        currentList.add(ChatMessage(userText, true))
        _chatMessages.value = currentList

        viewModelScope.launch {
            val response = procesarConsultaLocal(userText)
            val updatedList = _chatMessages.value.toMutableList()
            updatedList.add(ChatMessage(response, false))
            _chatMessages.value = updatedList
        }
    }

    private suspend fun procesarConsultaLocal(query: String): String {
        val text = query.lowercase()

        // 2. Extraemos todos los movimientos usando la función síncrona que ya existe en tu DAO
        val todosLosMovimientos = try {
            movimientoDao.obtenerTodosSync()
        } catch (e: Exception) {
            emptyList()
        }

        // 3. Calculamos los totales localmente (tipo 1 es Ingreso, lo demás es Gasto)
        val totalIngresos = todosLosMovimientos.filter { it.tipo == 1 }.sumOf { it.cantidad }
        val totalGastos = todosLosMovimientos.filter { it.tipo != 1 }.sumOf { it.cantidad }
        val saldoActual = totalIngresos - totalGastos

        return when {
            text.contains("gasté") || text.contains("gastos") -> {
                "Según tus registros locales en HelpCoin, has gastado un total de ${formatCurrency(totalGastos)}."
            }
            text.contains("saldo") || text.contains("tengo") -> {
                "Tu saldo actual es de ${formatCurrency(saldoActual)}."
            }
            text.contains("ayuda") || text.contains("puedes hacer") -> {
                "Por ahora puedo analizar tus totales de gastos y consultar tu saldo disponible. Todo esto funciona de manera local y privada sin salir de tu teléfono."
            }
            else -> {
                "Entiendo. Aún estoy aprendiendo a analizar esos datos específicos, pero puedes preguntarme por tus gastos totales o tu saldo actual."
            }
        }
    }

    private fun formatCurrency(amount: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
        return format.format(amount)
    }
}