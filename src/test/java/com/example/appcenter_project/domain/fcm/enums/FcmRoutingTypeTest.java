package com.example.appcenter_project.domain.fcm.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FcmRoutingTypeTest {

    // BR-792: analyticsLabelPrefix() 메서드 테스트

    @Test
    @DisplayName("analyticsLabelPrefix = openchat — BR-792 AC2: CHAT_OPEN 라벨 prefix")
    void should_return_openchat_prefix_when_CHAT_OPEN() {
        // given / when
        String prefix = FcmRoutingType.CHAT_OPEN.analyticsLabelPrefix();

        // then
        assertThat(prefix).isEqualTo("openchat");
    }

    @Test
    @DisplayName("analyticsLabelPrefix = personalchat — BR-792 AC2: CHAT_PERSONAL 라벨 prefix")
    void should_return_personalchat_prefix_when_CHAT_PERSONAL() {
        // given / when
        String prefix = FcmRoutingType.CHAT_PERSONAL.analyticsLabelPrefix();

        // then
        assertThat(prefix).isEqualTo("personalchat");
    }

    @Test
    @DisplayName("analyticsLabelPrefix = announcement — BR-792 AC2: ANNOUNCEMENT 라벨 prefix")
    void should_return_announcement_prefix_when_ANNOUNCEMENT() {
        // given / when
        String prefix = FcmRoutingType.ANNOUNCEMENT.analyticsLabelPrefix();

        // then
        assertThat(prefix).isEqualTo("announcement");
    }

    @Test
    @DisplayName("analyticsLabelPrefix = roommatechat — BR-792 AC2: CHAT_ROOMMATE 라벨 prefix")
    void should_return_roommatechat_prefix_when_CHAT_ROOMMATE() {
        // given / when
        String prefix = FcmRoutingType.CHAT_ROOMMATE.analyticsLabelPrefix();

        // then
        assertThat(prefix).isEqualTo("roommatechat");
    }

    @Test
    @DisplayName("analyticsLabelPrefix = roommatepost — BR-792 AC2: ROOMMATE_POST 라벨 prefix")
    void should_return_roommatepost_prefix_when_ROOMMATE_POST() {
        // given / when
        String prefix = FcmRoutingType.ROOMMATE_POST.analyticsLabelPrefix();

        // then
        assertThat(prefix).isEqualTo("roommatepost");
    }
}
