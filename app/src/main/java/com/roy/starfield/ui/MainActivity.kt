package com.roy.starfield.ui

import android.graphics.Color
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.transition.Scene
import androidx.transition.Transition
import androidx.transition.TransitionInflater
import androidx.transition.TransitionManager
import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxAdListener
import com.applovin.mediation.MaxError
import com.applovin.mediation.ads.MaxAdView
import com.applovin.mediation.ads.MaxInterstitialAd
import com.roy.starfield.AccelerometerManager
import com.roy.starfield.R
import com.roy.starfield.ext.createAdBanner
import com.roy.starfield.ext.destroyAdBanner
import com.roy.starfield.ext.e
import com.roy.starfield.ext.logI
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
import java.util.concurrent.TimeUnit
import kotlin.math.min
import kotlin.math.pow

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
            .inflateTransition(R.transition.anim_screen_transitions)
    }
    private var adView: MaxAdView? = null
    private var interstitialAd: MaxInterstitialAd? = null
    private var retryAttempt = 0

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
        adView = this@MainActivity.createAdBanner(
            logTag = MainActivity::class.java.simpleName,
            bkgColor = Color.TRANSPARENT,
            viewGroup = findViewById(R.id.flAd),
            isAdaptiveBanner = true,
        )
        createAdInter()
    }

    private fun startMusic() {
        mediaPlayer = MediaPlayer.create(this, R.raw.bkg_music)
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
        e("roy93~", "handleTouch ${viewModel.getCurrentState()}")
        when (viewModel.getCurrentState()) {
            ScreenStates.START_GAME -> {
                spaceShipView.boost()
                starFieldView.setTrails()
            }

            ScreenStates.GAME_MENU -> {
                showAd {
                    pushUIState(ScreenStates.START_GAME)
                }
            }

            else -> {
                e("roy93~", "else")
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

    override fun onDestroy() {
        findViewById<ViewGroup>(R.id.flAd).destroyAdBanner(adView)
        super.onDestroy()
    }

    private fun createAdInter() {
        val enableAdInter = getString(R.string.EnableAdInter) == "true"
        if (enableAdInter) {
            interstitialAd = MaxInterstitialAd(getString(R.string.INTER), this)
            interstitialAd?.let { ad ->
                ad.setListener(object : MaxAdListener {
                    override fun onAdLoaded(p0: MaxAd?) {
                        logI("onAdLoaded")
                        retryAttempt = 0
                    }

                    override fun onAdDisplayed(p0: MaxAd?) {
                        logI("onAdDisplayed")
                    }

                    override fun onAdHidden(p0: MaxAd?) {
                        logI("onAdHidden")
                        // Interstitial Ad is hidden. Pre-load the next ad
                        interstitialAd?.loadAd()
                    }

                    override fun onAdClicked(p0: MaxAd?) {
                        logI("onAdClicked")
                    }

                    override fun onAdLoadFailed(p0: String?, p1: MaxError?) {
                        logI("onAdLoadFailed")
                        retryAttempt++
                        val delayMillis =
                            TimeUnit.SECONDS.toMillis(2.0.pow(min(6, retryAttempt)).toLong())

                        Handler(Looper.getMainLooper()).postDelayed(
                            {
                                interstitialAd?.loadAd()
                            }, delayMillis
                        )
                    }

                    override fun onAdDisplayFailed(p0: MaxAd?, p1: MaxError?) {
                        logI("onAdDisplayFailed")
                        // Interstitial ad failed to display. We recommend loading the next ad.
                        interstitialAd?.loadAd()
                    }

                })
                ad.setRevenueListener {
                    logI("onAdDisplayed")
                }

                // Load the first ad.
                ad.loadAd()
            }
        }
    }

    private fun showAd(runnable: Runnable? = null) {
        val enableAdInter = getString(R.string.EnableAdInter) == "true"
        if (enableAdInter) {
            if (interstitialAd == null) {
                runnable?.run()
            } else {
                interstitialAd?.let { ad ->
                    if (ad.isReady) {
                        ad.showAd()
                        runnable?.run()
                    } else {
                        runnable?.run()
                    }
                }
            }
        } else {
            Toast.makeText(this, "Applovin show ad Inter in debug mode", Toast.LENGTH_SHORT).show()
            runnable?.run()
        }
    }
}
