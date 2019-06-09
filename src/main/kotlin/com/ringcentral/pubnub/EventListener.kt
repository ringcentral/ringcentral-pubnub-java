package com.ringcentral.pubnub

interface EventListener {
    fun listen(message: String)
}
