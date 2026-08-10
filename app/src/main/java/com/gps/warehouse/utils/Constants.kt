package com.gps.warehouse.utils

object Constants {
    const val BASE_URL_API = "http://gps-test.hmmr.ru/api/"
//    const val BASE_URL_API = "https://gps-rs.hmmr.ru/api/"

    const val BASE_URL_UPDATE = "http://10.168.143.7:8100/test"
//    const val BASE_URL_UPDATE = "http://10.168.143.7:8100/prod"
    // https://git-new.hmmr.ru/timurmalyshev/android-gps-warehouse-app/-/jobs/1086/artifacts/download?file_type=archive
    // https://git-new.hmmr.ru/timurmalyshev/android-gps-warehouse-app/-/jobs/1077/artifacts/download?file_type=archive
    const val TOKEN_PREFS_NAME = "gps_token_prefs"
    const val TOKEN_KEY = "jwt_token"

    const val ASSET_URL = "http://10.168.143.7:8800/api/"

    // Константа в миллисекундах: час*минута*секунда*миллисекунда
    const val SESSION_DURATION_MS = 12 * 60 * 60 * 1000L // 12 часов
}