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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.fillMaxWidth

@Composable
fun GenreListScreen(
    onBack: () -> Unit,
    onOpenGenre: (QuestionFolderEntity) -> Unit
) {
    val context = LocalContext.current.applicationContext
    val coroutineScope = rememberCoroutineScope()

    var genreItems by remember { mutableStateOf<List<FolderDisplayItem>>(emptyList()) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newGenreName by remember { mutableStateOf("新しいフォルダー") }
    var statusMessage by remember { mutableStateOf("") }
    var deleteMode by remember { mutableStateOf(false) }
    var deleteTargetFolder by remember { mutableStateOf<QuestionFolderEntity?>(null) }
    var exportTargetGenre by remember { mutableStateOf<QuestionFolderEntity?>(null) }
    var showExportGenreDialog by remember { mutableStateOf(false) }

    fun refresh() {
        coroutineScope.launch {
            genreItems = loadGenreItems(context)
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        val target = exportTargetGenre

        if (uri == null || target == null) {
            exportTargetGenre = null
            return@rememberLauncherForActivityResult
        }

        coroutineScope.launch {
            try {
                exportGenreToZip(
                    context = context,
                    genreId = target.id,
                    outputUri = uri
                )

                statusMessage = "エクスポートしました: ${target.name}"
            } catch (_: Exception) {
                statusMessage = "エクスポートに失敗しました"
            } finally {
                exportTargetGenre = null
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) {
            return@rememberLauncherForActivityResult
        }

        coroutineScope.launch {
            try {
                importGenreZipAsNewGenre(
                    context = context,
                    inputUri = uri
                )

                statusMessage = "インポートしました"
                refresh()
            } catch (_: Exception) {
                statusMessage = "インポートに失敗しました"
            }
        }
    }

    LaunchedEffect(Unit) {
        refresh()
    }

    if (showCreateDialog) {
        CreateFolderDialog(
            title = "ジャンルを作成",
            name = newGenreName,
            onNameChange = { newGenreName = it },
            onDismiss = {
                showCreateDialog = false
                newGenreName = "新しいフォルダー"
            },
            onConfirm = {
                val name = newGenreName.trim()

                if (name.isEmpty()) {
                    statusMessage = "ジャンル名を入力してください"
                    return@CreateFolderDialog
                }

                coroutineScope.launch {
                    QuestionRepository.addGenre(
                        context = context,
                        name = name
                    )

                    showCreateDialog = false
                    newGenreName = "新しいフォルダー"
                    statusMessage = "ジャンルを作成しました: $name"
                    refresh()
                }
            }
        )
    }

    if (showExportGenreDialog) {
        AlertDialog(
            onDismissRequest = {
                showExportGenreDialog = false
            },
            title = {
                Text("エクスポートするジャンル")
            },
            text = {
                Column {
                    genreItems.forEach { item ->
                        TextButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                exportTargetGenre = item.folder
                                showExportGenreDialog = false

                                exportLauncher.launch(
                                    buildGenreExportFileName(item.folder.name)
                                )
                            }
                        ) {
                            Text(item.folder.name)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = {
                    showExportGenreDialog = false
                }) {
                    Text("キャンセル")
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
                Text("ジャンルを削除しますか？")
            },
            text = {
                Text(
                    """
                ${target.name} を削除します。
                配下のサブジャンル、問題、回答履歴も削除されます。
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
                            statusMessage = "ジャンルを削除しました: ${target.name}"
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
                    title = "ジャンル",
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

                if (genreItems.isEmpty()) {
                    EmptyStateMessage(
                        title = "まだジャンルがありません",
                        message = "右上の＋からジャンルを作成できます。\nまたは下部のインポートから追加できます。"
                    )
                } else {
                    genreItems.forEach { item ->
                        FolderListRow(
                            item = item,
                            deleteMode = deleteMode,
                            onClick = {
                                onOpenGenre(item.folder)
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

                if (statusMessage.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(statusMessage)
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            Surface(
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        onClick = {
                            importLauncher.launch(
                                arrayOf(
                                    "application/zip",
                                    "application/octet-stream",
                                    "*/*"
                                )
                            )
                        }
                    ) {
                        Text("インポート")
                    }

                    Button(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        onClick = {
                            if (genreItems.isEmpty()) {
                                statusMessage = "エクスポートできるジャンルがありません"
                            } else {
                                showExportGenreDialog = true
                            }
                        }
                    ) {
                        Text("エクスポート")
                    }
                }
            }
        }
    }
}

private suspend fun loadGenreItems(
    context: Context
): List<FolderDisplayItem> {
    val db = AppDatabase.getDatabase(context)
    val answerLogDao = db.answerLogDao()

    return QuestionRepository.getGenres(context).map { folder ->
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