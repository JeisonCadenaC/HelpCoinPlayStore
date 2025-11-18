package com.example.finance_code.data

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.services.drive.DriveScopes
import com.google.android.gms.common.api.Scope
import com.google.firebase.auth.FirebaseAuth

class BackupWorker(val context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Log.d("BackupWorker", "Iniciando tarea de backup automático...")

        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        if (currentUser == null || currentUser.email == null) {
            Log.d("BackupWorker", "Usuario no autenticado, cancelando backup.")
            return Result.failure()
        }

        val userEmail = currentUser.email!!

        val googleAccount = GoogleSignIn.getLastSignedInAccount(context)
        if (googleAccount == null) {
            Log.d("BackupWorker", "No se encontró cuenta de Google (getLastSignedInAccount). El backup manual es necesario primero.")
            return Result.failure()
        }

        val requiredScope = Scope(DriveScopes.DRIVE_APPDATA)
        if (!googleAccount.grantedScopes.contains(requiredScope)) {
            Log.d("BackupWorker", "El permiso de Google Drive (appDataFolder) no está concedido. El backup manual es necesario primero.")
            return Result.failure()
        }

        val dbIdentifier = getDbIdentifier(userEmail)
        val dbName = "finance_db_$dbIdentifier"
        val dbFile = context.getDatabasePath(dbName)

        if (!dbFile.exists()) {
            Log.d("BackupWorker", "No se encontró el archivo de la base de datos local.")
            return Result.failure()
        }

        try {
            val driveService = DriveService(context, googleAccount, dbIdentifier)
            val fileId = driveService.uploadBackup(dbFile)

            return if (fileId != null) {
                Log.d("BackupWorker", "Backup automático completado exitosamente. File ID: $fileId")
                Result.success()
            } else {
                Log.d("BackupWorker", "driveService.uploadBackup falló (retornó null).")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e("BackupWorker", "Error durante la subida del backup", e)
            return Result.retry()
        }
    }

    private fun getDbIdentifier(email: String): String {
        return email.replace(Regex("[^a-zA-Z0-9]"), "_")
    }
}