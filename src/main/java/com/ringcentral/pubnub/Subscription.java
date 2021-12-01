package com.ringcentral.pubnub;

import com.pubnub.api.PNConfiguration;
import com.pubnub.api.PubNub;
import com.pubnub.api.callbacks.SubscribeCallback;
import com.pubnub.api.enums.PNReconnectionPolicy;
import com.pubnub.api.models.consumer.PNStatus;
import com.pubnub.api.models.consumer.pubsub.PNMessageResult;
import com.pubnub.api.models.consumer.pubsub.PNPresenceEventResult;
import com.pubnub.api.models.consumer.pubsub.PNSignalResult;
import com.pubnub.api.models.consumer.pubsub.message_actions.PNMessageActionResult;
import com.pubnub.api.models.consumer.pubsub.objects.PNMembershipResult;
import com.pubnub.api.models.consumer.pubsub.objects.PNSpaceResult;
import com.pubnub.api.models.consumer.pubsub.objects.PNUserResult;
import com.ringcentral.RestClient;
import com.ringcentral.RestException;
import com.ringcentral.definitions.CreateSubscriptionRequest;
import com.ringcentral.definitions.NotificationDeliveryModeRequest;
import com.ringcentral.definitions.SubscriptionInfo;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
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
            public void status(PubNub pubnub, PNStatus pnStatus){}

            public void presence(PubNub pubnub, PNPresenceEventResult pnPresenceEventResult){}

            public void signal(PubNub pubnub, PNSignalResult pnSignalResult){}

            public void user(PubNub pubnub, PNUserResult pnUserResult){}

            public void space(PubNub pubnub, PNSpaceResult pnSpaceResult){}

            public void membership(PubNub pubnub, PNMembershipResult pnMembershipResult){}

            public void messageAction(PubNub pubnub, PNMessageActionResult pnMessageActionResult){}

            @Override
            public void message(PubNub pubnub, PNMessageResult pnMessageResult) {
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
                    try {
                        refresh();
                    } catch (RestException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }, (subscription.expiresIn - 120) * 1000);
        }
    }

    public void subscribe() throws com.ringcentral.RestException, java.io.IOException {
        SubscriptionInfo subscriptionInfo = restClient.restapi().subscription().post(getPostParameters());
        setSubscription(subscriptionInfo);
        PNConfiguration pnConfiguration = new PNConfiguration();
        pnConfiguration.setReconnectionPolicy(PNReconnectionPolicy.LINEAR);
        pnConfiguration.setSubscribeKey(getSubscription().deliveryMode.subscriberKey);
        pubnub = new PubNub(pnConfiguration);
        pubnub.addListener(callback);
        pubnub.subscribe().channels(Collections.singletonList(getSubscription().deliveryMode.address)).execute();
    }

    public void refresh() throws com.ringcentral.RestException, java.io.IOException {
        if (getSubscription() == null) {
            return;
        }
        SubscriptionInfo subscriptionInfo = restClient.restapi().subscription(getSubscription().id).renew().post();
        setSubscription(subscriptionInfo);
    }

    public void revoke() throws com.ringcentral.RestException, java.io.IOException {
        if (getSubscription() == null) {
            return;
        }
        try {
            restClient.restapi().subscription(getSubscription().id).delete();
        } catch (RestException re) {
            if (re.response.code() == 404) {
                return;
            }
            throw re;
        } finally {
            pubnub.destroy();
            pubnub = null;
            setSubscription(null);
        }
    }

    private CreateSubscriptionRequest getPostParameters() {
        return new CreateSubscriptionRequest()
                .deliveryMode(new NotificationDeliveryModeRequest().transportType("PubNub").encryption(true))
                .eventFilters(eventFilters);
    }
}