package com.example.noticanswer

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class PendingImageExport(
    val zipEntryName: String,
    val file: File
)

suspend fun exportGenreToZip(
    context: Context,
    genreId: Long,
    outputUri: Uri
) {
    withContext(Dispatchers.IO) {
        val db = AppDatabase.getDatabase(context)
        val folderDao = db.questionFolderDao()

        val genre = folderDao.findById(genreId)
            ?: error("ジャンルが見つかりません")

        val foldersJson = JSONArray()
        val questionsJson = JSONArray()
        val pendingImages = mutableListOf<PendingImageExport>()

        var nextFolderExportId = 1L
        var nextQuestionExportId = 1L

        val genreExportId = nextFolderExportId++

        foldersJson.put(
            JSONObject()
                .put("exportId", genreExportId)
                .put("parentExportId", JSONObject.NULL)
                .put("name", genre.name)
                .put("kind", FolderKind.GENRE)
                .put("enabled", genre.enabled)
        )

        val subGenres = QuestionRepository.getSubGenres(
            context = context,
            genreId = genre.id
        )

        val folderExportIdMap = mutableMapOf<Long, Long>()

        subGenres.forEach { subGenre ->
            val subGenreExportId = nextFolderExportId++
            folderExportIdMap[subGenre.id] = subGenreExportId

            foldersJson.put(
                JSONObject()
                    .put("exportId", subGenreExportId)
                    .put("parentExportId", genreExportId)
                    .put("name", subGenre.name)
                    .put("kind", FolderKind.SUB_GENRE)
                    .put("enabled", subGenre.enabled)
            )

            val questions = QuestionRepository.getQuestionsByFolder(
                context = context,
                folderId = subGenre.id
            )

            questions.forEach { question ->
                val questionExportId = nextQuestionExportId++
                var imageFileName = ""

                if (question.imagePath.isNotBlank()) {
                    val imageFile = File(question.imagePath)

                    if (imageFile.exists()) {
                        imageFileName =
                            "images/q_${questionExportId}_${sanitizeFileName(imageFile.name)}"

                        pendingImages.add(
                            PendingImageExport(
                                zipEntryName = imageFileName,
                                file = imageFile
                            )
                        )
                    }
                }

                questionsJson.put(
                    JSONObject()
                        .put("exportId", questionExportId)
                        .put("folderExportId", subGenreExportId)
                        .put("promptText", question.promptText)
                        .put("correctAnswer", question.correctAnswer)
                        .put("aliasesText", question.aliases.joinToString("|"))
                        .put("explanation", question.explanation)
                        .put("enabled", question.enabled)
                        .put("imageFileName", imageFileName)
                        .put("imageResName", question.imageResName)
                )
            }
        }

        val rootJson = JSONObject()
            .put("schemaVersion", 1)
            .put("folders", foldersJson)
            .put("questions", questionsJson)

        val outputStream = context.contentResolver.openOutputStream(outputUri)
            ?: error("保存先を開けませんでした")

        outputStream.use { rawOutput ->
            ZipOutputStream(BufferedOutputStream(rawOutput)).use { zip ->
                zip.putNextEntry(ZipEntry("data.json"))
                zip.write(rootJson.toString(2).toByteArray(Charsets.UTF_8))
                zip.closeEntry()

                pendingImages.forEach { image ->
                    zip.putNextEntry(ZipEntry(image.zipEntryName))

                    image.file.inputStream().use { input ->
                        input.copyTo(zip)
                    }

                    zip.closeEntry()
                }
            }
        }
    }
}

suspend fun importGenreZipAsNewGenre(
    context: Context,
    inputUri: Uri
) {
    withContext(Dispatchers.IO) {
        val inputStream = context.contentResolver.openInputStream(inputUri)
            ?: error("ファイルを開けませんでした")

        var jsonText: String? = null
        val imageBytesMap = mutableMapOf<String, ByteArray>()

        inputStream.use { rawInput ->
            ZipInputStream(BufferedInputStream(rawInput)).use { zip ->
                var entry = zip.nextEntry

                while (entry != null) {
                    if (!entry.isDirectory) {
                        val bytes = zip.readBytes()

                        if (entry.name == "data.json") {
                            jsonText = bytes.toString(Charsets.UTF_8)
                        } else if (entry.name.startsWith("images/")) {
                            imageBytesMap[entry.name] = bytes
                        }
                    }

                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        }

        val json = jsonText ?: error("data.json が見つかりません")
        val root = JSONObject(json)

        val schemaVersion = root.optInt("schemaVersion", 0)

        if (schemaVersion != 1) {
            error("対応していないエクスポート形式です")
        }

        val foldersJson = root.getJSONArray("folders")
        val questionsJson = root.getJSONArray("questions")

        val db = AppDatabase.getDatabase(context)
        val folderDao = db.questionFolderDao()

        val folderIdMap = mutableMapOf<Long, Long>()

        // まずジャンルを作成
        for (i in 0 until foldersJson.length()) {
            val folderJson = foldersJson.getJSONObject(i)
            val kind = folderJson.getString("kind")

            if (kind != FolderKind.GENRE) {
                continue
            }

            val exportId = folderJson.getLong("exportId")

            val newGenreId = folderDao.insert(
                QuestionFolderEntity(
                    parentFolderId = null,
                    name = folderJson.getString("name"),
                    enabled = folderJson.optBoolean("enabled", true),
                    kind = FolderKind.GENRE
                )
            )

            folderIdMap[exportId] = newGenreId
        }

        // 次にサブジャンルを作成
        for (i in 0 until foldersJson.length()) {
            val folderJson = foldersJson.getJSONObject(i)
            val kind = folderJson.getString("kind")

            if (kind != FolderKind.SUB_GENRE) {
                continue
            }

            val exportId = folderJson.getLong("exportId")
            val parentExportId = folderJson.getLong("parentExportId")
            val newParentId = folderIdMap[parentExportId] ?: continue

            val newSubGenreId = folderDao.insert(
                QuestionFolderEntity(
                    parentFolderId = newParentId,
                    name = folderJson.getString("name"),
                    enabled = folderJson.optBoolean("enabled", true),
                    kind = FolderKind.SUB_GENRE
                )
            )

            folderIdMap[exportId] = newSubGenreId
        }

        // 最後に問題を作成
        for (i in 0 until questionsJson.length()) {
            val questionJson = questionsJson.getJSONObject(i)

            val folderExportId = questionJson.getLong("folderExportId")
            val newFolderId = folderIdMap[folderExportId] ?: continue

            val imageFileName = questionJson.optString("imageFileName", "")

            val imagePath = if (imageFileName.isNotBlank()) {
                val bytes = imageBytesMap[imageFileName]

                if (bytes != null) {
                    saveImportedQuestionImage(
                        context = context,
                        originalFileName = imageFileName,
                        bytes = bytes
                    )
                } else {
                    ""
                }
            } else {
                ""
            }

            QuestionRepository.addQuestion(
                context = context,
                folderId = newFolderId,
                promptText = questionJson.optString("promptText", ""),
                correctAnswer = questionJson.optString("correctAnswer", ""),
                aliasesText = questionJson.optString("aliasesText", ""),
                explanation = questionJson.optString("explanation", ""),
                imageResName = questionJson.optString("imageResName", ""),
                imagePath = imagePath,
                enabled = questionJson.optBoolean("enabled", true)
            )
        }
    }
}

fun buildGenreExportFileName(
    genreName: String
): String {
    return "${sanitizeFileName(genreName)}.zip"
}

private fun saveImportedQuestionImage(
    context: Context,
    originalFileName: String,
    bytes: ByteArray
): String {
    val dir = File(context.filesDir, "question_images")

    if (!dir.exists()) {
        dir.mkdirs()
    }

    val cleanName = sanitizeFileName(
        originalFileName.substringAfterLast("/")
    )

    val file = File(
        dir,
        "import_${System.currentTimeMillis()}_${UUID.randomUUID()}_$cleanName"
    )

    file.writeBytes(bytes)

    return file.absolutePath
}

private fun sanitizeFileName(
    name: String
): String {
    return name
        .replace(Regex("[\\\\/:*?\"<>|]"), "_")
        .ifBlank { "noticanswer_export" }
}