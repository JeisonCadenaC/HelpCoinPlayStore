package com.example.finance_code.data

import android.content.Context
import com.example.finance_code.R
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class DriveService(
    private val context: Context,
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

    private val BACKUP_FILE_NAME = "full_backup_${userIdentifier}.zip"

    suspend fun uploadFullBackup(userEmail: String, userUid: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val zipFile = java.io.File(context.cacheDir, BACKUP_FILE_NAME)
                val zipOutputStream = ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile)))

                val dbName = "finance_db_$userIdentifier"
                addFileToZip(zipOutputStream, context.getDatabasePath(dbName), "database.db")
                addFileToZip(zipOutputStream, context.getDatabasePath("$dbName-wal"), "database.db-wal")
                addFileToZip(zipOutputStream, context.getDatabasePath("$dbName-shm"), "database.db-shm")

                val prefsName = "${userUid}_UserProfilePrefs"
                val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
                val savedImagePath = prefs.getString("profile_image_path", null)
                var imageAdded = false

                if (savedImagePath != null) {
                    val customFile = java.io.File(savedImagePath)
                    if (customFile.exists()) {
                        addFileToZip(zipOutputStream, customFile, "profile_image.jpg")
                        imageAdded = true
                    }
                }

                if (!imageAdded) {
                    val defaultImg = java.io.File(context.filesDir, "${userUid}_profile_image.jpg")
                    if (defaultImg.exists()) {
                        addFileToZip(zipOutputStream, defaultImg, "profile_image.jpg")
                    }
                }

                val userPrefsFile = java.io.File(context.cacheDir, "user_prefs.json")
                savePrefsToJson(context, prefsName, userPrefsFile)
                addFileToZip(zipOutputStream, userPrefsFile, "user_prefs.json")

                val appPrefsFile = java.io.File(context.cacheDir, "app_prefs.json")
                savePrefsToJson(context, "AppPrefe", appPrefsFile)
                addFileToZip(zipOutputStream, appPrefsFile, "app_prefs.json")

                val loginPrefsFile = java.io.File(context.cacheDir, "login_prefs.json")
                savePrefsToJson(context, "LoginPrefs", loginPrefsFile)
                addFileToZip(zipOutputStream, loginPrefsFile, "login_prefs.json")

                val analisisPrefsName = "analisis_prefs_$userEmail"
                val analisisPrefsFile = java.io.File(context.cacheDir, "analisis_prefs.json")
                savePrefsToJson(context, analisisPrefsName, analisisPrefsFile)
                addFileToZip(zipOutputStream, analisisPrefsFile, "analisis_prefs.json")

                zipOutputStream.close()

                val matchingFiles = findBackupFiles()
                val fileMetadata = File().apply { name = BACKUP_FILE_NAME }
                val mediaContent = FileContent("application/zip", zipFile)
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
                            try { driveService.files().delete(matchingFiles[i].id).execute() } catch (_: Exception) { }
                        }
                    }
                }

                if(zipFile.exists()) zipFile.delete()
                if(userPrefsFile.exists()) userPrefsFile.delete()
                if(appPrefsFile.exists()) appPrefsFile.delete()
                if(loginPrefsFile.exists()) loginPrefsFile.delete()
                if(analisisPrefsFile.exists()) analisisPrefsFile.delete()

                fileIdToReturn
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    suspend fun restoreFullBackup(userEmail: String, userUid: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val matchingFiles = findBackupFiles()
                if (matchingFiles.isEmpty()) return@withContext false

                val fileId = matchingFiles[0].id
                val zipFile = java.io.File(context.cacheDir, "restore_temp.zip")
                val outputStream = FileOutputStream(zipFile)
                driveService.files().get(fileId).executeMediaAndDownloadTo(outputStream)
                outputStream.close()

                val tempDir = java.io.File(context.cacheDir, "restore_extracted")
                if (tempDir.exists()) tempDir.deleteRecursively()
                tempDir.mkdirs()

                unzipFile(zipFile, tempDir)
                zipFile.delete()

                val dbName = "finance_db_$userIdentifier"
                val dbFile = context.getDatabasePath(dbName)
                val dbWal = context.getDatabasePath("$dbName-wal")
                val dbShm = context.getDatabasePath("$dbName-shm")

                if (dbFile.exists()) dbFile.delete()
                if (dbWal.exists()) dbWal.delete()
                if (dbShm.exists()) dbShm.delete()

                val newDb = java.io.File(tempDir, "database.db")
                val newWal = java.io.File(tempDir, "database.db-wal")
                val newShm = java.io.File(tempDir, "database.db-shm")

                if (newDb.exists()) newDb.copyTo(dbFile)
                if (newWal.exists()) newWal.copyTo(dbWal)
                if (newShm.exists()) newShm.copyTo(dbShm)

                val destImgFile = java.io.File(context.filesDir, "${userUid}_profile_image.jpg")
                val newImg = java.io.File(tempDir, "profile_image.jpg")
                if (newImg.exists()) {
                    newImg.copyTo(destImgFile, overwrite = true)
                }

                restorePrefsFromJson(context, "${userUid}_UserProfilePrefs", java.io.File(tempDir, "user_prefs.json"))
                restorePrefsFromJson(context, "AppPrefe", java.io.File(tempDir, "app_prefs.json"))
                restorePrefsFromJson(context, "LoginPrefs", java.io.File(tempDir, "login_prefs.json"))

                val analisisPrefsName = "analisis_prefs_$userEmail"
                restorePrefsFromJson(context, analisisPrefsName, java.io.File(tempDir, "analisis_prefs.json"))

                if (destImgFile.exists()) {
                    val prefs = context.getSharedPreferences("${userUid}_UserProfilePrefs", Context.MODE_PRIVATE)
                    prefs.edit().putString("profile_image_path", destImgFile.absolutePath).commit()
                }

                tempDir.deleteRecursively()
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    private fun addFileToZip(zos: ZipOutputStream, file: java.io.File, entryName: String) {
        if (file.exists()) {
            val entry = ZipEntry(entryName)
            zos.putNextEntry(entry)
            FileInputStream(file).use { fis ->
                fis.copyTo(zos)
            }
            zos.closeEntry()
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

    private fun savePrefsToJson(context: Context, prefName: String, file: java.io.File) {
        try {
            val prefs = context.getSharedPreferences(prefName, Context.MODE_PRIVATE)
            val all = prefs.all
            if (all.isNotEmpty()) {
                val json = JSONObject(all as Map<*, *>)
                file.writeText(json.toString())
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun restorePrefsFromJson(context: Context, prefName: String, file: java.io.File) {
        if (!file.exists()) return
        try {
            val jsonStr = file.readText()
            val json = JSONObject(jsonStr)
            val prefs = context.getSharedPreferences(prefName, Context.MODE_PRIVATE).edit()

            prefs.clear()

            val iter = json.keys()
            val longKeys = setOf(
                "pInicioGastos", "pFinGastos", "cInicioGastos", "cFinGastos",
                "pInicioIngresos", "pFinIngresos", "cInicioIngresos", "cFinIngresos"
            )

            while (iter.hasNext()) {
                val key = iter.next()
                val value = json.get(key)
                when (value) {
                    is Boolean -> prefs.putBoolean(key, value)
                    is Int -> {
                        if (key in longKeys || key.lowercase().contains("time") || key.lowercase().contains("date") || key.lowercase().contains("millis")) {
                            prefs.putLong(key, value.toLong())
                        } else {
                            prefs.putInt(key, value)
                        }
                    }
                    is Long -> prefs.putLong(key, value)
                    is Double -> prefs.putFloat(key, value.toFloat())
                    is String -> prefs.putString(key, value)
                }
            }
            prefs.commit()
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun unzipFile(zipFile: java.io.File, targetDir: java.io.File) {
        ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val file = java.io.File(targetDir, entry.name)
                if (entry.isDirectory) {
                    file.mkdirs()
                } else {
                    file.parentFile?.mkdirs()
                    FileOutputStream(file).use { fos -> zis.copyTo(fos) }
                }
                entry = zis.nextEntry
            }
        }
    }
}