package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.core.di.ParkSpaceAppContainer
import com.example.core.navigation.ParkSpaceNavHost
import com.example.feature.foundation.ParkSpaceFoundationViewModel
import com.example.ui.theme.ParkSpaceTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val appContainer = remember { ParkSpaceAppContainer(applicationContext) }
      val foundationViewModel: ParkSpaceFoundationViewModel =
        viewModel(factory = ParkSpaceFoundationViewModel.Factory(appContainer))

      val systemDark = isSystemInDarkTheme()
      var useDarkTheme by rememberSaveable { mutableStateOf(systemDark) }

      ParkSpaceTheme(darkTheme = useDarkTheme) {
        ParkSpaceNavHost(
          appContainer = appContainer,
          viewModel = foundationViewModel,
          isDarkTheme = useDarkTheme,
          onToggleTheme = { useDarkTheme = !useDarkTheme },
        )
      }
    }
  }
}
