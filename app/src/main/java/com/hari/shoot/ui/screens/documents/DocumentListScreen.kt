package com.hari.shoot.ui.screens.documents

import android.annotation.SuppressLint
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.hari.shoot.data.model.Document
import java.time.format.DateTimeFormatter
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun DocumentListScreen(
    viewModel: DocumentListViewModel = hiltViewModel(),
    onDocumentClick: (Document) -> Unit,
    onUploadClick: () -> Unit
) {
    val documents by viewModel.documents.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current
    val padding = PaddingValues(16.dp)
    var showDialog by remember { mutableStateOf(false) }

    // Add logging to debug document data
    LaunchedEffect(documents) {
        documents.forEach { doc: Document ->
            android.util.Log.d("DocumentList", """
                Document:
                - ID: ${doc.id}
                - Name: ${doc.name}
                - Date: ${doc.date}
                - URI: ${doc.uri}
            """.trimIndent())
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Documents") },
                actions = {
                    IconButton(onClick = { /* TODO: Implement search */ }) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                    IconButton(onClick = { showDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete All")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onUploadClick,
                icon = { Icon(Icons.Default.Add, "Upload") },
                text = { Text("Upload") }
            )
        }
    ) { padding ->
        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Delete All Documents?") },
                text = { Text("Are you sure you want to delete all documents? This action cannot be undone.") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteAllDocuments()
                        showDialog = false
                    }) {
                        Text("Delete All")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (documents.isEmpty()) {
            EmptyState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                // Log document IDs to check for uniqueness
                documents.forEach { document ->
                    android.util.Log.d("DocumentList", "Document ID: ${document.id}")
                }

                items(documents, key = { document -> document.id }) { document ->
                    DocumentCard(
                        document = document,
                        onClick = { onDocumentClick(document) },
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@Composable
private fun DocumentCard(
    document: Document,
    onClick: () -> Unit,
    viewModel: DocumentListViewModel = hiltViewModel()
) {
    // Add validation before allowing click
    val isValid = remember(document) {
        document.uri.isNotBlank() && document.name.isNotBlank()
    }

    Card(
        onClick = {
            if (isValid) {
                android.util.Log.d("DocumentList", """
                    Clicked document:
                    - ID: ${document.id}
                    - Name: ${document.name}
                    - Date: ${document.date}
                    - URI: ${document.uri}
                """.trimIndent())
                onClick()
            } else {
                android.util.Log.e("DocumentList", "Attempted to click invalid document: $document")
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .alpha(if (isValid) 1f else 0.5f)  // Visual indication of invalid documents
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        document.uri.endsWith(".pdf", true) -> {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        document.uri.endsWith(".jpg", true) ||
                        document.uri.endsWith(".jpeg", true) ||
                        document.uri.endsWith(".png", true) -> {
                            AsyncImage(
                                model = Uri.parse(document.uri),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        else -> {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                IconButton(onClick = {
                    viewModel.deleteDocument(document)
                }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = formatDocumentName(document.name),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Text(
                text = formatDate(document.date),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatDocumentName(name: String): String {
    // Remove timestamp from name if it exists
    return name.substringBefore(".")
        .let { if (it.all { c -> c.isDigit() }) "Document $it" else it }
}

private fun formatDate(date: String): String {
    return try {
        val dateTime = LocalDateTime.parse(date)
        dateTime.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
    } catch (e: Exception) {
        date
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Description,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No documents yet",
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = "Upload a document to get started",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
} 