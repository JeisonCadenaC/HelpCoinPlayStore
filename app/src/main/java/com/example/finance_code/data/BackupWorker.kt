package com.example.finance_code.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.services.drive.DriveScopes
import com.google.android.gms.common.api.Scope
import com.google.firebase.auth.FirebaseAuth

class BackupWorker(val context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        if (currentUser == null || currentUser.email == null) {
            return Result.failure()
        }

        val userEmail = currentUser.email!!
        val userUid = currentUser.uid

        val googleAccount = GoogleSignIn.getLastSignedInAccount(context)
        if (googleAccount == null) {
            return Result.failure()
        }

        val requiredScope = Scope(DriveScopes.DRIVE_APPDATA)
        if (!googleAccount.grantedScopes.contains(requiredScope)) {
            return Result.failure()
        }

        try {
            AppDB.checkpointAndClose(context, userEmail)
        } catch (e: Exception) {
            return Result.retry()
        }

        try {
            val dbIdentifier = userEmail.replace(Regex("[^a-zA-Z0-9]"), "_")
            val driveService = DriveService(context, googleAccount, dbIdentifier)

            val fileId = driveService.uploadFullBackup(userEmail, userUid)

            return if (fileId != null) {
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            return Result.retry()
        }
    }
}