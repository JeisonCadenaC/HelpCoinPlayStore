package com.example.finance_code.utils

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject
import java.io.File

object PreferencesBackupHelper {

    private const val BACKUP_FILE_NAME = "prefs_backup.json"

    // Extrae todas las SharedPreferences a un archivo JSON físico
    fun exportPreferencesToFile(context: Context): File? {
        try {
            val appPrefs = context.getSharedPreferences("AppPrefe", Context.MODE_PRIVATE)
            val auraPrefs = context.getSharedPreferences("HelpCoinAuraPrefs", Context.MODE_PRIVATE)

            val jsonObject = JSONObject()

            // AppPrefe
            appPrefs.all.forEach { (key, value) -> jsonObject.put("AppPrefe_$key", value) }
            // AuraPrefs
            auraPrefs.all.forEach { (key, value) -> jsonObject.put("AuraPrefs_$key", value) }

            val backupFile = File(context.filesDir, BACKUP_FILE_NAME)
            backupFile.writeText(jsonObject.toString())
            return backupFile
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    // Restaura las SharedPreferences desde el archivo JSON físico
    fun importPreferencesFromFile(context: Context): Boolean {
        try {
            val backupFile = File(context.filesDir, BACKUP_FILE_NAME)
            if (!backupFile.exists()) return false

            val jsonContent = backupFile.readText()
            val jsonObject = JSONObject(jsonContent)

            val appPrefs = context.getSharedPreferences("AppPrefe", Context.MODE_PRIVATE).edit()
            val auraPrefs = context.getSharedPreferences("HelpCoinAuraPrefs", Context.MODE_PRIVATE).edit()

            jsonObject.keys().forEach { key ->
                val value = jsonObject.get(key)
                if (key.startsWith("AppPrefe_")) {
                    val realKey = key.removePrefix("AppPrefe_")
                    when (value) {
                        is Boolean -> appPrefs.putBoolean(realKey, value)
                        is String -> appPrefs.putString(realKey, value)
                        is Int -> appPrefs.putInt(realKey, value)
                        is Float -> appPrefs.putFloat(realKey, value)
                        is Long -> appPrefs.putLong(realKey, value)
                    }
                } else if (key.startsWith("AuraPrefs_")) {
                    val realKey = key.removePrefix("AuraPrefs_")
                    when (value) {
                        is String -> auraPrefs.putString(realKey, value)
                        is Int -> auraPrefs.putInt(realKey, value)
                    }
                }
            }
            appPrefs.apply()
            auraPrefs.apply()
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
}