package com.ringcentral.pubnub;

import com.ringcentral.RestClient;
import com.ringcentral.RestException;
import com.ringcentral.definitions.SubscriptionInfo;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class FaxPresenceTest {
    @Test
    public void testPresence() throws IOException, RestException, InterruptedException {
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
                new String[]{"/restapi/v1.0/account/~/extension/~/presence?detailedTelephonyState=true&sipData=true"},
                (jsonString) -> {
                    System.out.println(jsonString);
                }
        );
        subscription.subscribe();
        SubscriptionInfo sub = subscription.getSubscription();
        assert sub != null;
        assertEquals("Active", sub.status);

        Thread.sleep(16000);

        rc.revoke();
    }
}
