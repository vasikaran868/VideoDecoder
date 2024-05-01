package com.example.videcoder.googleTestCode

import android.os.Handler
import android.os.HandlerThread
import com.example.videcoder.rlog

class MyThreadWithLooper(name: String?) : HandlerThread(name) {
    private var mHandler: Handler? = null
    protected override fun onLooperPrepared() {
        super.onLooperPrepared()
        // Initialize your Handler here after Looper is prepared
        "looper prepared...${looper}".rlog()
        mHandler = Handler(looper)
    }

    fun postTask(task: Runnable?) {
        if (task != null) {
            mHandler?.post(task)
        }
    }
}