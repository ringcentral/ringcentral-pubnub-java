# RingCentral PubNub SDK for Java

This project is an extension of the [RingCentral SDK for Java](https://github.com/ringcentral/ringcentral-java) project.


## Getting help and support

If you are having difficulty using this SDK, or working with the RingCentral API, please visit our [developer community forums](https://community.ringcentral.com/spaces/144/) for help and to get quick answers to your questions. If you wish to contact the RingCentral Developer Support team directly, please [submit a help ticket](https://developers.ringcentral.com/support/create-case) from our developer website.


## Installation

This SDK is tested against JDK 11 so we recommend using the same. Earlier versions such as Java 8 should work as well, please report issues if you encounter any.

### Gradle

```groovy
repositories {
  mavenCentral()
}

dependencies {
  implementation 'com.ringcentral:ringcentral-pubnub:[version]'
}
```

Don't forget to replace `[version]` with expected version. You can find the latest versions in [Maven Central](https://search.maven.org/search?q=a:ringcentral).


### Maven

```xml
<dependency>
  <groupId>com.ringcentral</groupId>
  <artifactId>ringcentral-pubnub</artifactId>
  <version>[version]</version>
</dependency>
```

Don't forget to replace `[version]` with expected version. You can find the latest versions in [Maven Central](https://search.maven.org/search?q=a:ringcentral).


### Manually

[Download jar here](https://search.maven.org/classic/#search%7Cga%7C1%7Ca%3A%22ringcentral-pubnub%22) and save it into your java classpath.


## Usage

### Subscription & notification

```java
RestClient rc = new RestClient(clientId, clientSecret, server);
rc.authorize(username, extension, password);

Subscription subscription = new Subscription(rc,
    new String[]{
        "/restapi/v1.0/glip/posts",
        "/restapi/v1.0/account/~/extension/~/message-store",
        // more event filters here
    },
    (message) -> {
        // do something with message
    });
subscription.subscribe();
```


#### Deserialize notification message

The notification `message` you get is a java `String`. If you know the message body type, you can deserialize it like this:

```java
com.ringcentral.Utils.gson.fromJson(message, InstanceMessageEvent.class);
```
