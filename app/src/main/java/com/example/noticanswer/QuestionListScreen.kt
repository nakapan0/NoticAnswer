package com.example.noticanswer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

data class QuestionDisplayItem(
    val question: Question,
    val total: Int,
    val correct: Int,
    val rate: Int
)

@Composable
fun QuestionListScreen(
    subGenreId: Long,
    subGenreName: String,
    onBack: () -> Unit,
    onCreateQuestion: () -> Unit,
    onEditQuestion: (Question) -> Unit
) {
    val context = LocalContext.current.applicationContext
    val coroutineScope = rememberCoroutineScope()

    var questionItems by remember { mutableStateOf<List<QuestionDisplayItem>>(emptyList()) }
    var statusMessage by remember { mutableStateOf("") }

    var deleteMode by remember { mutableStateOf(false) }
    var deleteTargetQuestion by remember { mutableStateOf<Question?>(null) }

    fun refresh() {
        coroutineScope.launch {
            val db = AppDatabase.getDatabase(context)
            val answerLogDao = db.answerLogDao()

            val questions = QuestionRepository.getQuestionsByFolder(
                context = context,
                folderId = subGenreId
            )

            questionItems = questions.map { question ->
                val total = answerLogDao.getCountByQuestion(question.id)
                val correct = answerLogDao.getCorrectCountByQuestion(question.id)
                val rate = if (total == 0) 0 else correct * 100 / total

                QuestionDisplayItem(
                    question = question,
                    total = total,
                    correct = correct,
                    rate = rate
                )
            }
        }
    }

    LaunchedEffect(subGenreId) {
        refresh()
    }

    deleteTargetQuestion?.let { target ->
        AlertDialog(
            onDismissRequest = {
                deleteTargetQuestion = null
            },
            title = {
                Text("問題を削除しますか？")
            },
            text = {
                Text(
                    """
                    ${target.correctAnswer} を削除します。
                    この問題の回答履歴も削除されます。
                    """.trimIndent()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            QuestionRepository.deleteQuestion(
                                context = context,
                                questionId = target.id
                            )

                            deleteTargetQuestion = null
                            statusMessage = "問題を削除しました: ${target.correctAnswer}"
                            refresh()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD32F2F),
                        contentColor = Color.White
                    )
                ) {
                    Text("削除")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    deleteTargetQuestion = null
                }) {
                    Text("キャンセル")
                }
            }
        )
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                ExplorerHeader(
                    title = subGenreName,
                    onBack = onBack,
                    onAdd = null,
                    onDeleteModeToggle = {
                        deleteMode = !deleteMode
                    },
                    deleteMode = deleteMode
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (questionItems.isEmpty()) {
                    EmptyStateMessage(
                        title = "まだ問題がありません",
                        message = "下の「新規問題作成」から問題を追加できます。"
                    )
                } else {
                    questionItems.forEach { item ->
                        QuestionListRow(
                            item = item,
                            deleteMode = deleteMode,
                            onClick = {
                                onEditQuestion(item.question)
                            },
                            onDelete = {
                                deleteTargetQuestion = item.question
                            },
                            onCheckedChange = { checked ->
                                coroutineScope.launch {
                                    QuestionRepository.setQuestionEnabled(
                                        context = context,
                                        questionId = item.question.id,
                                        enabled = checked
                                    )
                                    statusMessage =
                                        "${item.question.correctAnswer}を${if (checked) "出題ON" else "出題OFF"}にしました"
                                    refresh()
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(statusMessage)

                Spacer(modifier = Modifier.height(16.dp))
            }

            Surface(
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp),
                        onClick = onCreateQuestion
                    ) {
                        Text(
                            text = "新規問題作成",
                            fontSize = 20.sp
                        )
                    }
                }
            }
        }
    }
}