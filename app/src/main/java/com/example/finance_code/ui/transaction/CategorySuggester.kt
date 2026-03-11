package com.example.finance_code.ui.transaction

import com.example.finance_code.data.Categoria

object CategorySuggester {
    // Diccionario Masivo y Optimizado (Colombia)
    private val keywords = mapOf(
        // ==========================================
        // 💰 SALARIO E INGRESOS (NUEVO)
        // ==========================================
        "salario" to "Salario e Ingresos", "sueldo" to "Salario e Ingresos", "quincena" to "Salario e Ingresos",
        "pago" to "Salario e Ingresos", "honorarios" to "Salario e Ingresos", "nomina" to "Salario e Ingresos",
        "prima" to "Salario e Ingresos", "liquidacion" to "Salario e Ingresos", "cesantias" to "Salario e Ingresos",
        "trabajo" to "Salario e Ingresos", "venta" to "Salario e Ingresos", "negocio" to "Salario e Ingresos",
        "ganancia" to "Salario e Ingresos", "utilidad" to "Salario e Ingresos", "bono" to "Salario e Ingresos",
        "ingreso" to "Salario e Ingresos", "mesada" to "Salario e Ingresos", "prestacion" to "Salario e Ingresos",

        // ==========================================
        // 🏍️ MOTOS (NUEVO)
        // ==========================================
        "moto" to "Motos", "motocicleta" to "Motos", "fz" to "Motos", "gixxer" to "Motos",
        "raider" to "Motos", "apache" to "Motos", "ns" to "Motos", "ns 200" to "Motos",
        "ns 160" to "Motos", "pulsar" to "Motos", "dominar" to "Motos", "boxer" to "Motos",
        "nkd" to "Motos", "akt" to "Motos", "yamaha" to "Motos", "suzuki" to "Motos",
        "honda" to "Motos", "kawasaki" to "Motos", "tvs" to "Motos", "bajaj" to "Motos",
        "hero" to "Motos", "kymco" to "Motos", "royal enfield" to "Motos", "duke" to "Motos",
        "mt09" to "Motos", "mt07" to "Motos", "mt03" to "Motos", "mt 09" to "Motos", "mt 03" to "Motos",
        "r15" to "Motos", "dr650" to "Motos", "xr150" to "Motos", "xtz" to "Motos", "cb190" to "Motos",
        "cbr" to "Motos", "gn 125" to "Motos", "gs 125" to "Motos", "libero" to "Motos",
        "agility" to "Motos", "bws" to "Motos", "vstrom" to "Motos", "casco" to "Motos",
        "impermeable" to "Motos", "rodilleras" to "Motos", "guantes moto" to "Motos", "mt" to "Motos",

        // ==========================================
        // 🚙 CARROS (NUEVO)
        // ==========================================
        "carro" to "Carros", "coche" to "Carros", "automovil" to "Carros", "chevrolet" to "Carros",
        "renault" to "Carros", "mazda" to "Carros", "kia" to "Carros", "toyota" to "Carros",
        "nissan" to "Carros", "ford" to "Carros", "volkswagen" to "Carros", "hyundai" to "Carros",
        "spark" to "Carros", "logan" to "Carros", "sandero" to "Carros", "picanto" to "Carros",
        "march" to "Carros", "twingo" to "Carros", "tracker" to "Carros", "duster" to "Carros",
        "cx5" to "Carros", "prado" to "Carros", "fortuner" to "Carros", "hilux" to "Carros",
        "sail" to "Carros", "aveo" to "Carros", "beat" to "Carros", "cruze" to "Carros",
        "corolla" to "Carros", "stepway" to "Carros", "clio" to "Carros", "fiesta" to "Carros",
        "ecosport" to "Carros", "escape" to "Carros", "jetta" to "Carros", "gol" to "Carros",
        "sportage" to "Carros", "tucson" to "Carros", "jeep" to "Carros", "bmw" to "Carros",
        "audi" to "Carros", "mercedes" to "Carros",

        // ==========================================
        // 🛠️ MANTENIMIENTO Y REPARACIÓN (NUEVO)
        // ==========================================
        "reparacion" to "Mantenimiento y Reparación", "arreglo" to "Mantenimiento y Reparación",
        "repuestos" to "Mantenimiento y Reparación", "mecanico" to "Mantenimiento y Reparación",
        "taller" to "Mantenimiento y Reparación", "aceite" to "Mantenimiento y Reparación",
        "pastillas de freno" to "Mantenimiento y Reparación", "frenos" to "Mantenimiento y Reparación",
        "llantas" to "Mantenimiento y Reparación", "bateria" to "Mantenimiento y Reparación",
        "pintura" to "Mantenimiento y Reparación", "latoneria" to "Mantenimiento y Reparación",
        "despinche" to "Mantenimiento y Reparación", "sincronizacion" to "Mantenimiento y Reparación",
        "alineacion" to "Mantenimiento y Reparación", "balanceo" to "Mantenimiento y Reparación",
        "revision" to "Mantenimiento y Reparación", "tecnomecanica" to "Mantenimiento y Reparación",
        "mantenimiento" to "Mantenimiento y Reparación", "lavadero" to "Mantenimiento y Reparación",

        // ==========================================
        // 🏦 BANCOS Y FINANZAS (NUEVO)
        // ==========================================
        "nequi" to "Bancos y Finanzas", "daviplata" to "Bancos y Finanzas", "bancolombia" to "Bancos y Finanzas",
        "davivienda" to "Bancos y Finanzas", "aval" to "Bancos y Finanzas", "bogota" to "Bancos y Finanzas",
        "occidente" to "Bancos y Finanzas", "popular" to "Bancos y Finanzas", "caja social" to "Bancos y Finanzas",
        "bbva" to "Bancos y Finanzas", "colpatria" to "Bancos y Finanzas", "nubank" to "Bancos y Finanzas",
        "lulo" to "Bancos y Finanzas", "dale" to "Bancos y Finanzas", "rappipay" to "Bancos y Finanzas",
        "tarjeta de credito" to "Bancos y Finanzas", "cuota" to "Bancos y Finanzas", "interes" to "Bancos y Finanzas",
        "comision" to "Bancos y Finanzas", "prestamo" to "Bancos y Finanzas", "abono" to "Bancos y Finanzas",
        "deuda" to "Bancos y Finanzas", "banco" to "Bancos y Finanzas", "credito" to "Bancos y Finanzas",
        "ahorro" to "Bancos y Finanzas", "inversion" to "Bancos y Finanzas", "cdt" to "Bancos y Finanzas",

        // ==========================================
        // 🍔 COMIDA Y RESTAURANTES
        // ==========================================
        "restaurante" to "Comida y Restaurantes", "almuerzo" to "Comida y Restaurantes",
        "cena" to "Comida y Restaurantes", "desayuno" to "Comida y Restaurantes",
        "pizza" to "Comida y Restaurantes", "hamburguesa" to "Comida y Restaurantes",
        "empanada" to "Comida y Restaurantes", "empanadas" to "Comida y Restaurantes",
        "corrientazo" to "Comida y Restaurantes", "asado" to "Comida y Restaurantes",
        "pollo" to "Comida y Restaurantes", "domicilio" to "Comida y Restaurantes",
        "rappi" to "Comida y Restaurantes", "tostao" to "Comida y Restaurantes",
        "crepes" to "Comida y Restaurantes", "kfc" to "Comida y Restaurantes",
        "mcdonalds" to "Comida y Restaurantes", "helado" to "Comida y Restaurantes",
        "frisby" to "Comida y Restaurantes", "el corral" to "Comida y Restaurantes",
        "corral" to "Comida y Restaurantes", "subway" to "Comida y Restaurantes",
        "juan valdez" to "Comida y Restaurantes", "oma" to "Comida y Restaurantes",
        "panaderia" to "Comida y Restaurantes", "pan" to "Comida y Restaurantes",
        "arepa" to "Comida y Restaurantes", "buñuelo" to "Comida y Restaurantes",
        "perro caliente" to "Comida y Restaurantes", "salchipapa" to "Comida y Restaurantes",
        "chuzo" to "Comida y Restaurantes", "tamal" to "Comida y Restaurantes",
        "lechona" to "Comida y Restaurantes", "ajiaco" to "Comida y Restaurantes",
        "sancocho" to "Comida y Restaurantes", "bandeja paisa" to "Comida y Restaurantes",

        // ==========================================
        // 🛒 SUPERMERCADO
        // ==========================================
        "mercado" to "Supermercado", "d1" to "Supermercado", "ara" to "Supermercado",
        "exito" to "Supermercado", "carulla" to "Supermercado", "jumbo" to "Supermercado",
        "despensa" to "Supermercado", "corabastos" to "Supermercado", "makro" to "Supermercado",
        "alkosto" to "Supermercado", "olimpica" to "Supermercado", "fruver" to "Supermercado",
        "surtimax" to "Supermercado", "isimo" to "Supermercado", "colsubsidio" to "Supermercado",

        // ==========================================
        // 🚌 TRANSPORTE PÚBLICO
        // ==========================================
        "bus" to "Transporte Público", "transmilenio" to "Transporte Público",
        "pasaje" to "Transporte Público", "sitp" to "Transporte Público",
        "taxi" to "Transporte Público", "uber" to "Transporte Público",
        "didi" to "Transporte Público", "cabify" to "Transporte Público",
        "picap" to "Transporte Público", "indriver" to "Transporte Público",
        "metro" to "Transporte Público", "mio" to "Transporte Público",
        "alimentador" to "Transporte Público", "flota" to "Transporte Público",

        // ==========================================
        // 🚗 VEHÍCULO Y GASOLINA (Gastos generales)
        // ==========================================
        "gasolina" to "Vehículo y Gasolina", "tanqueo" to "Vehículo y Gasolina",
        "parqueadero" to "Vehículo y Gasolina", "peaje" to "Vehículo y Gasolina",
        "soat" to "Vehículo y Gasolina", "acpm" to "Vehículo y Gasolina",

        // ==========================================
        // 🔞 OCIO NOCTURNO
        // ==========================================
        "bar" to "Ocio Nocturno", "cerveza" to "Ocio Nocturno", "pola" to "Ocio Nocturno",
        "polas" to "Ocio Nocturno", "guaro" to "Ocio Nocturno", "aguardiente" to "Ocio Nocturno",
        "ron" to "Ocio Nocturno", "discoteca" to "Ocio Nocturno", "fiesta" to "Ocio Nocturno",
        "rumba" to "Ocio Nocturno", "farra" to "Ocio Nocturno", "putas" to "Ocio Nocturno",
        "prostitutas" to "Ocio Nocturno", "cariñosas" to "Ocio Nocturno",
        "prepagos" to "Ocio Nocturno", "motel" to "Ocio Nocturno", "amanecedero" to "Ocio Nocturno",
        "club" to "Ocio Nocturno", "tequila" to "Ocio Nocturno", "vodka" to "Ocio Nocturno",
        "coctel" to "Ocio Nocturno", "tragos" to "Ocio Nocturno",

        // ==========================================
        // 🎬 CINE Y ENTRETENIMIENTO
        // ==========================================
        "cine" to "Cine y Entretenimiento", "netflix" to "Cine y Entretenimiento",
        "spotify" to "Cine y Entretenimiento", "videojuegos" to "Cine y Entretenimiento",
        "play" to "Cine y Entretenimiento", "xbox" to "Cine y Entretenimiento",
        "apuestas" to "Cine y Entretenimiento", "casino" to "Cine y Entretenimiento",
        "baloto" to "Cine y Entretenimiento", "concierto" to "Cine y Entretenimiento",
        "billar" to "Cine y Entretenimiento", "tejo" to "Cine y Entretenimiento",
        "sintetica" to "Cine y Entretenimiento", "futbol" to "Cine y Entretenimiento",
        "estadio" to "Cine y Entretenimiento", "cinecolombia" to "Cine y Entretenimiento",
        "cinemark" to "Cine y Entretenimiento", "procinal" to "Cine y Entretenimiento",
        "royal films" to "Cine y Entretenimiento", "prime video" to "Cine y Entretenimiento",
        "disney" to "Cine y Entretenimiento", "hbo" to "Cine y Entretenimiento", "max" to "Cine y Entretenimiento",

        // ==========================================
        // 💊 SALUD Y FARMACIA
        // ==========================================
        "farmacia" to "Salud y Farmacia", "medico" to "Salud y Farmacia",
        "pastillas" to "Salud y Farmacia", "eps" to "Salud y Farmacia",
        "cita medica" to "Salud y Farmacia", "urgencias" to "Salud y Farmacia",
        "condones" to "Salud y Farmacia", "preservativos" to "Salud y Farmacia",
        "drogueria" to "Salud y Farmacia", "odontologo" to "Salud y Farmacia",
        "dentista" to "Salud y Farmacia", "psicologo" to "Salud y Farmacia",
        "terapia" to "Salud y Farmacia", "cruz verde" to "Salud y Farmacia",
        "farmatodo" to "Salud y Farmacia", "colsanitas" to "Salud y Farmacia",
        "sura" to "Salud y Farmacia", "sanitas" to "Salud y Farmacia",
        "famisanar" to "Salud y Farmacia", "compensar" to "Salud y Farmacia",

        // ==========================================
        // 🏠 HOGAR Y SERVICIOS
        // ==========================================
        "arriendo" to "Hogar y Servicios", "agua" to "Hogar y Servicios",
        "luz" to "Hogar y Servicios", "gas" to "Hogar y Servicios",
        "internet" to "Hogar y Servicios", "claro" to "Hogar y Servicios",
        "movistar" to "Hogar y Servicios", "celular" to "Hogar y Servicios",
        "plan" to "Hogar y Servicios", "recarga" to "Hogar y Servicios",
        "administracion" to "Hogar y Servicios", "aseo" to "Hogar y Servicios",
        "ferreteria" to "Hogar y Servicios", "wom" to "Hogar y Servicios",
        "etb" to "Hogar y Servicios", "tigo" to "Hogar y Servicios",
        "directv" to "Hogar y Servicios", "vanti" to "Hogar y Servicios",
        "epm" to "Hogar y Servicios", "enel" to "Hogar y Servicios", "codensa" to "Hogar y Servicios",

        // ==========================================
        // 🛍️ ROPA Y CUIDADO
        // ==========================================
        "ropa" to "Ropa y Cuidado", "zapatos" to "Ropa y Cuidado",
        "tenis" to "Ropa y Cuidado", "chaqueta" to "Ropa y Cuidado",
        "pantalon" to "Ropa y Cuidado", "camisa" to "Ropa y Cuidado",
        "centro comercial" to "Ropa y Cuidado", "maquillaje" to "Ropa y Cuidado",
        "barberia" to "Ropa y Cuidado", "peluqueria" to "Ropa y Cuidado",
        "uñas" to "Ropa y Cuidado", "perfume" to "Ropa y Cuidado",
        "zara" to "Ropa y Cuidado", "bershka" to "Ropa y Cuidado",
        "h&m" to "Ropa y Cuidado", "falabella" to "Ropa y Cuidado",
        "koaj" to "Ropa y Cuidado", "gef" to "Ropa y Cuidado",
        "punto blanco" to "Ropa y Cuidado", "arturo calle" to "Ropa y Cuidado",
        "cuidado personal" to "Ropa y Cuidado", "skincare" to "Ropa y Cuidado",

        // ==========================================
        // 📚 EDUCACIÓN
        // ==========================================
        "universidad" to "Educación", "semestre" to "Educación",
        "cuadernos" to "Educación", "fotocopias" to "Educación",
        "libros" to "Educación", "pension" to "Educación",
        "colegio" to "Educación", "utiles" to "Educación",
        "matricula" to "Educación", "curso" to "Educación",

        // ==========================================
        // 🐶 MASCOTAS
        // ==========================================
        "perro" to "Mascotas", "gato" to "Mascotas",
        "veterinario" to "Mascotas", "purina" to "Mascotas",
        "concentrado" to "Mascotas", "croquetas" to "Mascotas",
        "mascota" to "Mascotas", "arena" to "Mascotas",

        // ==========================================
        // ✈️ VIAJES
        // ==========================================
        "vuelo" to "Viajes", "avion" to "Viajes",
        "hotel" to "Viajes", "viaje" to "Viajes",
        "vacaciones" to "Viajes", "turismo" to "Viajes",
        "airbnb" to "Viajes", "pasajes" to "Viajes",

        // ==========================================
        // 🏋️ GIMNASIO Y DEPORTE
        // ==========================================
        "gimnasio" to "Gimnasio y Deporte", "gym" to "Gimnasio y Deporte",
        "smartfit" to "Gimnasio y Deporte", "pesas" to "Gimnasio y Deporte",
        "suplementos" to "Gimnasio y Deporte", "proteina" to "Gimnasio y Deporte",
        "creatina" to "Gimnasio y Deporte", "bodytech" to "Gimnasio y Deporte",

        // ==========================================
        // 🎁 REGALOS
        // ==========================================
        "regalo" to "Regalos", "detalle" to "Regalos",
        "cumpleaños" to "Regalos", "aniversario" to "Regalos",
        "flores" to "Regalos", "chocolates" to "Regalos",

        // ==========================================
        // 💻 TECNOLOGÍA
        // ==========================================
        "celular nuevo" to "Tecnología", "computador" to "Tecnología",
        "pc" to "Tecnología", "audifonos" to "Tecnología",
        "cable" to "Tecnología", "cargador" to "Tecnología",
        "apple" to "Tecnología", "samsung" to "Tecnología",
        "xiaomi" to "Tecnología", "rtx" to "Tecnología"
    )

    fun suggestCategory(description: String): String? {
        val lowerDesc = description.lowercase()

        for ((word, categoryName) in keywords) {
            // Regex con límite de palabra (\b)
            // Ejemplo: si word es "pc", hará match con "mi pc nuevo" pero NO con "opcion"
            val regex = Regex("\\b$word\\b")
            if (regex.containsMatchIn(lowerDesc)) {
                return categoryName
            }
        }
        return null
    }

    // Lista Maestra Centralizada (Con las nuevas categorías añadidas)
    fun getDefaultCategories(): List<Categoria> {
        return listOf(
            Categoria(nombre = "Salario e Ingresos", emoji = "💰", colorHex = "#388E3C"),
            Categoria(nombre = "Motos", emoji = "🏍️", colorHex = "#E64A19"),
            Categoria(nombre = "Carros", emoji = "🚗", colorHex = "#1976D2"),
            Categoria(nombre = "Mantenimiento y Reparación", emoji = "🛠️", colorHex = "#5D4037"),
            Categoria(nombre = "Bancos y Finanzas", emoji = "🏦", colorHex = "#009688"),
            Categoria(nombre = "Comida y Restaurantes", emoji = "🍔", colorHex = "#FF9800"),
            Categoria(nombre = "Supermercado", emoji = "🛒", colorHex = "#4CAF50"),
            Categoria(nombre = "Transporte Público", emoji = "🚌", colorHex = "#03A9F4"),
            Categoria(nombre = "Vehículo y Gasolina", emoji = "⛽", colorHex = "#607D8B"),
            Categoria(nombre = "Ocio Nocturno", emoji = "🔞", colorHex = "#B71C1C"),
            Categoria(nombre = "Cine y Entretenimiento", emoji = "🎬", colorHex = "#673AB7"),
            Categoria(nombre = "Salud y Farmacia", emoji = "💊", colorHex = "#E91E63"),
            Categoria(nombre = "Hogar y Servicios", emoji = "🏠", colorHex = "#795548"),
            Categoria(nombre = "Ropa y Cuidado", emoji = "🛍️", colorHex = "#9C27B0"),
            Categoria(nombre = "Educación", emoji = "📚", colorHex = "#00BCD4"),
            Categoria(nombre = "Mascotas", emoji = "🐶", colorHex = "#FF5722"),
            Categoria(nombre = "Viajes", emoji = "✈️", colorHex = "#3F51B5"),
            Categoria(nombre = "Gimnasio y Deporte", emoji = "🏋️", colorHex = "#8BC34A"),
            Categoria(nombre = "Regalos", emoji = "🎁", colorHex = "#FFC107"),
            Categoria(nombre = "Tecnología", emoji = "💻", colorHex = "#607D8B"),
            Categoria(nombre = "Otros", emoji = "📦", colorHex = "#9E9E9E")
        )
    }
}