package com.funglejunk.stockecho.model

import android.content.Context
import android.content.Intent
import androidx.core.app.JobIntentService
import kotlinx.serialization.UnsafeSerializationApi

interface WorkerService {

    fun work()

    @UnsafeSerializationApi
    class Impl(private val context: Context) : WorkerService {

        override fun work() {
            JobIntentService.enqueueWork(
                context, UpdateService::class.java, 0x42, Intent()
            )
        }

    }

}