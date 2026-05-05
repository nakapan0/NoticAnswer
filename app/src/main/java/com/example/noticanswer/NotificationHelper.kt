package com.example.noticanswer

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat

@SuppressLint("MissingPermission")
suspend fun showNotification(context: Context) {
    val channelId = ReplyReceiver.CHANNEL_ID

    val manager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    val channel = NotificationChannel(
        channelId,
        "Question Channel",
        NotificationManager.IMPORTANCE_DEFAULT
    )
    manager.createNotificationChannel(channel)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        return
    }

    val question = QuestionRepository.getRandomQuestion(context)

    val remoteInput = RemoteInput.Builder(ReplyReceiver.KEY_REPLY)
        .setLabel("答えを入力")
        .build()

    val intent = Intent(context, ReplyReceiver::class.java).apply {
        putExtra(ReplyReceiver.EXTRA_QUESTION_ID, question.id)
    }

    val pendingIntent = PendingIntent.getBroadcast(
        context,
        question.id,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
    )

    val action = NotificationCompat.Action.Builder(
        android.R.drawable.ic_menu_send,
        "回答する",
        pendingIntent
    )
        .addRemoteInput(remoteInput)
        .build()

    val bitmap = loadQuestionBitmap(
        context = context,
        question = question
    )

    val builder = NotificationCompat.Builder(context, channelId)
        .setContentTitle(question.promptText)
        .setContentText("通知上で回答してください")
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .addAction(action)

    if (bitmap != null) {
        builder
            .setLargeIcon(bitmap)
            .setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(bitmap)
                    .bigLargeIcon(null as android.graphics.Bitmap?)
            )
    }

    manager.notify(
        ReplyReceiver.NOTIFICATION_ID,
        builder.build()
    )
}