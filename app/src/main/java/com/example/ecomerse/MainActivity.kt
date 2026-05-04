package com.example.ecomerse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.ecomerse.data.SessionManager
import com.example.ecomerse.model.UserRole
import com.example.ecomerse.ui.*
import com.example.ecomerse.ui.theme.EcomerseTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Push data to Firebase (Run once, then you can remove this line)
        com.example.ecomerse.data.AppRepository.seedDataToFirebase()

        setContent {
            val systemTheme = isSystemInDarkTheme()
            var isDarkMode by remember { mutableStateOf(systemTheme) }
            
            EcomerseTheme(darkTheme = isDarkMode) {
                MainContent(
                    isDarkMode = isDarkMode,
                    onToggleTheme = { isDarkMode = !isDarkMode }
                )
            }
        }
    }
}

enum class ScreenState {
    LANDING,
    LOGIN,
    SIGNUP
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContent(isDarkMode: Boolean, onToggleTheme: () -> Unit) {
    val currentUser by SessionManager.currentUser.collectAsState()
    var currentScreen by remember { mutableStateOf(ScreenState.LANDING) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        if (currentUser != null) "Ecomerse - ${currentUser?.role?.name?.replace("_", " ")}" 
                        else "ECOMERSE"
                    ) 
                },
                actions = {
                    IconButton(onClick = onToggleTheme) {
                        Icon(
                            if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Theme"
                        )
                    }
                    if (currentUser != null) {
                        IconButton(onClick = { SessionManager.logout() }) {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (val user = currentUser) {
                null -> {
                    when (currentScreen) {
                        ScreenState.LANDING -> LandingScreen(
                            onNavigateToLogin = { currentScreen = ScreenState.LOGIN },
                            onNavigateToSignUp = { currentScreen = ScreenState.SIGNUP }
                        )
                        ScreenState.LOGIN -> LoginScreen(onBack = { currentScreen = ScreenState.LANDING })
                        ScreenState.SIGNUP -> SignUpScreen(onBack = { currentScreen = ScreenState.LANDING })
                    }
                }
                else -> {
                    // Reset screen state when logged in
                    LaunchedEffect(Unit) { currentScreen = ScreenState.LANDING }

                    when (user.role) {
                        UserRole.CUSTOMER -> com.example.ecomerse.ui.CustomerDashboardScreen()
                        UserRole.DISTRIBUTOR -> DistributorInventoryScreen()
                        UserRole.COMPANY_MANAGER -> CompanyManagerDashboard()
                    }
                }
            }
        }
    }
}
