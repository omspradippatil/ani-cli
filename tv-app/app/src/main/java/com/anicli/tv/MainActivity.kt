package com.anicli.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.anicli.tv.core.theme.AniCliTvTheme
import com.anicli.tv.ui.TvApp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            AniCliTvTheme {
                TvApp()
            }
        }
    }
}
