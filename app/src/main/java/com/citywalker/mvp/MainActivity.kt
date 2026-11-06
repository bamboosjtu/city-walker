package com.citywalker.mvp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.citywalker.mvp.ui.CityWalkerApp

class MainActivity : ComponentActivity() {
    private val viewModel: CityWalkerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CityWalkerApp(viewModel)
        }
    }
}
