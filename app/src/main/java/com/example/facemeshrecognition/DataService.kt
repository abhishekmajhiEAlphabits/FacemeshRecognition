package com.example.facemeshrecognition

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

class DataService : Service() {


    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        while(true){
            Log.d("abhi","we are in..");
        }
        return START_STICKY
    }

    // execution of the service will
    // stop on calling this method
    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}