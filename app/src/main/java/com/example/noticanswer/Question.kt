package com.example.noticanswer

data class Question(
    val id: Int,
    val folderId: Long,
    val promptText: String,
    val correctAnswer: String,
    val aliases: List<String> = emptyList(),
    val explanation: String = "",
    val imageResId: Int,
    val imageResName: String = "",
    val imagePath: String = "",
    val enabled: Boolean = true
)