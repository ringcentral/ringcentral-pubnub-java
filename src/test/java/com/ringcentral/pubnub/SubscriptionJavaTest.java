package com.ringcentral.pubnub;

import com.ringcentral.RestClient;
import com.ringcentral.RestException;
import com.ringcentral.definitions.CreateSMSMessage;
import com.ringcentral.definitions.MessageStoreCallerInfoRequest;
import com.ringcentral.definitions.SubscriptionInfo;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;


public class SubscriptionJavaTest {

    @Test
    public void testSubscribe() throws IOException, RestException {
        RestClient rc = new RestClient(
                System.getenv("RINGCENTRAL_CLIENT_ID"),
                System.getenv("RINGCENTRAL_CLIENT_SECRET"),
                System.getenv("RINGCENTRAL_SERVER_URL")
        );

        rc.authorize(
                System.getenv("RINGCENTRAL_USERNAME"),
                System.getenv("RINGCENTRAL_EXTENSION"),
                System.getenv("RINGCENTRAL_PASSWORD")
        );
        Subscription subscription = new Subscription(rc,
                new String[]{"/restapi/v1.0/account/~/extension/~/message-store"},
                (jsonString) -> {
                }
        );
        subscription.subscribe();
        SubscriptionInfo sub = subscription.getSubscription();
        assertEquals("Active", sub.status);

        rc.revoke();
    }

    private void sendSms() throws IOException, RestException {
        RestClient rc = new RestClient(
                System.getenv("RINGCENTRAL_CLIENT_ID"),
                System.getenv("RINGCENTRAL_CLIENT_SECRET"),
                System.getenv("RINGCENTRAL_SERVER_URL")
        );

        rc.authorize(
                System.getenv("RINGCENTRAL_USERNAME"),
                System.getenv("RINGCENTRAL_EXTENSION"),
                System.getenv("RINGCENTRAL_PASSWORD")
        );

        rc.restapi().account().extension().sms().post(
                new CreateSMSMessage()
                        .text("hello world")
                        .from(new MessageStoreCallerInfoRequest().phoneNumber(System.getenv("RINGCENTRAL_USERNAME")))
                        .to(new MessageStoreCallerInfoRequest[]{
                                new MessageStoreCallerInfoRequest().phoneNumber(System.getenv("RINGCENTRAL_RECEIVER"))
                        })
        );

        rc.revoke();
    }

    @Test
    public void testNotification() throws IOException, RestException, InterruptedException {
        RestClient rc = new RestClient(
                System.getenv("RINGCENTRAL_CLIENT_ID"),
                System.getenv("RINGCENTRAL_CLIENT_SECRET"),
                System.getenv("RINGCENTRAL_SERVER_URL")
        );

        rc.authorize(
                System.getenv("RINGCENTRAL_USERNAME"),
                System.getenv("RINGCENTRAL_EXTENSION"),
                System.getenv("RINGCENTRAL_PASSWORD")
        );

        final String[] message = {null};
        Subscription subscription = new Subscription(rc,
                new String[]{"/restapi/v1.0/account/~/extension/~/message-store"},
                str -> message[0] = str);
        subscription.subscribe();
        Thread.sleep(3000);
        sendSms();
        Thread.sleep(16000);
        assertNotNull(message[0]);
        assertTrue(message[0].contains("uuid"));

        subscription.revoke();
        rc.revoke();
    }

    @Test
    public void testRefresh() throws IOException, RestException, InterruptedException {
        RestClient rc = new RestClient(
                System.getenv("RINGCENTRAL_CLIENT_ID"),
                System.getenv("RINGCENTRAL_CLIENT_SECRET"),
                System.getenv("RINGCENTRAL_SERVER_URL")
        );

        rc.authorize(
                System.getenv("RINGCENTRAL_USERNAME"),
                System.getenv("RINGCENTRAL_EXTENSION"),
                System.getenv("RINGCENTRAL_PASSWORD")
        );

        final String[] message = {null};
        Subscription subscription = new Subscription(rc,
                new String[]{"/restapi/v1.0/account/~/extension/~/message-store"},
                str -> message[0] = str);
        subscription.refresh(); // should not cause any issue when _subscription is null
        subscription.subscribe();
        Thread.sleep(3000);
        subscription.refresh();
        Thread.sleep(3000);
        sendSms();
        Thread.sleep(16000);
        assertNotNull(message[0]);
        assertTrue(message[0].contains("uuid"));

        subscription.revoke();
        rc.revoke();
    }

    @Test
    public void testRevoke() throws IOException, RestException, InterruptedException {
        RestClient rc = new RestClient(
                System.getenv("RINGCENTRAL_CLIENT_ID"),
                System.getenv("RINGCENTRAL_CLIENT_SECRET"),
                System.getenv("RINGCENTRAL_SERVER_URL")
        );

        rc.authorize(
                System.getenv("RINGCENTRAL_USERNAME"),
                System.getenv("RINGCENTRAL_EXTENSION"),
                System.getenv("RINGCENTRAL_PASSWORD")
        );

        final String[] message = {null};
        Subscription subscription = new Subscription(rc,
                new String[]{"/restapi/v1.0/account/~/extension/~/message-store"},
                str -> message[0] = str);
        subscription.revoke(); // should not cause any issue when _subscription is null
        subscription.subscribe();
        Thread.sleep(1000);
        subscription.revoke();
        Thread.sleep(1000);
        sendSms();
        Thread.sleep(16000);
        assertNull(message[0]);

        rc.revoke();
    }

    @Test
    public void testAutoRefresh() throws IOException, RestException, InterruptedException {
        RestClient rc = new RestClient(
                System.getenv("RINGCENTRAL_CLIENT_ID"),
                System.getenv("RINGCENTRAL_CLIENT_SECRET"),
                System.getenv("RINGCENTRAL_SERVER_URL")
        );

        rc.authorize(
                System.getenv("RINGCENTRAL_USERNAME"),
                System.getenv("RINGCENTRAL_EXTENSION"),
                System.getenv("RINGCENTRAL_PASSWORD")
        );

        final String[] message = {null};
        Subscription subscription = new Subscription(rc,
                new String[]{"/restapi/v1.0/account/~/extension/~/message-store"},
                str -> message[0] = str);
        subscription.subscribe();
        SubscriptionInfo subInfo = subscription.getSubscription();
        subInfo.expiresIn = 123L;
        subscription.setSubscription(subInfo);
        Thread.sleep(6000);
        sendSms();
        Thread.sleep(16000);
        assertNotNull(message[0]);
        assertTrue(message[0].contains("uuid"));

        subscription.revoke();
        rc.revoke();
    }
}
