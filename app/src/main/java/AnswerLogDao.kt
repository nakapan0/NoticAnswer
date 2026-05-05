package com.example.noticanswer

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface AnswerLogDao {

    @Insert
    suspend fun insert(log: AnswerLogEntity)

    @Query("SELECT COUNT(*) FROM answer_logs")
    suspend fun getTotalCount(): Int

    @Query("SELECT COUNT(*) FROM answer_logs WHERE isCorrect = 1")
    suspend fun getCorrectCount(): Int

    @Query("SELECT COUNT(*) FROM answer_logs WHERE questionId = :questionId")
    suspend fun getCountByQuestion(questionId: Int): Int

    @Query("SELECT COUNT(*) FROM answer_logs WHERE questionId = :questionId AND isCorrect = 1")
    suspend fun getCorrectCountByQuestion(questionId: Int): Int

    @Query(
        """
    WITH RECURSIVE descendants(id) AS (
        SELECT id FROM question_folders WHERE id = :folderId
        UNION ALL
        SELECT qf.id
        FROM question_folders qf
        INNER JOIN descendants d
            ON qf.parentFolderId = d.id
    )
    SELECT COUNT(*) FROM answer_logs
    INNER JOIN questions
        ON answer_logs.questionId = questions.id
    WHERE questions.folderId IN (SELECT id FROM descendants)
    """
    )
    suspend fun getCountByFolder(folderId: Long): Int

    @Query(
        """
    WITH RECURSIVE descendants(id) AS (
        SELECT id FROM question_folders WHERE id = :folderId
        UNION ALL
        SELECT qf.id
        FROM question_folders qf
        INNER JOIN descendants d
            ON qf.parentFolderId = d.id
    )
    SELECT COUNT(*) FROM answer_logs
    INNER JOIN questions
        ON answer_logs.questionId = questions.id
    WHERE questions.folderId IN (SELECT id FROM descendants)
      AND answer_logs.isCorrect = 1
    """
    )
    suspend fun getCorrectCountByFolder(folderId: Long): Int

    @Query("DELETE FROM answer_logs WHERE questionId = :questionId")
    suspend fun deleteByQuestionId(questionId: Int)

    @Query("DELETE FROM answer_logs WHERE questionId IN (:questionIds)")
    suspend fun deleteByQuestionIds(questionIds: List<Int>)
}