package me.dgmieth.kungfubbq.httpRequets.responsesObjects.userValidation

import com.google.gson.annotations.SerializedName

data class ForgotPasswordResponseValidation (
    @SerializedName("hasErrors") var hasErros: Boolean,
    @SerializedName("msg") var msg: String
)