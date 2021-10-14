package me.dgmieth.kungfubbq.httpRequets

import io.reactivex.Observable
import me.dgmieth.kungfubbq.httpRequets.responsesObjects.userValidation.UserResponseValidation
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST

interface Endpoints {
    @Headers("Content-Type: application/json")
    @POST("login/login")
    fun login(@Body userCredentials: UserAuthentication) : Observable<UserResponseValidation>
}