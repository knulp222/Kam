package com.example

import android.app.Application
import com.example.data.db.AppDatabase
import com.example.data.repository.KaamelottRepository

class KaamelottApp : Application() {

    lateinit var repository: KaamelottRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val database = AppDatabase.getInstance(this)
        repository = KaamelottRepository(this, database)
    }
}
