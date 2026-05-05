package com.example.noticanswer

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color

@Composable
fun ExplorerHeader(
    title: String,
    onBack: (() -> Unit)? = null,
    onAdd: (() -> Unit)? = null,
    onDeleteModeToggle: (() -> Unit)? = null,
    deleteMode: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.width(112.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (onBack != null) {
                TextButton(onClick = onBack) {
                    Text(
                        text = "←",
                        fontSize = 28.sp
                    )
                }
            }
        }

        Text(
            text = title,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Row(
            modifier = Modifier.width(112.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onDeleteModeToggle != null) {
                TextButton(onClick = onDeleteModeToggle) {
                    Text(
                        text = if (deleteMode) "完了" else "🗑",
                        fontSize = if (deleteMode) 16.sp else 22.sp,
                        color = if (deleteMode) Color(0xFFD32F2F) else Color.Unspecified,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (onAdd != null) {
                TextButton(onClick = onAdd) {
                    Text(
                        text = "+",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun FolderListRow(
    item: FolderDisplayItem,
    deleteMode: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable {
                if (!deleteMode) {
                    onClick()
                }
            },
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "📁",
                fontSize = 40.sp
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.folder.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                StatRow(
                    total = item.total,
                    rate = item.rate
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Checkbox(
                checked = item.folder.enabled,
                onCheckedChange = onCheckedChange
            )

            if (deleteMode) {
                Text(
                    text = "削\n除",
                    color = Color(0xFFD32F2F),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .width(40.dp)
                        .clickable {
                            onDelete()
                        }
                )
            } else {
                Text(
                    text = "›",
                    fontSize = 32.sp,
                    modifier = Modifier.width(40.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun QuestionListRow(
    item: QuestionDisplayItem,
    deleteMode: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable {
                if (!deleteMode) {
                    onClick()
                }
            },
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 78.dp, height = 58.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(
                        width = 1.dp,
                        color = androidx.compose.ui.graphics.Color.LightGray,
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                QuestionImagePreviewBox(
                    question = item.question,
                    modifier = Modifier.size(width = 78.dp, height = 58.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.question.correctAnswer,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(0.45f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = item.question.promptText,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(0.55f)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                StatRow(
                    total = item.total,
                    rate = item.rate
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Checkbox(
                checked = item.question.enabled,
                onCheckedChange = onCheckedChange
            )

            if (deleteMode) {
                Text(
                    text = "削\n除",
                    color = Color(0xFFD32F2F),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .width(40.dp)
                        .clickable {
                            onDelete()
                        }
                )
            }
        }
    }
}

@Composable
fun StatRow(
    total: Int,
    rate: Int
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "回答数: $total",
            fontSize = 12.sp,
            maxLines = 1
        )

        Text(
            text = "正解率: $rate%",
            fontSize = 12.sp,
            maxLines = 1
        )
    }
}

@Composable
fun CreateFolderDialog(
    title: String,
    name: String,
    onNameChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("フォルダ名") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("キャンセル")
            }
        }
    )
}

@Composable
fun EmptyStateMessage(
    title: String,
    message: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = message,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
    }
}