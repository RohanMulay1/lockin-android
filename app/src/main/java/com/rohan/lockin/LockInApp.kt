package com.rohan.lockin

import android.app.Application
import android.content.Context
import com.rohan.lockin.data.LockInRepository
import com.rohan.lockin.data.SettingsStore
import com.rohan.lockin.data.db.LockInDatabase
import com.rohan.lockin.notify.Notifications

class LockInApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Notifications.createChannels(this)
    }
}

/** Manual constructor-injection container; one instance per process. */
class AppContainer(context: Context) {
    val store = SettingsStore(context)
    private val database = LockInDatabase.build(context)
    val repository = LockInRepository(store, database)
}

val Context.lockInApp: LockInApp
    get() = applicationContext as LockInApp
