package com.jiandanlangman.ageh

import android.app.ActivityManager
import android.app.Service
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Environment
import android.os.IBinder
import android.util.Log
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import kotlin.collections.HashMap

class AGEHDaemonService : Service() {

    companion object {
        const val PARAM_THROW_EXCEPTION_PROCESS_ID_INTEGER = "throwExceptionPid"
        const val PARAM_THROWABLE = "throwable"
        const val PARAM_CONFIG_SERIALIZABLE = " config"
    }

    private val am by lazy { getSystemService(ACTIVITY_SERVICE) as ActivityManager }
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

    private val sysVerCode = android.os.Build.VERSION.SDK_INT
    private val sysVerName = android.os.Build.VERSION.CODENAME
    private val deviceModel = "${android.os.Build.BRAND}[${android.os.Build.MODEL}]"
    private val appVerCode by lazy { packageManager.getPackageInfo(packageName, PackageManager.GET_META_DATA).versionCode }
    private val appVerName by lazy { packageManager.getPackageInfo(packageName, PackageManager.GET_META_DATA).versionName }


    private val recordDir = File(Environment.getExternalStorageDirectory(), "AGEH")
    private val recordToFileSystemThreadPool = Executors.newFixedThreadPool(1)
    private val recordDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private val uploadToServerThreadPool = Executors.newFixedThreadPool(3)


    override fun onBind(intent: Intent): IBinder? = null

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val config = intent.getSerializableExtra(PARAM_CONFIG_SERIALIZABLE) as? HashMap<*, *>
        if (config != null) {
            val restartApp = config["restartApp"] as? Boolean ?: true
            val recordToFileSystem = config["recordToFileSystem"] as? Boolean ?: false
            val uploadUrl = config["uploadUrl"] as? String ?: ""
            val cusParams = config["customParams"] as? HashMap<*, *>
            val tr = intent.getSerializableExtra(PARAM_THROWABLE) as? Throwable
            if (tr != null) {
                val throwExceptionProcessId = intent.getIntExtra(PARAM_THROW_EXCEPTION_PROCESS_ID_INTEGER, -1)
                if ((applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0)
                    tr.printStackTrace()
                val exceptionInfo = generateExceptionInfo(tr)
                if (recordToFileSystem)
                    recordToFileSystem(exceptionInfo)
                if ("" != uploadUrl)
                    uploadToServer(uploadUrl, exceptionInfo, cusParams)
                if (throwExceptionProcessId != -1) {
                    var mainProcessId = -1
                    run getMainProcessId@{
                        am.runningAppProcesses.forEach {
                            if (it.processName == packageName) {
                                mainProcessId = it.pid
                                return@getMainProcessId
                            }
                        }
                    }
                    android.os.Process.killProcess(throwExceptionProcessId)
                    //只有主进程崩溃才需要重启APP
                    if (restartApp && throwExceptionProcessId == mainProcessId)
                        startActivity(packageManager.getLaunchIntentForPackage(packageName)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED))
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }


    private fun generateExceptionInfo(tr: Throwable): ExceptionInfoModel {
        val baos = ByteArrayOutputStream()
        tr.printStackTrace(PrintStream(baos))
        val exception = String(baos.toByteArray(), Charset.forName("UTF-8"))
        baos.reset()
        baos.close()
        return ExceptionInfoModel(deviceModel, sysVerCode, sysVerName,
                packageName, appVerCode, appVerName,
                exception, dateFormat.format(Date()))
    }

    private fun recordToFileSystem(exceptionInfo: ExceptionInfoModel) = recordToFileSystemThreadPool.execute {
        try {
            if (recordDir.exists() && recordDir.isFile)
                recordDir.delete()
            if (!recordDir.exists())
                recordDir.mkdirs()
            val recordFile = File(recordDir, "${packageName}_${recordDateFormat.format(Date())}.log")
            val fos = FileOutputStream(recordFile, true)
            fos.write("${exceptionInfo.time} AGEH/${exceptionInfo.packageName} E/AGEHDaemonService: ${exceptionInfo.exception}".toByteArray())
            fos.write("\n\n".toByteArray())
            fos.flush()
            fos.close()
        } catch (ignore: Throwable) {

        }
    }

    private fun uploadToServer(url: String, exceptionInfo: ExceptionInfoModel, customParams: HashMap<*, *>?) = uploadToServerThreadPool.execute {
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doInput = true
            connection.doOutput = true
            val os = connection.outputStream
            os.write(("exception=${exceptionInfo.exception}&packageName=${exceptionInfo.packageName}&sysVerCode=${exceptionInfo.sysVerCode}&sysVerName=${exceptionInfo.sysVerName}&deviceModel=${exceptionInfo.deviceModel}&appVerCode=${exceptionInfo.appVerCode}&appVerName=${exceptionInfo.appVerName}&time=${exceptionInfo.time}").toByteArray())
            if (customParams?.isNotEmpty() == true) {
                val sb = StringBuilder()
                customParams.keys.forEach { sb.append("&$it=${customParams[it]}") }
                os.write(sb.toString().toByteArray())
            }
            os.flush()
            os.close()
            val ins = connection.inputStream
            val baos = ByteArrayOutputStream()
            val buffer = ByteArray(4096)
            var readLength = 0
            while (readLength != -1) {
                readLength = ins.read(buffer)
                if (readLength != -1)
                    baos.write(buffer, 0, readLength)
            }
            val response = baos.toString("UTF-8")
            baos.close()
            if ((applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0)
                Log.i(javaClass.simpleName, "uploadExceptionResponse:$response")
            connection.disconnect()
        } catch (ignore: Throwable) {

        }
    }

}