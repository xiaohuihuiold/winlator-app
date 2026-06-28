package com.xhhold.winlator.ui.screens.home

import android.app.Activity
import android.app.Application
import android.content.Intent
import androidx.compose.runtime.compositionLocalOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import com.winlator.WinlatorActivity
import com.winlator.XServerDisplayActivity
import com.winlator.XrActivity

val LocalHomeViewModel = compositionLocalOf<HomeViewModel> {
    error("HomeViewModel not provided")
}

class HomeViewModel() : ViewModel() {

    fun openWinlator(activity: Activity?) {
        if (activity == null) {
            throw IllegalArgumentException("Activity cannot be null")
        }
        if (!XrActivity.isSupported()) {
            val intent = Intent(activity, WinlatorActivity::class.java)
            activity.startActivity(intent)
        } else {
            XrActivity.openIntent(activity, 0, null)
        }
    }

    fun runContainer(activity: Activity?, containerId: Int) {
        if (activity == null) {
            throw IllegalArgumentException("Activity cannot be null")
        }
        if (!XrActivity.isSupported()) {
            val intent = Intent(activity, XServerDisplayActivity::class.java)
            intent.putExtra("container_id", containerId)
            activity.startActivity(intent)
        } else XrActivity.openIntent(activity, containerId, null)
    }

    fun openEditor(activity: Activity?, containerId: Int) {}
    fun showInfo(activity: Activity?, containerId: Int) {}
}