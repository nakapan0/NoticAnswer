package com.example.noticanswer

import android.content.Context
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "questions")
data class QuestionEntity(
    @PrimaryKey
    val id: Int,

    val folderId: Long,

    val promptText: String,
    val correctAnswer: String,
    val aliasesText: String = "",
    val explanation: String = "",
    val imageResName: String,
    val imagePath: String = "",
    val enabled: Boolean = true
)

fun QuestionEntity.toQuestion(context: Context): Question {
    val imageResId = context.resources.getIdentifier(
        imageResName,
        "drawable",
        context.packageName
    ).let { resId ->
        if (resId != 0) resId else android.R.drawable.ic_dialog_info
    }

    val aliases = aliasesText
        .split("|")
        .map { it.trim() }
        .filter { it.isNotEmpty() }

    return Question(
        id = id,
        folderId = folderId,
        promptText = promptText,
        correctAnswer = correctAnswer,
        aliases = aliases,
        explanation = explanation,
        imageResId = imageResId,
        imageResName = imageResName,
        imagePath = imagePath,
        enabled = enabled
    )
}