package com.hari.shoot.ui.screens.upload

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.hari.shoot.data.repository.DocumentRepository
import com.hari.shoot.data.model.Document
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@HiltViewModel
class UploadDocumentViewModel @Inject constructor(
    private val documentRepository: DocumentRepository
) : ViewModel() {

    private val _uploadState = MutableStateFlow<UploadState>(UploadState.Initial)
    val uploadState: StateFlow<UploadState> = _uploadState.asStateFlow()

    companion object {
        private val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
    }

    fun uploadDocument(uri: Uri?, name: String, title: String, author: String, description: String) {
        _uploadState.value = UploadState.Initial // Reset state at the start of the operation

        if (uri == null) {
            _uploadState.value = UploadState.Error("No file selected")
            return
        }
        if (name.isBlank()) {
            _uploadState.value = UploadState.Error("Document name cannot be empty")
            return
        }
        if (title.isBlank()) {
            _uploadState.value = UploadState.Error("Document title cannot be empty")
            return
        }
        if (author.isBlank()) {
            _uploadState.value = UploadState.Error("Author name cannot be empty")
            return
        }
        if (description.isBlank()) {
            _uploadState.value = UploadState.Error("Description cannot be empty")
            return
        }

        val document = Document(
            id = System.currentTimeMillis(),
            name = name,
            date = LocalDateTime.now().format(dateFormatter),
            uri = uri.toString(),
            title = title,
            author = author,
            description = description
        )

        viewModelScope.launch {
            _uploadState.value = UploadState.Loading
            try {
                documentRepository.saveDocument(document)
                _uploadState.value = UploadState.Success
            } catch (e: Exception) {
                _uploadState.value = UploadState.Error(e.message ?: "Upload failed")
                e.printStackTrace() // Log the exception
            }
        }
    }
}

sealed class UploadState {
    object Initial : UploadState()
    object Loading : UploadState()
    object Success : UploadState()
    data class Error(val message: String) : UploadState()
}
