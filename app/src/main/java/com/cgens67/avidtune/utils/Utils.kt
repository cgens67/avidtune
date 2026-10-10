package com.cgens67.avidtune.utils

import android.util.Log

fun reportException(throwable: Throwable) {
    throwable.printStackTrace()
    LogManager.log(
        priority = Log.ERROR,
        tag = "Exception",
        message = throwable.localizedMessage ?: throwable.message ?: "Exception occurred",
        t = throwable
    )
}
