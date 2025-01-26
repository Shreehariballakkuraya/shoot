package com.hari.shoot.ui.screens.documents

import android.annotation.SuppressLint
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button

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
                    IconButton(onClick = { /* TODO: Implement menu */ }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options")
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
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 150.dp),
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(
                    count = documents.size,
                    key = { index -> documents[index].uniqueKey }
                ) { index ->
                    val document = documents[index]
                    DocumentCard(
                        document = document,
                        onClick = { onDocumentClick(document) },
                        onDelete = { viewModel.deleteDocument(document) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DocumentCard(
    document: Document,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val isValid = document.uri.isNotBlank()
    AnimatedVisibility(
        visible = true,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = tween(durationMillis = 300)
        )
    ) {
        Card(
            modifier = Modifier
                .animateContentSize(
                    animationSpec = tween(
                        durationMillis = 300,
                    )
                )
                .fillMaxWidth()
                .aspectRatio(1f)
                .alpha(if (isValid) 1f else 0.5f),
            onClick = onClick
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            document.uri.endsWith(".pdf", ignoreCase = true) -> {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            document.uri.endsWith(".jpg", ignoreCase = true) || document.uri.endsWith(".png", ignoreCase = true) -> {
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
                    IconButton(onClick = onDelete) {
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