package com.example.appcenter_project.global.scheduler;

import com.example.appcenter_project.common.file.entity.CrawledAnnouncementFile;
import com.example.appcenter_project.domain.announcement.entity.CrawledAnnouncement;
import com.example.appcenter_project.domain.announcement.enums.AnnouncementCategory;
import com.example.appcenter_project.domain.announcement.service.CrawledAnnouncementUpdateService;
import com.example.appcenter_project.domain.notification.entity.Notification;
import com.example.appcenter_project.domain.notification.entity.UserNotification;
import com.example.appcenter_project.domain.user.entity.User;
import com.example.appcenter_project.shared.enums.ApiType;
import com.example.appcenter_project.domain.announcement.enums.AnnouncementType;
import com.example.appcenter_project.domain.user.enums.NotificationType;
import com.example.appcenter_project.domain.user.enums.Role;
import com.example.appcenter_project.domain.announcement.repository.CrawledAnnouncementRepository;
import com.example.appcenter_project.common.file.repository.CrawledAnnouncementFileRepository;
import com.example.appcenter_project.domain.notification.repository.NotificationRepository;
import com.example.appcenter_project.domain.notification.repository.UserNotificationRepository;
import com.example.appcenter_project.domain.user.repository.UserRepository;
import com.example.appcenter_project.domain.fcm.service.FcmMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static com.example.appcenter_project.domain.calender.service.AiScheduleService.CHANGE_DETECT_WEEKS;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnnouncementCrawlScheduler {

    private static final String GENERAL_NOTICE_BASE_URL = "https://dorm.inu.ac.kr/dorm/6528/subview.do";
    private static final String DORMITORY_MOVE_BASE_URL = "https://dorm.inu.ac.kr/dorm/6521/subview.do";

    private final CrawledAnnouncementRepository crawledAnnouncementRepository;
    private final CrawledAnnouncementFileRepository crawledAnnouncementFileRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final UserNotificationRepository userNotificationRepository;
    private final FcmMessageService fcmMessageService;
    private final CrawledAnnouncementUpdateService crawledAnnouncementUpdateService;

    @Scheduled(cron = "0 0 9,14,18 * * ?")
    public void crawling() {
        WebDriver driver = null;

        try {
            ChromeOptions options = createChromeOptions();
            driver = new ChromeDriver(options);
            log.info("WebDriver 인스턴스를 성공적으로 생성했습니다.");

            crawlAndSaveBoard(driver, GENERAL_NOTICE_BASE_URL, false);
            crawlAndSaveBoard(driver, DORMITORY_MOVE_BASE_URL, true);

        } catch (Exception e) {
            log.error("전체 크롤링 작업 중 치명적인 오류 발생: {}", e.getMessage(), e);
        } finally {
            if (driver != null) {
                driver.quit();
                log.info("WebDriver 인스턴스를 성공적으로 종료했습니다.");
            }
        }
    }

    private void crawlAndSaveBoard(WebDriver driver, String baseUrl, boolean isDormMove) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        try {
            driver.get(baseUrl);
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("table.board-table")));
            int totalPages = getTotalPages(driver);
            log.info("총 페이지 수: {}", totalPages);

            for (int page = 1; page <= 2; page++) {
                log.info("페이지 {} 크롤링 시작...", page);

                int rowCount = getRowCountForPage(driver, wait, baseUrl, page);
                log.info("페이지 {}에서 {}개 공지사항 발견", page, rowCount);

                for (int i = 0; i < rowCount; i++) {
                    try {
                        navigateToListPage(driver, wait, baseUrl, page);

                        List<WebElement> linkElements = driver.findElements(By.cssSelector("td.td-subject a"));
                        if (i >= linkElements.size()) break;

                        linkElements.get(i).click();
                        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".view-title")));

                        String detailUrl = driver.getCurrentUrl();
                        log.info("상세 페이지 이동: {}", detailUrl);
                        saveCrawlAnnouncement(driver, detailUrl, isDormMove);

                    } catch (Exception e) {
                        log.error("공지사항 저장 실패 (page: {}, index: {}): {}", page, i, e.getMessage());
                    }
                }
            }
            log.info("전체 크롤링 완료 (baseUrl: {})", baseUrl);
        } catch (Exception e) {
            log.error("Selenium 크롤링 실패 (URL: {}): ", baseUrl, e);
        }
    }

    private int getRowCountForPage(WebDriver driver, WebDriverWait wait, String baseUrl, int page) throws InterruptedException {
        navigateToListPage(driver, wait, baseUrl, page);
        return driver.findElements(By.cssSelector("td.td-subject a")).size();
    }

    private void navigateToListPage(WebDriver driver, WebDriverWait wait, String baseUrl, int page) throws InterruptedException {
        driver.get(baseUrl);
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("table.board-table")));
        if (page > 1) {
            ((JavascriptExecutor) driver).executeScript("page_link('" + page + "')");
            Thread.sleep(1000);
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("table.board-table")));
        }
    }

    private ChromeOptions createChromeOptions() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--disable-software-rasterizer");
        options.addArguments("--disable-extensions");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--user-agent=Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        options.addArguments("--disable-background-networking");
        options.addArguments("--disable-default-apps");
        options.addArguments("--no-first-run");
        options.addArguments("--disable-sync");
        options.addArguments("--disable-crash-reporter");
        return options;
    }


    private void saveCrawlAnnouncements(WebDriver driver, List<String> crawlLinks, Set<String> dormitoryMoveLinks) {
        for (String crawlLink : crawlLinks) {
            try {
                // 드라이버 인스턴스 전달
                saveCrawlAnnouncement(driver, crawlLink, dormitoryMoveLinks.contains(crawlLink));
            } catch (Exception e) {
                log.error("공지사항 저장 실패 (링크: {}): {}", crawlLink, e.getMessage());
                // 한 건 실패해도 계속 진행
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveCrawlAnnouncement(WebDriver driver, String link, boolean isDormitoryMove) {
        try {
            driver.get(link);
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".view-title")));

            String title = "";
            String category = "";
            try {
                WebElement titleElement = driver.findElement(By.cssSelector(".view-title"));
                String fullTitle = titleElement.getText().trim();
                if (isDormitoryMove) {
                    category = "입퇴사 공지";
                    title = fullTitle;
                } else if (fullTitle.startsWith("[") && fullTitle.contains("]")) {
                    int endIndex = fullTitle.indexOf("]");
                    category = fullTitle.substring(1, endIndex);
                    title = fullTitle.substring(endIndex + 1).trim();
                } else {
                    category = "기타";
                    title = fullTitle;
                }
            } catch (Exception e) {
                log.debug("제목 추출 실패");
                category = isDormitoryMove ? "입퇴사 공지" : "기타";
            }

            int viewCountInt = 0;
            try {
                WebElement viewCountElement = driver.findElement(By.cssSelector("dl.count dd"));
                String viewCount = viewCountElement.getText().trim();
                if (!viewCount.isEmpty() && viewCount.matches("\\d+")) {
                    viewCountInt = Integer.parseInt(viewCount);
                }
            } catch (Exception e) {
                log.debug("조회수 추출 실패, 기본값 0 사용");
            }

            String number = "";
            try {
                WebElement numberElement = driver.findElement(By.cssSelector("dl.view-num dd"));
                number = numberElement.getText().trim();
                if (number.isEmpty()) { log.warn("빈 글번호, 건너뛰기"); return; }
            } catch (Exception e) {
                log.error("글번호 추출 실패: {}", e.getMessage());
                return;
            }

            String createDate = "";
            try {
                createDate = driver.findElement(By.cssSelector("dl.write dd")).getText().trim();
            } catch (Exception e) { log.debug("작성일 추출 실패"); }

            String writer = "";
            try {
                writer = driver.findElement(By.cssSelector("dl.writer dd")).getText().trim();
            } catch (Exception e) { log.debug("작성자 추출 실패"); }

            String content = "";
            try {
                WebElement contentElement = driver.findElement(By.cssSelector(".view-con"));
                JavascriptExecutor jsExec = (JavascriptExecutor) driver;
                for (WebElement child : contentElement.findElements(By.xpath("./*"))) {
                    String childHtml = (String) jsExec.executeScript("return arguments[0].innerHTML;", child);
                    String cleaned = Jsoup.clean(childHtml,
                            Safelist.none()
                                    .addTags("a")
                                    .addAttributes("a", "href")
                                    .addProtocols("a", "href", "http", "https"));
                    content = content + cleaned.trim() + "\n";
                }
                content = content.replaceAll("[^\\u0000-\\uFFFF]", "");
            } catch (Exception e) { log.debug("본문 내용 추출 실패"); }

            List<CrawledAnnouncementFile> crawledAnnouncementFiles = new ArrayList<>();
            try {
                List<WebElement> fileElements = driver.findElements(By.cssSelector(".view-file .insert ul li"));
                for (WebElement fileElement : fileElements) {
                    try {
                        WebElement linkElement = fileElement.findElement(By.tagName("a"));
                        String fileName = linkElement.getText().trim();
                        String downloadUrl = linkElement.getAttribute("href");
                        if (!fileName.isEmpty() && downloadUrl != null && !downloadUrl.isEmpty()) {
                            if (!downloadUrl.startsWith("http")) downloadUrl = "https://dorm.inu.ac.kr" + downloadUrl;
                            crawledAnnouncementFiles.add(CrawledAnnouncementFile.builder()
                                    .fileName(fileName).filePath(downloadUrl).build());
                        }
                    } catch (Exception e) { log.debug("개별 파일 추출 실패: {}", e.getMessage()); }
                }
            } catch (Exception e) { log.debug("첨부파일 목록 추출 실패"); }

            log.info("상세 정보 크롤링 완료: {}", title);

            Optional<CrawledAnnouncement> existingOpt = crawledAnnouncementRepository.findByNumber(number);

            if (existingOpt.isPresent()) {
                CrawledAnnouncement existing = existingOpt.get();

                boolean extractionValid = !title.isBlank() && !content.isBlank();
                boolean changed = !Objects.equals(existing.getTitle(), title)
                        || !Objects.equals(existing.getContent(), content);
                // 수정 감지 재크롤은 최근 2주 이내 작성 공지에만 적용
                boolean recent = existing.getCrawledDate() != null
                        && !existing.getCrawledDate().isBefore(LocalDate.now().minusWeeks(CHANGE_DETECT_WEEKS));

                if (!extractionValid || !changed || !recent) {
                    existing.updateViewCount(viewCountInt);   // 변경없음/추출실패/2주 초과 → 조회수만
                    crawledAnnouncementRepository.saveAndFlush(existing);
                    log.info("기존 공지 조회수 업데이트 - 번호: {}, 조회수: {}", number, viewCountInt);
                    return;
                }

                crawledAnnouncementUpdateService.applyCrawlUpdate(
                        existing.getId(), AnnouncementCategory.from(category), title, content,
                        writer, LocalDate.parse(createDate), viewCountInt, crawledAnnouncementFiles);
                log.info("공지 변경 감지 → 갱신 완료 - 번호: {}", number);
                return;
            }

            CrawledAnnouncement crawledAnnouncement = CrawledAnnouncement.builder()
                    .category(AnnouncementCategory.from(category))
                    .number(number).title(title).writer(writer).viewCount(viewCountInt)
                    .announcementType(AnnouncementType.DORMITORY).content(content)
                    .crawledAnnouncementFiles(crawledAnnouncementFiles)
                    .crawledDate(LocalDate.parse(createDate)).link(link)
                    .build();
            crawledAnnouncementRepository.save(crawledAnnouncement);

            for (CrawledAnnouncementFile attachedFile : crawledAnnouncementFiles) {
                attachedFile.updateCrawledAnnouncement(crawledAnnouncement);
                crawledAnnouncementFileRepository.save(attachedFile);
            }

            Notification notification = Notification.builder()
                    .boardId(crawledAnnouncement.getId())
                    .title("새로운 공지사항이 올라왔어요!")
                    .body(crawledAnnouncement.getTitle())
                    .notificationType(NotificationType.DORMITORY)
                    .apiType(ApiType.ANNOUNCEMENT).build();
            notificationRepository.save(notification);

            List<Role> dormitoryUserRoles = Arrays.asList(
                    Role.ROLE_DORM_MANAGER, Role.ROLE_DORM_LIFE_MANAGER, Role.ROLE_DORM_ROOMMATE_MANAGER);
            List<User> allUsers = userRepository.findByReceiveNotificationTypesContainsAndRoleNotIn(
                    NotificationType.DORMITORY, dormitoryUserRoles);
            for (User receiveUser : allUsers) {
                userNotificationRepository.save(UserNotification.of(receiveUser, notification));
                fcmMessageService.sendNotification(receiveUser, notification.getTitle(), notification.getBody());
            }

        } catch (Exception e) {
            log.error("링크 크롤링 실패: {}", e.getMessage(), e);
        }
    }

    private List<Map<String, String>> extractNoticesFromPage(WebDriver driver) {
        List<Map<String, String>> notices = new ArrayList<>();
        // ... (기존 로직 유지) ...

        try {
            List<WebElement> rows = driver.findElements(By.cssSelector("table.board-table tbody tr"));

            for (WebElement row : rows) {
                try {
                    Map<String, String> noticeInfo = new HashMap<>();

                    // 링크 추출
                    String link = "";
                    try {
                        WebElement linkElement = row.findElement(By.cssSelector("td.td-subject a"));
                        String href = linkElement.getAttribute("href");
                        if (href != null && href.startsWith("http")) {
                            link = href;
                        } else {
                            String onclick = linkElement.getAttribute("onclick");
                            if (onclick != null && onclick.contains("jf_viewArtcl")) {
                                java.util.regex.Matcher m = java.util.regex.Pattern
                                        .compile("jf_viewArtcl\\('([^']+)',\\s*'([^']+)'\\)")
                                        .matcher(onclick);
                                if (m.find()) {
                                    link = "https://dorm.inu.ac.kr/bbs/dorm/" + m.group(1) + "/" + m.group(2) + "/artclView";
                                }
                            }
                        }
                    } catch (Exception e) {
                        // 링크 없는 경우 무시
                    }

                    // 날짜 추출
                    String date = "";
                    try {
                        WebElement dateElement = row.findElement(By.cssSelector("td.td-date"));
                        date = dateElement.getText().trim();
                    } catch (Exception e) {
                        log.warn("날짜 파싱 실패: {}", e.getMessage());
                    }

                    noticeInfo.put(link, date);
                    notices.add(noticeInfo);

                } catch (Exception e) {
                    log.warn("행 파싱 중 오류 발생: {}", e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("페이지 파싱 실패: ", e);
        }
        return notices;
    }

    /**
     * 💡 변경됨: 두 크롤링 메서드를 하나로 통합하고, WebDriver와 URL을 인수로 받습니다.
     */
    public List<Map<String, String>> crawlWithSeleniumNotices(WebDriver driver, String baseUrl) {
        List<Map<String, String>> crawlLinks = new ArrayList<>();
        // WebDriver driver = null; // ❌ 삭제됨

        try {
            // ChromeOptions 설정 및 driver = new ChromeDriver(options); ❌ 삭제됨

            driver.get(baseUrl); // 재활용된 드라이버 사용

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("table.board-table")));

            int totalPages = getTotalPages(driver);
            log.info("총 페이지 수: {}", totalPages);

            // 기존 로직 유지 (1~2 페이지 크롤링)
            for (int page = 1; page <= 2; page++) {
                log.info("페이지 {} 크롤링 시작...", page);

                if (page > 1) {
                    JavascriptExecutor js = (JavascriptExecutor) driver;
                    js.executeScript("page_link('" + page + "')");
                    // Thread.sleep()은 권장되지 않지만, 페이지 로딩 문제로 임시 유지
                    Thread.sleep(1000);
                    wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("table.board-table")));
                }

                List<Map<String, String>> extractCrawlLinks = extractNoticesFromPage(driver);
                crawlLinks.addAll(extractCrawlLinks);

                log.info("페이지 {} 완료: {}개의 공지사항 수집", page, extractCrawlLinks.size());
            }

            log.info("전체 크롤링 완료: 총 {}개의 공지사항 수집", crawlLinks.size());

        } catch (Exception e) {
            log.error("Selenium 크롤링 실패 (URL: {}): ", baseUrl, e);
        }
        // finally 블록에서 driver.quit(); ❌ 삭제됨 (crawling()에서 최종적으로 처리)

        return crawlLinks;
    }


    private int getTotalPages(WebDriver driver) {
        // ... (기존 로직 유지) ...

        try {
            WebElement totPageElement = driver.findElement(By.cssSelector("._paging ._totPage"));
            String totalPagesText = totPageElement.getText().trim();

            log.debug("추출된 총 페이지 텍스트: '{}'", totalPagesText);

            if (totalPagesText.isEmpty()) {
                JavascriptExecutor js = (JavascriptExecutor) driver;
                Object result = js.executeScript("return document.querySelector('._paging ._totPage').textContent;");
                totalPagesText = result != null ? result.toString().trim() : "";
                log.debug("JavaScript로 추출된 텍스트: '{}'", totalPagesText);
            }

            if (!totalPagesText.isEmpty()) {
                return Integer.parseInt(totalPagesText);
            }

            List<WebElement> pageLinks = driver.findElements(By.cssSelector("._paging ul li a"));
            int maxPage = 1;
            for (WebElement link : pageLinks) {
                try {
                    String pageText = link.getText().trim();
                    if (!pageText.isEmpty() && pageText.matches("\\d+")) {
                        int pageNum = Integer.parseInt(pageText);
                        if (pageNum > maxPage) {
                            maxPage = pageNum;
                        }
                    }
                } catch (Exception e) {
                    // 숫자가 아닌 경우 무시
                }
            }

            try {
                WebElement lastButton = driver.findElement(By.cssSelector("._paging ._last"));
                String onclick = lastButton.getAttribute("href");
                if (onclick != null && onclick.contains("page_link")) {
                    String pageNum = onclick.replaceAll("[^0-9]", "");
                    if (!pageNum.isEmpty()) {
                        return Integer.parseInt(pageNum);
                    }
                }
            } catch (Exception e) {
                log.debug("끝 버튼에서 페이지 추출 실패");
            }

            log.warn("총 페이지 수를 정확히 파악할 수 없어 최댓값 {}을 사용합니다", maxPage);
            return maxPage > 1 ? maxPage : 1;

        } catch (Exception e) {
            log.warn("총 페이지 수를 가져올 수 없습니다. 기본값 1로 설정: {}", e.getMessage());
            return 1;
        }
    }
}