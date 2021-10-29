package me.dgmieth.kungfubbq.httpCtrl

import android.util.Log
import okhttp3.*

class HttpCtrl {

    companion object{
        var shared = OkHttpClient()
        fun get(url:String, endpoint:String,httpUrl:HttpUrl?=null,header: String?=null) : Request {
            httpUrl?.let{
                val url = it
                header?.let{
                Log.d("HttpCtrl","return with authorization header ${it}")
                return Request.Builder()
                    .header("Authorization","Bearer ${it}")
                    .header("Content-type","application/json")
                    .url(url)
                    .build()
                }
            }
            return Request.Builder()
                .header("Content-type","application/json")
                .url("$url$endpoint")
                .build()
        }
        fun post(url:String,endpoint: String,body:FormBody, header:String?=null) : Request{
            header?.let{
                Log.d("HttpCtrl","return with authorization header $it")
                return Request.Builder()
                    .header("Content-type","application/json")
                    .header("Authorization","Bearer $it")
                    .url("$url$endpoint")
                    .post(body)
                    .build()
            }
            Log.d("HttpCtrl","return without authorization header")
            return Request.Builder()
                .header("Content-type","application/json")
                .url("$url$endpoint")
                .post(body)
                .build()
        }
    }
}