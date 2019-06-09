package com.ringcentral;

import com.pubnub.api.PNConfiguration;
import com.pubnub.api.PubNub;
import com.pubnub.api.callbacks.SubscribeCallback;
import com.pubnub.api.models.consumer.PNStatus;
import com.pubnub.api.models.consumer.pubsub.PNMessageResult;
import com.pubnub.api.models.consumer.pubsub.PNPresenceEventResult;
import com.ringcentral.definitions.CreateSubscriptionRequest;
import com.ringcentral.definitions.NotificationDeliveryModeRequest;
import com.ringcentral.definitions.SubscriptionInfo;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Collections;
import java.util.Timer;
import java.util.TimerTask;

public class Subscription {
    private String[] eventFilters;
    private RestClient restClient;
    private SubscribeCallback callback;
    private SubscriptionInfo _subscription;
    private Timer timer;
    private PubNub pubnub;

    public Subscription(RestClient restClient, String[] eventFilters, EventListener eventListener) {
        this.restClient = restClient;
        this.eventFilters = eventFilters;
        callback = new SubscribeCallback() {
            public void status(PubNub pubnub, PNStatus status) {
            }

            public void presence(PubNub pubnub, PNPresenceEventResult presence) {
            }

            @Override
            public void message(PubNub pubNub, PNMessageResult pnMessageResult) {
                if (eventListener == null) {
                    return;
                }
                byte[] encrypted = Base64.getDecoder().decode(pnMessageResult.getMessage().getAsString());
                final byte[] encryptionKey = Base64.getDecoder().decode(getSubscription().deliveryMode.encryptionKey);
                Cipher cipher;
                byte[] decrypted;
                try {
                    cipher = Cipher.getInstance("AES/ECB/NoPadding");
                    cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(encryptionKey, "AES"));
                    decrypted = cipher.doFinal(encrypted);
                } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException e) {
                    e.printStackTrace();
                    return;
                }
                String jsonString = new String(decrypted, StandardCharsets.UTF_8);
                eventListener.listen(jsonString);
            }
        };
    }

    public SubscriptionInfo getSubscription() {
        return _subscription;
    }

    void setSubscription(SubscriptionInfo subscription) {
        _subscription = subscription;
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        if (subscription != null) {
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    refresh();
                }
            }, (subscription.expiresIn - 120) * 1000);
        }
    }

    public void subscribe() {
        SubscriptionInfo subscriptionInfo = restClient.restapi().subscription().post(getPostParameters());
        setSubscription(subscriptionInfo);
        PNConfiguration pnConfiguration = new PNConfiguration();
        pnConfiguration.setSubscribeKey(getSubscription().deliveryMode.subscriberKey);
        pubnub = new PubNub(pnConfiguration);
        pubnub.addListener(callback);
        pubnub.subscribe().channels(Collections.singletonList(getSubscription().deliveryMode.address)).execute();
    }

    public void refresh() {
        if (getSubscription() == null) {
            return;
        }
        SubscriptionInfo subscriptionInfo = restClient.restapi().subscription(getSubscription().id).renew().post();
        setSubscription(subscriptionInfo);
    }

    public void revoke() {
        if (getSubscription() == null) {
            return;
        }
        pubnub.unsubscribe().channels(Collections.singletonList(getSubscription().deliveryMode.address)).execute();
        pubnub.removeListener(callback);
        pubnub = null;
        restClient.restapi().subscription(getSubscription().id).delete();
        setSubscription(null);
    }

    private CreateSubscriptionRequest getPostParameters() {
        return new CreateSubscriptionRequest()
                .deliveryMode(new NotificationDeliveryModeRequest().transportType("PubNub").encryption(true))
                .eventFilters(eventFilters);
    }
}