package com.jiandanlangman.ageh

import android.app.Application
import android.content.Intent
import java.io.Serializable

object AGEH {

    private var extraUncaughtExceptionHandler: ((Thread, Throwable) -> Unit) = { _, _ -> }

    fun initialize(application: Application) = initialize(application, ConfigBuilder())

    fun initialize(application: Application, configBuilder: ConfigBuilder) {
        val config = configBuilder.build()
        application.startService(Intent(application, AGEHDaemonService::class.java))
        Thread.setDefaultUncaughtExceptionHandler { tr, e ->
            application.startService(Intent(application, AGEHDaemonService::class.java)
                    .putExtra(AGEHDaemonService.PARAM_CONFIG_SERIALIZABLE, config)
                    .putExtra(AGEHDaemonService.PARAM_THROW_EXCEPTION_PROCESS_ID_INTEGER, android.os.Process.myPid())
                    .putExtra(AGEHDaemonService.PARAM_THROWABLE, e))
            extraUncaughtExceptionHandler.invoke(tr, e)
        }
    }

    fun setExtraUncaughtExceptionHandler(handler: (t: Thread, e: Throwable) -> Unit) {
        this.extraUncaughtExceptionHandler = handler
    }

    class ConfigBuilder {


        private var restartApp = true
        private var recordToFileSystem = false
        private var uploadUrl = ""
        private var customParams: HashMap<String, Serializable>? = null

        fun setRestartApp(restartApp: Boolean): ConfigBuilder {
            this.restartApp = restartApp
            return this
        }

        fun setRecordToFileSystem(recordToFileSystem: Boolean): ConfigBuilder {
            this.recordToFileSystem = true
            return this
        }

        fun setUploadUrl(uploadUrl: String): ConfigBuilder {
            this.uploadUrl = uploadUrl
            return this
        }

        fun setCustomParams(params: HashMap<String, Serializable>): ConfigBuilder {
            customParams = params
            return this
        }

        internal fun build(): HashMap<String, Serializable> {
            val map = HashMap<String, Serializable>()
            map["restartApp"] = restartApp
            map["recordToFileSystem"] = recordToFileSystem
            map["uploadUrl"] = uploadUrl
            if (customParams != null)
                map["customParams"] = customParams!!
            return map
        }

    }

}