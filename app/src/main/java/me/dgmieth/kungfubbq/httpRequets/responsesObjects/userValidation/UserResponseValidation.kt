package me.dgmieth.kungfubbq.httpRequets.responsesObjects.userValidation

import com.google.gson.annotations.SerializedName

data class UserResponseValidation (
    @SerializedName("hasErrors") var hasErros: Boolean,
    @SerializedName("data") var data: UserInfo
)
data class UserInfo(
    @SerializedName("id") var id: Int,
    @SerializedName("name") var name: String,
    @SerializedName("email") var email: String,
    @SerializedName("memberSince") var memberSince:String,
    @SerializedName("phoneNumber") var phoneNumber:String,
    @SerializedName("socialMediaInfo") var socialMediaInfo: List<SocialMediaInfo>,
    @SerializedName("token") var token:String
    )

data class SocialMediaInfo (
    @SerializedName("socialMedia") var socialMedia: String,
    @SerializedName("socialMediaName") var sociaMediaName: String
)

