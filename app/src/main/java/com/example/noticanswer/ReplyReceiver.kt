package com.example.noticanswer

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReplyReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val replyText = RemoteInput.getResultsFromIntent(intent)
            ?.getCharSequence(KEY_REPLY)
            ?.toString()
            ?: ""

        val questionId = intent.getIntExtra(EXTRA_QUESTION_ID, -1)

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val appContext = context.applicationContext
                val question = QuestionRepository.findById(appContext, questionId)

                if (question == null) {
                    showErrorNotification(context)
                    return@launch
                }

                val isCorrect = isCorrectAnswer(replyText, question)

                val db = AppDatabase.getDatabase(appContext)

                db.answerLogDao().insert(
                    AnswerLogEntity(
                        questionId = question.id,
                        userAnswer = replyText,
                        isCorrect = isCorrect,
                        answeredAtMillis = System.currentTimeMillis()
                    )
                )

                showResultNotification(
                    context = context,
                    userAnswer = replyText,
                    question = question,
                    isCorrect = isCorrect
                )
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun isCorrectAnswer(userAnswer: String, question: Question): Boolean {
        val normalizedUserAnswer = normalize(userAnswer)

        val validAnswers = listOf(question.correctAnswer) + question.aliases

        return validAnswers.any { answer ->
            normalize(answer) == normalizedUserAnswer
        }
    }

    private fun normalize(text: String): String {
        return text
            .trim()
            .lowercase()
            .replace("　", "")
            .replace(" ", "")
    }

    @SuppressLint("MissingPermission")
    private fun showResultNotification(
        context: Context,
        userAnswer: String,
        question: Question,
        isCorrect: Boolean
    ) {
        val manager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            CHANNEL_ID,
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

        val title = if (isCorrect) {
            "正解"
        } else {
            "不正解"
        }

        val body = if (isCorrect) {
            "あなたの回答：$userAnswer\n${question.explanation}"
        } else {
            "あなたの回答：$userAnswer\n正解：${question.correctAnswer}\n${question.explanation}"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()

        manager.notify(NOTIFICATION_ID, notification)
    }

    @SuppressLint("MissingPermission")
    private fun showErrorNotification(context: Context) {
        val manager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("エラー")
            .setContentText("問題データが見つかりませんでした")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()

        manager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val KEY_REPLY = "key_reply"
        const val EXTRA_QUESTION_ID = "extra_question_id"
        const val CHANNEL_ID = "question_channel"
        const val NOTIFICATION_ID = 1
    }
}