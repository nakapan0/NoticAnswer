package com.example.noticanswer

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "question_folders")
data class QuestionFolderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val parentFolderId: Long? = null,
    val name: String,
    val enabled: Boolean = true,

    val kind: String = FolderKind.GENRE,

    val reminderCount: Int = 3,
    val reminderMode: String = "RANDOM",
    val startHour: Int = 10,
    val endHour: Int = 22,
    val minIntervalMinutes: Int = 30
)