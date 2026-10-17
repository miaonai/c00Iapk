package com.example.c001apk.logic.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

data class TotalReplyResponse(
    val status: Int?,
    val error: com.google.gson.JsonElement?,
    val message: String?,
    val data: List<Data>?,
    val code: Int? = null,
    val messageStatus: String? = null,
    val messageExtra: com.google.gson.JsonElement? = null,
    val extra: com.google.gson.JsonElement? = null,
    val captchaId: String? = null,
    val captchaField: String? = null
) {

    @Parcelize
    data class Data(
        var lastupdate: Long?,
        @SerializedName("extra_key") val extraKey: String?,
        val entityType: String?,
        val id: String,
        val ruid: String?,
        val uid: String,
        val feedUid: String?,
        var username: String,
        val rusername: String?,
        var message: String,
        val pic: String?,
        val picArr: List<String>?,
        val dateline: Long,
        var likenum: String,
        val replynum: String,
        val userAvatar: String,
        var replyRows: MutableList<Data>?,
        val replyRowsMore: Int?,
        val userAction: UserAction?,
        val userInfo: UserInfo
    ) : Parcelable

    @Parcelize
    data class UserAction(var like: Int) : Parcelable

    @Parcelize
    data class UserInfo(val username: String) : Parcelable


}

