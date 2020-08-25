package com.funglejunk.stockecho.model

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.JobIntentService
import kotlinx.serialization.UnsafeSerializationApi

class UpdateCommandReceiver : BroadcastReceiver() {

    @UnsafeSerializationApi
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null && intent != null) {
            JobIntentService.enqueueWork(
                context, UpdateService::class.java, 0x42, intent
            )
        }
    }

}