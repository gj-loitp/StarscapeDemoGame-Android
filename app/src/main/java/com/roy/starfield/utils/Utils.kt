package com.roy.starfield.utils

const val ALPHA = 0.05F

/**
 * Low Pass filter which smoothes out the data
 * We will only be using the
 */
fun lowPass(
    input: FloatArray,
    output: FloatArray
) {
    output[0] = ALPHA * input[1] + output[0] * 1.0f - ALPHA
}

sealed class ScreenStates {
    object APP_INIT : ScreenStates()
    object GAME_MENU : ScreenStates()
    object START_GAME : ScreenStates()
}