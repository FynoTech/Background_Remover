package com.example.background_remove

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope


class MyApp : Application(), Application.ActivityLifecycleCallbacks,
    DefaultLifecycleObserver, CoroutineScope by MainScope() {

    override fun onCreate(){
        super<Application>.onCreate()

        registerActivityLifecycleCallbacks(this)




        FirebaseApp.initializeApp(this)


    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)


    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)



    }

    override fun onActivityCreated(p0: Activity, p1: Bundle?) {

    }

    override fun onActivityStarted(activity: Activity) {
        // An ad activity is started when an ad is showing, which could be AdActivity class from Google

    }

    override fun onActivityResumed(p0: Activity) {

    }

    override fun onActivityPaused(p0: Activity) {

    }

    override fun onActivityStopped(p0: Activity) {

    }

    override fun onActivitySaveInstanceState(p0: Activity, p1: Bundle) {

    }

    override fun onActivityDestroyed(p0: Activity) {

    }








}