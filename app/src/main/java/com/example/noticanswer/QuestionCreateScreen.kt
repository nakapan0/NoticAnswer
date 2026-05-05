package com.example.noticanswer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

@Composable
fun QuestionCreateScreen(
    subGenreId: Long,
    subGenreName: String,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current.applicationContext
    val coroutineScope = rememberCoroutineScope()

    var promptText by remember { mutableStateOf("この国旗は？") }
    var correctAnswer by remember { mutableStateOf("") }
    var aliasesText by remember { mutableStateOf("") }
    var explanation by remember { mutableStateOf("") }
    var imageResName by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf("") }
    var imagePath by remember { mutableStateOf("") }

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
                statusMessage = "画像を設定しました"
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
            title = "問題作成",
            onBack = onBack,
            onAdd = null
        )
        Text("保存先: $subGenreName")

        Spacer(modifier = Modifier.height(16.dp))

        Spacer(modifier = Modifier.height(16.dp))

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
            singleLine = false,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = correctAnswer,
            onValueChange = { correctAnswer = it },
            label = { Text("正解") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = aliasesText,
            onValueChange = { aliasesText = it },
            label = { Text("別解（| 区切り） 例: にほん|ニホン|japan") },
            singleLine = false,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = explanation,
            onValueChange = { explanation = it },
            label = { Text("解説") },
            singleLine = false,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

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
                    QuestionRepository.addQuestion(
                        context = context,
                        folderId = subGenreId,
                        promptText = prompt,
                        correctAnswer = correct,
                        aliasesText = aliasesText.trim(),
                        explanation = explanation.trim(),
                        imageResName = "",
                        imagePath = imagePath,
                        enabled = true
                    )

                    onSaved()
                }
            }
        ) {
            Text("保存")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(statusMessage)
    }
}