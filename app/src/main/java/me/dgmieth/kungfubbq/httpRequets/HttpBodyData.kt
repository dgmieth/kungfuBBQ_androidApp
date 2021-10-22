package me.dgmieth.kungfubbq.httpRequets

import com.google.gson.annotations.SerializedName

data class LoginBodyData (
    @SerializedName("password") val password: String,
    @SerializedName("email") val email: String
)
data class RegisterBodyData(
    @SerializedName("code") val code: String,
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("confirmPassword") val confirmPassword: String
)
data class ForgotPasswordBodyData(
    @SerializedName("email") val email: String
)