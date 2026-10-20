package com.example.appcenter_project.common;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Embeddable
@NoArgsConstructor
public class DeletedMessageState {

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    private LocalDateTime deletedAt;

    public void delete() {
        if(this.isDeleted) return;

        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
    }
}