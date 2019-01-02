package com.jwl.myapplication

import android.Manifest
import android.content.Context
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.Environment
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import okhttp3.*
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_NETWORK_STATE),
            100
        )
        var httpCacheDirectory = File(Environment.getExternalStorageDirectory(), "111cache");
        var cacheSize = 10L * 1024 * 1024; // 10 MiB
        var cache = Cache(httpCacheDirectory, cacheSize)
        var client = OkHttpClient.Builder()
            .addNetworkInterceptor(netInterceptor)
            .addInterceptor(offlineInterceptor)
            .cache(cache)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
        val request = Request.Builder()
            .url("https://gank.io/api/data/Android/10/1")
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {

            }

            override fun onResponse(call: Call, response: Response) {
                Log.d("aaa", response.body()!!.string())
            }

        })

    }

    var netInterceptor: Interceptor = (object : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            var request = chain.request()
            var response = chain.proceed(request);
            var onlineCacheTime: Int = 30;//在线的时候的缓存过期时间，如果想要不缓存，直接时间设置为0
            return response.newBuilder()
                .header("Cache-Control", "public, max-age=" + onlineCacheTime)
                .removeHeader("Pragma")
                .build()
        }

    })


    var offlineInterceptor: Interceptor = (object : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            var request = chain.request()
            var offlineCacheTime = 60;//离线的时候的缓存的过期时间
            if (!isNetWorkAvailable(applicationContext)){
                request = request.newBuilder()
                    .header("Cache-Control", "public, only-if-cached, max-stale=" + offlineCacheTime)
                    .build()
            }
            return chain.proceed(request)
        }
    })

    fun isNetWorkAvailable(context: Context): Boolean {
        var isAvailable = false
        var cm: ConnectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = cm.getActiveNetworkInfo()
        if (activeNetworkInfo != null && activeNetworkInfo.isAvailable()) {
            isAvailable = true
        }
        return isAvailable
    }


}
