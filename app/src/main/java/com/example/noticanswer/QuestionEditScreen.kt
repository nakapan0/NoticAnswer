package com.example.noticanswer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
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
import kotlinx.coroutines.launch
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

@Composable
fun QuestionEditScreen(
    questionId: Int,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current.applicationContext
    val coroutineScope = rememberCoroutineScope()

    var loaded by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }

    var promptText by remember { mutableStateOf("") }
    var correctAnswer by remember { mutableStateOf("") }
    var aliasesText by remember { mutableStateOf("") }
    var explanation by remember { mutableStateOf("") }
    var imageResName by remember { mutableStateOf("") }
    var imagePath by remember { mutableStateOf("") }

    LaunchedEffect(questionId) {
        val question = QuestionRepository.findById(context, questionId)

        if (question == null) {
            statusMessage = "問題が見つかりませんでした"
            loaded = true
            return@LaunchedEffect
        }

        promptText = question.promptText
        correctAnswer = question.correctAnswer
        aliasesText = question.aliases.joinToString("|")
        explanation = question.explanation
        imageResName = question.imageResName
        loaded = true
        imagePath = question.imagePath
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
            },
            title = {
                Text("この問題を削除しますか？")
            },
            text = {
                Text("この問題の回答履歴も一緒に削除されます。")
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            QuestionRepository.deleteQuestion(
                                context = context,
                                questionId = questionId
                            )
                            showDeleteDialog = false
                            onSaved()
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
                    showDeleteDialog = false
                }) {
                    Text("キャンセル")
                }
            }
        )
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult

        coroutineScope.launch {
            try {
                imagePath = saveQuestionImageToInternalStorage(
                    context = context,
                    sourceUri = uri
                )
                statusMessage = "画像を変更しました"
            } catch (_: Exception) {
                statusMessage = "画像の保存に失敗しました"
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        ExplorerHeader(
            title = "問題編集",
            onBack = onBack,
            onAdd = null
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (!loaded) {
            Text("読み込み中...")
            return@Column
        }

        QuestionImagePreviewBox(
            imagePath = imagePath,
            imageResName = imageResName,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                imagePickerLauncher.launch("image/*")
            }
        ) {
            Text("画像を選択")
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = promptText,
            onValueChange = { promptText = it },
            label = { Text("問題文") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = correctAnswer,
            onValueChange = { correctAnswer = it },
            label = { Text("正解") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = aliasesText,
            onValueChange = { aliasesText = it },
            label = { Text("別解（| 区切り）") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = explanation,
            onValueChange = { explanation = it },
            label = { Text("解説") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val prompt = promptText.trim()
                val correct = correctAnswer.trim()

                if (prompt.isEmpty() || correct.isEmpty()) {
                    statusMessage = "問題文・正解は必須です"
                    return@Button
                }

                coroutineScope.launch {
                    QuestionRepository.updateQuestion(
                        context = context,
                        questionId = questionId,
                        promptText = prompt,
                        correctAnswer = correct,
                        aliasesText = aliasesText.trim(),
                        explanation = explanation.trim(),
                        imageResName = imageResName,
                        imagePath = imagePath
                    )
                    onSaved()
                }
            }
        ) {
            Text("保存")
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                showDeleteDialog = true
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFD32F2F),
                contentColor = Color.White
            )
        ) {
            Text("削除")
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(statusMessage)
    }
}