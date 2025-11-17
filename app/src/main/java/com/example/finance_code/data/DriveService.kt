package com.example.finance_code.data

import android.content.Context
import android.util.Log
import com.example.finance_code.R
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import java.io.FileOutputStream
import java.io.OutputStream

class DriveService(context: Context, account: GoogleSignInAccount) {

    private val driveService: Drive = run {
        val credential = GoogleAccountCredential.usingOAuth2(
            context, setOf(DriveScopes.DRIVE_APPDATA)
        ).setSelectedAccount(account.account)

        Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName(context.getString(R.string.app_name))
            .build()
    }

    companion object {
        const val BACKUP_FILE_NAME = "finance_db_backup.db"
    }

    suspend fun uploadBackup(databaseFile: java.io.File): String? {
        return try {
            val existingFileId = findBackupFileId()

            val fileMetadata = File().apply {
                name = BACKUP_FILE_NAME
                if (existingFileId == null) {
                    parents = listOf("appDataFolder")
                }
            }
            val mediaContent = FileContent("application/x-sqlite3", databaseFile)

            val file: File = if (existingFileId != null) {
                Log.d("DriveService", "Actualizando backup existente...")
                driveService.files().update(existingFileId, fileMetadata, mediaContent).execute()
            } else {
                Log.d("DriveService", "Creando nuevo backup...")
                driveService.files().create(fileMetadata, mediaContent).setFields("id").execute()
            }
            Log.d("DriveService", "Backup completado. File ID: ${file.id}")
            file.id
        } catch (e: Exception) {
            Log.e("DriveService", "Error al subir backup", e)
            null
        }
    }

    private fun findBackupFileId(): String? {
        val result = driveService.files().list()
            .setSpaces("appDataFolder")
            .setFields("files(id, name)")
            .execute()

        return result.files.find { it.name == BACKUP_FILE_NAME }?.id
    }

    suspend fun downloadRestore(destinationFile: java.io.File): Boolean {
        val fileId = findBackupFileId()
        if (fileId == null) {
            Log.d("DriveService", "No se encontró ningún backup para restaurar.")
            return false
        }

        return try {
            val outputStream: OutputStream = FileOutputStream(destinationFile)
            Log.d("DriveService", "Descargando backup de $fileId...")

            driveService.files().get(fileId)
                .executeMediaAndDownloadTo(outputStream)

            outputStream.flush()
            outputStream.close()
            Log.d("DriveService", "Restauración completada.")
            true
        } catch (e: Exception) {
            Log.e("DriveService", "Error al descargar restauración", e)
            false
        }
    }
}
