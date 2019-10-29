package com.ringcentral.pubnub

import com.pubnub.api.PNConfiguration
import com.pubnub.api.PubNub
import com.pubnub.api.callbacks.SubscribeCallback
import com.pubnub.api.models.consumer.PNStatus
import com.pubnub.api.models.consumer.pubsub.PNMessageResult
import com.pubnub.api.models.consumer.pubsub.PNPresenceEventResult
import com.pubnub.api.models.consumer.pubsub.PNSignalResult
import com.pubnub.api.models.consumer.pubsub.message_actions.PNMessageActionResult
import com.pubnub.api.models.consumer.pubsub.objects.PNMembershipResult
import com.pubnub.api.models.consumer.pubsub.objects.PNSpaceResult
import com.pubnub.api.models.consumer.pubsub.objects.PNUserResult
import com.ringcentral.RestClient
import com.ringcentral.definitions.CreateSubscriptionRequest
import com.ringcentral.definitions.NotificationDeliveryModeRequest
import com.ringcentral.definitions.SubscriptionInfo
import java.nio.charset.StandardCharsets
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.util.*
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException
import javax.crypto.NoSuchPaddingException
import javax.crypto.spec.SecretKeySpec

class Subscription(private val restClient: RestClient, private val eventFilters: Array<String>, eventListener: EventListener?) {
    private val callback: SubscribeCallback
    var subscription: SubscriptionInfo? = null
        internal set(subscription) {
            field = subscription
            if (timer != null) {
                timer!!.cancel()
                timer = null
            }
            if (subscription != null) {
                timer = Timer()
                timer!!.schedule(object : TimerTask() {
                    override fun run() {
                        refresh()
                    }
                }, (subscription.expiresIn - 120) * 1000)
            }
        }
    private var timer: Timer? = null
    private var pubnub: PubNub? = null

    private val postParameters: CreateSubscriptionRequest
        get() = CreateSubscriptionRequest()
                .deliveryMode(NotificationDeliveryModeRequest().transportType("PubNub").encryption(true))
                .eventFilters(eventFilters)

    init {
        callback = object : SubscribeCallback() {
            override fun status(pubnub: PubNub, status: PNStatus) {}
            override fun signal(pubnub: PubNub, pnSignalResult: PNSignalResult) {}
            override fun user(pubnub: PubNub, pnUserResult: PNUserResult) {}
            override fun messageAction(pubnub: PubNub, pnMessageActionResult: PNMessageActionResult) {}
            override fun presence(pubnub: PubNub, presence: PNPresenceEventResult) {}
            override fun membership(pubnub: PubNub, pnMembershipResult: PNMembershipResult) {}
            override fun space(pubnub: PubNub, pnSpaceResult: PNSpaceResult) {}

            override fun message(pubNub: PubNub, pnMessageResult: PNMessageResult) {
                if (eventListener == null) {
                    return
                }
                val encrypted = Base64.getDecoder().decode(pnMessageResult.message.asString)
                val encryptionKey = Base64.getDecoder().decode(subscription!!.deliveryMode.encryptionKey)
                val cipher: Cipher
                val decrypted: ByteArray
                try {
                    cipher = Cipher.getInstance("AES/ECB/NoPadding")
                    cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(encryptionKey, "AES"))
                    decrypted = cipher.doFinal(encrypted)
                } catch (e: NoSuchAlgorithmException) {
                    e.printStackTrace()
                    return
                } catch (e: NoSuchPaddingException) {
                    e.printStackTrace()
                    return
                } catch (e: InvalidKeyException) {
                    e.printStackTrace()
                    return
                } catch (e: BadPaddingException) {
                    e.printStackTrace()
                    return
                } catch (e: IllegalBlockSizeException) {
                    e.printStackTrace()
                    return
                }

                val jsonString = String(decrypted, StandardCharsets.UTF_8)
                eventListener.listen(jsonString)
            }
        }
    }

    fun subscribe() {
        val subscriptionInfo = restClient.restapi().subscription().post(postParameters)
        subscription = subscriptionInfo
        val pnConfiguration = PNConfiguration()
        pnConfiguration.subscribeKey = subscription!!.deliveryMode.subscriberKey
        pubnub = PubNub(pnConfiguration)
        pubnub!!.addListener(callback)
        pubnub!!.subscribe().channels(listOf(subscription!!.deliveryMode.address)).execute()
    }

    fun refresh() {
        if (subscription == null) {
            return
        }
        val subscriptionInfo = restClient.restapi().subscription(subscription!!.id).renew().post()
        subscription = subscriptionInfo
    }

    fun revoke() {
        if (subscription == null) {
            return
        }
        pubnub!!.unsubscribe().channels(listOf(subscription!!.deliveryMode.address)).execute()
        pubnub!!.removeListener(callback)
        pubnub = null
        restClient.restapi().subscription(subscription!!.id).delete()
        subscription = null
    }
}