package com.hari.shoot.data.repository

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import com.hari.shoot.data.model.Document
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.UUID

@Singleton
class DocumentRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun saveDocument(document: Document) {
        val documentsDir = File(context.filesDir, "documents").apply { mkdirs() }
        val mimeType = context.contentResolver.getType(Uri.parse(document.uri))
        val extension = when (mimeType) {
            "application/pdf" -> "pdf"
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            else -> {
                android.util.Log.e("DocumentRepository", "Unsupported file type: $mimeType")
                throw IllegalArgumentException("Unsupported file type: $mimeType")
            }
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
                writer.write("date=${document.date}\n")
            }
        } catch (e: IOException) {
            android.util.Log.e("DocumentRepository", "Failed to save metadata", e)
            throw IOException("Failed to save metadata: ${e.message}")
        }

        android.util.Log.d("DocumentRepository", "Document saved successfully: ${file.absolutePath}")
    }

    suspend fun getDocuments(): List<Document> = withContext(Dispatchers.IO) {
        val documentsDir = File(context.filesDir, "documents")
        if (!documentsDir.exists()) return@withContext emptyList()

        documentsDir.listFiles()
            ?.filter { it.extension != "meta" } // Exclude metadata files
            ?.mapNotNull { file ->
                try {
                    val metadata = loadMetadata(File(documentsDir, "${file.nameWithoutExtension}.meta"))
                    Document(
                        id = file.nameWithoutExtension.toLong(),
                        name = file.nameWithoutExtension,
                        date = metadata["date"] ?: LocalDateTime.now().format(DateTimeFormatter.ISO_DATE),
                        uri = file.absolutePath,
                        title = metadata["title"] ?: "",
                        author = metadata["author"] ?: "",
                        description = metadata["description"] ?: ""
                    )
                } catch (e: Exception) {
                    android.util.Log.e("DocumentRepository", "Failed to load document", e)
                    null
                }
            } ?: emptyList()
    }

    suspend fun deleteDocument(document: Document) = withContext(Dispatchers.IO) {
        val file = File(document.uri)
        val metaFile = File(file.parent, "${file.nameWithoutExtension}.meta")

        try {
            if (!file.exists()) throw IllegalStateException("File does not exist: ${file.absolutePath}")

            val isDeleted = file.delete()
            if (!isDeleted) throw IllegalStateException("Failed to delete file: ${file.absolutePath}")

            if (metaFile.exists() && !metaFile.delete()) {
                throw IllegalStateException("Failed to delete metadata: ${metaFile.absolutePath}")
            }

            android.util.Log.d("DocumentRepository", "Document deleted successfully")
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
                    date = metadata["date"] ?: LocalDateTime.now().format(DateTimeFormatter.ISO_DATE),
                    uri = file.absolutePath,
                    title = metadata["title"] ?: "",
                    author = metadata["author"] ?: "",
                    description = metadata["description"] ?: ""
                )
                break
            }
        }
        emit(foundDocument)
    }

    private fun loadMetadata(metaFile: File): Map<String, String> {
        val metadata = mutableMapOf<String, String>()
        if (!metaFile.exists()) return metadata

        try {
            metaFile.bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    val parts = line.split("=", limit = 2)
                    if (parts.size == 2) {
                        metadata[parts[0]] = parts[1]
                    }
                }
            }
        } catch (e: IOException) {
            android.util.Log.e("DocumentRepository", "Failed to load metadata from ${metaFile.name}", e)
        }
        return metadata
    }
}
