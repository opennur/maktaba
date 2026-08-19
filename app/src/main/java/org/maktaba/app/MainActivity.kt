package org.maktaba.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import org.maktaba.app.ui.MaktabaApp
import org.maktaba.app.ui.MaktabaTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MaktabaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaktabaTheme {
                MaktabaApp(viewModel)
            }
        }
    }
}
