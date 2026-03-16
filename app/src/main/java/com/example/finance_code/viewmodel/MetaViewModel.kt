package com.example.finance_code.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.finance_code.data.AporteDB
import com.example.finance_code.data.AppDB
import com.example.finance_code.data.MetaDB
import com.example.finance_code.data.MetaRepository
import com.example.finance_code.ui.home.ReminderHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class MetaViewModel(application: Application) : AndroidViewModel(application) {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val metaRepository: MetaRepository

    private val liveDataLocalMetas: LiveData<List<MetaDB>>
    private val liveDataSharedMetas = MutableLiveData<List<MetaDB>>()

    private val _allMetas = MediatorLiveData<List<MetaDB>>()
    val allMetas: LiveData<List<MetaDB>> = _allMetas

    private var listaInvitaciones = listOf<MetaDB>()

    val userEmail: String get() = auth.currentUser?.email ?: ""

    init {
        val metaDao = AppDB.getDatabase(application, userEmail).metaDao()
        metaRepository = MetaRepository(metaDao)
        liveDataLocalMetas = metaRepository.allLocalMetas

        setupHybridMetas()
        listenToSharedMetas()
    }

    private fun setupHybridMetas() {
        _allMetas.addSource(liveDataLocalMetas) { localMetas ->
            combineMetas(localMetas, liveDataSharedMetas.value ?: emptyList())
        }
        _allMetas.addSource(liveDataSharedMetas) { sharedMetas ->
            combineMetas(liveDataLocalMetas.value ?: emptyList(), sharedMetas)
        }
    }

    private fun combineMetas(local: List<MetaDB>, shared: List<MetaDB>) {
        val todasLasMetas = (local + shared)
            .sortedWith(
                compareBy<MetaDB> { !it.invitaciones.contains(userEmail) }
                    .thenBy { it.orden }
                    .thenBy { it.completada }
            )
        _allMetas.value = todasLasMetas
    }

    fun guardarNuevoOrden(listaOrdenada: List<MetaDB>) = viewModelScope.launch(Dispatchers.IO) {
        listaOrdenada.forEachIndexed { index, meta ->
            if (meta.orden != index) {
                val metaActualizada = meta.copy(orden = index)
                if (meta.usuarios.isEmpty()) {
                    metaRepository.update(metaActualizada)
                } else {
                    db.collection("metas").document(meta.id).update("orden", index)
                }
            }
        }
    }

    private fun listenToSharedMetas() {
        val email = userEmail
        if (email.isEmpty()) return

        db.collection("metas")
            .whereArrayContains("usuarios", email)
            .addSnapshotListener { value, _ ->
                val activeMetas = value?.toObjects(MetaDB::class.java) ?: emptyList()

                value?.documents?.forEach { document ->
                    val data = document.data
                    val metaId = document.id
                    val statusInv = data?.get("invitation_status") as? Map<String, Any>
                    val statusAporte = data?.get("aporte_status") as? Map<String, Any>
                    val statusImage = data?.get("image_status") as? Map<String, Any>

                    if (statusInv != null) {
                        val recipientEmail = statusInv["recipient"] as? String ?: ""
                        if (recipientEmail == email) {
                            val collaboratorEmail = statusInv["by"] as? String ?: "Alguien"
                            val action = statusInv["action"] as? String ?: "actualizado"
                            val nombreMeta = data["nombre"] as? String ?: "una de tus metas"

                            ReminderHelper.showMetaCollaborationNotification(
                                getApplication(),
                                nombreMeta,
                                collaboratorEmail,
                                action
                            )

                            viewModelScope.launch(Dispatchers.IO) {
                                db.collection("metas").document(metaId)
                                    .update("invitation_status", FieldValue.delete())
                                    .addOnFailureListener {
                                        Log.e("MetaVM", "Error al limpiar status de invitación: ${it.message}")
                                    }
                            }
                        }
                    }

                    if (statusAporte != null) {
                        val byEmail = statusAporte["by"] as? String ?: ""
                        if (byEmail.isNotEmpty() && byEmail != email) {
                            val montoAbsoluto = statusAporte["monto"] as? String ?: ""
                            val tipo = statusAporte["tipo"] as? String ?: ""
                            val nombreMeta = data["nombre"] as? String ?: "una de tus metas"

                            ReminderHelper.showAporteNotification(
                                getApplication(),
                                nombreMeta,
                                byEmail,
                                montoAbsoluto,
                                tipo
                            )

                            viewModelScope.launch(Dispatchers.IO) {
                                db.collection("metas").document(metaId)
                                    .update("aporte_status", FieldValue.delete())
                                    .addOnFailureListener {
                                        Log.e("MetaVM", "Error al limpiar status de aporte: ${it.message}")
                                    }
                            }
                        }
                    }

                    if (statusImage != null) {
                        val byEmail = statusImage["by"] as? String ?: ""
                        if (byEmail.isNotEmpty() && byEmail != email) {
                            val nombreMeta = data["nombre"] as? String ?: "una de tus metas"

                            ReminderHelper.showImageUpdateNotification(
                                getApplication(),
                                nombreMeta,
                                byEmail
                            )

                            viewModelScope.launch(Dispatchers.IO) {
                                db.collection("metas").document(metaId)
                                    .update("image_status", FieldValue.delete())
                                    .addOnFailureListener {
                                        Log.e("MetaVM", "Error al limpiar status de imagen: ${it.message}")
                                    }
                            }
                        }
                    }
                }

                combineShared(activeMetas, listaInvitaciones)
            }

        db.collection("metas")
            .whereArrayContains("invitaciones", email)
            .addSnapshotListener { value, _ ->
                val nuevasInvitaciones = value?.toObjects(MetaDB::class.java) ?: emptyList()

                val nuevasInvitacionesUnicas = nuevasInvitaciones.filter { nuevaMeta ->
                    !listaInvitaciones.any { it.id == nuevaMeta.id }
                }

                nuevasInvitacionesUnicas.forEach { meta ->
                    val inviterEmail = meta.usuarios.firstOrNull() ?: "Alguien"
                    ReminderHelper.showInvitationReceivedNotification(getApplication(), meta.nombre, inviterEmail)
                }

                listaInvitaciones = nuevasInvitaciones

                db.collection("metas")
                    .whereArrayContains("usuarios", email)
                    .get()
                    .addOnSuccessListener { activeSnapshot ->
                        val activeMetas = activeSnapshot.toObjects(MetaDB::class.java)
                        combineShared(activeMetas, listaInvitaciones)
                    }
            }
    }

    private fun combineShared(active: List<MetaDB>, invites: List<MetaDB>) {
        val todasLasMetas = (active + invites)
            .distinctBy { it.id }
            .filter { it.usuarios.isNotEmpty() }
        liveDataSharedMetas.value = todasLasMetas
    }

    fun getAportesLog(metaId: String): LiveData<List<AporteDB>> {
        val liveData = MutableLiveData<List<AporteDB>>()
        db.collection("metas").document(metaId).collection("aportes")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { value, error ->
                if (error == null && value != null) {
                    liveData.value = value.toObjects(AporteDB::class.java)
                }
            }
        return liveData
    }

    private fun obtenerBitmapCuadrado600(uri: Uri): Bitmap? {
        return try {
            val context = getApplication<Application>().applicationContext

            var rotation = 0f
            try {
                context.contentResolver.query(uri, arrayOf(android.provider.MediaStore.Images.ImageColumns.ORIENTATION), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        rotation = cursor.getInt(0).toFloat()
                    }
                }

                if (rotation == 0f && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        val exif = android.media.ExifInterface(inputStream)
                        val orientation = exif.getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION, android.media.ExifInterface.ORIENTATION_NORMAL)
                        rotation = when (orientation) {
                            android.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                            android.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                            android.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                            else -> 0f
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("MetaVM", "Error leyendo EXIF: ${e.message}")
            }

            var originalBitmap: Bitmap? = null
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                originalBitmap = BitmapFactory.decodeStream(inputStream)
            }

            if (originalBitmap == null) return null

            val rotatedBitmap = if (rotation != 0f) {
                val matrix = Matrix()
                matrix.postRotate(rotation)
                val rotated = Bitmap.createBitmap(originalBitmap!!, 0, 0, originalBitmap!!.width, originalBitmap!!.height, matrix, true)
                if (rotated != originalBitmap) originalBitmap!!.recycle()
                rotated
            } else {
                originalBitmap!!
            }

            val width = rotatedBitmap.width
            val height = rotatedBitmap.height
            val newSize = Math.min(width, height)

            val startX = (width - newSize) / 2
            val startY = (height - newSize) / 2

            val squareBitmap = Bitmap.createBitmap(rotatedBitmap, startX, startY, newSize, newSize)

            val scaledBitmap = Bitmap.createScaledBitmap(squareBitmap, 600, 600, true)

            if (squareBitmap != rotatedBitmap) squareBitmap.recycle()
            if (rotatedBitmap != scaledBitmap && !rotatedBitmap.isRecycled) rotatedBitmap.recycle()

            scaledBitmap
        } catch (e: Exception) {
            Log.e("MetaVM", "Error procesando imagen: ${e.message}")
            null
        }
    }

    private fun obtenerImagenBase64Comprimida(uri: Uri): String? {
        return try {
            val bitmapCuadrado = obtenerBitmapCuadrado600(uri) ?: return null

            val outputStream = ByteArrayOutputStream()
            bitmapCuadrado.compress(Bitmap.CompressFormat.JPEG, 60, outputStream)
            val byteArray = outputStream.toByteArray()

            val base64String = Base64.encodeToString(byteArray, Base64.NO_WRAP)
            "data:image/jpeg;base64,$base64String"
        } catch (e: Exception) {
            Log.e("MetaVM", "Error obteniendo Base64: ${e.message}")
            null
        }
    }

    private fun guardarImagenLocal(uri: Uri, metaId: String): String? {
        return try {
            val bitmapCuadrado = obtenerBitmapCuadrado600(uri) ?: return null

            val context = getApplication<Application>().applicationContext
            val file = File(context.filesDir, "meta_$metaId.jpg")
            val outputStream = FileOutputStream(file)

            bitmapCuadrado.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            outputStream.flush()
            outputStream.close()

            file.absolutePath
        } catch (e: Exception) {
            Log.e("MetaVM", "Error guardando local: ${e.message}")
            null
        }
    }

    fun insert(metaDB: MetaDB, emailsInvitados: String = "", uri: Uri? = null) = viewModelScope.launch(Dispatchers.IO) {
        val currentUserEmail = userEmail
        var urlFinal: String? = null

        val invitadosList = emailsInvitados.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() && it != currentUserEmail }
            .distinct()

        if (uri != null) {
            urlFinal = if (invitadosList.isEmpty()) {
                guardarImagenLocal(uri, metaDB.id)
            } else {
                obtenerImagenBase64Comprimida(uri)
            }
        }

        val metaConImagen = metaDB.copy(imagenUrl = urlFinal)

        if (invitadosList.isEmpty()) {
            metaRepository.insert(metaConImagen.copy(usuarios = emptyList(), invitaciones = emptyList()))
        } else {
            val listaUsuarios = listOf(currentUserEmail)
            val nuevaMeta = metaConImagen.copy(
                usuarios = listaUsuarios,
                invitaciones = invitadosList
            )
            db.collection("metas").document(nuevaMeta.id).set(nuevaMeta)
        }
    }

    fun registrarAporte(metaDBExistente: MetaDB, montoCambio: Double, tipoOperacion: String) = viewModelScope.launch(Dispatchers.IO) {
        val montoAbsolutoStr = "%.0f".format(kotlin.math.abs(montoCambio))

        ReminderHelper.showAporteNotification(
            getApplication(),
            metaDBExistente.nombre,
            "Tú",
            montoAbsolutoStr,
            tipoOperacion
        )

        if (metaDBExistente.usuarios.isEmpty()) {
            val nuevoMonto = metaDBExistente.montoActual + montoCambio
            val montoFinal = if (nuevoMonto < 0) 0.0 else nuevoMonto
            val esCompletadaAhora = montoFinal >= metaDBExistente.montoObjetivo
            metaRepository.update(metaDBExistente.copy(montoActual = montoFinal, completada = esCompletadaAhora))
            return@launch
        }

        val email = userEmail
        val metaId = metaDBExistente.id
        if (email.isEmpty() || metaId.isEmpty() || montoCambio == 0.0) return@launch

        val metaRef = db.collection("metas").document(metaId)
        val aportesRef = metaRef.collection("aportes")

        val nuevoAporte = AporteDB(
            id = UUID.randomUUID().toString(),
            metaId = metaId,
            userId = email,
            monto = montoCambio,
            tipo = tipoOperacion
        )

        db.runTransaction { transaction ->
            val snapshot = transaction.get(metaRef)
            val metaDB = snapshot.toObject(MetaDB::class.java)

            if (metaDB != null) {
                val nuevoMontoActual = metaDB.montoActual + montoCambio
                val montoFinal = if (nuevoMontoActual < 0) 0.0 else nuevoMontoActual
                val esCompletadaAhora = montoFinal >= metaDB.montoObjetivo

                transaction.update(
                    metaRef,
                    mapOf(
                        "montoActual" to montoFinal,
                        "completada" to esCompletadaAhora
                    )
                )
                transaction.set(aportesRef.document(nuevoAporte.id), nuevoAporte)
            }
            null
        }.addOnSuccessListener {
            metaRef.update(
                "aporte_status", mapOf(
                    "tipo" to tipoOperacion,
                    "by" to email,
                    "monto" to montoAbsolutoStr,
                    "timestamp" to System.currentTimeMillis()
                )
            )
        }
    }

    fun aceptarInvitacion(metaDB: MetaDB) = viewModelScope.launch(Dispatchers.IO) {
        val email = userEmail
        val metaRef = db.collection("metas").document(metaDB.id)
        val inviterEmail = metaDB.usuarios.firstOrNull() ?: ""

        metaRef.update(
            mapOf(
                "usuarios" to FieldValue.arrayUnion(email),
                "invitaciones" to FieldValue.arrayRemove(email)
            )
        ).addOnSuccessListener {
            metaRef.update(
                "invitation_status", mapOf(
                    "action" to "aceptó",
                    "by" to email,
                    "timestamp" to System.currentTimeMillis(),
                    "recipient" to inviterEmail
                )
            )
        }
    }

    fun rechazarInvitacion(metaDB: MetaDB) = viewModelScope.launch(Dispatchers.IO) {
        val email = userEmail
        val metaRef = db.collection("metas").document(metaDB.id)
        val inviterEmail = metaDB.usuarios.firstOrNull() ?: ""

        metaRef.update(
            "invitaciones", FieldValue.arrayRemove(email)
        ).addOnSuccessListener {
            metaRef.update(
                "invitation_status", mapOf(
                    "action" to "denegó",
                    "by" to email,
                    "timestamp" to System.currentTimeMillis(),
                    "recipient" to inviterEmail
                )
            )
        }
    }

    fun update(metaDB: MetaDB, uri: Uri? = null) = viewModelScope.launch(Dispatchers.IO) {
        var urlFinal = metaDB.imagenUrl
        var imagenCambiada = false

        if (uri != null) {
            urlFinal = if (metaDB.usuarios.isEmpty()) {
                guardarImagenLocal(uri, metaDB.id)
            } else {
                obtenerImagenBase64Comprimida(uri)
            }
            if (urlFinal != metaDB.imagenUrl) {
                imagenCambiada = true
            }
        }

        val metaActualizada = metaDB.copy(imagenUrl = urlFinal ?: metaDB.imagenUrl)

        if (imagenCambiada) {
            ReminderHelper.showImageUpdateNotification(getApplication(), metaActualizada.nombre, "Tú")
        }

        if (metaDB.usuarios.isEmpty()) {
            metaRepository.update(metaActualizada)
        } else {
            val updates = mutableMapOf<String, Any>()
            updates["nombre"] = metaActualizada.nombre
            updates["montoObjetivo"] = metaActualizada.montoObjetivo
            updates["completada"] = metaActualizada.completada
            if (metaActualizada.fechaLimite != null) {
                updates["fechaLimite"] = metaActualizada.fechaLimite!!
            }
            updates["imagenUrl"] = metaActualizada.imagenUrl ?: ""

            if (imagenCambiada) {
                updates["image_status"] = mapOf(
                    "by" to userEmail,
                    "timestamp" to System.currentTimeMillis()
                )
            }

            db.collection("metas").document(metaActualizada.id).update(updates)
        }
    }

    fun delete(metaDB: MetaDB) = viewModelScope.launch(Dispatchers.IO) {
        if (metaDB.usuarios.isEmpty()) {
            metaRepository.delete(metaDB)
        } else {
            db.collection("metas").document(metaDB.id).delete()
        }
    }
}