package com.example.coded.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.coded.R
import com.example.coded.navigation.Screen
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavController) {
    var logoVisible by remember { mutableStateOf(false) }
    var poweredByVisible by remember { mutableStateOf(false) }

    val logoScale by animateFloatAsState(
        targetValue = if (logoVisible) 1f else 0.3f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ), label = "logo_scale"
    )

    val logoAlpha by animateFloatAsState(
        targetValue = if (logoVisible) 1f else 0f,
        animationSpec = tween(800), label = "logo_alpha"
    )

    val poweredByAlpha by animateFloatAsState(
        targetValue = if (poweredByVisible) 1f else 0f,
        animationSpec = tween(600), label = "powered_alpha"
    )

    LaunchedEffect(Unit) {
        logoVisible = true
        delay(1500)
        poweredByVisible = true
        delay(1500)
        // CHANGED: Navigate to Login for testing
        navController.navigate(Screen.Login.route) {
            popUpTo(Screen.Splash.route) { inclusive = true }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF013B33)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(80.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.herdmat_logo),
                    contentDescription = "Herdmat Logo",
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .scale(logoScale)
                        .alpha(logoAlpha),
                    contentScale = ContentScale.Fit
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .alpha(poweredByAlpha)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "Powered by",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Image(
                    painter = painterResource(id = R.drawable.coded_logo),
                    contentDescription = "Coded Logo",
                    modifier = Modifier.height(40.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}
