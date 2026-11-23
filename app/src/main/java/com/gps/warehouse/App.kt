package com.gps.warehouse

import android.app.Application
import dagger.hilt.android.HiltAndroidApp


@HiltAndroidApp
class App : Application()

//import android.app.Application
//import coil.ImageLoader
//import coil.ImageLoaderFactory
//import coil.decode.ImageDecoderDecoder
//import dagger.hilt.android.HiltAndroidApp
//
//@HiltAndroidApp
//class App : Application(), ImageLoaderFactory {
//    override fun newImageLoader(): ImageLoader =
//        ImageLoader.Builder(this)
//            .components { add(ImageDecoderDecoder.Factory()) }
//            .build()
//}