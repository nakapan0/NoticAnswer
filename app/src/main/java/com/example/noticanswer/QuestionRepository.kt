package com.example.noticanswer

import android.content.Context

object QuestionRepository {

    private var lastQuestionId: Int? = null

    suspend fun getRandomQuestion(context: Context): Question {
        seedIfEmpty(context)

        val dao = AppDatabase.getDatabase(context).questionDao()
        val enabledQuestions = dao.getEnabledQuestionsForNotification()
            .map { it.toQuestion(context) }

        if (enabledQuestions.isEmpty()) {
            error("出題可能な問題がありません")
        }

        val candidates = if (enabledQuestions.size > 1) {
            enabledQuestions.filter { it.id != lastQuestionId }
        } else {
            enabledQuestions
        }

        val selected = candidates.random()
        lastQuestionId = selected.id

        return selected
    }

    suspend fun findById(context: Context, id: Int): Question? {
        seedIfEmpty(context)

        return AppDatabase.getDatabase(context)
            .questionDao()
            .findById(id)
            ?.toQuestion(context)
    }

    suspend fun getAllQuestions(context: Context): List<Question> {
        seedIfEmpty(context)

        return AppDatabase.getDatabase(context)
            .questionDao()
            .getAllQuestions()
            .map { it.toQuestion(context) }
    }

    suspend fun getQuestionsByFolder(
        context: Context,
        folderId: Long
    ): List<Question> {
        seedIfEmpty(context)

        return AppDatabase.getDatabase(context)
            .questionDao()
            .getQuestionsByFolder(folderId)
            .map { it.toQuestion(context) }
    }

    suspend fun setQuestionEnabled(
        context: Context,
        questionId: Int,
        enabled: Boolean
    ) {
        seedIfEmpty(context)

        AppDatabase.getDatabase(context)
            .questionDao()
            .updateEnabled(questionId, enabled)
    }

    suspend fun moveQuestionToFolder(
        context: Context,
        questionId: Int,
        newFolderId: Long
    ) {
        seedIfEmpty(context)

        val db = AppDatabase.getDatabase(context)
        val targetFolder = db.questionFolderDao().findById(newFolderId)

        if (targetFolder?.kind != FolderKind.SUB_GENRE) {
            error("移動先がサブジャンルではありません")
        }

        db.questionDao().moveQuestionToFolder(
            questionId = questionId,
            newFolderId = newFolderId
        )
    }

    suspend fun addQuestion(
        context: Context,
        folderId: Long,
        promptText: String,
        correctAnswer: String,
        aliasesText: String,
        explanation: String,
        imageResName: String,
        imagePath: String = "",
        enabled: Boolean = true,
        showImageInNotification: Boolean
    ) {
        seedIfEmpty(context)

        val dao = AppDatabase.getDatabase(context).questionDao()
        val nextId = dao.getMaxQuestionId() + 1

        dao.insert(
            QuestionEntity(
                id = nextId,
                folderId = folderId,
                promptText = promptText,
                correctAnswer = correctAnswer,
                aliasesText = aliasesText,
                explanation = explanation,
                imageResName = imageResName,
                imagePath = imagePath,
                enabled = enabled,
                showImageInNotification = showImageInNotification
            )
        )
    }

    suspend fun updateQuestion(
        context: Context,
        questionId: Int,
        promptText: String,
        correctAnswer: String,
        aliasesText: String,
        explanation: String,
        imageResName: String,
        imagePath: String = "",
        showImageInNotification: Boolean

    ) {
        seedIfEmpty(context)

        val db = AppDatabase.getDatabase(context)
        val questionDao = db.questionDao()

        val oldQuestion = questionDao.findById(questionId)

        questionDao.updateQuestionContent(
            id = questionId,
            promptText = promptText,
            correctAnswer = correctAnswer,
            aliasesText = aliasesText,
            explanation = explanation,
            imageResName = imageResName,
            imagePath = imagePath
        )

        if (
            oldQuestion != null &&
            oldQuestion.imagePath.isNotBlank() &&
            oldQuestion.imagePath != imagePath
        ) {
            deleteQuestionImageFileIfExists(
                context = context,
                imagePath = oldQuestion.imagePath
            )
        }
        questionDao.updateShowImage(
            id = questionId,
            showImageInNotification = showImageInNotification
        )
    }

    suspend fun deleteQuestion(
        context: Context,
        questionId: Int
    ) {
        val db = AppDatabase.getDatabase(context)

        val question = db.questionDao().findById(questionId)

        db.answerLogDao().deleteByQuestionId(questionId)
        db.questionDao().deleteQuestionById(questionId)

        if (question != null) {
            deleteQuestionImageFileIfExists(
                context = context,
                imagePath = question.imagePath
            )
        }
    }

    suspend fun getGenres(context: Context): List<QuestionFolderEntity> {
        seedIfEmpty(context)

        return AppDatabase.getDatabase(context)
            .questionFolderDao()
            .getRootFoldersByKind(FolderKind.GENRE)
    }

    suspend fun getSubGenres(
        context: Context,
        genreId: Long
    ): List<QuestionFolderEntity> {
        seedIfEmpty(context)

        return AppDatabase.getDatabase(context)
            .questionFolderDao()
            .getChildFoldersByKind(
                parentFolderId = genreId,
                kind = FolderKind.SUB_GENRE
            )
    }

    suspend fun addGenre(
        context: Context,
        name: String
    ): Long {
        seedIfEmpty(context)

        return AppDatabase.getDatabase(context)
            .questionFolderDao()
            .insert(
                QuestionFolderEntity(
                    parentFolderId = null,
                    name = name,
                    enabled = true,
                    kind = FolderKind.GENRE
                )
            )
    }

    suspend fun addSubGenre(
        context: Context,
        genreId: Long,
        name: String
    ): Long {
        seedIfEmpty(context)

        return AppDatabase.getDatabase(context)
            .questionFolderDao()
            .insert(
                QuestionFolderEntity(
                    parentFolderId = genreId,
                    name = name,
                    enabled = true,
                    kind = FolderKind.SUB_GENRE
                )
            )
    }

    suspend fun setFolderEnabled(
        context: Context,
        folderId: Long,
        enabled: Boolean
    ) {
        seedIfEmpty(context)

        AppDatabase.getDatabase(context)
            .questionFolderDao()
            .updateEnabled(folderId, enabled)
    }

    suspend fun deleteFolderTree(
        context: Context,
        folderId: Long
    ) {
        seedIfEmpty(context)

        val db = AppDatabase.getDatabase(context)
        val folderDao = db.questionFolderDao()
        val questionDao = db.questionDao()
        val answerLogDao = db.answerLogDao()

        val folderIds = folderDao.getFolderTreeIds(folderId)

        if (folderIds.isEmpty()) {
            return
        }

        val questions = questionDao.getQuestionsByFolderIds(folderIds)
        val questionIds = questions.map { it.id }

        if (questionIds.isNotEmpty()) {
            answerLogDao.deleteByQuestionIds(questionIds)
        }

        questionDao.deleteQuestionsByFolderIds(folderIds)
        folderDao.deleteFoldersByIds(folderIds)

        questions.forEach { question ->
            deleteQuestionImageFileIfExists(
                context = context,
                imagePath = question.imagePath
            )
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private suspend fun seedIfEmpty(context: Context) {
        // 初期サンプルデータの自動生成は停止
        // 空状態UIを表示するため、フォルダや問題が0件でも何もしない
    }
    suspend fun getRootFolders(context: Context): List<QuestionFolderEntity> {
        return getGenres(context)
    }

    suspend fun getChildFolders(
        context: Context,
        parentFolderId: Long
    ): List<QuestionFolderEntity> {
        seedIfEmpty(context)

        val dao = AppDatabase.getDatabase(context).questionFolderDao()
        val parent = dao.findById(parentFolderId) ?: return emptyList()

        val childKind = when (parent.kind) {
            FolderKind.GENRE -> FolderKind.SUB_GENRE
            FolderKind.SUB_GENRE -> return emptyList()
            else -> return emptyList()
        }

        return dao.getChildFoldersByKind(
            parentFolderId = parentFolderId,
            kind = childKind
        )
    }

    suspend fun addFolder(
        context: Context,
        parentFolderId: Long?,
        name: String
    ): Long {
        seedIfEmpty(context)

        val dao = AppDatabase.getDatabase(context).questionFolderDao()

        val kind = if (parentFolderId == null) {
            FolderKind.GENRE
        } else {
            val parent = dao.findById(parentFolderId)

            when (parent?.kind) {
                FolderKind.GENRE -> FolderKind.SUB_GENRE
                FolderKind.SUB_GENRE -> error("サブジャンルの下にはフォルダを作成できません")
                else -> error("親フォルダの種類が不正です")
            }
        }

        return dao.insert(
            QuestionFolderEntity(
                parentFolderId = parentFolderId,
                name = name,
                enabled = true,
                kind = kind
            )
        )
    }
}