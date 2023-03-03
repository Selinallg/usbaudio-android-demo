libusb专用编译项目
=================

版本
-----
libusb-1.0.23

编译原理
------
使用ndk-build

流程
----
step1. 下载源码，地址：https://github.com/libusb/libusb/releases

step2. 拷贝源码里面的libusb目录到libs下

step3. 拷贝源码里面的android/jni目录两个文件到libs目录下：android.mk,application.mk

step4. 拷贝源码里面的android目录下的config.h到libs目录下

step5. 修改build.gradle,增加行： ndkPath "/e:/android-ndk-r21b"

step6. 修改build.gradle,增加行：
    sourceSets {
        main {
            jniLibs.srcDirs =["libs"]
        }
    }

step7. 修改build.gradle,增加行：
    externalNativeBuild {
        ndkBuild {
            // 指定JNI代码的入口MK文件路径
            path 'libs/Android.mk'
        }
    }

so文件位置
-------
BuildLibUSB\app\build\intermediates\ndkBuild\release\obj\local
或者
BuildLibUSB\app\build\intermediates\ndkBuild\debug\obj\local

参考
----
https://www.cnblogs.com/yongdaimi/p/11934783.html
https://www.dazhuanlan.com/2019/10/02/5d945baad2be9/