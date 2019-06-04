package com.ringcentral

import com.pubnub.api.models.consumer.PNStatus
import com.pubnub.api.models.consumer.pubsub.PNPresenceEventResult
import com.ringcentral.definitions.CreateSMSMessage
import com.ringcentral.definitions.MessageStoreCallerInfoRequest
import org.junit.Test
import org.mockito.ArgumentCaptor

import java.io.IOException
import java.util.function.Consumer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.mockito.Mockito.*

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
        ) { jsonString -> }
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

        val consumer: Consumer<String> = mock(Consumer::class.java) as Consumer<String>
        val subscription = Subscription(rc,
                arrayOf("/restapi/v1.0/account/~/extension/~/message-store"),
                consumer)
        subscription.subscribe()
        Thread.sleep(3000)
        sendSms()
        Thread.sleep(16000)
        val argument = ArgumentCaptor.forClass(String::class.java)
        verify<Consumer<String>>(consumer, atLeastOnce()).accept(argument.capture())
        assertTrue(argument.value.contains("uuid"))
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

        val consumer: Consumer<String> = mock(Consumer::class.java) as Consumer<String>
        val subscription = Subscription(rc,
                arrayOf("/restapi/v1.0/account/~/extension/~/message-store"),
                consumer)
        subscription.refresh() // should not cause any issue when _subscription is null
        subscription.subscribe()
        Thread.sleep(3000)
        subscription.refresh()
        Thread.sleep(3000)
        sendSms()
        Thread.sleep(16000)
        val argument = ArgumentCaptor.forClass(String::class.java)
        verify<Consumer<String>>(consumer, atLeastOnce()).accept(argument.capture())
        assertTrue(argument.value.contains("uuid"))
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

        val consumer: Consumer<String> = mock(Consumer::class.java) as Consumer<String>
        val subscription = Subscription(rc,
                arrayOf("/restapi/v1.0/account/~/extension/~/message-store"),
                consumer)
        subscription.revoke() // should not cause any issue when _subscription is null
        subscription.subscribe()
        Thread.sleep(1000)
        subscription.revoke()
        Thread.sleep(1000)
        sendSms()
        Thread.sleep(16000)
        verify<Consumer<String>>(consumer, never()).accept(any())

        rc.revoke()
    }

    @Test
    @Throws(IOException::class, RestException::class, InterruptedException::class)
    fun testStatusCallback() {
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

        val consumer1: Consumer<String> = mock(Consumer::class.java) as Consumer<String>
        val consumer2: Consumer<PNStatus> = mock(Consumer::class.java) as Consumer<PNStatus>
        val consumer3: Consumer<PNPresenceEventResult> = mock(Consumer::class.java) as Consumer<PNPresenceEventResult>
        val subscription = Subscription(rc,
                arrayOf("/restapi/v1.0/account/~/extension/~/message-store"),
                consumer1, consumer2, consumer3)
        subscription.subscribe()
        Thread.sleep(3000)
        sendSms()
        Thread.sleep(16000)
        val argument = ArgumentCaptor.forClass(PNStatus::class.java)
        verify<Consumer<PNStatus>>(consumer2, atLeastOnce()).accept(argument.capture())
        assertEquals(argument.value.statusCode.toLong(), 200)
        subscription.revoke()

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

        val consumer: Consumer<String> = mock(Consumer::class.java) as Consumer<String>
        val subscription = Subscription(rc,
                arrayOf("/restapi/v1.0/account/~/extension/~/message-store"),
                consumer)
        subscription.subscribe()
        val subInfo = subscription.subscription
        subInfo.expiresIn = 123L
        subscription.subscription = subInfo
        Thread.sleep(6000)
        sendSms()
        Thread.sleep(16000)
        val argument = ArgumentCaptor.forClass(String::class.java)
        verify<Consumer<String>>(consumer, atLeastOnce()).accept(argument.capture())
        assertTrue(argument.value.contains("uuid"))
        subscription.revoke()

        rc.revoke()
    }
}