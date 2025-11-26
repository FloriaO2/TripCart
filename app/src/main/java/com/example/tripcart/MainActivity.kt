package com.example.tripcart

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.tripcart.navigation.TripCartNavGraph
import com.example.tripcart.ui.theme.TripCartTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TripCartTheme {
                TripCartNavGraph()
            }
        }
    }
}