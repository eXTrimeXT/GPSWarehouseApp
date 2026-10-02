package com.gps.warehouse.utils

object Constants {
    /**
     * Обязательно отредактировать файл build.gradle.kts
     */
    // TEST
//    const val BASE_URL_API = "http://gps-test.hmmr.ru/api/"
//    const val BASE_URL_UPDATE = "http://10.168.143.7:8100/test"
//    const val TAB_VISIBLE = true
//    const val ASSET_URL = "http://10.168.143.7:8800/api/"

    // PROD
    const val TAB_VISIBLE = false
    const val BASE_URL_UPDATE = "http://10.168.143.7:8100/prod"
    const val BASE_URL_API = "https://gps-rs.hmmr.ru/api/"
    const val ASSET_URL = "http://10.168.130.86:8800/api/"

    // https://git-new.hmmr.ru/timurmalyshev/android-gps-warehouse-app/-/jobs/1077/artifacts/download?file_type=archive
}