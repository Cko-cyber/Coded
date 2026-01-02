package com.herdmat.coded.ui.entry

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.herdmat.coded.managers.AnonymousClientSessionManager

@Composable
fun EntryScreen(navController: NavController) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = {
            navController.navigate("provider_login")
        }) {
            Text("Provider Login")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            val sessionManager = AnonymousClientSessionManager.getInstance()
            sessionManager.startNewSession()
            navController.navigate("job_creation")
        }) {
            Text("Post a Job")
        }
    }
}