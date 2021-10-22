package me.dgmieth.kungfubbq.httpCtrl

import okhttp3.FormBody
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request

class HttpCtrl {

    companion object{
        var shared = OkHttpClient()
        fun get(url:String, endpoint:String) : Request {
            return Request.Builder()
                .header("Content-type","application/json")
                .url("$url$endpoint")
                .build()
        }
        fun post(url:String,endpoint: String,body:FormBody) : Request{
            return Request.Builder()
                .header("Content-type","application/json")
                .url("$url$endpoint")
                .post(body)
                .build()
        }
    }
}