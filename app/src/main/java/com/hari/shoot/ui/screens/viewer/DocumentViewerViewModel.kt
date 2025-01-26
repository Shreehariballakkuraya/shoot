package com.hari.shoot.ui.screens.viewer

import androidx.lifecycle.ViewModel
import com.hari.shoot.data.model.Document
import com.hari.shoot.data.repository.DocumentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class DocumentViewerViewModel @Inject constructor(
    private val documentRepository: DocumentRepository
) : ViewModel() {

    fun getDocument(documentId: Long): Flow<Document?> {
        return documentRepository.getDocument(documentId)
    }
} 