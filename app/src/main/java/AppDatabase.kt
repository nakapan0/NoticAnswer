package com.example.noticanswer

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        AnswerLogEntity::class,
        QuestionEntity::class,
        QuestionFolderEntity::class
    ],
    version = 8
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun answerLogDao(): AnswerLogDao
    abstract fun questionDao(): QuestionDao
    abstract fun questionFolderDao(): QuestionFolderDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "notic_answer_database"
                )
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}