package com.roy.starfield.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.roy.starfield.utils.ScreenStates

class ViewModel : ViewModel() {

    private val screenStateLiveData = MutableLiveData<ScreenStates>()

    fun observeScreenState(): LiveData<ScreenStates> = screenStateLiveData

    fun updateUIState(screenStates: ScreenStates) {
        screenStateLiveData.postValue(screenStates)
    }

    fun getCurrentState(): ScreenStates? = observeScreenState().value
}
