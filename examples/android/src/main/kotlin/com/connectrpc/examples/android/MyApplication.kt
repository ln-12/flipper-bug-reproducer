package com.connectrpc.examples.android

import android.app.Application
import com.facebook.flipper.android.AndroidFlipperClient
import com.facebook.flipper.plugins.inspector.DescriptorMapping
import com.facebook.flipper.plugins.inspector.InspectorFlipperPlugin
import com.facebook.flipper.plugins.network.NetworkFlipperPlugin
import com.facebook.soloader.SoLoader

class MyApplication : Application() {

    companion object {
        val networkPlugin = object : NetworkFlipperPlugin() {}
    }

    override fun onCreate() {
        super.onCreate()

        // Flipper config
        SoLoader.init(this, false)
        val client = AndroidFlipperClient.getInstance(this)
        client.addPlugin(InspectorFlipperPlugin(this, DescriptorMapping.withDefaults()))
        client.addPlugin(networkPlugin)
        client.start()
    }
}
