package com.anicli.tv

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface as MobileSurface
import androidx.compose.ui.Modifier
import com.anicli.tv.core.theme.AniCliTvTheme
import com.anicli.tv.ui.TvApp
import com.anicli.tv.ui.mobile.MobileApp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val uiModeManager = getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        val isTv = uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION

        setContent {
            if (isTv) {
                AniCliTvTheme {
                    TvApp()
                }
            } else {
                MaterialTheme {
                    MobileSurface(modifier = Modifier.fillMaxSize()) {
                        MobileApp()
                    }
                }
            }
        }
    }
}
