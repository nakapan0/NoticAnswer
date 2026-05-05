package com.example.noticanswer

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface QuestionFolderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(folder: QuestionFolderEntity): Long

    @Query(
        """
        SELECT * FROM question_folders
        WHERE parentFolderId IS NULL
          AND kind = :kind
        ORDER BY id ASC
        """
    )
    suspend fun getRootFoldersByKind(kind: String): List<QuestionFolderEntity>

    @Query(
        """
        SELECT * FROM question_folders
        WHERE parentFolderId = :parentFolderId
          AND kind = :kind
        ORDER BY id ASC
        """
    )
    suspend fun getChildFoldersByKind(
        parentFolderId: Long,
        kind: String
    ): List<QuestionFolderEntity>

    @Query("SELECT * FROM question_folders WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): QuestionFolderEntity?

    @Query("UPDATE question_folders SET enabled = :enabled WHERE id = :id")
    suspend fun updateEnabled(id: Long, enabled: Boolean)

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
        SELECT id FROM descendants
        """
    )
    suspend fun getFolderTreeIds(folderId: Long): List<Long>

    @Query("DELETE FROM question_folders WHERE id IN (:folderIds)")
    suspend fun deleteFoldersByIds(folderIds: List<Long>)

    @Query("SELECT COUNT(*) FROM question_folders WHERE parentFolderId = :folderId")
    suspend fun getChildFolderCount(folderId: Long): Int
}