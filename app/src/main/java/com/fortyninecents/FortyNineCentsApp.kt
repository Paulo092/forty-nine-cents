package com.fortyninecents

import android.app.Application
import com.fortyninecents.di.AppContainer

class FortyNineCentsApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
