package com.example.noticanswer

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color

@Composable
fun QuestionImagePreviewBox(
    imagePath: String,
    imageResName: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val bitmap = remember(imagePath, imageResName) {
        loadQuestionBitmap(
            context = context,
            imagePath = imagePath,
            imageResName = imageResName
        )
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = 1.dp,
                color = Color.LightGray,
                shape = RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),
                contentScale = ContentScale.Fit
            )
        } else {
            Text(
                text = "画像なし",
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun QuestionImagePreviewBox(
    question: Question,
    modifier: Modifier = Modifier
) {
    QuestionImagePreviewBox(
        imagePath = question.imagePath,
        imageResName = question.imageResName,
        modifier = modifier
    )
}