package me.dgmieth.kungfubbq.httpRequets

import me.dgmieth.kungfubbq.R
import me.dgmieth.kungfubbq.VariablesGlobal
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.util.*

class HttpRequestCtrl {
    companion object {
        private fun getInstance(path:String):Retrofit {
            return Retrofit.Builder()
                .baseUrl(VariablesGlobal.Companion.kungfuRooUrl)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        fun <T> buildService(service: Class<T>, path: String):T {
            return this.getInstance(path).create(service)
        }
    }
}