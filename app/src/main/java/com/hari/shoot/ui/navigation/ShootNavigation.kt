package com.hari.shoot.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hari.shoot.ui.screens.documents.DocumentListScreen
import com.hari.shoot.ui.screens.login.LoginScreen
import com.hari.shoot.ui.screens.viewer.DocumentViewerScreen
import com.hari.shoot.data.model.Document
import com.hari.shoot.ui.screens.upload.UploadDocumentScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.material3.Text
import com.hari.shoot.ui.screens.viewer.DocumentViewerViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Documents : Screen("documents")
    object Viewer : Screen("viewer")
    object Upload : Screen("upload")
}

@Composable
fun ShootNavigation(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Documents.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.Documents.route) {
            DocumentListScreen(
                onUploadClick = { navController.navigate(Screen.Upload.route) },
                onDocumentClick = { document ->
                    navController.navigate("${Screen.Viewer.route}/${document.id}")
                }
            )
        }
        
        composable(
            route = "${Screen.Viewer.route}/{documentId}",
            arguments = listOf(
                navArgument("documentId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val documentId = backStackEntry.arguments?.getLong("documentId") ?: 0L
            val viewModel: DocumentViewerViewModel = hiltViewModel()
            val document = viewModel.getDocument(documentId).collectAsState(initial = null).value
            
            document?.let { doc ->
                DocumentViewerScreen(
                    document = doc,
                    onBackClick = { navController.navigateUp() },
                    onShareClick = { /* TODO */ }
                )
            } ?: run {
                // Handle the case when the document is not found
                Text("Document not found")
            }
        }
        composable(Screen.Upload.route) {
            UploadDocumentScreen(
                onUploadComplete = { navController.popBackStack() }
            )
        }
    }
} 