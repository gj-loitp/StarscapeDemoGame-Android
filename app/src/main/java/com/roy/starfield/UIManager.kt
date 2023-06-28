package com.roy.starfield

import android.widget.ImageView
import androidx.lifecycle.Observer
import com.roy.starfield.utils.ScreenStates
import com.roy.starfield.views.BlinkingImage
import com.roy.starfield.views.LogoView
import com.thelumierguy.starfield.R
import kotlinx.android.synthetic.main.activity_main.*

fun MainActivity.observeScreenStates() {
    mainViewModel.observeScreenState().observe(this, Observer {
        star_field?.processScreenState(it)
        when (it) {
            ScreenStates.APP_INIT -> {
                transitionToScene(appInitScene)
            }
            ScreenStates.GAME_MENU -> {
                transitionToScene(gameMenuScene)
                gameMenuScene.sceneRoot.findViewById<LogoView>(R.id.imageView)?.enableTinkling =
                    true
                gameMenuScene.sceneRoot.findViewById<BlinkingImage>(R.id.iv_text)?.startBlinking()
            }
            ScreenStates.START_GAME -> {
                transitionToScene(startGameScene)
                startGameScene.sceneRoot.findViewById<ImageView>(R.id.iv_pause).setOnClickListener {
                    onBackPressed()
                }
            }
        }
    })
}