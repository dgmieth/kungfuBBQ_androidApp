package me.dgmieth.kungfubbq.httpRequets

import io.reactivex.Observable
import me.dgmieth.kungfubbq.httpRequets.responsesObjects.userValidation.ForgotPasswordResponseValidation
import me.dgmieth.kungfubbq.httpRequets.responsesObjects.userValidation.UserResponseValidation
import org.json.JSONObject
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST

interface Endpoints {
    //REGISTER
    @Headers("Content-Type: application/json")
    @POST("/api/user/forgotPassword")
    fun forgotPassword(@Body email: ForgotPasswordBodyData) : Observable<ForgotPasswordResponseValidation>
    //PASSWORD RECOVERY

    //LOGIN
    @Headers("Content-Type: application/json")
    @POST("login/login")
    fun login(@Body userCredentials: UserAuthentication) : Observable<UserResponseValidation>
    //CALENDAR

}