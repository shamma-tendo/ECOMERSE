package com.example.ecomerse

import android.os.Bundle
import android.util.Log
import android.content.pm.ApplicationInfo
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.ecomerse.data.SessionManager
import com.example.ecomerse.model.UserRole
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.example.ecomerse.ui.*
import com.example.ecomerse.ui.theme.EcomerseTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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

private const val FIREBASE_TEST_TAG = "FirebaseProbe"

private data class FirebaseProbeUiState(
    val isRunning: Boolean = false,
    val isSuccessful: Boolean? = null,
    val title: String = "",
    val message: String = "",
    val details: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContent(isDarkMode: Boolean, onToggleTheme: () -> Unit) {
    val currentUser by SessionManager.currentUser.collectAsState()
    var currentScreen by remember { mutableStateOf(ScreenState.LANDING) }
    var showFirebaseProbeDialog by remember { mutableStateOf(false) }
    var firebaseProbeState by remember { mutableStateOf(FirebaseProbeUiState()) }
    val context = LocalContext.current
    val isDebuggableBuild = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    fun startFirebaseProbe() {
        showFirebaseProbeDialog = true
        firebaseProbeState = FirebaseProbeUiState(
            isRunning = true,
            title = "Testing Firestore connection",
            message = "Writing a temporary document to the debug test collection...",
            details = ""
        )

        val firestore = FirebaseFirestore.getInstance()
        val probeCollection = "_debug_connection_checks"
        val probeDocId = "android_${System.currentTimeMillis()}"
        val probeDoc = firestore.collection(probeCollection).document(probeDocId)
        val probePayload = mapOf(
            "source" to "android-debug",
            "buildType" to "debug-probe",
            "clientTimestamp" to System.currentTimeMillis(),
            "serverTimestamp" to FieldValue.serverTimestamp()
        )

        Log.d(FIREBASE_TEST_TAG, "Starting Firestore probe at $probeCollection/$probeDocId")

        probeDoc.set(probePayload)
            .addOnSuccessListener {
                Log.d(FIREBASE_TEST_TAG, "Write succeeded for $probeCollection/$probeDocId")
                firebaseProbeState = firebaseProbeState.copy(
                    message = "Write succeeded. Reading the document back..."
                )

                probeDoc.get()
                    .addOnSuccessListener { snapshot ->
                        val exists = snapshot.exists()
                        val keys = snapshot.data?.keys?.joinToString().orEmpty()
                        Log.d(
                            FIREBASE_TEST_TAG,
                            "Read succeeded for $probeCollection/$probeDocId exists=$exists keys=$keys"
                        )

                        probeDoc.delete()
                            .addOnCompleteListener { deleteTask ->
                                val cleanupMessage = if (deleteTask.isSuccessful) {
                                    "Temporary document cleaned up successfully."
                                } else {
                                    "Cleanup failed: ${deleteTask.exception?.localizedMessage.orEmpty()}"
                                }

                                firebaseProbeState = FirebaseProbeUiState(
                                    isRunning = false,
                                    isSuccessful = true,
                                    title = "Firestore connection works",
                                    message = "Read succeeded from Firestore.",
                                    details = "exists=$exists\nfields=$keys\n$cleanupMessage"
                                )
                            }
                    }
                    .addOnFailureListener { error ->
                        Log.e(FIREBASE_TEST_TAG, "Read failed for $probeCollection/$probeDocId", error)
                        probeDoc.delete()
                        firebaseProbeState = FirebaseProbeUiState(
                            isRunning = false,
                            isSuccessful = false,
                            title = "Firestore read failed",
                            message = error.localizedMessage ?: "Unknown error",
                            details = "The write succeeded, but the read-back step failed. Check Firestore rules."
                        )
                    }
            }
            .addOnFailureListener { error ->
                Log.e(FIREBASE_TEST_TAG, "Write failed for $probeCollection/$probeDocId", error)
                firebaseProbeState = FirebaseProbeUiState(
                    isRunning = false,
                    isSuccessful = false,
                    title = "Firestore write failed",
                    message = error.localizedMessage ?: "Unknown error",
                    details = "Check your Firebase project, internet permission, and Firestore rules."
                )
            }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.foundation.Image(
                            painter = painterResource(id = R.drawable.ic_app_logo),
                            contentDescription = "Ecomerse logo",
                            modifier = Modifier.size(28.dp),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            if (currentUser != null) "Ecomerse - ${currentUser?.role?.name?.replace("_", " ")}"
                            else "ECOMERSE"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onToggleTheme) {
                        Icon(
                            if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Theme"
                        )
                    }
                    if (isDebuggableBuild) {
                        IconButton(onClick = { startFirebaseProbe() }) {
                            Icon(
                                Icons.Default.BugReport,
                                contentDescription = "Test Firebase connection"
                            )
                        }
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
                        UserRole.CUSTOMER -> CustomerDashboardScreen()
                        UserRole.DISTRIBUTOR -> DistributorInventoryScreen()
                        UserRole.COMPANY_MANAGER -> CompanyManagerDashboard()
                    }
                }
            }
        }
    }

    if (showFirebaseProbeDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!firebaseProbeState.isRunning) {
                    showFirebaseProbeDialog = false
                }
            },
            title = { Text(firebaseProbeState.title.ifBlank { "Firestore connection test" }) },
            text = {
                Column {
                    Text(firebaseProbeState.message.ifBlank { "Running Firestore probe..." })
                    if (firebaseProbeState.details.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                        Text(firebaseProbeState.details)
                    }
                    if (firebaseProbeState.isRunning) {
                                Spacer(modifier = Modifier.height(16.dp))
                        CircularProgressIndicator()
                    }
                }
            },
            confirmButton = {
                if (!firebaseProbeState.isRunning) {
                    TextButton(onClick = { startFirebaseProbe() }) {
                        Text("Run again")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showFirebaseProbeDialog = false },
                    enabled = !firebaseProbeState.isRunning
                ) {
                    Text("Close")
                }
            }
        )
    }
}
