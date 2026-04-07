package com.help.finance_code.data

data class AporteDB(
    var id: String = "",
    val metaId: String = "",
    val userId: String = "",
    val monto: Double = 0.0,
    val tipo: String = "",
    val timestamp: Long = System.currentTimeMillis()
)