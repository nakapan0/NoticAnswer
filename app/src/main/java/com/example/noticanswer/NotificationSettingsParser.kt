package com.example.noticanswer

fun parseNotificationSettingsOrNull(
    startHourText: String,
    endHourText: String,
    countText: String,
    questionsPerSessionText: String,
    minIntervalText: String,
    autoEnabled: Boolean,
    quietHoursEnabled: Boolean = false,
    quietStartHourText: String = "22",
    quietEndHourText: String = "6"
): NotificationSettings? {
    val startHour = startHourText.toIntOrNull() ?: return null
    val endHour = endHourText.toIntOrNull() ?: return null
    val count = countText.toIntOrNull() ?: return null
    val questionsPerSession = questionsPerSessionText.toIntOrNull() ?: return null
    val minIntervalMinutes = minIntervalText.toIntOrNull() ?: return null
    val quietStartHour = quietStartHourText.toIntOrNull() ?: return null
    val quietEndHour = quietEndHourText.toIntOrNull() ?: return null

    if (startHour !in 0..23) return null
    if (endHour !in 0..23) return null
    if (startHour >= endHour) return null

    if (count !in 0..10) return null
    if (questionsPerSession !in 1..10) return null

    if (minIntervalMinutes < 1) return null

    if (quietStartHour !in 0..23) return null
    if (quietEndHour !in 0..23) return null
    if (quietStartHour == quietEndHour) return null

    return NotificationSettings(
        autoEnabled = autoEnabled,
        startHour = startHour,
        endHour = endHour,
        count = count,
        questionsPerSession = questionsPerSession,
        minIntervalMinutes = minIntervalMinutes,
        quietHoursEnabled = quietHoursEnabled,
        quietStartHour = quietStartHour,
        quietEndHour = quietEndHour
    )
}