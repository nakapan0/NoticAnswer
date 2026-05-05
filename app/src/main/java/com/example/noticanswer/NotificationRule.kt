package com.example.noticanswer

import java.time.LocalDateTime

fun isInQuietHours(
    currentHour: Int,
    quietStartHour: Int,
    quietEndHour: Int
): Boolean {
    return if (quietStartHour < quietEndHour) {
        currentHour in quietStartHour until quietEndHour
    } else {
        currentHour in quietStartHour..23 || currentHour in 0 until quietEndHour
    }
}

fun shouldSkipScheduledNotification(
    settings: NotificationSettings,
    currentHour: Int = LocalDateTime.now().hour
): Boolean {
    if (!settings.quietHoursEnabled) {
        return false
    }

    return isInQuietHours(
        currentHour = currentHour,
        quietStartHour = settings.quietStartHour,
        quietEndHour = settings.quietEndHour
    )
}