package com.example.appcenter_project.common.image.service;

import com.example.appcenter_project.common.image.dto.ImageLinkDto;
import com.example.appcenter_project.common.image.entity.Image;
import com.example.appcenter_project.common.image.enums.ImageType;
import com.example.appcenter_project.common.image.repository.ImageRepository;
import com.example.appcenter_project.global.exception.CustomException;
import com.example.appcenter_project.global.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static com.example.appcenter_project.global.exception.ErrorCode.*;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ImageService {

    private static final String USER_IMAGE_PREFIX = "user";
    private static final String TIP_IMAGE_PREFIX = "tip";
    private static final String GROUP_ORDER_IMAGE_PREFIX = "group_order";
    private static final String TIME_TABLE_IMAGE_PREFIX = "time_table";
    private static final String COMPLAINT_IMAGE_PREFIX = "complaint";
    private static final String COMPLAINT_REPLY_IMAGE_PREFIX = "complaint_reply";
    private static final String POPUP_NOTIFICATION_IMAGE_PREFIX = "popup_notification";
    private static final String OPEN_CHAT_MESSAGE_IMAGE_PREFIX = "open_chat_message";
    private static final String ROOMMATE_CHAT_MESSAGE_IMAGE_PREFIX = "roommate_chat_message";
    private static final String DEFAULT_IMAGE_EXTENSION = ".jpg";

    private final ImageRepository imageRepository;

    // todo saveImages
    public void saveImages(ImageType imageType, Long entityId, List<MultipartFile> images) {
        if (isImagesNotEmpty(images)) {
            String directoryPath = createDirectory(imageType);
            createAndSaveImages(directoryPath, images, entityId, imageType);
        }
    }

    // todo saveImage
    public void saveImage(ImageType imageType, Long entityId, MultipartFile image) {
        if (isImageNotEmpty(image)) {
            String directoryPath = createDirectory(imageType);
            createAndSaveImage(directoryPath, image, entityId, imageType, true);
        }
    }

    // todo findImages
    public List<ImageLinkDto> findImages(ImageType imageType, Long entityId, HttpServletRequest request) {
        try {
            List<Image> images = findImagesByImageTypeAndEntityId(imageType, entityId);
            return createImageDtos(imageType, request, images);
        } catch (Exception e) {
            log.error("타입: {} id: {} 이미지 조회에 실패했습니다", imageType, entityId, e);
            throw new CustomException(IMAGE_NOT_FOUND);
        }
    }

    // todo findImage
    public ImageLinkDto findImage(ImageType imageType, Long entityId, HttpServletRequest request) {
        try {
            Optional<Image> image = findImageByImageTypeAndEntityId(imageType, entityId);
            return createImageDto(imageType, request, image);
        } catch (Exception e) {
            log.error("타입: {} id: {} 이미지 조회에 실패했습니다", imageType, entityId, e);
            throw new CustomException(IMAGE_NOT_FOUND);
        }
    }

    public List<String> findStaticImageUrls(ImageType imageType, Long entityId, HttpServletRequest request) {
        try {
            return findImagesByImageTypeAndEntityId(imageType, entityId).stream()
                    .map(findImage -> createImageUrl(imageType, request, findImage))
                    .toList();
        } catch (Exception e) {
            log.error("타입: {} id: {} 이미지 조회에 실패했습니다", imageType, entityId, e);
            throw new CustomException(IMAGE_NOT_FOUND);
        }
    }

    // todo updateImages
    public void updateImages(ImageType imageType, Long entityId, List<MultipartFile> images) {
        deleteImages(imageType, entityId);
        saveImages(imageType, entityId, images);
    }

    public void updateGroupOrderImages(ImageType imageType, Long entityId, List<MultipartFile> images) {
        deleteImages(imageType, entityId);
        saveImages(imageType, entityId, images);
    }

    // todo updateImage
    public void updateImage(ImageType imageType, Long entityId, MultipartFile image) {
        deleteImage(imageType, entityId);
        saveImage(imageType, entityId, image);
    }

    // todo deleteImages
    public void deleteImages(ImageType imageType, Long entityId) {
        List<Image> images = findImagesByImageTypeAndEntityId(imageType, entityId);
        for (Image image : images) {
            deleteImageFromDirectory(image);
            deleteImageFromDB(image);
        }
    }

    // todo deleteImage
    public void deleteImage(ImageType imageType, Long entityId) {
        Optional<Image> image = findImageByImageTypeAndEntityId(imageType, entityId);

        image.ifPresent(img -> {
            deleteImageFromDirectory(img);
            deleteImageFromDB(img);
        });
    }

    public void deleteImage(Image image) {
        deleteImageFromDirectory(image);
        deleteImageFromDB(image);
    }

    public String findStaticImageUrl(ImageType imageType, Long entityId, HttpServletRequest request) {
        return findImage(imageType, entityId, request).getImageUrl();
    }

    private String findStaticImageUrl(ImageType imageType, String imagePath, String appcenterHttpsUrl) {
        try {
            String imageName = getImageName(imagePath);
            if (imageType == ImageType.USER) {
                imageName = "/images/" + USER_IMAGE_PREFIX + "/" + imageName;
            }
            if (imageType == ImageType.TIP) {
                imageName = "/images/" + TIP_IMAGE_PREFIX + "/" + imageName;
            }
            if (imageType == ImageType.GROUP_ORDER) {
                imageName = "/images/" + GROUP_ORDER_IMAGE_PREFIX + "/" + imageName;
            }
            if (imageType == ImageType.TIME_TABLE) {
                imageName = "/images/" + TIME_TABLE_IMAGE_PREFIX + "/" + imageName;
            }
            if (imageType == ImageType.COMPLAINT) {
                imageName = "/images/" + COMPLAINT_IMAGE_PREFIX + "/" + imageName;
            }
            if (imageType == ImageType.COMPLAINT_REPLY) {
                imageName = "/images/" + COMPLAINT_REPLY_IMAGE_PREFIX + "/" + imageName;
            }
            if (imageType == ImageType.POPUP_NOTIFICATION) {
                imageName = "/images/" + POPUP_NOTIFICATION_IMAGE_PREFIX + "/" + imageName;
            }
            if (imageType == ImageType.OPEN_CHAT_MESSAGE) {
                imageName = "/images/" + OPEN_CHAT_MESSAGE_IMAGE_PREFIX + "/" + imageName;
            }
            if (imageType == ImageType.ROOMMATE_CHAT_MESSAGE) {
                imageName = "/images/" + ROOMMATE_CHAT_MESSAGE_IMAGE_PREFIX + "/" + imageName;
            }

            return appcenterHttpsUrl + imageName;
        } catch (Exception e) {
            log.warn("Could not generate static URL for group-order image path: {}", imagePath);
            return null;
        }
    }

    public String getImageUrl(ImageType imageType, Image image, HttpServletRequest request) {
        return createImageUrl(imageType, request, image);
    }

    private boolean isImagesNotEmpty(List<MultipartFile> images) {
        return images != null && !images.isEmpty();
    }

    private boolean isImageNotEmpty(MultipartFile image) {
        return image != null && !image.isEmpty();
    }


    private String createDirectory(ImageType imageType) {
        String directoryPath = createDirectoryPath(imageType);
        createDirectoryDetail(directoryPath);

        return directoryPath;
    }

    private String createDirectoryPath(ImageType imageType) {
        String basePath = System.getProperty("user.dir");
        String directoryPath = basePath + "/images/";

        if (imageType == ImageType.USER) {
            directoryPath += (USER_IMAGE_PREFIX + "/");
        }
        if (imageType == ImageType.TIP) {
            directoryPath += (TIP_IMAGE_PREFIX + "/");
        }
        if (imageType == ImageType.GROUP_ORDER) {
            directoryPath += (GROUP_ORDER_IMAGE_PREFIX + "/");
        }
        if (imageType == ImageType.TIME_TABLE) {
            directoryPath += (TIME_TABLE_IMAGE_PREFIX + "/");
        }
        if (imageType == ImageType.COMPLAINT) {
            directoryPath += (COMPLAINT_IMAGE_PREFIX + "/");
        }
        if (imageType == ImageType.COMPLAINT_REPLY) {
            directoryPath += (COMPLAINT_REPLY_IMAGE_PREFIX + "/");
        }
        if (imageType == ImageType.POPUP_NOTIFICATION) {
            directoryPath += (POPUP_NOTIFICATION_IMAGE_PREFIX + "/");
        }
        if (imageType == ImageType.OPEN_CHAT_MESSAGE) {
            directoryPath += (OPEN_CHAT_MESSAGE_IMAGE_PREFIX + "/");
        }
        if (imageType == ImageType.ROOMMATE_CHAT_MESSAGE) {
            directoryPath += (ROOMMATE_CHAT_MESSAGE_IMAGE_PREFIX + "/");
        }

        return directoryPath;
    }

    private void createDirectoryDetail(String directoryPath) {
        File directory = new File(directoryPath);
        if (doesNotDirectoryExists(directory)) {
            boolean created = directory.mkdirs();
            if (!created) {
                log.error("폴더를 만드는 것을 실패했습니다: {}", directoryPath);
                throw new CustomException(IMAGE_DIRECTORY_SAVE_FAIL);
            }
        }
    }

    private static boolean doesNotDirectoryExists(File directory) {
        return !directory.exists();
    }

    private void createAndSaveImages(String directoryPath, List<MultipartFile> images, Long entityId, ImageType imageType) {
        for (int i = 0; i < images.size(); i++) {
            createAndSaveImage(directoryPath, images.get(i), entityId, imageType, i == 0);
        }
    }

    private void createAndSaveImage(String directoryPath, MultipartFile image, Long entityId, ImageType imageType, boolean isDefault) {
        try {
            // HEIC 파일인 경우 처리
            MultipartFile processedImage = image;
            String imageName;

            if (isHeicFile(image)) {
                log.warn("HEIC 파일 감지: {}. 변환을 시도합니다.", image.getOriginalFilename());
                try {
                    processedImage = convertHeicToJpeg(image, entityId);
                    imageName = createImageName(entityId, processedImage, imageType);
                    log.info("HEIC 파일 변환 성공: {}", imageName);
                } catch (Exception e) {
                    log.error("HEIC 변환 실패. 원본 파일명 그대로 저장합니다: {}", e.getMessage());
                    imageName = createImageName(entityId, image, imageType);
                }
            } else {
                imageName = createImageName(entityId, image, imageType);
            }

            File savedImageInDirectory = saveImageToDirectory(directoryPath, processedImage, imageName);
            saveImageToDB(entityId, imageType, savedImageInDirectory, imageName, isDefault);
        } catch (IOException e) {
            log.error("{} 타입의 entityId : {} 이미지가 저장 실패했습니다 ", imageType, entityId, e);
            throw new CustomException(IMAGE_SAVE_FAIL);
        }
    }

    private String createImageName(Long entityId, MultipartFile imageFile, ImageType imageType) {
        String imageName = createFixedImageName(entityId, imageFile);

        if (imageType == ImageType.USER) {
            imageName = USER_IMAGE_PREFIX + "_" + imageName;
        }
        if (imageType == ImageType.TIP) {
            imageName = TIP_IMAGE_PREFIX + "_" + imageName;
        }
        if (imageType == ImageType.GROUP_ORDER) {
            imageName = GROUP_ORDER_IMAGE_PREFIX + "_" + imageName;
        }
        if (imageType == ImageType.TIME_TABLE) {
            imageName = TIME_TABLE_IMAGE_PREFIX + "_" + imageName;
        }
        if (imageType == ImageType.COMPLAINT) {
            imageName = COMPLAINT_IMAGE_PREFIX + "_" + imageName;
        }
        if (imageType == ImageType.COMPLAINT_REPLY) {
            imageName = COMPLAINT_REPLY_IMAGE_PREFIX + "_" + imageName;
        }
        if (imageType == ImageType.POPUP_NOTIFICATION) {
            imageName = POPUP_NOTIFICATION_IMAGE_PREFIX + "_" + imageName;
        }
        if (imageType == ImageType.OPEN_CHAT_MESSAGE) {
            imageName = OPEN_CHAT_MESSAGE_IMAGE_PREFIX + "_" + imageName;
        }
        if (imageType == ImageType.ROOMMATE_CHAT_MESSAGE) {
            imageName = ROOMMATE_CHAT_MESSAGE_IMAGE_PREFIX + "_" + imageName;
        }

        return imageName;
    }

    private String createFixedImageName(Long entityId, MultipartFile imageFile) {
        String fileExtension = getFileExtension(imageFile.getOriginalFilename());
        String uuid = createImageUUID();
        String imageName = entityId + "_" + uuid + fileExtension;
        return imageName;
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return DEFAULT_IMAGE_EXTENSION;
        }

        int lastDotIndex = fileName.lastIndexOf(".");
        if (lastDotIndex == -1) {
            return DEFAULT_IMAGE_EXTENSION;
        }

        return fileName.substring(lastDotIndex).toLowerCase();
    }

    private String createImageUUID() {
        return UUID.randomUUID().toString();
    }

    private File saveImageToDirectory(String directoryPath, MultipartFile imageFile, String imageName) throws IOException {
        File destinationFile = new File(directoryPath + imageName);
        imageFile.transferTo(destinationFile);

        log.info("Image transfer successfully: {}", destinationFile.getAbsolutePath());

        return destinationFile;
    }

    private void saveImageToDB(Long entityId, ImageType imageType, File destinationFile, String imageName, boolean isDefault) {
        Image image = Image.builder()
                .filePath(destinationFile.getAbsolutePath())
                .fileName(imageName)
                .imageType(imageType)
                .entityId(entityId)
                .isDefault(isDefault)
                .build();
        imageRepository.save(image);
    }

    private List<Image> findImagesByImageTypeAndEntityId(ImageType imageType, Long entityId) {
        return imageRepository.findByImageTypeAndEntityId(imageType, entityId);
    }

    private Optional<Image> findImageByImageTypeAndEntityId(ImageType imageType, Long entityId) {
        List<Image> images = imageRepository.findByImageTypeAndEntityId(imageType, entityId);

        if (images == null || images.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(images.get(0));
    }

    private List<ImageLinkDto> createImageDtos(ImageType imageType, HttpServletRequest request, List<Image> images) {
        List<ImageLinkDto> imageLinkDtos = new ArrayList<>();

        for (Image image : images) {
            ImageLinkDto imageLinkDto = createImageDto(imageType, request, image);
            imageLinkDtos.add(imageLinkDto);
        }

        return imageLinkDtos;
    }

    public ImageLinkDto createImageDto(ImageType imageType, HttpServletRequest request, Optional<Image> optionalImage) {
        if (isOptionalImageNull(optionalImage)) {
            return ImageLinkDto.ofNull();
        }
        Image image = optionalImage.get();

        File findImage = getImageInDirectory(image);

        checkImageAndDirectoryExists(image, findImage);

        String staticImageUrl = createImageUrl(imageType, request, image);

        String contentType = getImageContentType(findImage);

        String imageName = getImageName(image.getImagePath());

        return ImageLinkDto.of(imageName, staticImageUrl, contentType, findImage.length());
    }

    public ImageLinkDto createImageDto(ImageType imageType, HttpServletRequest request, Image image) {
        File findImage = getImageInDirectory(image);

        checkImageAndDirectoryExists(image, findImage);

        String staticImageUrl = createImageUrl(imageType, request, image);

        String contentType = getImageContentType(findImage);

        String imageName = getImageName(image.getImagePath());

        return ImageLinkDto.of(imageName, staticImageUrl, contentType, findImage.length());
    }

    private static boolean isOptionalImageNull(Optional<Image> optionalImage) {
        return !optionalImage.isPresent();
    }

    private String createImageUrl(ImageType imageType, HttpServletRequest request, Image image) {
        String appcenterHttpsUrl = getAppcenterHttpsUrl(request);
        String imagePath = (image != null) ? image.getImagePath() : null;
        return findStaticImageUrl(imageType, imagePath, appcenterHttpsUrl);
    }

    private File getImageInDirectory(Image image) {
        File file = new File(image.getImagePath());
        return file;
    }

    private void checkImageAndDirectoryExists(Image image, File file) {
        if (!file.exists()) {
            log.error("이미지 파일이 존재하지 않습니다 : {}", image.getImagePath());

            // 디렉토리 존재 여부도 확인
            File directory = file.getParentFile();
            log.error("해당 이미지의 폴더가 존재한다에 대한 여부 : {}", directory.exists());
            if (directory.exists()) {
                log.error("폴더는 존재하는데 이미지 파일이 존재하지 않는다: {}", Arrays.toString(directory.listFiles()));
            }

            throw new CustomException(IMAGE_NOT_FOUND);
        }
    }

    private String getAppcenterHttpsUrl(HttpServletRequest request) {
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        String scheme = (forwardedProto != null && !forwardedProto.isBlank()) ? forwardedProto : request.getScheme();
        String serverName = request.getServerName();
        String contextPath = request.getContextPath();

        String forwardedPort = request.getHeader("X-Forwarded-Port");
        int serverPort;
        if (forwardedPort != null && !forwardedPort.isBlank()) {
            serverPort = Integer.parseInt(forwardedPort.trim());
        } else if (forwardedProto != null && !forwardedProto.isBlank()) {
            serverPort = scheme.equals("https") ? 443 : 80;
        } else {
            serverPort = request.getServerPort();
        }

        StringBuilder baseUrl = new StringBuilder();
        baseUrl.append(scheme).append("://").append(serverName);

        if ((scheme.equals("http") && serverPort != 80) ||
                (scheme.equals("https") && serverPort != 443)) {
            baseUrl.append(":").append(serverPort);
        }

        baseUrl.append(contextPath);
        return baseUrl.toString();
    }

    private String getImageContentType(File file) {
        try {
            String fileName = file.getName().toLowerCase();

            if (fileName.endsWith(DEFAULT_IMAGE_EXTENSION) || fileName.endsWith(".jpeg")) {
                return "image/jpeg";
            } else if (fileName.endsWith(".png")) {
                return "image/png";
            } else if (fileName.endsWith(".gif")) {
                return "image/gif";
            } else if (fileName.endsWith(".webp")) {
                return "image/webp";
            } else if (fileName.endsWith(".svg")) {
                return "image/svg+xml";
            } else if (fileName.endsWith(".heic") || fileName.endsWith(".heif")) {
                // HEIC는 이미 JPEG로 변환되었으므로 jpeg 타입 반환
                return "image/jpeg";
            }

            try {
                String detectedType = Files.probeContentType(file.toPath());
                if (detectedType != null && detectedType.startsWith("image/")) {
                    return detectedType;
                }
            } catch (Exception e) {
                log.warn("파일의 콘텐츠 타입을 감지할 수 없습니다: {}", file.getName());
            }

            return "image/jpeg";
        } catch (Exception e) {
            log.error("파일의 콘텐츠 타입을 결정하는 중 오류가 발생했습니다: {}", file.getName(), e);
            return "image/jpeg";
        }
    }

    private String getImageName(String imagePath) {
        return Paths.get(imagePath).getFileName().toString();
    }

    private void deleteImageFromDirectory(Image image) {
        File findImage = new File(image.getImagePath());
        if (findImage.exists()) {
            if (canNotDeleteImage(findImage)) {
                log.warn("{} 이미지 삭제에 실패했습니다", image.getImagePath());
            }
        }
    }

    private boolean canNotDeleteImage(File findImage) {
        return !findImage.delete();
    }

    private void deleteImageFromDB(Image image) {
        imageRepository.delete(image);
    }

    /**
     * HEIC 파일 여부 확인
     */
    private boolean isHeicFile(MultipartFile file) {
        if (file == null || file.getOriginalFilename() == null) {
            return false;
        }
        String fileName = file.getOriginalFilename().toLowerCase();
        return fileName.endsWith(".heic") || fileName.endsWith(".heif");
    }

    /**
     * HEIC 파일을 JPEG로 변환
     */
    private MultipartFile convertHeicToJpeg(MultipartFile heicFile, Long entityId) throws IOException {
        try {
            log.info("HEIC 파일 변환 시작: {}", heicFile.getOriginalFilename());
            
            // 임시 HEIC 파일 저장
            File tempHeicFile = File.createTempFile("heic_input_" + entityId + "_", ".heic");
            heicFile.transferTo(tempHeicFile);
            
            // 출력 JPEG 파일 생성
            File tempJpegFile = File.createTempFile("converted_" + entityId + "_", ".jpg");
            
            // ImageMagick 명령어 실행 (convert 또는 magick 명령어)
            ProcessBuilder processBuilder = new ProcessBuilder();
            
            // Windows의 경우 "magick", Linux/Mac의 경우 "convert" 사용
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                processBuilder.command("magick", "convert", 
                    tempHeicFile.getAbsolutePath(), 
                    "-quality", "90",
                    tempJpegFile.getAbsolutePath());
            } else {
                processBuilder.command("convert", 
                    tempHeicFile.getAbsolutePath(), 
                    "-quality", "90",
                    tempJpegFile.getAbsolutePath());
            }
            
            try {
                Process process = processBuilder.start();
                int exitCode = process.waitFor();
                
                if (exitCode != 0) {
                    // ImageMagick이 없거나 실패한 경우 - 순수 Java로 시도
                    log.warn("ImageMagick 변환 실패. 순수 Java 방식으로 시도합니다.");
                    return convertHeicToJpegPureJava(heicFile, entityId, tempHeicFile, tempJpegFile);
                }
                
                log.info("HEIC -> JPEG 변환 성공: {}", heicFile.getOriginalFilename());
                
                // 임시 HEIC 파일 삭제
                tempHeicFile.delete();
                tempJpegFile.deleteOnExit();
                
                // MultipartFile로 변환하여 반환
                return new ConvertedMultipartFile(tempJpegFile, getJpegFileName(heicFile.getOriginalFilename()));
                
            } catch (Exception e) {
                log.error("ImageMagick 실행 중 오류: {}", e.getMessage());
                // ImageMagick이 설치되지 않은 경우 대체 방법 사용
                return convertHeicToJpegPureJava(heicFile, entityId, tempHeicFile, tempJpegFile);
            }
            
        } catch (IOException e) {
            log.error("HEIC 변환 중 오류 발생: {}", heicFile.getOriginalFilename(), e);
            throw new CustomException(IMAGE_SAVE_FAIL);
        }
    }

    /**
     * 순수 Java로 HEIC 변환 시도 (ImageIO 사용)
     */
    private MultipartFile convertHeicToJpegPureJava(MultipartFile heicFile, Long entityId, 
                                                      File tempHeicFile, File tempJpegFile) throws IOException {
        try {
            // ImageIO로 읽기 시도
            BufferedImage bufferedImage = ImageIO.read(tempHeicFile);
            
            if (bufferedImage == null) {
                log.error("HEIC 파일을 읽을 수 없습니다. 원본 파일을 사용합니다: {}", heicFile.getOriginalFilename());
                // 변환 실패 시 원본 파일 그대로 반환
                throw new CustomException(IMAGE_SAVE_FAIL);
            }
            
            // JPEG로 저장
            boolean written = ImageIO.write(bufferedImage, "jpg", tempJpegFile);
            if (!written) {
                log.error("JPEG 변환에 실패했습니다: {}", heicFile.getOriginalFilename());
                throw new CustomException(IMAGE_SAVE_FAIL);
            }
            
            log.info("순수 Java 방식으로 HEIC -> JPEG 변환 성공");
            tempHeicFile.delete();
            tempJpegFile.deleteOnExit();
            
            return new ConvertedMultipartFile(tempJpegFile, getJpegFileName(heicFile.getOriginalFilename()));
            
        } catch (Exception e) {
            log.error("순수 Java 변환도 실패: {}", e.getMessage());
            tempHeicFile.delete();
            tempJpegFile.delete();
            throw new CustomException(IMAGE_SAVE_FAIL);
        }
    }

    /**
     * HEIC 파일명을 JPEG 파일명으로 변환
     */
    private String getJpegFileName(String originalFileName) {
        if (originalFileName == null) {
            return "converted.jpg";
        }
        return originalFileName.replaceAll("(?i)\\.heic$|\\.heif$", ".jpg");
    }

    /**
     * 변환된 파일을 MultipartFile로 래핑하는 내부 클래스
     */
    private static class ConvertedMultipartFile implements MultipartFile {
        private final File file;
        private final String originalFilename;

        public ConvertedMultipartFile(File file, String originalFilename) {
            this.file = file;
            this.originalFilename = originalFilename;
        }

        @Override
        public String getName() {
            return "file";
        }

        @Override
        public String getOriginalFilename() {
            return originalFilename;
        }

        @Override
        public String getContentType() {
            return "image/jpeg";
        }

        @Override
        public boolean isEmpty() {
            return file.length() == 0;
        }

        @Override
        public long getSize() {
            return file.length();
        }

        @Override
        public byte[] getBytes() throws IOException {
            return Files.readAllBytes(file.toPath());
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return new FileInputStream(file);
        }

        @Override
        public void transferTo(File dest) throws IOException {
            Files.copy(file.toPath(), dest.toPath());
        }
    }
}