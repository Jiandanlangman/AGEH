package com.jiandanlangman.ageh

data class ExceptionInfoModel(val deviceModel: String = "", val sysVerCode: Int = 0, val sysVerName: String = "",
                              val packageName: String = "", val appVerCode: Int = 0, val appVerName: String = "",
                              val exception: String = "", var time: String = "1970-01-01 00:00:00.000")