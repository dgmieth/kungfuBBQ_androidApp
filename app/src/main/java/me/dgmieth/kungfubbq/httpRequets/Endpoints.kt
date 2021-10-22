package me.dgmieth.kungfubbq.httpRequets

import io.reactivex.Observable
import me.dgmieth.kungfubbq.httpRequets.responsesObjects.userValidation.ForgotPasswordResponseValidation
import me.dgmieth.kungfubbq.httpRequets.responsesObjects.userValidation.LoggedUserInfo
import me.dgmieth.kungfubbq.httpRequets.responsesObjects.userValidation.RegisteredUserInfo
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface Endpoints {
    //REGISTER
    @Headers("Content-Type: application/json")
    @POST("/login/register")
    fun register(@Body data: RegisterBodyData) : Observable<RegisteredUserInfo>
    //PASSWORD RECOVERY
    @Headers("Content-Type: application/json")
    @POST("/api/user/forgotPassword")
    fun forgotPassword(@Body email: ForgotPasswordBodyData) : Observable<ForgotPasswordResponseValidation>
    //LOGIN
    @Headers("Content-Type: application/json")
    @POST("login/login")
    fun login(@Body userCredentials: LoginBodyData) : Observable<LoggedUserInfo>
    //CALENDAR

}