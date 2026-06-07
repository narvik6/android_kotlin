package com.bibo.android.core.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.koin.java.KoinJavaComponent

class SyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val syncManager: SyncManager = KoinJavaComponent.get(SyncManager::class.java)
        return if (syncManager.syncCurrentUser()) {
            Result.success()
        } else {
            Result.retry()
        }
    }
}
