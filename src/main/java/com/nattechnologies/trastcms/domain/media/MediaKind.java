package com.nattechnologies.trastcms.domain.media;

public enum MediaKind {
    IMAGE,
    VIDEO,
    AUDIO,
    DOCUMENT,
    OTHER;

    public static MediaKind fromContentType(String contentType) {
        if (contentType == null) return OTHER;
        if (contentType.startsWith("image/")) return IMAGE;
        if (contentType.startsWith("video/")) return VIDEO;
        if (contentType.startsWith("audio/")) return AUDIO;
        if ("application/pdf".equals(contentType)) return DOCUMENT;
        return OTHER;
    }
}
