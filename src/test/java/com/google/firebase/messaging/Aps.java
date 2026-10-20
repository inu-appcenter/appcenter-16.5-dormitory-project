package com.google.firebase.messaging;

public class Aps {
    private String sound;
    private String threadId;
    private boolean contentAvailable;

    public boolean isContentAvailable() { return contentAvailable; }

    private Aps() {}

    public String getThreadId() {
        return threadId;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Aps aps = new Aps();

        public Builder setContentAvailable(boolean value) {
            aps.contentAvailable = value;
            return this;
        }

        public Builder setSound(String sound) {
            aps.sound = sound;
            return this;
        }

        public Builder setThreadId(String threadId) {
            aps.threadId = threadId;
            return this;
        }

        public Aps build() {
            return aps;
        }
    }
}
