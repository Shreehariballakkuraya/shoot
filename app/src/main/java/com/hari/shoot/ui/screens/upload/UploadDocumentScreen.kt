package com.hari.shoot.ui.screens.upload

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import android.provider.OpenableColumns
import androidx.compose.material.icons.automirrored.filled.ArrowBack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadDocumentScreen(
    onUploadComplete: () -> Unit,
    viewModel: UploadDocumentViewModel = hiltViewModel()
) {
    var documentName by remember { mutableStateOf("") }
    var documentTitle by remember { mutableStateOf("") }
    var documentAuthor by remember { mutableStateOf("") }
    var documentDescription by remember { mutableStateOf("") }
    val uploadState by viewModel.uploadState.collectAsState()
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    
    val context = LocalContext.current

    LaunchedEffect(uploadState) {
        if (uploadState is UploadState.Success) {
            onUploadComplete()
        }
    }

    val pickDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedUri = it
            // Get file name from uri
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (it.moveToFirst()) {
                    selectedFileName = it.getString(nameIndex)
                    if (documentName.isBlank()) {
                        documentName = selectedFileName ?: ""
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Upload Document") },
                navigationIcon = {
                    IconButton(onClick = onUploadComplete) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TextField(
                value = documentTitle,
                onValueChange = { documentTitle = it },
                label = { Text("Document Title") }
            )
            TextField(
                value = documentAuthor,
                onValueChange = { documentAuthor = it },
                label = { Text("Author") }
            )
            TextField(
                value = documentDescription,
                onValueChange = { documentDescription = it },
                label = { Text("Description") }
            )

            selectedFileName?.let { fileName ->
                Text(
                    text = "Selected file: $fileName",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = { pickDocument.launch("*/*") },
                enabled = uploadState !is UploadState.Loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Select Document")
            }

            selectedUri?.let { uri ->
                Button(
                    onClick = { viewModel.uploadDocument(uri, documentName, documentTitle, documentAuthor, documentDescription) },
                    enabled = documentName.isNotBlank() && uploadState !is UploadState.Loading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (uploadState is UploadState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Upload")
                    }
                }
            }
        }
    }
} 