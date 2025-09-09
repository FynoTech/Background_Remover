package com.example.background_remove.adsManager

import android.content.Context
import android.util.Log


object SharedPreferenceUtils {
    val TAG = SharedPreferenceUtils::class.java.simpleName

    @JvmStatic
    fun saveVolume(context: Context?, key: String?, value: Int) {
        if (context == null) {
            return
        }
        val preferences = context.getSharedPreferences(
            "com.photo.editor.background.eraser.bgeraser",
            Context.MODE_PRIVATE
        )
        val editor = preferences.edit()
        editor.putInt(key, value)
        editor.commit()
    }

    @JvmStatic
    fun getVolume(context: Context?, key: String?): Int {
        if (context == null) {
            return 333
        }
        val preferences = context.getSharedPreferences(
            "com.photo.editor.background.eraser.bgeraser",
            Context.MODE_PRIVATE
        )
        return preferences.getInt(key, 20)
    }

    @JvmStatic
    fun saveInt(context: Context?, key: String?, value: Int) {
        if (context == null) {
            return
        }
        val preferences = context.getSharedPreferences(
            "com.photo.editor.background.eraser.bgeraser",
            Context.MODE_PRIVATE
        )
        val editor = preferences.edit()
        editor.putInt(key, value)
        editor.commit()
    }

    fun savefloat(context: Context?, key: String?, value: Float) {
        if (context == null) {
            return
        }
        val preferences = context.getSharedPreferences(
            "com.photo.editor.background.eraser.bgeraser",
            Context.MODE_PRIVATE
        )
        val editor = preferences.edit()
        editor.putFloat(key, value)
        editor.commit()
    }

    fun getfloatValue(context: Context?, key: String?): Float {
        if (context == null) {
            return 333f
        }
        val preferences = context.getSharedPreferences(
            "com.photo.editor.background.eraser.bgeraser",
            Context.MODE_PRIVATE
        )
        return preferences.getFloat(key, 111f)
    }

    @JvmStatic
    fun getIntValue(context: Context?, key: String?): Int {
        if (context == null) {
            return 333
        }
        val preferences = context.getSharedPreferences(
            "com.photo.editor.background.eraser.bgeraser",
            Context.MODE_PRIVATE
        )
        return preferences.getInt(key, 111)
    }

    @JvmStatic
    fun saveBool(context: Context?, key: String?, value: Boolean?) {
        if (context == null) {
            return
        }
        val preferences = context.getSharedPreferences(
            "com.photo.editor.background.eraser.bgeraser",
            Context.MODE_PRIVATE
        )
        val editor = preferences.edit()
        editor.putBoolean(key, value!!)
        editor.commit()
        editor.apply()
    }

    @JvmStatic
    fun getBoolValue(context: Context?, key: String?): Boolean {
        if (context == null) {
            return false
        }
        val preferences = context.getSharedPreferences(
            "com.photo.editor.background.eraser.bgeraser",
            Context.MODE_PRIVATE
        )
        return preferences.getBoolean(key, false)
    }

    @JvmStatic
    fun save(context: Context?, key: String?, value: String?) {
        if (context == null) {
            return
        }
        val preferences = context.getSharedPreferences(
            "com.photo.editor.background.eraser.bgeraser",
            Context.MODE_PRIVATE
        )
        val editor = preferences.edit()
        editor.putString(key, value)
        editor.apply()
    }

    @JvmStatic
    fun getValue(context: Context?, key: String?): String? {
        if (context == null) {
            return null
        }
        val preferences = context.getSharedPreferences(
            "com.photo.editor.background.eraser.bgeraser",
            Context.MODE_PRIVATE
        )
        return preferences.getString(key, null)
    }

    @JvmStatic
    fun saveBoolean(context: Context?, key: String, b: Boolean) {
        Log.e("context", "C: $context")
        Log.e("value", "C: $b")
        Log.e("key", "C: $key")
        if (context == null) {
            return
        }
        val preferences = context.getSharedPreferences(
            "com.photo.editor.background.eraser.bgeraser",
            Context.MODE_PRIVATE
        )
        val editor = preferences.edit()
        editor.putBoolean(key, b)
        editor.apply()
    }
}