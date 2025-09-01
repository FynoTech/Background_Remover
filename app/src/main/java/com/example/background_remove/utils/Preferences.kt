package com.example.background_remove.utils

import android.content.Context
import android.preference.PreferenceManager

object Preferences {


    @JvmStatic
    fun getBoolValue(context: Context,string: String): Boolean {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        return preferences.getBoolean(string,false)
    }
    @JvmStatic
    fun setBoolValue(context: Context, city: String,boolean: Boolean) {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        preferences.edit().putBoolean(city, boolean).apply()
    }
    fun setFirstTime(context: Context, isRated: Boolean) {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        preferences.edit().putBoolean("isFirstTime", isRated).apply()
    }

    fun getFirstTime(context: Context): Boolean {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        return preferences.getBoolean("isFirstTime", true)
    }
}