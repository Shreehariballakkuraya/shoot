package com.hari.shoot.ui.screens.documents

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hari.shoot.data.model.Document
import com.hari.shoot.data.repository.DocumentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class DocumentListViewModel @Inject constructor(
    private val documentRepository: DocumentRepository
) : ViewModel() {
    private val _documents = MutableStateFlow<List<Document>>(emptyList())
    val documents: StateFlow<List<Document>> = _documents.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    init {
        loadDocuments()
    }

    private fun loadDocuments() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _documents.value = documentRepository.getAllDocuments()
            } catch (e: Exception) {
                android.util.Log.e("DocumentListViewModel", "Failed to load documents", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun uploadDocument(uri: Uri, name: String, title: String, author: String, description: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val document = Document(
                    id = System.currentTimeMillis(),
                    name = name,
                    date = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE),
                    uri = uri.toString(),
                    title = title,
                    author = author,
                    description = description
                )
                documentRepository.saveDocument(document)
                loadDocuments() // Reload documents after upload
            } catch (e: Exception) {
                android.util.Log.e("DocumentListViewModel", "Failed to upload document", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteDocument(document: Document) {
        viewModelScope.launch {
            try {
                documentRepository.deleteDocument(document)
                _documents.value = _documents.value.filter { it.id != document.id }
            } catch (e: Exception) {
                android.util.Log.e("DocumentListViewModel", "Failed to delete document", e)
            }
        }
    }

    fun deleteAllDocuments() {
        viewModelScope.launch {
            try {
                documentRepository.deleteAllDocuments()
                _documents.value = emptyList()
            } catch (e: Exception) {
                android.util.Log.e("DocumentListViewModel", "Failed to delete all documents", e)
            }
        }
    }
} 