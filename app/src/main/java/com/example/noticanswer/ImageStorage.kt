package com.example.noticanswer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.util.UUID

fun saveQuestionImageToInternalStorage(
    context: Context,
    sourceUri: Uri
): String {
    val dir = File(context.filesDir, "question_images")

    if (!dir.exists()) {
        dir.mkdirs()
    }

    val file = File(
        dir,
        "question_${System.currentTimeMillis()}_${UUID.randomUUID()}.img"
    )

    val inputStream = context.contentResolver.openInputStream(sourceUri)
        ?: error("画像を読み込めませんでした")

    inputStream.use { input ->
        file.outputStream().use { output ->
            input.copyTo(output)
        }
    }

    return file.absolutePath
}

fun loadQuestionBitmap(
    context: Context,
    question: Question
): Bitmap? {
    return loadQuestionBitmap(
        context = context,
        imagePath = question.imagePath,
        imageResName = question.imageResName,
        fallbackResId = question.imageResId
    )
}

fun loadQuestionBitmap(
    context: Context,
    imagePath: String,
    imageResName: String,
    fallbackResId: Int = android.R.drawable.ic_dialog_info
): Bitmap? {
    if (imagePath.isNotBlank()) {
        val file = File(imagePath)

        if (file.exists()) {
            BitmapFactory.decodeFile(file.absolutePath)?.let { bitmap ->
                return bitmap
            }
        }
    }

    if (imageResName.isNotBlank()) {
        val resId = context.resources.getIdentifier(
            imageResName,
            "drawable",
            context.packageName
        )

        if (resId != 0) {
            BitmapFactory.decodeResource(context.resources, resId)?.let { bitmap ->
                return bitmap
            }
        }
    }

    return BitmapFactory.decodeResource(context.resources, fallbackResId)
}

fun deleteQuestionImageFileIfExists(
    context: Context,
    imagePath: String
) {
    if (imagePath.isBlank()) {
        return
    }

    val imageFile = File(imagePath)
    val questionImageDir = File(context.filesDir, "question_images")

    try {
        val canonicalImageFile = imageFile.canonicalFile
        val canonicalImageDir = questionImageDir.canonicalFile

        val isInsideQuestionImageDir = canonicalImageFile.path
            .startsWith(canonicalImageDir.path)

        if (!isInsideQuestionImageDir) {
            return
        }

        if (canonicalImageFile.exists()) {
            canonicalImageFile.delete()
        }
    } catch (_: Exception) {
        // 画像削除に失敗しても、問題削除自体は止めない
    }
}