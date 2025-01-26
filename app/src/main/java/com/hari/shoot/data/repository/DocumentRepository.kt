package com.hari.shoot.data.repository

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import com.hari.shoot.data.model.Document
import java.io.BufferedReader
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException

interface DocumentRepository {
    suspend fun saveDocument(document: Document)
    suspend fun deleteDocument(document: Document)
    suspend fun getAllDocuments(): List<Document>
    suspend fun deleteAllDocuments()
}

@Singleton
class DocumentRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : DocumentRepository {
    suspend fun saveDocument(document: Document) {
        val documentsDir = File(context.filesDir, "documents").apply { mkdirs() }
        val mimeType = context.contentResolver.getType(Uri.parse(document.uri))
        val extension = when (mimeType) {
            "application/pdf" -> "pdf"
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            else -> "unknown"
        }
        val file = File(documentsDir, "${document.id}.$extension")
        context.contentResolver.openInputStream(Uri.parse(document.uri))?.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        // Save metadata to a .meta file
        val metaFile = File(documentsDir, "${document.id}.meta")
        try {
            FileWriter(metaFile).use { writer ->
                writer.write("title=${document.title}\n")
                writer.write("author=${document.author}\n")
                writer.write("description=${document.description}\n")
            }
        } catch (e: IOException) {
            android.util.Log.e("DocumentRepository", "Failed to save metadata", e)
        }

        android.util.Log.d("DocumentRepository", """
            Created document:
            - ID: ${document.id}
            - Name: ${document.name}
            - Title: ${document.title}
            - Author: ${document.author}
            - Description: ${document.description}
            - Date: ${document.date}
            - URI: ${file.absolutePath}
        """.trimIndent())
    }

    suspend fun getDocuments(): List<Document> = withContext(Dispatchers.IO) {
        try {
            val documentsDir = File(context.filesDir, "documents")
            if (!documentsDir.exists()) return@withContext emptyList()

            return@withContext documentsDir.listFiles()?.mapNotNull { file ->
                try {
                    val metaFile = File(documentsDir, "${file.nameWithoutExtension}.meta")
                    val metadata = loadMetadata(metaFile)

                    Document(
                        id = file.nameWithoutExtension.toLongOrNull() ?: return@mapNotNull null,
                        name = file.nameWithoutExtension,
                        date = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE),
                        uri = file.absolutePath,
                        title = metadata["title"] ?: "Sample Title",
                        author = metadata["author"] ?: "Sample Author",
                        description = metadata["description"] ?: "Sample Description"
                    )
                } catch (e: Exception) {
                    android.util.Log.e("DocumentRepository", "Failed to load document", e)
                    null
                }
            } ?: emptyList()
        } catch (e: Exception) {
            android.util.Log.e("DocumentRepository", "Failed to load documents", e)
            emptyList()
        }
    }

    private fun loadMetadata(metaFile: File): Map<String, String> {
        val metadata = mutableMapOf<String, String>()
        if (!metaFile.exists()) return metadata

        try {
            BufferedReader(FileReader(metaFile)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val parts = line?.split("=", limit = 2)
                    if (parts?.size == 2) {
                        metadata[parts[0]] = parts[1]
                    }
                }
            }
        } catch (e: IOException) {
            android.util.Log.e("DocumentRepository", "Failed to load metadata", e)
        }
        return metadata
    }

    suspend fun deleteDocument(document: Document) = withContext(Dispatchers.IO) {
        try {
            val file = File(document.uri)
            android.util.Log.d("DocumentRepository", "Deleting file: ${file.absolutePath}")
            if (!file.delete()) {
                throw IllegalStateException("Failed to delete file")
            }
            android.util.Log.d("DocumentRepository", "File deleted successfully")
        } catch (e: Exception) {
            android.util.Log.e("DocumentRepository", "Failed to delete document", e)
            throw IllegalStateException("Failed to delete document: ${e.message}")
        }
    }

    fun getDocument(documentId: Long): Flow<Document?> = flow {
        val documentsDir = File(context.filesDir, "documents")
        val possibleExtensions = listOf("pdf", "png", "jpg")

        var foundDocument: Document? = null
        for (extension in possibleExtensions) {
            val file = File(documentsDir, "$documentId.$extension")
            if (file.exists()) {
                val metaFile = File(documentsDir, "$documentId.meta")
                val metadata = loadMetadata(metaFile)

                foundDocument = Document(
                    id = documentId,
                    name = file.nameWithoutExtension,
                    date = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE),
                    uri = file.absolutePath,
                    title = metadata["title"] ?: "Sample Title",
                    author = metadata["author"] ?: "Sample Author",
                    description = metadata["description"] ?: "Sample Description"
                )
                break
            }
        }
        emit(foundDocument)
    }

    suspend fun getAllDocuments(): List<Document>
    suspend fun deleteAllDocuments()
} 