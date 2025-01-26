    package com.hari.shoot.ui.screens.viewer

    import android.annotation.SuppressLint
    import android.graphics.Bitmap
    import android.graphics.ColorFilter
    import android.graphics.ColorMatrix
    import android.graphics.ColorMatrixColorFilter
    import android.graphics.Paint
    import android.graphics.Canvas
    import android.graphics.pdf.PdfRenderer
    import android.net.Uri
    import android.util.Log
    import androidx.compose.foundation.Image
    import androidx.compose.foundation.layout.*
    import androidx.compose.material.icons.Icons
    import androidx.compose.material.icons.automirrored.filled.ArrowBack
    import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
    import androidx.compose.material.icons.filled.Delete
    import androidx.compose.material.icons.filled.ErrorOutline
    import androidx.compose.material.icons.filled.Share
    import androidx.compose.material3.*
    import androidx.compose.runtime.*
    import androidx.compose.ui.Alignment
    import androidx.compose.ui.Modifier
    import androidx.compose.ui.graphics.asImageBitmap
    import androidx.compose.ui.graphics.graphicsLayer
    import androidx.compose.ui.layout.ContentScale
    import androidx.compose.ui.platform.LocalContext
    import androidx.compose.ui.text.style.TextAlign
    import androidx.compose.ui.text.style.TextOverflow
    import androidx.compose.ui.unit.dp
    import coil.compose.AsyncImage
    import androidx.hilt.navigation.compose.hiltViewModel
    import coil.request.ImageRequest
    import com.hari.shoot.data.model.Document
    import com.hari.shoot.ui.screens.documents.DocumentListViewModel
    import java.io.File

    private const val TAG = "DocumentViewer"

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    fun DocumentViewerScreen(
        document: Document,
        onBackClick: () -> Unit,
        onShareClick: () -> Unit,
        viewModel: DocumentListViewModel = hiltViewModel()
    ) {
        val context = LocalContext.current
        var currentPage by remember { mutableStateOf(0) }
        var pageCount by remember { mutableStateOf(1) }
        var bitmap by remember { mutableStateOf<Bitmap?>(null) }
        var error by remember { mutableStateOf<String?>(null) }
        val isLoading by viewModel.isLoading.collectAsState()
        var zoomLevel by remember { mutableStateOf(1f) }

        LaunchedEffect(document) {
            try {
                val file = File(document.uri)
                if (!file.exists() || !file.canRead()) {
                    error = "Cannot access file: ${file.absolutePath}"
                    return@LaunchedEffect
                }

                if (file.extension.lowercase() == "pdf") {
                    context.contentResolver.openFileDescriptor(Uri.fromFile(file), "r")?.use { pfd ->
                        PdfRenderer(pfd).use { renderer ->
                            pageCount = renderer.pageCount
                            renderer.openPage(currentPage).use { page ->
                                val newBitmap = Bitmap.createBitmap(
                                    page.width,
                                    page.height,
                                    Bitmap.Config.ARGB_8888
                                )
                                page.render(newBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                                // Adjust the brightness of the bitmap
                                val brightBitmap = adjustBitmapBrightness(newBitmap, 1.5f)
                                bitmap = brightBitmap
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading document", e)
                error = "Error loading document: ${e.message}"
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(document.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = onShareClick) {
                            Icon(Icons.Default.Share, contentDescription = "Share")
                        }
                        IconButton(onClick = { 
                            viewModel.deleteDocument(document)
                            onBackClick()
                        }){
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (error != null) {
                    Text(
                        text = error ?: "Error",
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    val fileExtension = File(document.uri).extension
                    when (fileExtension.lowercase()) {
                        "pdf" -> {
                            bitmap?.let { btm ->
                                Image(
                                    bitmap = btm.asImageBitmap(),
                                    contentDescription = "PDF Page ${currentPage + 1}",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer(scaleX = zoomLevel, scaleY = zoomLevel),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                        "jpg", "jpeg", "png" -> {
                            val imageUri = Uri.parse(document.uri)
                            ImageContent(imageUri, document)
                        }
                        else -> {
                            UnsupportedFileContent()
                        }
                    }

                    // Zoom controls
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .align(Alignment.BottomCenter),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Button(onClick = { zoomLevel /= 1.1f }, enabled = zoomLevel > 0.5f) {
                            Text("Zoom Out")
                        }
                        Button(onClick = { zoomLevel *= 1.1f }, enabled = zoomLevel < 3f) {
                            Text("Zoom In")
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun ImageContent(imageUri: Uri, document: Document) {
        Log.d(TAG, "ImageContent started with URI: $imageUri")
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageUri)
                .build(),
            contentDescription = document.name,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
            onState = { state ->
                when (state) {
                    is coil.compose.AsyncImagePainter.State.Loading -> {
                        Log.d(TAG, "Image is loading...")
                    }
                    is coil.compose.AsyncImagePainter.State.Success -> {
                        Log.d(TAG, "Image loaded successfully")
                    }
                    is coil.compose.AsyncImagePainter.State.Error -> {
                        Log.e(TAG, "Image loading failed: ${state.result.throwable?.message}")
                    }
                    else -> {
                        Log.d(TAG, "Unknown state: $state")
                    }
                }
            }
        )
    }

    @Composable
    fun UnsupportedFileContent() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
                contentDescription = null,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Unsupported file format",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            Text(
                text = "This type of file cannot be previewed",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    @Composable
    fun ErrorContent(errorMessage: String) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.error
            )
        }
    }

    fun adjustBitmapBrightness(bitmap: Bitmap, factor: Float): Bitmap {
        val colorMatrix = ColorMatrix(floatArrayOf(
            factor, 0f, 0f, 0f, 0f,
            0f, factor, 0f, 0f, 0f,
            0f, 0f, factor, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        ))
        val paint = Paint()
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)  // Correct usage
        val brightBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config)
        val canvas = Canvas(brightBitmap)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return brightBitmap
    }

