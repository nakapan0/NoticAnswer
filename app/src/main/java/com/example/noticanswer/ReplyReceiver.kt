package com.example.noticanswer

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
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
        when (intent.action) {
            ACTION_NEXT -> {
                handleNextQuestion(context, intent)
                return
            }

            ACTION_OK -> {
                closeNotification(context)
                return
            }
        }

        val replyText = RemoteInput.getResultsFromIntent(intent)
            ?.getCharSequence(KEY_REPLY)
            ?.toString()
            ?: ""

        val questionId = intent.getIntExtra(EXTRA_QUESTION_ID, -1)

        val remainingQuestions = intent
            .getIntExtra(EXTRA_REMAINING_QUESTIONS, 1)
            .coerceAtLeast(1)

        val sessionIndex = intent
            .getIntExtra(EXTRA_SESSION_INDEX, 1)
            .coerceAtLeast(1)

        val sessionTotal = intent
            .getIntExtra(EXTRA_SESSION_TOTAL, remainingQuestions)
            .coerceAtLeast(1)

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
                    isCorrect = isCorrect,
                    remainingQuestions = remainingQuestions,
                    sessionIndex = sessionIndex,
                    sessionTotal = sessionTotal
                )
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun handleNextQuestion(
        context: Context,
        intent: Intent
    ) {
        val remainingQuestions = intent
            .getIntExtra(EXTRA_REMAINING_QUESTIONS, 1)
            .coerceAtLeast(1)

        val sessionIndex = intent
            .getIntExtra(EXTRA_SESSION_INDEX, 1)
            .coerceAtLeast(1)

        val sessionTotal = intent
            .getIntExtra(EXTRA_SESSION_TOTAL, remainingQuestions)
            .coerceAtLeast(1)

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                showNotification(
                    context = context.applicationContext,
                    questionsPerSession = sessionTotal,
                    remainingQuestions = remainingQuestions,
                    sessionIndex = sessionIndex,
                    sessionTotal = sessionTotal
                )
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun closeNotification(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(NOTIFICATION_ID)
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
            .replace(" ", "")
            .replace("　", "")
    }

    @SuppressLint("MissingPermission")
    private fun showResultNotification(
        context: Context,
        userAnswer: String,
        question: Question,
        isCorrect: Boolean,
        remainingQuestions: Int,
        sessionIndex: Int,
        sessionTotal: Int
    ) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Question Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        manager.createNotificationChannel(channel)

        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val titlePrefix = if (sessionTotal > 1) {
            "第${sessionIndex}/${sessionTotal}問 "
        } else {
            ""
        }

        val title = if (isCorrect) {
            "${titlePrefix}正解"
        } else {
            "${titlePrefix}不正解"
        }

        val body = if (isCorrect) {
            "あなたの回答：$userAnswer\n${question.explanation}"
        } else {
            "あなたの回答：$userAnswer\n正解：${question.correctAnswer}\n${question.explanation}"
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setSmallIcon(android.R.drawable.ic_dialog_info)

        if (remainingQuestions > 1) {
            val nextRemaining = remainingQuestions - 1
            val nextIndex = sessionIndex + 1

            val nextIntent = Intent(context, ReplyReceiver::class.java).apply {
                action = ACTION_NEXT
                putExtra(EXTRA_REMAINING_QUESTIONS, nextRemaining)
                putExtra(EXTRA_SESSION_INDEX, nextIndex)
                putExtra(EXTRA_SESSION_TOTAL, sessionTotal)
            }

            val nextPendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_NEXT + nextIndex,
                nextIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val nextAction = NotificationCompat.Action.Builder(
                android.R.drawable.ic_media_next,
                "次の問題へ",
                nextPendingIntent
            ).build()

            builder.addAction(nextAction)
        } else {
            val okIntent = Intent(context, ReplyReceiver::class.java).apply {
                action = ACTION_OK
            }

            val okPendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_OK,
                okIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val okAction = NotificationCompat.Action.Builder(
                android.R.drawable.ic_menu_close_clear_cancel,
                "OK",
                okPendingIntent
            ).build()

            builder.addAction(okAction)
        }

        manager.notify(
            NOTIFICATION_ID,
            builder.build()
        )
    }

    @SuppressLint("MissingPermission")
    private fun showErrorNotification(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

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
        const val EXTRA_REMAINING_QUESTIONS = "extra_remaining_questions"
        const val EXTRA_SESSION_INDEX = "extra_session_index"
        const val EXTRA_SESSION_TOTAL = "extra_session_total"

        const val ACTION_NEXT = "com.example.noticanswer.ACTION_NEXT"
        const val ACTION_OK = "com.example.noticanswer.ACTION_OK"

        const val CHANNEL_ID = "question_channel"
        const val NOTIFICATION_ID = 1

        private const val REQUEST_CODE_NEXT = 2000
        private const val REQUEST_CODE_OK = 3000
    }
}