package com.silisten.app.data.repository

import com.silisten.app.data.source.NeteaseActionResult
import com.silisten.app.data.source.NeteaseApiClient
import com.silisten.app.data.source.NeteaseLoginState
import com.silisten.app.data.source.NeteaseQrLoginCheck
import com.silisten.app.data.source.NeteaseQrLoginCode

class AccountRepository(
    private val neteaseApiClient: NeteaseApiClient
) {
    suspend fun refreshNeteaseLogin(): NeteaseLoginState = neteaseApiClient.refreshLoginState()

    suspend fun sendNeteaseSmsCode(phone: String): NeteaseActionResult =
        neteaseApiClient.sendSmsCode(phone)

    suspend fun loginNeteaseBySms(phone: String, captcha: String): NeteaseLoginState =
        neteaseApiClient.loginBySms(phone, captcha)

    suspend fun createNeteaseQrLogin(): NeteaseQrLoginCode =
        neteaseApiClient.createQrLoginCodeLocal()

    suspend fun checkNeteaseQrLogin(key: String): NeteaseQrLoginCheck =
        neteaseApiClient.checkQrLogin(key)

    suspend fun logoutNetease(): NeteaseLoginState = neteaseApiClient.logout()
}
