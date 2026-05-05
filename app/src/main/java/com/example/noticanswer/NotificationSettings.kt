package com.example.noticanswer

data class NotificationSettings(
    val autoEnabled: Boolean = false,
    val startHour: Int = 10,
    val endHour: Int = 22,
    val count: Int = 3,
    val minIntervalMinutes: Int = 30,

    val quietHoursEnabled: Boolean = false,
    val quietStartHour: Int = 22,
    val quietEndHour: Int = 6
)