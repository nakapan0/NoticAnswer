package com.example.noticanswer

import android.content.Context
import android.widget.Toast
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

fun scheduleDailyRandomNotifications(
    context: Context,
    startHour: Int,
    endHour: Int,
    count: Int,
    minIntervalMinutes: Int,
    showToast: Boolean = true
) {
    val workManager = WorkManager.getInstance(context)

    workManager.cancelAllWorkByTag("daily_random_notification")

    if (count <= 0) {
        if (showToast) {
            Toast.makeText(
                context,
                "通知回数が0回なので予約しません",
                Toast.LENGTH_LONG
            ).show()
        }
        return
    }

    val now = LocalDateTime.now()
    val today = LocalDate.now()

    var targetStart = LocalDateTime.of(today, LocalTime.of(startHour, 0))
    var targetEnd = LocalDateTime.of(today, LocalTime.of(endHour, 0))

    if (now.isAfter(targetEnd)) {
        val tomorrow = today.plusDays(1)
        targetStart = LocalDateTime.of(tomorrow, LocalTime.of(startHour, 0))
        targetEnd = LocalDateTime.of(tomorrow, LocalTime.of(endHour, 0))
    }

    if (now.isAfter(targetStart) && now.isBefore(targetEnd)) {
        targetStart = now.plusMinutes(1)
    }

    val notificationTimes = generateRandomDateTimes(
        start = targetStart,
        end = targetEnd,
        count = count,
        minIntervalMinutes = minIntervalMinutes
    )

    if (notificationTimes.isEmpty()) {
        if (showToast) {
            Toast.makeText(
                context,
                "条件に合う通知時刻を作れませんでした",
                Toast.LENGTH_LONG
            ).show()
        }
        return
    }

    val requests = notificationTimes.map { targetTime ->
        val delayMinutes = Duration.between(now, targetTime)
            .toMinutes()
            .coerceAtLeast(1)

        OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .addTag("daily_random_notification")
            .build()
    }

    workManager.enqueue(requests)

    if (showToast) {
        val message = notificationTimes.joinToString("\n") {
            "${it.toLocalDate()} ${it.toLocalTime()}"
        }

        Toast.makeText(
            context,
            "ランダム通知を予約:\n$message",
            Toast.LENGTH_LONG
        ).show()
    }
}

fun generateRandomDateTimes(
    start: LocalDateTime,
    end: LocalDateTime,
    count: Int,
    minIntervalMinutes: Int
): List<LocalDateTime> {
    val totalMinutes = Duration.between(start, end).toMinutes().toInt()

    if (totalMinutes <= 0) {
        return emptyList()
    }

    val candidates = (0..totalMinutes).toMutableList()
    val selected = mutableListOf<Int>()

    while (selected.size < count && candidates.isNotEmpty()) {
        val picked = candidates.random()
        selected.add(picked)

        candidates.removeAll { candidate ->
            kotlin.math.abs(candidate - picked) < minIntervalMinutes
        }
    }

    return selected
        .sorted()
        .map { offsetMinutes ->
            start.plusMinutes(offsetMinutes.toLong())
        }
}

fun startDailyAutoSchedule(
    context: Context,
    settings: NotificationSettings
) {
    scheduleDailyRandomNotifications(
        context = context,
        startHour = settings.startHour,
        endHour = settings.endHour,
        count = settings.count,
        minIntervalMinutes = settings.minIntervalMinutes,
        showToast = true
    )

    val delayMinutes = minutesUntilNextDailySetup()

    val request = PeriodicWorkRequestBuilder<DailyScheduleWorker>(
        1,
        TimeUnit.DAYS
    )
        .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
        .addTag("daily_schedule_worker")
        .build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "daily_schedule_worker",
        ExistingPeriodicWorkPolicy.REPLACE,
        request
    )

    Toast.makeText(
        context,
        "毎日自動通知をONにしました",
        Toast.LENGTH_LONG
    ).show()
}

fun minutesUntilNextDailySetup(): Long {
    val now = LocalDateTime.now()

    val nextSetupTime = LocalDateTime.of(
        LocalDate.now().plusDays(1),
        LocalTime.of(0, 5)
    )

    return Duration.between(now, nextSetupTime)
        .toMinutes()
        .coerceAtLeast(1)
}

fun stopDailyAutoSchedule(context: Context) {
    val workManager = WorkManager.getInstance(context)

    workManager.cancelUniqueWork("daily_schedule_worker")
    workManager.cancelAllWorkByTag("daily_random_notification")

    Toast.makeText(
        context,
        "毎日自動通知をOFFにしました",
        Toast.LENGTH_LONG
    ).show()
}