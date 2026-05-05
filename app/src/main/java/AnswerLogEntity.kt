package com.example.noticanswer

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "answer_logs")
data class AnswerLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val questionId: Int,
    val userAnswer: String,
    val isCorrect: Boolean,
    val answeredAtMillis: Long
)