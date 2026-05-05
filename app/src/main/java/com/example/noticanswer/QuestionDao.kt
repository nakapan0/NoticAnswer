package com.example.noticanswer

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface QuestionDao {

    @Query("SELECT * FROM questions ORDER BY id ASC")
    suspend fun getAllQuestions(): List<QuestionEntity>

    @Query("SELECT * FROM questions WHERE folderId = :folderId ORDER BY id ASC")
    suspend fun getQuestionsByFolder(folderId: Long): List<QuestionEntity>

    @Query(
        """
        SELECT q.* FROM questions q
        INNER JOIN question_folders sub_genre
            ON q.folderId = sub_genre.id
        INNER JOIN question_folders genre
            ON sub_genre.parentFolderId = genre.id
        WHERE q.enabled = 1
          AND sub_genre.enabled = 1
          AND genre.enabled = 1
          AND sub_genre.kind = 'SUB_GENRE'
          AND genre.kind = 'GENRE'
        ORDER BY q.id ASC
        """
    )
    suspend fun getEnabledQuestionsForNotification(): List<QuestionEntity>

    @Query("SELECT * FROM questions WHERE id = :id LIMIT 1")
    suspend fun findById(id: Int): QuestionEntity?

    @Query("UPDATE questions SET enabled = :enabled WHERE id = :id")
    suspend fun updateEnabled(id: Int, enabled: Boolean)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(question: QuestionEntity)

    @Query("SELECT COALESCE(MAX(id), 0) FROM questions")
    suspend fun getMaxQuestionId(): Int

    @Query(
        """
        UPDATE questions
        SET promptText = :promptText,
            correctAnswer = :correctAnswer,
            aliasesText = :aliasesText,
            explanation = :explanation,
            imageResName = :imageResName,
            imagePath = :imagePath
        WHERE id = :id
        """
    )
    suspend fun updateQuestionContent(
        id: Int,
        promptText: String,
        correctAnswer: String,
        aliasesText: String,
        explanation: String,
        imageResName: String,
        imagePath: String
    )
    @Query("""
        UPDATE questions
        SET show_image = :showImageInNotification
        WHERE id = :id
    """)
    suspend fun updateShowImage(
        id: Int,
        showImageInNotification: Boolean
    )

    @Query("DELETE FROM questions WHERE id = :id")
    suspend fun deleteQuestionById(id: Int)

    @Query("SELECT id FROM questions WHERE folderId IN (:folderIds)")
    suspend fun getQuestionIdsByFolderIds(folderIds: List<Long>): List<Int>

    @Query("DELETE FROM questions WHERE folderId IN (:folderIds)")
    suspend fun deleteQuestionsByFolderIds(folderIds: List<Long>)

    @Query("SELECT * FROM questions WHERE folderId IN (:folderIds)")
    suspend fun getQuestionsByFolderIds(folderIds: List<Long>): List<QuestionEntity>

    @Query("SELECT COUNT(*) FROM questions WHERE folderId = :folderId")
    suspend fun getQuestionCountByFolder(folderId: Long): Int

    @Query("""
    SELECT COUNT(*)
    FROM questions
    WHERE folderId IN (
        SELECT id FROM question_folders WHERE parentFolderId = :genreId
    )
""")
    suspend fun getQuestionCountByGenre(genreId: Long): Int
}