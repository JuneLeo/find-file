package com.cheetah.kotlin

import android.Manifest
import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.core.content.ContextCompat
import java.io.File
import java.lang.Exception
import java.lang.RuntimeException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class FileScanner {


    var executor: ExecutorService? = null
    var callback: Callback? = null
    var list: MutableList<File> = mutableListOf()

    fun scan(context: Context, c: Callback) {
        list.clear()
        this.callback = c
        executor = Executors.newFixedThreadPool(8)

        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            callback?.onError(RuntimeException("没有权限"))
            return
        }
        callback?.onStart()
        val external = Environment.getExternalStorageDirectory()

        scanInner(external)


    }

    fun stop() {
        if (executor != null && !(executor!!.isShutdown)) {
            executor?.shutdownNow()
        }
    }

    var cout: AtomicInteger = AtomicInteger()
    private fun scanInner(file: File) {
        if (file.isDirectory) {
            cout.incrementAndGet()
            executor?.execute(Runnable {
                file.listFiles()?.forEach { scanInner(it) }.also {
                    if (cout.decrementAndGet() == 0) {
                        if (list.size > 0) {
                            callback?.onScanner(list)
                            list.clear()
                            callback?.onEnd()
                            stop()
                        }
                    }
                }
            })
        } else {
            if (isHide(file) && file.length() > 0) {
                file.isHidden
                Log.d("song", file.absolutePath)
                synchronized(list) {
                    list.add(file)
                    if (list.size >= 100) {
                        callback?.onScanner(list)
                        list.clear()
                    }

                }
            }
        }
    }

    private fun isHide(file: File): Boolean {
        if (file.isHidden) {
            return true
        }
        var parentFile = file.parentFile;
        if (parentFile != null) {
            return isHide(parentFile)
        }
        return false
    }


}

interface Callback {
    fun onStart()
    fun onScanner(files: List<File>)
    fun onEnd()
    fun onError(e: Exception)
}
