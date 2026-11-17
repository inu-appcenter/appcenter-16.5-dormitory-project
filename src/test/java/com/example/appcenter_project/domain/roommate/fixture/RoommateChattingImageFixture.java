package com.example.appcenter_project.domain.roommate.fixture;

import com.example.appcenter_project.common.image.entity.Image;
import com.example.appcenter_project.common.image.enums.ImageType;
import com.example.appcenter_project.domain.roommate.entity.RoommateChattingChat;
import com.example.appcenter_project.domain.roommate.entity.RoommateChattingRoom;
import com.example.appcenter_project.domain.user.entity.User;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class RoommateChattingImageFixture {

    public static List<MultipartFile> createImages(int count) {
        List<MultipartFile> files = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            files.add(new MockMultipartFile("images", "img" + (i + 1) + ".jpg", "image/jpeg", ("content-" + i).getBytes()));
        }
        return files;
    }

    public static MultipartFile createImage(String filename, String contentType, int sizeBytes) {
        return new MockMultipartFile("images", filename, contentType, new byte[sizeBytes]);
    }

    public static RoommateChattingRoom createRoom(User host, User guest) {
        return RoommateChattingRoom.create(host, guest);
    }

    // DB에 저장된 것처럼 id·createdDate를 채운 이미지 메시지
    public static RoommateChattingChat createSavedImageChat(Long id, RoommateChattingRoom room, User sender, boolean deleted) {
        RoommateChattingChat chat = RoommateChattingChat.createImageMessage(room, sender, false);
        ReflectionTestUtils.setField(chat, "id", id);
        ReflectionTestUtils.setField(chat, "createdDate", LocalDateTime.now());
        if (deleted) {
            chat.softDelete();
        }
        return chat;
    }

    public static RoommateChattingChat createSavedTextChat(Long id, RoommateChattingRoom room, User sender) {
        RoommateChattingChat chat = RoommateChattingChat.create(room, sender, "안녕하세요", LocalDateTime.now());
        ReflectionTestUtils.setField(chat, "id", id);
        return chat;
    }

    public static Image createStoredImage(Long chatId) {
        return Image.of("/images/roommate_chat_message/" + chatId + ".jpg",
                "roommate_chat_message_" + chatId + "_uuid.jpg", ImageType.ROOMMATE_CHAT_MESSAGE, chatId);
    }
}