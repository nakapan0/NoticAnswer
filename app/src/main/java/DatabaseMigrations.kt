package com.example.noticanswer

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            ALTER TABLE questions
            ADD COLUMN show_image INTEGER NOT NULL DEFAULT 0
        """.trimIndent())
    }
}