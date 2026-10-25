package com.example.appcenter_project.domain.openChat.dto.response;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Getter
@Builder
public class ResponseChatRoomListDto {

    private List<ResponseOpenChatRoomDto> content;
    private long totalElements;
    private int totalPages;
    private int pageNumber;
    private int pageSize;
    private int totalUnreadCount;

    public static ResponseChatRoomListDto of(
            List<ResponseOpenChatRoomDto> all, Pageable pageable, int totalUnreadCount) {
        int total = all.size();
        int size = pageable.getPageSize();
        int num = pageable.getPageNumber();
        int start = Math.min(num * size, total);
        int end = Math.min(start + size, total);
        return ResponseChatRoomListDto.builder()
                .content(all.subList(start, end))
                .totalElements(total)
                .totalPages(size == 0 ? 1 : (int) Math.ceil((double) total / size))
                .pageNumber(num)
                .pageSize(size)
                .totalUnreadCount(totalUnreadCount)
                .build();
    }

    public static ResponseChatRoomListDto of(
            List<ResponseOpenChatRoomDto> content, long total, Pageable pageable, int totalUnreadCount) {
        int size = pageable.getPageSize();
        return ResponseChatRoomListDto.builder()
                .content(content)
                .totalElements(total)
                .totalPages(size == 0 ? 1 : (int) Math.ceil((double) total / size))
                .pageNumber(pageable.getPageNumber())
                .pageSize(size)
                .totalUnreadCount(totalUnreadCount)
                .build();
    }
}
