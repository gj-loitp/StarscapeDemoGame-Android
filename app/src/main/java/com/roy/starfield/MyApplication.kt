package com.roy.starfield

import android.app.Application
import com.roy.starfield.ext.setupApplovinAd

//TODO applovin
//TODO double to exit app

//done
//permission ad_id
//leakcanary
//proguard
//change icon launcher
//policy
//rate app, share app, more app
//keystore

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        this.setupApplovinAd()
    }
}