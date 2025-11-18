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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DriveService(
    context: Context,
    account: GoogleSignInAccount,
    private val userIdentifier: String
) {

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

    private val BACKUP_FILE_NAME = "finance_db_backup_${userIdentifier}.db"

    suspend fun uploadBackup(databaseFile: java.io.File): String? {
        return try {
            withContext(Dispatchers.IO) {
                val matchingFiles = findBackupFiles()
                val fileMetadata = File().apply {
                    name = BACKUP_FILE_NAME
                }
                val mediaContent = FileContent("application/x-sqlite3", databaseFile)

                val fileIdToReturn: String

                if (matchingFiles.isEmpty()) {
                    fileMetadata.parents = listOf("appDataFolder")
                    val createdFile = driveService.files().create(fileMetadata, mediaContent)
                        .setFields("id")
                        .execute()
                    fileIdToReturn = createdFile.id
                } else {
                    val mainFileId = matchingFiles[0].id
                    driveService.files().update(mainFileId, fileMetadata, mediaContent).execute()
                    fileIdToReturn = mainFileId

                    if (matchingFiles.size > 1) {
                        for (i in 1 until matchingFiles.size) {
                            try {
                                driveService.files().delete(matchingFiles[i].id).execute()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
                fileIdToReturn
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun findBackupFiles(): List<File> {
        val foundFiles = mutableListOf<File>()
        try {
            var pageToken: String? = null
            do {
                val result = driveService.files().list()
                    .setSpaces("appDataFolder")
                    .setQ("name = '$BACKUP_FILE_NAME' and trashed = false")
                    .setFields("nextPageToken, files(id, name)")
                    .setPageToken(pageToken)
                    .execute()

                foundFiles.addAll(result.files)
                pageToken = result.nextPageToken
            } while (pageToken != null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return foundFiles
    }

    suspend fun downloadRestore(destinationFile: java.io.File): Boolean {
        return withContext(Dispatchers.IO) {
            val matchingFiles = findBackupFiles()
            if (matchingFiles.isEmpty()) {
                false
            } else {
                try {
                    val fileId = matchingFiles[0].id
                    val outputStream: OutputStream = FileOutputStream(destinationFile)

                    driveService.files().get(fileId)
                        .executeMediaAndDownloadTo(outputStream)

                    outputStream.flush()
                    outputStream.close()
                    true
                } catch (e: Exception) {
                    e.printStackTrace()
                    false
                }
            }
        }
    }
}