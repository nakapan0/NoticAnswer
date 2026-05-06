package com.example.noticanswer

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class NotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val settings = SettingsRepository.getSettings(applicationContext)

            if (shouldSkipScheduledNotification(settings)) {
                return Result.success()
            }

            showNotification(
                context = applicationContext,
                questionsPerSession = settings.questionsPerSession
            )

            Result.success()
        } catch (_: Exception) {
            Result.success()
        }
    }
}