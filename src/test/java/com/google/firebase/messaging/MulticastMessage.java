package com.google.firebase.messaging;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MulticastMessage {
    private List<String> tokens;
    private Notification notification;
    private ApnsConfig apnsConfig;
    private AndroidConfig androidConfig;
    private Map<String, String> data;

    private MulticastMessage() {}

    public List<String> getTokens() { return tokens; }

    public Notification getNotification() { return notification; }

    public ApnsConfig getApnsConfig() {
        return apnsConfig;
    }

    public AndroidConfig getAndroidConfig() {
        return androidConfig;
    }

    public Map<String, String> getData() {
        return data != null ? data : Collections.emptyMap();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final List<String> tokens = new ArrayList<>();
        private Notification notification;
        private ApnsConfig apnsConfig;
        private AndroidConfig androidConfig;
        private final Map<String, String> data = new HashMap<>();

        public Builder addAllTokens(Collection<String> tokens) {
            this.tokens.addAll(tokens);
            return this;
        }

        public Builder setNotification(Notification notification) {
            this.notification = notification;
            return this;
        }

        public Builder setApnsConfig(ApnsConfig apnsConfig) {
            this.apnsConfig = apnsConfig;
            return this;
        }

        public Builder setAndroidConfig(AndroidConfig androidConfig) {
            this.androidConfig = androidConfig;
            return this;
        }

        public Builder putData(String key, String value) {
            this.data.put(key, value);
            return this;
        }

        public MulticastMessage build() {
            MulticastMessage msg = new MulticastMessage();
            msg.tokens = new ArrayList<>(tokens);
            msg.notification = this.notification;
            msg.apnsConfig = this.apnsConfig;
            msg.androidConfig = this.androidConfig;
            msg.data = this.data.isEmpty() ? null : new HashMap<>(this.data);
            return msg;
        }
    }
}
