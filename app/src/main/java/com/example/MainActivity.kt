package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.data.repository.VfxRepository
import com.example.ui.navigation.VfxAppNavigation
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.VfxBlack

class MainActivity : ComponentActivity() {
    private lateinit var repository: VfxRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        repository = VfxRepository(applicationContext)

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = VfxBlack
                ) {
                    VfxAppNavigation(repository = repository)
                }
            }
        }
    }
}
