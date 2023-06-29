package com.roy.starfield

import android.widget.ImageView
import com.roy.starfield.ui.MainActivity
import com.roy.starfield.utils.ScreenStates
import com.roy.starfield.views.BlinkingImage
import com.roy.starfield.views.LogoView
import kotlinx.android.synthetic.main.a_main.starFieldView

fun MainActivity.observeScreenStates() {
    viewModel.observeScreenState().observe(this) {
        starFieldView?.processScreenState(it)
        when (it) {
            ScreenStates.APP_INIT -> {
                transitionToScene(appInitScene)
            }

            ScreenStates.GAME_MENU -> {
                transitionToScene(gameMenuScene)
                gameMenuScene.sceneRoot.findViewById<LogoView>(R.id.logoView)?.enableTinkling =
                    true
                gameMenuScene.sceneRoot.findViewById<BlinkingImage>(R.id.blinkingImage)
                    ?.startBlinking()
            }

            ScreenStates.START_GAME -> {
                transitionToScene(startGameScene)
                startGameScene.sceneRoot.findViewById<ImageView>(R.id.ivPause).setOnClickListener {
                    onBackPressed()
                }
            }
        }
    }
}
