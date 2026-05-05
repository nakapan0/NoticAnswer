package com.example.noticanswer

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.Color

@Composable
fun SubGenreListScreen(
    genreId: Long,
    genreName: String,
    onBack: () -> Unit,
    onOpenSubGenre: (QuestionFolderEntity) -> Unit
) {
    val context = LocalContext.current.applicationContext
    val coroutineScope = rememberCoroutineScope()

    var subGenreItems by remember { mutableStateOf<List<FolderDisplayItem>>(emptyList()) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newSubGenreName by remember { mutableStateOf("新しいフォルダー") }
    var statusMessage by remember { mutableStateOf("") }
    var deleteMode by remember { mutableStateOf(false) }
    var deleteTargetFolder by remember { mutableStateOf<QuestionFolderEntity?>(null) }

    fun refresh() {
        coroutineScope.launch {
            subGenreItems = loadSubGenreItems(
                context = context,
                genreId = genreId
            )
        }
    }

    LaunchedEffect(genreId) {
        refresh()
    }

    if (showCreateDialog) {
        CreateFolderDialog(
            title = "サブジャンルを作成",
            name = newSubGenreName,
            onNameChange = { newSubGenreName = it },
            onDismiss = {
                showCreateDialog = false
                newSubGenreName = "新しいフォルダー"
            },
            onConfirm = {
                val name = newSubGenreName.trim()

                if (name.isEmpty()) {
                    statusMessage = "サブジャンル名を入力してください"
                    return@CreateFolderDialog
                }

                coroutineScope.launch {
                    QuestionRepository.addSubGenre(
                        context = context,
                        genreId = genreId,
                        name = name
                    )

                    showCreateDialog = false
                    newSubGenreName = "新しいフォルダー"
                    statusMessage = "サブジャンルを作成しました: $name"
                    refresh()
                }
            }
        )
    }

    deleteTargetFolder?.let { target ->
        AlertDialog(
            onDismissRequest = {
                deleteTargetFolder = null
            },
            title = {
                Text("サブジャンルを削除しますか？")
            },
            text = {
                Text(
                    """
                ${target.name} を削除します。
                配下の問題と回答履歴も削除されます。
                """.trimIndent()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            QuestionRepository.deleteFolderTree(
                                context = context,
                                folderId = target.id
                            )

                            deleteTargetFolder = null
                            statusMessage = "サブジャンルを削除しました: ${target.name}"
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
                    deleteTargetFolder = null
                }) {
                    Text("キャンセル")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        ExplorerHeader(
            title = genreName,
            onBack = onBack,
            onAdd = {
                showCreateDialog = true
            },
            onDeleteModeToggle = {
                deleteMode = !deleteMode
            },
            deleteMode = deleteMode
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (subGenreItems.isEmpty()) {
            EmptyStateMessage(
                title = "まだサブジャンルがありません",
                message = "右上の＋からサブジャンルを作成できます。"
            )
        } else {
            subGenreItems.forEach { item ->
                FolderListRow(
                    item = item,
                    deleteMode = deleteMode,
                    onClick = {
                        onOpenSubGenre(item.folder)
                    },
                    onDelete = {
                        deleteTargetFolder = item.folder
                    },
                    onCheckedChange = { checked ->
                        coroutineScope.launch {
                            QuestionRepository.setFolderEnabled(
                                context = context,
                                folderId = item.folder.id,
                                enabled = checked
                            )
                            refresh()
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(statusMessage)
    }
}

private suspend fun loadSubGenreItems(
    context: Context,
    genreId: Long
): List<FolderDisplayItem> {
    val db = AppDatabase.getDatabase(context)
    val answerLogDao = db.answerLogDao()

    return QuestionRepository.getSubGenres(
        context = context,
        genreId = genreId
    ).map { folder ->
        val total = answerLogDao.getCountByFolder(folder.id)
        val correct = answerLogDao.getCorrectCountByFolder(folder.id)
        val rate = if (total == 0) 0 else correct * 100 / total

        FolderDisplayItem(
            folder = folder,
            total = total,
            correct = correct,
            rate = rate
        )
    }
}