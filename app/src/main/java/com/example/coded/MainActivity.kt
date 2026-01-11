package com.example.coded

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import com.example.coded.data.OasisAuthRepository
import com.example.coded.navigation.OasisNavGraph
import com.example.coded.ui.theme.CodedTheme
import com.google.firebase.FirebaseApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        setContent {
            CodedTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    OasisApp()
                }
            }
        }
    }
}

@Composable
fun OasisApp() {
    val navController = rememberNavController()
    val context = LocalContext.current

    // Initialize auth repository (from data package)
    val authRepository = OasisAuthRepository(context)

    OasisNavGraph(
        navController = navController,
        authRepository = authRepository
    )
}