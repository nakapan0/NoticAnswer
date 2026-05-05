package com.example.noticanswer

data class FolderDisplayItem(
    val folder: QuestionFolderEntity,
    val total: Int,
    val correct: Int,
    val rate: Int
)