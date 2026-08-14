package com.example.appcenter_project.domain.feature.service;

import com.example.appcenter_project.domain.feature.dto.request.RequestFeatureDto;
import com.example.appcenter_project.domain.feature.dto.response.ResponseFeatureDto;
import com.example.appcenter_project.domain.feature.entity.Feature;
import com.example.appcenter_project.domain.feature.enums.AbGroup;
import com.example.appcenter_project.domain.feature.repository.FeatureRepository;
import com.example.appcenter_project.global.exception.CustomException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.example.appcenter_project.global.exception.ErrorCode.DUPLICATE_FEATURE_KEY;
import static com.example.appcenter_project.global.exception.ErrorCode.FEATURE_NOT_FOUND;

@Service
@RequiredArgsConstructor
@Transactional
public class FeatureService {

    private final FeatureRepository featureRepository;

    public void saveFeature(RequestFeatureDto dto) {
        if (featureRepository.findByKey(dto.getKey()).isPresent()) {
            throw new CustomException(DUPLICATE_FEATURE_KEY);
        }
        Feature feature = Feature.builder().key(dto.getKey()).flag(dto.isFlag()).build();
        featureRepository.save(feature);
    }

    public ResponseFeatureDto findFeature(String key) {
        Feature feature = featureRepository.findByKey(key).orElseThrow(() -> new CustomException(FEATURE_NOT_FOUND));
        return ResponseFeatureDto.of(feature);
    }

    public List<ResponseFeatureDto> findAllFeatures() {
        return featureRepository.findAll().stream()
                .map(ResponseFeatureDto::of)
                .toList();
    }

    public void updateFeature(RequestFeatureDto dto) {
        Feature feature = featureRepository.findByKey(dto.getKey()).orElseThrow(() -> new CustomException(FEATURE_NOT_FOUND));
        feature.updateFlag(dto.isFlag());
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public AbGroup getAbGroup(String key, Long userId) {
        boolean enabled = featureRepository.findByKey(key)
                .map(Feature::isFlag)
                .orElse(false);
        if (!enabled) return AbGroup.OFF;
        int hash = Math.abs((userId + ":" + key).hashCode());
        return hash % 100 < 50 ? AbGroup.A : AbGroup.B;
    }
}
