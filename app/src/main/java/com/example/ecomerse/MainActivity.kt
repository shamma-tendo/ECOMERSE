package com.example.ecomerse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.ecomerse.data.SessionManager
import com.example.ecomerse.model.UserRole
import com.example.ecomerse.ui.*
import com.example.ecomerse.ui.theme.EcomerseTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EcomerseTheme {
                MainContent()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContent() {
    val currentUser by SessionManager.currentUser.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (currentUser != null) {
                TopAppBar(
                    title = { Text("Ecomerse - ${currentUser?.role?.name?.replace("_", " ")}") },
                    actions = {
                        IconButton(onClick = { SessionManager.logout() }) {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout")
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (val user = currentUser) {
                null -> LoginScreen()
                else -> {
                    when (user.role) {
                        UserRole.SALES_AGENT -> SalesAgentScreen()
                        UserRole.DISTRIBUTOR -> DistributorInventoryScreen()
                        UserRole.STOCK_SUPERVISOR -> DistributorInventoryScreen()
                        UserRole.COMPANY_MANAGER -> DashboardScreen()
                    }
                }
            }
        }
    }
}
