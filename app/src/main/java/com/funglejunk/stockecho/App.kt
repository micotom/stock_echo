package com.funglejunk.stockecho

import android.app.Application
import arrow.core.extensions.list.traverse.sequence
import arrow.core.extensions.listk.monad.map
import arrow.fx.IO
import arrow.fx.extensions.io.applicative.applicative
import arrow.fx.fix
import com.funglejunk.stockecho.repo.Allocation
import com.funglejunk.stockecho.repo.SharedPrefs
import timber.log.Timber
import java.math.BigDecimal

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
        injectMockData()
    }

    private fun injectMockData() {
        val androidPrefs = SharedPrefs(this)
        androidPrefs.clear()
        listOf(
            Allocation("LU1737652823", 8.0, BigDecimal.valueOf(61.99)),
            Allocation("IE00BKX55T58", 132.916, BigDecimal.valueOf(59.807)),
            Allocation("IE00BZ163L38", 35.559, BigDecimal.valueOf(46.3424)),
            Allocation("IE00B3VVMM84", 47.234, BigDecimal.valueOf(50.5778)),
            Allocation("IE00B1XNHC34", 68.0, BigDecimal.valueOf(7.4)),
            Allocation("IE00BK5BQV03", 37.0, BigDecimal.valueOf(55.58)),
            Allocation("IE00BK5BR733", 13.0, BigDecimal.valueOf(45.99))
        ).map {
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

}