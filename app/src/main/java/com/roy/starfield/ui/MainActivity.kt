package com.roy.starfield.ui

import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.transition.Scene
import androidx.transition.Transition
import androidx.transition.TransitionInflater
import androidx.transition.TransitionManager
import com.roy.starfield.AccelerometerManager
import com.roy.starfield.R
import com.roy.starfield.observeScreenStates
import com.roy.starfield.utils.ScreenStates
import com.roy.starfield.viewmodels.ViewModel
import com.roy.starfield.views.SpaceShipView
import kotlinx.android.synthetic.main.a_main.flFrame
import kotlinx.android.synthetic.main.a_main.mRootView
import kotlinx.android.synthetic.main.a_main.starFieldView
import kotlinx.android.synthetic.main.view_scene_menu.spaceShipView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private val transitionManager: TransitionManager by lazy {
        TransitionManager().apply {
            setTransition(appInitScene, gameMenuScene, transition)
            setTransition(gameMenuScene, startGameScene, transition)
            setTransition(startGameScene, gameMenuScene, transition)
        }
    }
    private var mediaPlayer: MediaPlayer? = null
    private var accelerometerManager: AccelerometerManager? = null
    val viewModel by lazy {
        ViewModelProvider(this)[ViewModel::class.java]
    }
    val appInitScene: Scene by lazy { createScene(R.layout.view_scene_app_init) }
    val gameMenuScene: Scene by lazy { createScene(R.layout.view_scene_menu) }
    val startGameScene: Scene by lazy { createScene(R.layout.view_scene_game_start) }
    private val transition: Transition by lazy {
        TransitionInflater.from(this)
            .inflateTransition(R.transition.screen_transitions)
    }

    fun transitionToScene(scene: Scene) {
        transitionManager.transitionTo(scene)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        goFullScreen()
        setContentView(R.layout.a_main)
        addTouchHandler()
        observeScreenStates()
        addAccelerometerListener()
        initMenu()
    }

    private fun startMusic() {
        mediaPlayer = MediaPlayer.create(this, R.raw.music)
        mediaPlayer?.setOnPreparedListener {
            it.start()
        }
        mediaPlayer?.setOnCompletionListener {
            it.start()
        }
    }

    private fun addAccelerometerListener() {
        accelerometerManager = AccelerometerManager(this) { sensorEvent ->
            if (viewModel.getCurrentState() == ScreenStates.START_GAME) {
                startGameScene.sceneRoot.findViewById<SpaceShipView>(R.id.spaceShipView)
                    ?.processSensorEvents(sensorEvent)
            }
            starFieldView?.processSensorEvents(sensorEvent)
        }
        accelerometerManager?.let {
            lifecycle.addObserver(it)
        }
    }

    private fun addTouchHandler() {
        mRootView.setOnClickListener {
            handleTouch()
        }
    }

    private fun handleTouch() {
        when (viewModel.getCurrentState()) {
            ScreenStates.START_GAME -> {
                spaceShipView.boost()
                starFieldView.setTrails()
            }

            ScreenStates.GAME_MENU -> {
                pushUIState(ScreenStates.START_GAME)
            }

            else -> {
            }
        }
    }

    private fun initMenu() {
        lifecycleScope.launch(Dispatchers.Main) {
            pushUIState(ScreenStates.APP_INIT)
            delay(3000)
            pushUIState(ScreenStates.GAME_MENU)
        }
    }

    private fun pushUIState(screenStates: ScreenStates) {
        viewModel.updateUIState(screenStates)
    }

    private fun goFullScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        } else {
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE
                    // Set the content to appear under the system bars so that the
                    // content doesn't resize when the system bars hide and show.
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    // Hide the nav bar and status bar
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN)
        }
    }

    private fun createScene(@LayoutRes layout: Int) =
        Scene.getSceneForLayout(flFrame as ViewGroup, layout, this)

    //TODO fix onBackPressed
    override fun onBackPressed() {
        if (viewModel.getCurrentState() == ScreenStates.START_GAME) {
            pushUIState(ScreenStates.GAME_MENU)
        } else {
            super.onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        startMusic()
    }

    override fun onPause() {
        super.onPause()
        mediaPlayer?.apply {
            stop()
            release()
        }
    }
}
