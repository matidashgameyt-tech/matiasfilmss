package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.ClientViewModel
import com.example.ui.MatiasFilmsApp
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val clientViewModel: ClientViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MatiasFilmsApp(viewModel = clientViewModel)
            }
        }
    }
}
