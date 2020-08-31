package com.funglejunk.stockecho.model

import arrow.fx.IO
import com.funglejunk.stockecho.data.ACTION_REQUEST_UPDATE

class WidgetProviderInteractor(private val workerService: WorkerService) {

    fun onNewIntent(action: String, callback: IO<Unit>): IO<Unit> = when (action) {
        ACTION_REQUEST_UPDATE -> requestUpdate(callback)
        else -> callback
    }

    private fun requestUpdate(callback: IO<Unit>): IO<Unit> = IO {
        workerService.work()
    }.flatMap {
        callback
    }

}