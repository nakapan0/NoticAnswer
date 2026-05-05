package com.example.noticanswer

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class DailyScheduleWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val settings = SettingsRepository.getSettings(applicationContext)

        if (!settings.autoEnabled) {
            return Result.success()
        }

        scheduleDailyRandomNotifications(
            context = applicationContext,
            startHour = settings.startHour,
            endHour = settings.endHour,
            count = settings.count,
            minIntervalMinutes = settings.minIntervalMinutes,
            showToast = false
        )

        return Result.success()
    }
}