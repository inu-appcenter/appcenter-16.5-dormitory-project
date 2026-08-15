package com.example.appcenter_project.domain.feature.entity;

import com.example.appcenter_project.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@Getter
public class Feature extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "feature_key")
    private String key;
    private boolean flag;

    @Builder
    public Feature(String key, boolean flag) {
        this.key = key;
        this.flag = flag;
    }

    public void updateFlag(boolean flag) {
        this.flag = flag;
    }
}
