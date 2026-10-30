package com.example.ui

import android.opengl.GLSurfaceView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ecs.EntityManager
import com.example.engine.GameMap
import com.example.opengl.ReactorGLRenderer

@Composable
fun GameGLView(
  entityManager: EntityManager,
  map: GameMap,
  getWardenId: () -> Int,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val glSurfaceView = remember(context) {
    GLSurfaceView(context).apply {
      setEGLContextClientVersion(2)
      setRenderer(ReactorGLRenderer(entityManager, map, getWardenId))
      renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
    }
  }

  DisposableEffect(glSurfaceView) {
    glSurfaceView.onResume()
    onDispose {
      glSurfaceView.onPause()
    }
  }

  AndroidView(
    factory = { glSurfaceView },
    modifier = modifier.fillMaxSize()
  )
}
