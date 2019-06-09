package com.ringcentral.pubnub

import com.ringcentral.RestClient
import com.ringcentral.RestException
import com.ringcentral.definitions.CreateSMSMessage
import com.ringcentral.definitions.MessageStoreCallerInfoRequest
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class SubscriptionTest {
    @Test
    @Throws(IOException::class, RestException::class)
    fun testSubscribe() {
        val rc = RestClient(
                System.getenv("RINGCENTRAL_CLIENT_ID"),
                System.getenv("RINGCENTRAL_CLIENT_SECRET"),
                System.getenv("RINGCENTRAL_SERVER_URL")
        )

        rc.authorize(
                System.getenv("RINGCENTRAL_USERNAME"),
                System.getenv("RINGCENTRAL_EXTENSION"),
                System.getenv("RINGCENTRAL_PASSWORD")
        )
        val subscription = Subscription(rc,
                arrayOf("/restapi/v1.0/account/~/extension/~/message-store")
        ) { }
        subscription.subscribe()
        val sub = subscription.subscription
        assertEquals("Active", sub.status)

        rc.revoke()
    }

    @Throws(IOException::class, RestException::class)
    private fun sendSms() {
        val rc = RestClient(
                System.getenv("RINGCENTRAL_CLIENT_ID"),
                System.getenv("RINGCENTRAL_CLIENT_SECRET"),
                System.getenv("RINGCENTRAL_SERVER_URL")
        )

        rc.authorize(
                System.getenv("RINGCENTRAL_USERNAME"),
                System.getenv("RINGCENTRAL_EXTENSION"),
                System.getenv("RINGCENTRAL_PASSWORD")
        )

        rc.restapi().account().extension().sms().post(
                CreateSMSMessage()
                        .text("hello world")
                        .from(MessageStoreCallerInfoRequest().phoneNumber(System.getenv("RINGCENTRAL_USERNAME")))
                        .to(arrayOf(MessageStoreCallerInfoRequest().phoneNumber(System.getenv("RINGCENTRAL_RECEIVER"))))
        )

        rc.revoke()
    }

    @Test
    @Throws(IOException::class, RestException::class, InterruptedException::class)
    fun testNotification() {
        val rc = RestClient(
                System.getenv("RINGCENTRAL_CLIENT_ID"),
                System.getenv("RINGCENTRAL_CLIENT_SECRET"),
                System.getenv("RINGCENTRAL_SERVER_URL")
        )

        rc.authorize(
                System.getenv("RINGCENTRAL_USERNAME"),
                System.getenv("RINGCENTRAL_EXTENSION"),
                System.getenv("RINGCENTRAL_PASSWORD")
        )

        var message: String? = null
        val subscription = Subscription(rc,
                arrayOf("/restapi/v1.0/account/~/extension/~/message-store"),
                EventListener { str ->
                    run {
                        message = str
                    }
                })
        subscription.subscribe()
        Thread.sleep(3000)
        sendSms()
        Thread.sleep(16000)
        assertNotNull(message)
        assertTrue(message!!.contains("uuid"))

        subscription.revoke()
        rc.revoke()
    }

    @Test
    @Throws(IOException::class, RestException::class, InterruptedException::class)
    fun testRefresh() {
        val rc = RestClient(
                System.getenv("RINGCENTRAL_CLIENT_ID"),
                System.getenv("RINGCENTRAL_CLIENT_SECRET"),
                System.getenv("RINGCENTRAL_SERVER_URL")
        )

        rc.authorize(
                System.getenv("RINGCENTRAL_USERNAME"),
                System.getenv("RINGCENTRAL_EXTENSION"),
                System.getenv("RINGCENTRAL_PASSWORD")
        )

        var message: String? = null
        val subscription = Subscription(rc,
                arrayOf("/restapi/v1.0/account/~/extension/~/message-store"),
                EventListener { str ->
                    run {
                        message = str
                    }
                })
        subscription.refresh() // should not cause any issue when _subscription is null
        subscription.subscribe()
        Thread.sleep(3000)
        subscription.refresh()
        Thread.sleep(3000)
        sendSms()
        Thread.sleep(16000)
        assertNotNull(message)
        assertTrue(message!!.contains("uuid"))

        subscription.revoke()
        rc.revoke()
    }

    @Test
    @Throws(IOException::class, RestException::class, InterruptedException::class)
    fun testRevoke() {
        val rc = RestClient(
                System.getenv("RINGCENTRAL_CLIENT_ID"),
                System.getenv("RINGCENTRAL_CLIENT_SECRET"),
                System.getenv("RINGCENTRAL_SERVER_URL")
        )

        rc.authorize(
                System.getenv("RINGCENTRAL_USERNAME"),
                System.getenv("RINGCENTRAL_EXTENSION"),
                System.getenv("RINGCENTRAL_PASSWORD")
        )

        var message: String? = null
        val subscription = Subscription(rc,
                arrayOf("/restapi/v1.0/account/~/extension/~/message-store"),
                EventListener { str ->
                    run {
                        message = str
                    }
                })
        subscription.revoke() // should not cause any issue when _subscription is null
        subscription.subscribe()
        Thread.sleep(1000)
        subscription.revoke()
        Thread.sleep(1000)
        sendSms()
        Thread.sleep(16000)
        assertNull(message)

        rc.revoke()
    }

    @Test
    @Throws(IOException::class, RestException::class, InterruptedException::class)
    fun testAutoRefresh() {
        val rc = RestClient(
                System.getenv("RINGCENTRAL_CLIENT_ID"),
                System.getenv("RINGCENTRAL_CLIENT_SECRET"),
                System.getenv("RINGCENTRAL_SERVER_URL")
        )

        rc.authorize(
                System.getenv("RINGCENTRAL_USERNAME"),
                System.getenv("RINGCENTRAL_EXTENSION"),
                System.getenv("RINGCENTRAL_PASSWORD")
        )

        var message: String? = null
        val subscription = Subscription(rc,
                arrayOf("/restapi/v1.0/account/~/extension/~/message-store"),
                EventListener { str ->
                    run {
                        message = str
                    }
                })
        subscription.subscribe()
        val subInfo = subscription.subscription
        subInfo.expiresIn = 123L
        subscription.subscription = subInfo
        Thread.sleep(6000)
        sendSms()
        Thread.sleep(16000)
        assertNotNull(message)
        assertTrue(message!!.contains("uuid"))

        subscription.revoke()
        rc.revoke()
    }
}