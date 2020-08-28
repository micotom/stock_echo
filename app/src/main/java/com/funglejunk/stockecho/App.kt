package com.funglejunk.stockecho

import android.app.Application
import arrow.core.extensions.list.traverse.sequence
import arrow.core.extensions.listk.monad.map
import arrow.fx.IO
import arrow.fx.extensions.fx
import arrow.fx.extensions.io.applicative.applicative
import arrow.fx.fix
import com.facebook.stetho.Stetho
import com.funglejunk.stockecho.repo.Allocation
import com.funglejunk.stockecho.repo.MockPrefs
import com.funglejunk.stockecho.repo.SharedPrefs
import timber.log.Timber
import java.math.BigDecimal

@Suppress("unused")
class App : Application() {

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
        Stetho.initializeWithDefaults(this)
        injectMockData()
    }

    private fun injectMockData() {
        val androidPrefs = SharedPrefs(this)
        IO.fx { androidPrefs.clear().bind() }.attempt().unsafeRunAsync {
            it.fold(
                {
                    Timber.e("unable to clear prefs. abort.")
                },
                {
                    MockPrefs.mockAllocations.map {
                        androidPrefs.write(it)
                    }.sequence(IO.applicative()).fix().attempt().unsafeRunSync().fold(
                        {
                            Timber.e("error injecting mock data: $it")
                        },
                        {
                            Timber.d("mock data injected: ${it.map { it }.joinToString()}")
                        }
                    )
                }
            )
        }
    }

}