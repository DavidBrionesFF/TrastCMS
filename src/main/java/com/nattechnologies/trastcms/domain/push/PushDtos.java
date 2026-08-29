package com.nattechnologies.trastcms.domain.push;

public class PushDtos {
    public record SubscribeRequest(String endpoint, String p256dhKey, String authKey, String userAgent) {}
    public record SendTestRequest(String title, String body, String url) {}
}
