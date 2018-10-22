
# AGEH
Android全局异常捕捉工具  
这个东西或许没什么用处，因为很多第三方统计都会有这个功能，但是第三方一般注册麻烦，接入麻烦，数据还走第三方  
这个东西是使用kotlin编写的，所以你的项目必须引入kotlin，不然是没法正常使用的。推荐你也使用kotlin，因为java调用这个框架的话，方法名参数名等可能会有些奇怪  
### 特点
- 全局捕捉异常并处理
- 非主进程的异常只做记录，因为非主进程异常并不会引起整个APP崩溃
- 主进程的异常记录并可配置是否重启APP
- 可配置将异常信息记录到本地或上传到服务器
- 框架轻量，接入简单，安全可靠
### 接入方式
- 下载源码，将名为ageh的module加入到你的工程，或者将module打包成aar格式
- maven方式接入(推荐使用上面的方法，我服务器配置垃圾)
    编辑Project的build.gradle文件
    ```
    allprojects {
        repositories {
            google()
            jcenter()
            mavenCentral()
            maven { url "http://101.132.235.215/repor" } //加入这一行
        }
    }
    ```
    然后编辑你app module的build.gradle文件，在dependencies节点下加入
    ```
    implementation "com.jiandanlangman:ageh:1.0.1@aar"
    ```
### 启用全局异常处理
最简单的方法，在Application的onCreate方法里加入一行代码即可
```
class DaemonApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        //使用这个方式初始化，只会在崩溃后自动重启app，并且debug版本会在logcat打印崩溃信息
        AGEH.initialize(this)
    }
}
```
也可以使用这种方式启用，有更多的配置选项
```
class DaemonApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        AGEH.initialize(this, AGEH.ConfigBuilder()
                .setRestartApp(true) //是否在主进程崩溃时重启app，如果为false则会在崩溃时什么也不做，默认true
                .setRecordToFileSystem(true) //是否将异常信息记录到文件系统。默认false。记录地址在/sdcard/AGEH/packagename_yyyy-MM-dd.log
                .setCustomParams(HashMap()) //添加上传异常信息的自定义参数
                .setUploadUrl("http://xxx.xxx.xxx/uploadException")) //设置上传异常信息的url，默认不上传
    }
    
}
```
如果在发生异常时自己还想做点别的事情也是可以的
```
class DaemonApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AGEH.initialize(this)
        AGEH.setExtraUncaughtExceptionHandler { t, e -> 
            //当然如果你还想自己处理下异常也是可以的，就是使用这个方法来自行处理，但这里处理的结果不会影响到AGEH的处理结果
        }
    }
}
```
### 异常信息上传的字段说明
- exception：异常信息，String类型
- packageName：发生异常的应用程序包名，String类型
- sysVerCode：设备的系统版本，Int型
- sysVerName：设备的系统名称，String型
- deviceModel：设备型号，String型
- appVerCode：APP版本号，Int型
- appVerName：APP版本名称，String型
- time：异常发生的时间，String型，格式为”yyyy-MM-dd HH:mm:ss.SSS“
