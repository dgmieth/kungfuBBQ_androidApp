package me.dgmieth.kungfubbq.httpRequets

import com.google.gson.annotations.SerializedName

data class UserAuthentication (
    @SerializedName("password") val passowrd: String,
    @SerializedName("email") val email: String
)
data class ForgotPasswordBodyData(
    @SerializedName("email") val email: String
)