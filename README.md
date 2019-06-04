# RingCentral PubNub SDK for Java

[![Download](https://api.bintray.com/packages/tylerlong/maven/ringcentral-pubnub/images/download.svg)](https://bintray.com/tylerlong/maven/ringcentral-pubnub/_latestVersion)

This project is an extension of the [RingCentral SDK for Java](https://github.com/ringcentral/ringcentral-java) project.


## Installation

### Gradle

```groovy
repositories {
  jcenter()
}

dependencies {
  compile 'com.ringcentral:ringcentral-pubnub:[version]'
}
```

Don't forget to replace `[version]` with expected version.


### Maven

```xml
<repositories>
  <repository>
    <id>jcenter</id>
    <url>https://jcenter.bintray.com/</url>
  </repository>
</repositories>

<dependency>
  <groupId>com.ringcentral</groupId>
  <artifactId>ringcentral-pubnub</artifactId>
  <version>[version]</version>
</dependency>
```

Don't forget to replace `[version]` with expected version.


### Manually

[Download jar here](https://bintray.com/tylerlong/maven/ringcentral-pubnub/_latestVersion) and save it into your java classpath.


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
InstanceMessageEvent instanceMessageEvent = com.alibaba.fastjson.JSON.parseObject(str, InstanceMessageEvent.class);
```
