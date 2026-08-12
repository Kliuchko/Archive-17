package com.kliuchko.archive17

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.kliuchko.archive17.presentation.navigation.Archive17App
import com.kliuchko.archive17.presentation.theme.Archive17Theme
import com.kliuchko.archive17.presentation.welcome.WelcomeScreen

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Archive17Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val context = LocalContext.current
                    val preferences = remember {
                        context.getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE)
                    }
                    var showWelcome by remember {
                        mutableStateOf(!preferences.getBoolean(KEY_HAS_ENTERED, false))
                    }

                    if (showWelcome) {
                        WelcomeScreen(
                            onEnter = {
                                preferences.edit().putBoolean(KEY_HAS_ENTERED, true).apply()
                                showWelcome = false
                            },
                        )
                    } else {
                        Archive17App()
                    }
                }
            }
        }
    }

    private companion object {
        const val PREFERENCES_NAME = "archive17_preferences"
        const val KEY_HAS_ENTERED = "has_entered_archive"
    }
}
