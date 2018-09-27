package com.jiandanlangman.ageh.demo

import android.app.Application
import com.jiandanlangman.ageh.AGEH

class DaemonApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        //使用这个方式初始化，只会在崩溃后自动重启app，并且debug版本会在logcat打印崩溃信息
        AGEH.initialize(this)

        //或者使用这种方式初始化
        AGEH.initialize(this, AGEH.ConfigBuilder()
                .setRestartApp(true) //是否在主进程崩溃时重启app，默认true
                .setRecordToFileSystem(true) //是否将异常信息记录到文件系统。默认false。记录地址在/sdcard/AGEH/packagename_yyyy-MM-dd.log
                .setUploadUrl("http://101.132.115.133/uploadException")) //设置上传异常信息的url，默认不上传
    }
}