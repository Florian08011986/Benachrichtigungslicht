package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.EdgeDashboardScreen
import com.example.ui.EdgeDashboardViewModel
import com.example.ui.theme.EdgeLightingTheme

class MainActivity : ComponentActivity() {

    private val viewModel: EdgeDashboardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            EdgeLightingTheme {
                EdgeDashboardScreen(viewModel = viewModel)
            }
        }
    }
}
