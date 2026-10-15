package com.google.firebase.messaging;

public class ApnsConfig {
    private Aps aps;
    private final java.util.Map<String, String> headers = new java.util.HashMap<>();

    public java.util.Map<String, String> getHeaders() { return headers; }

    private ApnsConfig() {}

    public Aps getAps() {
        return aps;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final ApnsConfig config = new ApnsConfig();

        public Builder putHeader(String key, String value) {
            config.headers.put(key, value);
            return this;
        }

        public Builder setAps(Aps aps) {
            config.aps = aps;
            return this;
        }

        public ApnsConfig build() {
            return config;
        }
    }
}
