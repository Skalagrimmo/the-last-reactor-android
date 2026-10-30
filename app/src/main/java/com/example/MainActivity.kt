package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.GameScreen
import com.example.ui.GameViewModel
import com.example.ui.ScreenState
import com.example.ui.TitleScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = Color(0xFF090D14)
        ) {
          val gameViewModel: GameViewModel = viewModel()
          val screenState by gameViewModel.screenState.collectAsState()
          val hasSavedGame by gameViewModel.hasSavedGame.collectAsState()

          when (screenState) {
            ScreenState.TITLE -> {
              TitleScreen(
                hasSavedGame = hasSavedGame,
                onNewGame = { gameViewModel.startNewGame() },
                onContinueGame = { gameViewModel.continueGame() }
              )
            }
            ScreenState.PLAYING -> {
              GameScreen(viewModel = gameViewModel)
            }
          }
        }
      }
    }
  }
}
