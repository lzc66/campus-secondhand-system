package com.campus.secondhand.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.secondhand.common.exception.BusinessException;
import com.campus.secondhand.config.StorageProperties;
import com.campus.secondhand.dto.admin.UpdateDemoModeRequest;
import com.campus.secondhand.entity.AdminOperationLog;
import com.campus.secondhand.entity.Announcement;
import com.campus.secondhand.entity.Item;
import com.campus.secondhand.entity.ItemCategory;
import com.campus.secondhand.entity.ItemComment;
import com.campus.secondhand.entity.ItemImage;
import com.campus.secondhand.entity.MediaFile;
import com.campus.secondhand.entity.Notification;
import com.campus.secondhand.entity.OrderItem;
import com.campus.secondhand.entity.OrderStatusLog;
import com.campus.secondhand.entity.RegistrationApplication;
import com.campus.secondhand.entity.SearchHistory;
import com.campus.secondhand.entity.SystemSetting;
import com.campus.secondhand.entity.TradeOrder;
import com.campus.secondhand.entity.User;
import com.campus.secondhand.entity.UserRecommendation;
import com.campus.secondhand.entity.WantedPost;
import com.campus.secondhand.enums.AnnouncementPublishStatus;
import com.campus.secondhand.enums.DeliveryType;
import com.campus.secondhand.enums.Gender;
import com.campus.secondhand.enums.ItemCommentStatus;
import com.campus.secondhand.enums.ItemConditionLevel;
import com.campus.secondhand.enums.ItemStatus;
import com.campus.secondhand.enums.ItemTradeMode;
import com.campus.secondhand.enums.NotificationBusinessType;
import com.campus.secondhand.enums.NotificationChannel;
import com.campus.secondhand.enums.NotificationSendStatus;
import com.campus.secondhand.enums.OrderOperatorType;
import com.campus.secondhand.enums.OrderStatus;
import com.campus.secondhand.enums.OrderType;
import com.campus.secondhand.enums.PaymentMethod;
import com.campus.secondhand.enums.PaymentStatus;
import com.campus.secondhand.enums.RecommendationReasonCode;
import com.campus.secondhand.enums.RegistrationStatus;
import com.campus.secondhand.enums.SearchSortType;
import com.campus.secondhand.enums.UserAccountStatus;
import com.campus.secondhand.enums.WantedPostStatus;
import com.campus.secondhand.mapper.AdminOperationLogMapper;
import com.campus.secondhand.mapper.AnnouncementMapper;
import com.campus.secondhand.mapper.ItemCategoryMapper;
import com.campus.secondhand.mapper.ItemCommentMapper;
import com.campus.secondhand.mapper.ItemImageMapper;
import com.campus.secondhand.mapper.ItemMapper;
import com.campus.secondhand.mapper.MediaFileMapper;
import com.campus.secondhand.mapper.NotificationMapper;
import com.campus.secondhand.mapper.OrderItemMapper;
import com.campus.secondhand.mapper.OrderStatusLogMapper;
import com.campus.secondhand.mapper.RegistrationApplicationMapper;
import com.campus.secondhand.mapper.SearchHistoryMapper;
import com.campus.secondhand.mapper.SystemSettingMapper;
import com.campus.secondhand.mapper.TradeOrderMapper;
import com.campus.secondhand.mapper.UserMapper;
import com.campus.secondhand.mapper.UserRecommendationMapper;
import com.campus.secondhand.mapper.WantedPostMapper;
import com.campus.secondhand.security.AdminPrincipal;
import com.campus.secondhand.service.AdminDemoModeService;
import com.campus.secondhand.vo.admin.DemoDataSeedResponse;
import com.campus.secondhand.vo.admin.DemoDataSummaryResponse;
import com.campus.secondhand.vo.admin.DemoModeStatusResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class AdminDemoModeServiceImpl implements AdminDemoModeService {

    private static final String SETTING_DEMO_MODE_ENABLED = "demo_mode_enabled";
    private static final String SETTING_DEMO_ITEM_NOTES_ENABLED = "demo_item_notes_enabled";
    private static final String SETTING_DEMO_DATA_SEEDED_AT = "demo_data_seeded_at";
    private static final String DEFAULT_DEMO_PASSWORD = "123456";

    private static final List<String> DEMO_USER_STUDENT_NOS = List.of("20250001", "20250002", "20250003", "20250004");
    private static final List<String> DEMO_ITEM_TITLES = List.of(
            "[\u6F14\u793A] \u7F57\u6280 K380 \u84DD\u7259\u952E\u76D8",
            "[\u6F14\u793A] \u9AD8\u7B49\u6570\u5B66\u671F\u672B\u6574\u7406\u7B14\u8BB0",
            "[\u6F14\u793A] \u5BBF\u820D\u9759\u97F3\u5C0F\u98CE\u6247",
            "[\u6F14\u793A] \u97F3\u4E50\u8282\u95E8\u7968\u5361\u5957",
            "[\u6F14\u793A] \u7FBD\u6BDB\u7403\u62CD\u53CC\u62CD\u5957\u88C5",
            "[\u6F14\u793A] \u5BBF\u820D\u62A4\u773C\u53F0\u706F",
            "[\u6F14\u793A] \u55B7\u58A8\u6253\u5370\u673A"
    );
    private static final List<String> DEMO_ORDER_NOS = List.of("DEMO20260326001", "DEMO20260326002", "DEMO20260326003");
    private static final List<String> DEMO_WANTED_TITLES = List.of(
            "[\u6F14\u793A] \u6C42\u8D2D\u663E\u793A\u5668\u652F\u67B6",
            "[\u6F14\u793A] \u6C42\u8D2D\u8003\u7814\u82F1\u8BED\u5355\u8BCD\u4E66",
            "[\u6F14\u793A] \u6C42\u8D2D\u5BBF\u820D\u6298\u53E0\u6905"
    );
    private static final List<String> DEMO_ANNOUNCEMENT_TITLES = List.of(
            "[\u6F14\u793A] \u5E73\u53F0\u8BD5\u8FD0\u884C\u8BF4\u660E",
            "[\u6F14\u793A] \u5BBF\u820D\u914D\u9001\u65F6\u95F4\u8C03\u6574",
            "[\u6F14\u793A] \u6BD5\u4E1A\u5B63\u95F2\u7F6E\u4EA4\u6613\u5468\u9884\u544A"
    );
    private static final List<String> DEMO_APPLICATION_NOS = List.of("RA-DEMO-20260326-001");

    private static final List<String> LEGACY_DEMO_ITEM_TITLES = List.of(
            "[Demo] Logitech K380 Keyboard",
            "[Demo] Calculus Final Notes",
            "[Demo] Quiet Desk Fan",
            "[Demo] Festival Card Holder",
            "[Demo] Badminton Racket Set",
            "[Demo] Dorm Reading Lamp",
            "[Demo] Canon Inkjet Printer"
    );
    private static final List<String> LEGACY_DEMO_WANTED_TITLES = List.of(
            "[Demo] Wanted Monitor Arm",
            "[Demo] Wanted English Vocabulary Book",
            "[Demo] Wanted Folding Chair"
    );
    private static final List<String> LEGACY_DEMO_ANNOUNCEMENT_TITLES = List.of(
            "[Demo] Trial Run Notice",
            "[Demo] Dorm Delivery Hours",
            "[Demo] Graduation Trading Week"
    );

    private final StorageProperties storageProperties;
    private final SystemSettingMapper systemSettingMapper;
    private final UserMapper userMapper;
    private final ItemCategoryMapper itemCategoryMapper;
    private final MediaFileMapper mediaFileMapper;
    private final ItemMapper itemMapper;
    private final ItemImageMapper itemImageMapper;
    private final ItemCommentMapper itemCommentMapper;
    private final WantedPostMapper wantedPostMapper;
    private final AnnouncementMapper announcementMapper;
    private final RegistrationApplicationMapper registrationApplicationMapper;
    private final TradeOrderMapper tradeOrderMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderStatusLogMapper orderStatusLogMapper;
    private final SearchHistoryMapper searchHistoryMapper;
    private final NotificationMapper notificationMapper;
    private final UserRecommendationMapper userRecommendationMapper;
    private final AdminOperationLogMapper adminOperationLogMapper;
    private final PasswordEncoder passwordEncoder;

    public AdminDemoModeServiceImpl(StorageProperties storageProperties,
                                    SystemSettingMapper systemSettingMapper,
                                    UserMapper userMapper,
                                    ItemCategoryMapper itemCategoryMapper,
                                    MediaFileMapper mediaFileMapper,
                                    ItemMapper itemMapper,
                                    ItemImageMapper itemImageMapper,
                                    ItemCommentMapper itemCommentMapper,
                                    WantedPostMapper wantedPostMapper,
                                    AnnouncementMapper announcementMapper,
                                    RegistrationApplicationMapper registrationApplicationMapper,
                                    TradeOrderMapper tradeOrderMapper,
                                    OrderItemMapper orderItemMapper,
                                    OrderStatusLogMapper orderStatusLogMapper,
                                    SearchHistoryMapper searchHistoryMapper,
                                    NotificationMapper notificationMapper,
                                    UserRecommendationMapper userRecommendationMapper,
                                    AdminOperationLogMapper adminOperationLogMapper,
                                    PasswordEncoder passwordEncoder) {
        this.storageProperties = storageProperties;
        this.systemSettingMapper = systemSettingMapper;
        this.userMapper = userMapper;
        this.itemCategoryMapper = itemCategoryMapper;
        this.mediaFileMapper = mediaFileMapper;
        this.itemMapper = itemMapper;
        this.itemImageMapper = itemImageMapper;
        this.itemCommentMapper = itemCommentMapper;
        this.wantedPostMapper = wantedPostMapper;
        this.announcementMapper = announcementMapper;
        this.registrationApplicationMapper = registrationApplicationMapper;
        this.tradeOrderMapper = tradeOrderMapper;
        this.orderItemMapper = orderItemMapper;
        this.orderStatusLogMapper = orderStatusLogMapper;
        this.searchHistoryMapper = searchHistoryMapper;
        this.notificationMapper = notificationMapper;
        this.userRecommendationMapper = userRecommendationMapper;
        this.adminOperationLogMapper = adminOperationLogMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public DemoModeStatusResponse getStatus() {
        return buildStatusResponse();
    }

    @Override
    @Transactional
    public DemoDataSeedResponse seedDemoData(AdminPrincipal principal) {
        LocalDateTime existingSeededAt = readDateTimeSetting(SETTING_DEMO_DATA_SEEDED_AT);
        if (existingSeededAt != null) {
            localizeDemoData(principal);
            return new DemoDataSeedResponse(new Counter().toSummary(), buildDemoSummary(), true, existingSeededAt);
        }

        Counter created = new Counter();
        LocalDateTime now = LocalDateTime.now();
        Map<String, Long> categoryIdMap = loadCategoryIds();

        MediaFile pendingCard = ensureSvgMedia("demo/student-card-pending.svg", "student-card-pending.svg", "image", principal.getAdminId(), "Campus Bazaar", "Pending Review", "#1d4d63", "#d97a38");

        User userAmy = ensureDemoUser(created, "20250001", "amy.chen@example.com", "Amy Chen", Gender.FEMALE, "13800000001", "281900001", "amy-campus", "School of Information", "Software Engineering", "SE-231", "Dorm 1-304", now.minusDays(18));
        User userLin = ensureDemoUser(created, "20250002", "leo.lin@example.com", "Leo Lin", Gender.MALE, "13800000002", "281900002", "leo-note", "Business School", "Marketing", "MK-232", "Dorm 7-210", now.minusDays(15));
        User userZhou = ensureDemoUser(created, "20250003", "zoe.zhou@example.com", "Zoe Zhou", Gender.FEMALE, "13800000003", "281900003", "zoe-study", "School of Arts", "Literature", "LT-221", "Dorm 9-516", now.minusDays(11));
        User userXu = ensureDemoUser(created, "20250004", "ethan.xu@example.com", "Ethan Xu", Gender.MALE, "13800000004", "281900004", "ethan-auto", "School of Engineering", "Automation", "AU-224", "Dorm 10-105", now.minusDays(8));

        ensureRegistrationApplication(created, "RA-DEMO-20260326-001", "20259999", "Pending Applicant", Gender.UNKNOWN, "pending.demo@example.com", "13800000999", "School of Design", "Environmental Design", "ED-241", pendingCard.getFileId(), now.minusHours(20));

        Item keyboard = ensureItem(created, userAmy, requireCategory(categoryIdMap, "digital_devices"), "[Demo] Logitech K380 Keyboard", "Logitech", "K380", "Bluetooth keyboard for tablet and laptop. Great for dorm study setups.", ItemConditionLevel.ALMOST_NEW, new BigDecimal("99.00"), new BigDecimal("199.00"), 1, ItemTradeMode.BOTH, true, userAmy.getPhone(), userAmy.getQqNo(), userAmy.getWechatNo(), "Dorm 1 pickup or campus delivery", ItemStatus.ON_SALE, now.minusDays(6), null, 126, 2, now.minusDays(6));
        Item notes = ensureItem(created, userLin, requireCategory(categoryIdMap, "books_notes"), "[Demo] Calculus Final Notes", "Self-made", "Tongji Edition", "Clean final review notes with highlighted key formulas and question types.", ItemConditionLevel.LIGHTLY_USED, new BigDecimal("18.00"), new BigDecimal("45.00"), 1, ItemTradeMode.OFFLINE, false, userLin.getPhone(), userLin.getQqNo(), userLin.getWechatNo(), "Library south gate", ItemStatus.ON_SALE, now.minusDays(5), null, 84, 0, now.minusDays(5));
        Item fan = ensureItem(created, userZhou, requireCategory(categoryIdMap, "daily_use"), "[Demo] Quiet Desk Fan", "Midea", "FS09", "Three speed levels and low noise for summer dorm use.", ItemConditionLevel.LIGHTLY_USED, new BigDecimal("35.00"), new BigDecimal("89.00"), 1, ItemTradeMode.BOTH, true, userZhou.getPhone(), userZhou.getQqNo(), userZhou.getWechatNo(), "Dorm 9 pickup", ItemStatus.ON_SALE, now.minusDays(4), null, 68, 0, now.minusDays(4));        Item ticket = ensureItem(created, userAmy, requireCategory(categoryIdMap, "tickets_cards"), "[Demo] Festival Card Holder", "Campus Live", "Collector Edition", "A small collector item for the campus music festival week.", ItemConditionLevel.NEW, new BigDecimal("12.00"), new BigDecimal("29.00"), 1, ItemTradeMode.OFFLINE, false, userAmy.getPhone(), userAmy.getQqNo(), userAmy.getWechatNo(), "Student center", ItemStatus.ON_SALE, now.minusDays(3), null, 42, 0, now.minusDays(3));
        Item racket = ensureItem(created, userAmy, requireCategory(categoryIdMap, "sports_goods"), "[Demo] Badminton Racket Set", "YONEX", "Starter Set", "Two rackets with cover bag for casual campus games.", ItemConditionLevel.LIGHTLY_USED, new BigDecimal("168.00"), new BigDecimal("299.00"), 1, ItemTradeMode.BOTH, false, userAmy.getPhone(), userAmy.getQqNo(), userAmy.getWechatNo(), "East playground gate", ItemStatus.SOLD, now.minusDays(7), now.minusDays(3), 153, 2, now.minusDays(7));
        Item lamp = ensureItem(created, userLin, requireCategory(categoryIdMap, "dorm_supplies"), "[Demo] Dorm Reading Lamp", "Philips", "DormLight Mini", "Adjustable color temperature for night reading and desk work.", ItemConditionLevel.ALMOST_NEW, new BigDecimal("58.00"), new BigDecimal("139.00"), 1, ItemTradeMode.ONLINE, false, userLin.getPhone(), userLin.getQqNo(), userLin.getWechatNo(), "Dorm 7-210", ItemStatus.RESERVED, now.minusDays(2), null, 95, 0, now.minusDays(2));
        Item printer = ensureItem(created, userLin, requireCategory(categoryIdMap, "digital_devices"), "[Demo] Canon Inkjet Printer", "Canon", "TS3380", "Working printer for course handouts, best for campus self pickup.", ItemConditionLevel.USED, new BigDecimal("140.00"), new BigDecimal("399.00"), 1, ItemTradeMode.BOTH, true, userLin.getPhone(), userLin.getQqNo(), userLin.getWechatNo(), "Near printing center", ItemStatus.RESERVED, now.minusHours(10), null, 57, 0, now.minusHours(10));

        attachDemoItemImage(keyboard, principal.getAdminId(), "demo/items/keyboard.svg", "keyboard.svg", "Bluetooth Keyboard", "Digital Device");
        attachDemoItemImage(notes, principal.getAdminId(), "demo/items/math-notes.svg", "math-notes.svg", "Math Notes", "Books and Notes");
        attachDemoItemImage(fan, principal.getAdminId(), "demo/items/fan.svg", "fan.svg", "Desk Fan", "Daily Use");
        attachDemoItemImage(ticket, principal.getAdminId(), "demo/items/ticket.svg", "ticket.svg", "Card Holder", "Tickets and Cards");
        attachDemoItemImage(racket, principal.getAdminId(), "demo/items/racket.svg", "racket.svg", "Racket Set", "Sports Goods");
        attachDemoItemImage(lamp, principal.getAdminId(), "demo/items/lamp.svg", "desk-lamp.svg", "Reading Lamp", "Dorm Supplies");
        attachDemoItemImage(printer, principal.getAdminId(), "demo/items/printer.svg", "printer.svg", "Inkjet Printer", "Digital Device");

        ensureComments(keyboard, userZhou, userAmy, now.minusDays(1));
        ensureComments(racket, userXu, userAmy, now.minusDays(3));

        ensureWantedPost(created, userZhou.getUserId(), requireCategory(categoryIdMap, "digital_devices"), "[Demo] Wanted Monitor Arm", "North Bayou", "NB-F80", "Budget around 80, stable for dorm desk use.", new BigDecimal("50.00"), new BigDecimal("90.00"), userZhou.getPhone(), WantedPostStatus.OPEN, now.plusDays(10), now.minusDays(1));
        ensureWantedPost(created, userXu.getUserId(), requireCategory(categoryIdMap, "books_notes"), "[Demo] Wanted English Vocabulary Book", "Red Treasure", "Latest", "Looking for a fairly new exam vocabulary book with light notes.", new BigDecimal("20.00"), new BigDecimal("45.00"), userXu.getPhone(), WantedPostStatus.OPEN, now.plusDays(7), now.minusDays(2));
        ensureWantedPost(created, userAmy.getUserId(), requireCategory(categoryIdMap, "dorm_supplies"), "[Demo] Wanted Folding Chair", "Generic", "Portable", "Need a spare folding chair for the dorm room.", new BigDecimal("30.00"), new BigDecimal("60.00"), userAmy.getPhone(), WantedPostStatus.OPEN, now.plusDays(5), now.minusHours(20));

        ensureAnnouncement(created, principal.getAdminId(), "[Demo] Trial Run Notice", "The system now contains seeded data for registration review, listing, comments, wanted posts, orders, and dashboard metrics.", 1, AnnouncementPublishStatus.PUBLISHED, now.minusDays(2), null, now.minusDays(2));
        ensureAnnouncement(created, principal.getAdminId(), "[Demo] Dorm Delivery Hours", "Weekday dorm delivery is adjusted to 18:30 - 21:30. Please contact the seller in advance for pickup.", 0, AnnouncementPublishStatus.PUBLISHED, now.minusHours(12), now.plusDays(7), now.minusHours(12));
        ensureAnnouncement(created, principal.getAdminId(), "[Demo] Graduation Trading Week", "A themed campaign page for books, desks, and digital devices will be prepared next week.", 0, AnnouncementPublishStatus.DRAFT, null, null, now.minusHours(6));

        ensureOrder(created, "DEMO20260326001", userZhou, userAmy, racket, OrderStatus.COMPLETED, PaymentStatus.PAID, DeliveryType.FACE_TO_FACE,
                "Zoe Zhou", userZhou.getPhone(), "East playground gate", "Meet after badminton practice", null,
                now.minusDays(4), now.minusDays(4).plusHours(2), now.minusDays(3).plusHours(1), now.minusDays(3).plusHours(4), null, null,
                List.of(
                        new OrderLogSeed(OrderOperatorType.BUYER, userZhou.getUserId(), null, OrderStatus.PENDING_CONFIRM.getValue(), "Buyer created demo order", now.minusDays(4)),
                        new OrderLogSeed(OrderOperatorType.SELLER, userAmy.getUserId(), OrderStatus.PENDING_CONFIRM.getValue(), OrderStatus.AWAITING_DELIVERY.getValue(), "Seller confirmed order", now.minusDays(4).plusHours(2)),
                        new OrderLogSeed(OrderOperatorType.SELLER, userAmy.getUserId(), OrderStatus.AWAITING_DELIVERY.getValue(), OrderStatus.DELIVERING.getValue(), "Face to face meeting arranged", now.minusDays(3).plusHours(1)),
                        new OrderLogSeed(OrderOperatorType.BUYER, userZhou.getUserId(), OrderStatus.DELIVERING.getValue(), OrderStatus.COMPLETED.getValue(), "Buyer marked order complete", now.minusDays(3).plusHours(4))
                ));

        ensureOrder(created, "DEMO20260326002", userXu, userLin, lamp, OrderStatus.DELIVERING, PaymentStatus.UNPAID, DeliveryType.DORM_DELIVERY,
                "Ethan Xu", userXu.getPhone(), "Dorm 10-105", "Please deliver before 8 PM", "Seller on the way",
                now.minusDays(2), now.minusDays(2).plusHours(1), now.minusHours(18), null, null, null,
                List.of(
                        new OrderLogSeed(OrderOperatorType.BUYER, userXu.getUserId(), null, OrderStatus.PENDING_CONFIRM.getValue(), "Buyer created demo order", now.minusDays(2)),
                        new OrderLogSeed(OrderOperatorType.SELLER, userLin.getUserId(), OrderStatus.PENDING_CONFIRM.getValue(), OrderStatus.AWAITING_DELIVERY.getValue(), "Seller confirmed order", now.minusDays(2).plusHours(1)),
                        new OrderLogSeed(OrderOperatorType.SELLER, userLin.getUserId(), OrderStatus.AWAITING_DELIVERY.getValue(), OrderStatus.DELIVERING.getValue(), "Seller marked order delivering", now.minusHours(18))
                ));

        ensureOrder(created, "DEMO20260326003", userAmy, userLin, printer, OrderStatus.PENDING_CONFIRM, PaymentStatus.UNPAID, DeliveryType.SELF_PICKUP,
                "Amy Chen", userAmy.getPhone(), "Library main gate", "I can pick it up tonight if the printer is working", null,
                now.minusHours(8), null, null, null, null, null,
                List.of(new OrderLogSeed(OrderOperatorType.BUYER, userAmy.getUserId(), null, OrderStatus.PENDING_CONFIRM.getValue(), "Buyer created demo order", now.minusHours(8))));

        ensureSearchHistory(userAmy.getUserId(), "keyboard", requireCategory(categoryIdMap, "digital_devices"), SearchSortType.LATEST, now.minusDays(5));
        ensureSearchHistory(userAmy.getUserId(), "desk fan", requireCategory(categoryIdMap, "daily_use"), SearchSortType.PRICE_ASC, now.minusDays(2));
        ensureSearchHistory(userZhou.getUserId(), "lamp", requireCategory(categoryIdMap, "dorm_supplies"), SearchSortType.LATEST, now.minusDays(1));
        ensureSearchHistory(userXu.getUserId(), "printer", requireCategory(categoryIdMap, "digital_devices"), SearchSortType.PRICE_DESC, now.minusHours(10));

        ensureNotification(userZhou.getUserId(), userZhou.getEmail(), principal.getAdminId(), NotificationChannel.SITE, NotificationBusinessType.ORDER_UPDATE, null,
                "[Demo] Order Completed", "The badminton racket order has been completed. Browse more recommended items in your personal center.", NotificationSendStatus.SENT, now.minusDays(3).plusHours(4), now.minusDays(2));
        ensureNotification(userXu.getUserId(), userXu.getEmail(), principal.getAdminId(), NotificationChannel.SITE, NotificationBusinessType.ORDER_UPDATE, null,
                "[Demo] Seller Is Delivering", "The dorm reading lamp order is now in delivering status. Please keep your phone available.", NotificationSendStatus.SENT, now.minusHours(18), null);
        ensureNotification(userAmy.getUserId(), userAmy.getEmail(), principal.getAdminId(), NotificationChannel.SITE, NotificationBusinessType.SYSTEM, null,
                "[Demo] Recommendation Feed Ready", "The system has prepared seeded recommendation data for your demo account.", NotificationSendStatus.SENT, now.minusHours(6), null);

        ensureRecommendation(userAmy.getUserId(), notes.getItemId(), new BigDecimal("0.92"), RecommendationReasonCode.KEYWORD_MATCH, now.minusHours(6));
        ensureRecommendation(userAmy.getUserId(), fan.getItemId(), new BigDecimal("0.88"), RecommendationReasonCode.HOT_SALE, now.minusHours(6));
        ensureRecommendation(userZhou.getUserId(), keyboard.getItemId(), new BigDecimal("0.95"), RecommendationReasonCode.MANUAL, now.minusHours(5));

        ensureAdminOperationLog(principal.getAdminId(), "registration", 1L, "approve", "{\"note\":\"Demo seed inserted a pending registration sample\"}", now.minusHours(20));
        ensureAdminOperationLog(principal.getAdminId(), "announcement", 1L, "publish", "{\"note\":\"Demo seed created published announcements\"}", now.minusHours(12));
        ensureAdminOperationLog(principal.getAdminId(), "order", 1L, "other", "{\"note\":\"Demo seed prepared order lifecycle samples\"}", now.minusHours(6));
        ensureAdminOperationLog(principal.getAdminId(), "item", 1L, "edit", "{\"note\":\"Demo seed added item images and descriptions\"}", now.minusHours(2));

        localizeDemoData(principal);

        upsertSetting(SETTING_DEMO_MODE_ENABLED, "true", "boolean", principal.getAdminId());
        upsertSetting(SETTING_DEMO_ITEM_NOTES_ENABLED, "true", "boolean", principal.getAdminId());
        upsertSetting(SETTING_DEMO_DATA_SEEDED_AT, now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), "datetime", principal.getAdminId());

        return new DemoDataSeedResponse(created.toSummary(), buildDemoSummary(), true, now);
    }

    @Override
    @Transactional
    public DemoModeStatusResponse updateSettings(AdminPrincipal principal, UpdateDemoModeRequest request) {
        upsertSetting(SETTING_DEMO_MODE_ENABLED, String.valueOf(Boolean.TRUE.equals(request.demoModeEnabled())), "boolean", principal.getAdminId());
        upsertSetting(SETTING_DEMO_ITEM_NOTES_ENABLED, String.valueOf(Boolean.TRUE.equals(request.demoItemNotesEnabled())), "boolean", principal.getAdminId());
        return buildStatusResponse();
    }

    private DemoModeStatusResponse buildStatusResponse() {
        LocalDateTime seededAt = readDateTimeSetting(SETTING_DEMO_DATA_SEEDED_AT);
        return new DemoModeStatusResponse(
                readBooleanSetting(SETTING_DEMO_MODE_ENABLED, false),
                readBooleanSetting(SETTING_DEMO_ITEM_NOTES_ENABLED, true),
                seededAt != null,
                seededAt,
                buildDemoSummary()
        );
    }

    private DemoDataSummaryResponse buildDemoSummary() {
        return new DemoDataSummaryResponse(countUsers(), countItems(), countOrders(), countWantedPosts(), countAnnouncements(), countPendingRegistrations());
    }

    private long countUsers() {
        return zeroSafe(userMapper.selectCount(new LambdaQueryWrapper<User>().in(User::getStudentNo, DEMO_USER_STUDENT_NOS).isNull(User::getDeletedAt)));
    }

    private long countItems() {
        return zeroSafe(itemMapper.selectCount(new LambdaQueryWrapper<Item>().in(Item::getTitle, DEMO_ITEM_TITLES).isNull(Item::getDeletedAt)));
    }

    private long countOrders() {
        return zeroSafe(tradeOrderMapper.selectCount(new LambdaQueryWrapper<TradeOrder>().in(TradeOrder::getOrderNo, DEMO_ORDER_NOS)));
    }

    private long countWantedPosts() {
        return zeroSafe(wantedPostMapper.selectCount(new LambdaQueryWrapper<WantedPost>().in(WantedPost::getTitle, DEMO_WANTED_TITLES).isNull(WantedPost::getDeletedAt)));
    }

    private long countAnnouncements() {
        return zeroSafe(announcementMapper.selectCount(new LambdaQueryWrapper<Announcement>().in(Announcement::getTitle, DEMO_ANNOUNCEMENT_TITLES)));
    }

    private long countPendingRegistrations() {
        return zeroSafe(registrationApplicationMapper.selectCount(new LambdaQueryWrapper<RegistrationApplication>().in(RegistrationApplication::getApplicationNo, DEMO_APPLICATION_NOS)));
    }    private void localizeDemoData(AdminPrincipal principal) {
        rewriteDemoSvg("demo/student-card-pending.svg", "\u6821\u56ED\u4E8C\u624B\u5E73\u53F0", "\u5F85\u5BA1\u6838\u5B66\u751F\u8BC1", "#1d4d63", "#d97a38");
        rewriteDemoSvg("demo/items/keyboard.svg", "\u7F57\u6280 K380 \u84DD\u7259\u952E\u76D8", "\u6570\u7801\u8BBE\u5907", "#1f4d46", "#d07a39");
        rewriteDemoSvg("demo/items/math-notes.svg", "\u9AD8\u6570\u671F\u672B\u6574\u7406\u7B14\u8BB0", "\u6559\u6750\u4E66\u7C4D", "#1f4d46", "#d07a39");
        rewriteDemoSvg("demo/items/fan.svg", "\u5BBF\u820D\u9759\u97F3\u5C0F\u98CE\u6247", "\u65E5\u5E38\u751F\u6D3B", "#1f4d46", "#d07a39");
        rewriteDemoSvg("demo/items/ticket.svg", "\u97F3\u4E50\u8282\u95E8\u7968\u5361\u5957", "\u7968\u5238\u5361\u5238", "#1f4d46", "#d07a39");
        rewriteDemoSvg("demo/items/racket.svg", "\u7FBD\u6BDB\u7403\u62CD\u53CC\u62CD\u5957\u88C5", "\u8FD0\u52A8\u7528\u54C1", "#1f4d46", "#d07a39");
        rewriteDemoSvg("demo/items/lamp.svg", "\u5BBF\u820D\u62A4\u773C\u53F0\u706F", "\u5BBF\u820D\u7528\u54C1", "#1f4d46", "#d07a39");
        rewriteDemoSvg("demo/items/printer.svg", "\u55B7\u58A8\u6253\u5370\u673A", "\u6570\u7801\u8BBE\u5907", "#1f4d46", "#d07a39");

        localizeUser("20250001", "\u9648\u6653\u5F64", "\u4FE1\u606F\u5DE5\u7A0B\u5B66\u9662", "\u8F6F\u4EF6\u5DE5\u7A0B", "\u8F6F\u5DE5 231 \u73ED", "1\u820D 304");
        localizeUser("20250002", "\u6797\u4E66\u8A00", "\u5546\u5B66\u9662", "\u5E02\u573A\u8425\u9500", "\u8425\u9500 232 \u73ED", "7\u820D 210");
        localizeUser("20250003", "\u5468\u53EF\u5FC3", "\u6587\u5B66\u4E0E\u4F20\u5A92\u5B66\u9662", "\u6C49\u8BED\u8A00\u6587\u5B66", "\u6C49\u6587 221 \u73ED", "9\u820D 516");
        localizeUser("20250004", "\u8BB8\u77E5\u884C", "\u673A\u7535\u5DE5\u7A0B\u5B66\u9662", "\u81EA\u52A8\u5316", "\u81EA\u52A8\u5316 224 \u73ED", "10\u820D 105");

        RegistrationApplication application = registrationApplicationMapper.selectOne(new LambdaQueryWrapper<RegistrationApplication>()
                .eq(RegistrationApplication::getApplicationNo, "RA-DEMO-20260326-001")
                .last("LIMIT 1"));
        if (application != null) {
            application.setRealName("\u5B8B\u4E88\u5B89");
            application.setCollegeName("\u827A\u672F\u8BBE\u8BA1\u5B66\u9662");
            application.setMajorName("\u73AF\u5883\u8BBE\u8BA1");
            application.setClassName("\u73AF\u8BBE 241 \u73ED");
            registrationApplicationMapper.updateById(application);
        }

        localizeItem("[Demo] Logitech K380 Keyboard", "[\u6F14\u793A] \u7F57\u6280 K380 \u84DD\u7259\u952E\u76D8", "\u652F\u6301\u5E73\u677F\u548C\u7B14\u8BB0\u672C\u591A\u8BBE\u5907\u5207\u6362\uFF0C\u5F88\u9002\u5408\u5BBF\u820D\u5B66\u4E60\u4F7F\u7528\u3002", "1\u820D\u697C\u4E0B\u81EA\u63D0\u6216\u5BBF\u820D\u914D\u9001");
        localizeItem("[Demo] Calculus Final Notes", "[\u6F14\u793A] \u9AD8\u7B49\u6570\u5B66\u671F\u672B\u6574\u7406\u7B14\u8BB0", "\u6309\u9898\u578B\u548C\u516C\u5F0F\u6574\u7406\u7684\u590D\u4E60\u7B14\u8BB0\uFF0C\u9002\u5408\u671F\u672B\u51B2\u523A\u9636\u6BB5\u3002", "\u56FE\u4E66\u9986\u5357\u95E8");
        localizeItem("[Demo] Quiet Desk Fan", "[\u6F14\u793A] \u5BBF\u820D\u9759\u97F3\u5C0F\u98CE\u6247", "\u4E09\u6863\u98CE\u529B\u53EF\u8C03\uFF0C\u4F4E\u566A\u97F3\uFF0C\u9002\u5408\u590F\u5929\u5BBF\u820D\u684C\u9762\u4F7F\u7528\u3002", "9\u820D\u697C\u4E0B");
        localizeItem("[Demo] Festival Card Holder", "[\u6F14\u793A] \u97F3\u4E50\u8282\u95E8\u7968\u5361\u5957", "\u9002\u5408\u642D\u914D\u6D3B\u52A8\u95E8\u7968\u6536\u85CF\u7684\u6821\u56ED\u7EAA\u5FF5\u5468\u8FB9\u3002", "\u6D3B\u52A8\u4E2D\u5FC3");
        localizeItem("[Demo] Badminton Racket Set", "[\u6F14\u793A] \u7FBD\u6BDB\u7403\u62CD\u53CC\u62CD\u5957\u88C5", "\u53CC\u62CD\u5E26\u7403\u5305\uFF0C\u5F88\u9002\u5408\u548C\u5BBF\u820D\u540C\u5B66\u4E00\u8D77\u6253\u7403\u3002", "\u4E1C\u64CD\u573A\u95E8\u53E3");
        localizeItem("[Demo] Dorm Reading Lamp", "[\u6F14\u793A] \u5BBF\u820D\u62A4\u773C\u53F0\u706F", "\u8272\u6E29\u53EF\u8C03\uFF0C\u9002\u5408\u665A\u4E0A\u81EA\u4E60\u548C\u5BBF\u820D\u9605\u8BFB\u3002", "7\u820D 210");
        localizeItem("[Demo] Canon Inkjet Printer", "[\u6F14\u793A] \u55B7\u58A8\u6253\u5370\u673A", "\u529F\u80FD\u6B63\u5E38\uFF0C\u9002\u5408\u6253\u5370\u8BFE\u7A0B\u8D44\u6599\uFF0C\u5EFA\u8BAE\u6821\u5185\u81EA\u63D0\u3002", "\u56FE\u6587\u4E2D\u5FC3\u9644\u8FD1");

        localizeWantedPost("[Demo] Wanted Monitor Arm", "[\u6F14\u793A] \u6C42\u8D2D\u663E\u793A\u5668\u652F\u67B6", "\u9884\u7B97 80 \u5DE6\u53F3\uFF0C\u5E0C\u671B\u53EF\u5347\u964D\u3001\u9002\u5408\u5BBF\u820D\u4E66\u684C\u3002");
        localizeWantedPost("[Demo] Wanted English Vocabulary Book", "[\u6F14\u793A] \u6C42\u8D2D\u8003\u7814\u82F1\u8BED\u5355\u8BCD\u4E66", "\u5E0C\u671B\u6709\u8F83\u65B0\u7684\u8003\u7814\u82F1\u8BED\u5355\u8BCD\u4E66\uFF0C\u5E26\u7B80\u5355\u505A\u9898\u75D5\u8FF9\u4E5F\u53EF\u4EE5\u3002");
        localizeWantedPost("[Demo] Wanted Folding Chair", "[\u6F14\u793A] \u6C42\u8D2D\u5BBF\u820D\u6298\u53E0\u6905", "\u5BDD\u5BA4\u4E34\u65F6\u52A0\u5EA7\u4F4D\u7528\uFF0C\u989C\u8272\u65E0\u8981\u6C42\uFF0C\u80FD\u627F\u91CD\u5C31\u884C\u3002");

        localizeAnnouncement("[Demo] Trial Run Notice", "[\u6F14\u793A] \u5E73\u53F0\u8BD5\u8FD0\u884C\u8BF4\u660E", "\u5F53\u524D\u7CFB\u7EDF\u5DF2\u8FDB\u5165\u6F14\u793A\u9636\u6BB5\uFF0C\u53EF\u5B8C\u6574\u4F53\u9A8C\u6CE8\u518C\u5BA1\u6838\u3001\u5546\u54C1\u53D1\u5E03\u3001\u8BC4\u8BBA\u3001\u6C42\u8D2D\u3001\u8BA2\u5355\u548C\u540E\u53F0\u7BA1\u7406\u6D41\u7A0B\u3002");
        localizeAnnouncement("[Demo] Dorm Delivery Hours", "[\u6F14\u793A] \u5BBF\u820D\u914D\u9001\u65F6\u95F4\u8C03\u6574", "\u5DE5\u4F5C\u65E5\u914D\u9001\u65F6\u95F4\u8C03\u6574\u4E3A 18:30 - 21:30\uFF0C\u82E5\u9009\u62E9\u7EBF\u4E0B\u81EA\u63D0\u8BF7\u63D0\u524D\u548C\u5356\u5BB6\u6C9F\u901A\u3002");
        localizeAnnouncement("[Demo] Graduation Trading Week", "[\u6F14\u793A] \u6BD5\u4E1A\u5B63\u95F2\u7F6E\u4EA4\u6613\u5468\u9884\u544A", "\u4E0B\u5468\u5C06\u4E0A\u7EBF\u6BD5\u4E1A\u5B63\u4E13\u9898\u6D3B\u52A8\u9875\uFF0C\u9F13\u52B1\u540C\u5B66\u96C6\u4E2D\u53D1\u5E03\u6559\u6750\u3001\u684C\u6905\u548C\u7535\u5B50\u8BBE\u5907\u3002");

        localizeOrder("DEMO20260326001", "\u5468\u53EF\u5FC3", "\u4E1C\u64CD\u573A\u95E8\u53E3", "\u4ECA\u665A\u6253\u5B8C\u7403\u540E\u5F53\u9762\u6210\u4EA4", null);
        localizeOrder("DEMO20260326002", "\u8BB8\u77E5\u884C", "10\u820D 105", "\u9001\u5230\u5BBF\u820D\u697C\u4E0B\u5373\u53EF", "\u4ECA\u665A 8 \u70B9\u524D\u53EF\u4EE5\u9001\u8FBE");
        localizeOrder("DEMO20260326003", "\u9648\u6653\u5F64", "\u56FE\u4E66\u9986\u95E8\u53E3", "\u5982\u679C\u58A8\u76D2\u72B6\u6001\u6B63\u5E38\u6211\u4ECA\u665A\u81EA\u63D0", null);

        localizeSearchKeyword("keyboard", "\u84DD\u7259\u952E\u76D8");
        localizeSearchKeyword("desk fan", "\u5C0F\u98CE\u6247");
        localizeSearchKeyword("lamp", "\u53F0\u706F");
        localizeSearchKeyword("printer", "\u6253\u5370\u673A");

        localizeNotification("[Demo] Order Completed", "[\u6F14\u793A] \u8BA2\u5355\u5DF2\u5B8C\u6210", "\u7FBD\u6BDB\u7403\u62CD\u8BA2\u5355\u5DF2\u5B8C\u6210\uFF0C\u53EF\u5728\u4E2A\u4EBA\u4E2D\u5FC3\u7EE7\u7EED\u67E5\u770B\u66F4\u591A\u63A8\u8350\u5546\u54C1\u3002");
        localizeNotification("[Demo] Seller Is Delivering", "[\u6F14\u793A] \u5356\u5BB6\u6B63\u5728\u914D\u9001", "\u5BBF\u820D\u62A4\u773C\u53F0\u706F\u8BA2\u5355\u5DF2\u8FDB\u5165\u914D\u9001\u4E2D\u72B6\u6001\uFF0C\u8BF7\u7559\u610F\u7535\u8BDD\u3002");
        localizeNotification("[Demo] Recommendation Feed Ready", "[\u6F14\u793A] \u63A8\u8350\u5165\u53E3\u5DF2\u51C6\u5907", "\u7CFB\u7EDF\u5DF2\u4E3A\u4F60\u51C6\u5907\u6F14\u793A\u63A8\u8350\u6570\u636E\uFF0C\u53EF\u524D\u5F80\u63A8\u8350\u9875\u67E5\u770B\u3002");
    }

    private void localizeUser(String studentNo, String realName, String collegeName, String majorName, String className, String dormitoryAddress) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getStudentNo, studentNo).isNull(User::getDeletedAt).last("LIMIT 1"));
        if (user == null) {
            return;
        }
        user.setRealName(realName);
        user.setCollegeName(collegeName);
        user.setMajorName(majorName);
        user.setClassName(className);
        user.setDormitoryAddress(dormitoryAddress);
        userMapper.updateById(user);
    }

    private void localizeItem(String legacyTitle, String currentTitle, String description, String pickupAddress) {
        Item item = itemMapper.selectOne(new LambdaQueryWrapper<Item>().in(Item::getTitle, List.of(legacyTitle, currentTitle)).isNull(Item::getDeletedAt).last("LIMIT 1"));
        if (item == null) {
            return;
        }
        item.setTitle(currentTitle);
        item.setDescription(description);
        item.setPickupAddress(pickupAddress);
        itemMapper.updateById(item);

        OrderItem orderItem = orderItemMapper.selectOne(new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getItemId, item.getItemId()).last("LIMIT 1"));
        if (orderItem != null) {
            orderItem.setItemTitleSnapshot(currentTitle);
            orderItemMapper.updateById(orderItem);
        }

        if (currentTitle.contains("K380")) {
            localizeCommentPair(item.getItemId(), "\u53EF\u4EE5\u8FDE iPad \u548C\u624B\u673A\u5417\uFF1F", "\u53EF\u4EE5\uFF0C\u84DD\u7259\u5207\u6362\u6B63\u5E38\uFF0C\u6211\u4E00\u76F4\u7528\u6765\u8FDE\u5E73\u677F\u548C\u7535\u8111\u3002");
        }
        if (currentTitle.contains("\u7FBD\u6BDB\u7403\u62CD")) {
            localizeCommentPair(item.getItemId(), "\u62CD\u6846\u6709\u6CA1\u6709\u660E\u663E\u78D5\u78B0\uFF0C\u624B\u80F6\u662F\u5426\u8FD8\u6BD4\u8F83\u65B0\uFF1F", "\u62CD\u6846\u53EA\u6709\u8F7B\u5FAE\u4F7F\u7528\u75D5\u8FF9\uFF0C\u624B\u80F6\u4E0A\u5468\u521A\u6362\u8FC7\u3002");
        }
    }

    private void localizeCommentPair(Long itemId, String rootContent, String replyContent) {
        ItemComment root = itemCommentMapper.selectOne(new LambdaQueryWrapper<ItemComment>().eq(ItemComment::getItemId, itemId).isNull(ItemComment::getParentCommentId).isNull(ItemComment::getDeletedAt).last("LIMIT 1"));
        if (root != null) {
            root.setContent(rootContent);
            itemCommentMapper.updateById(root);
            ItemComment reply = itemCommentMapper.selectOne(new LambdaQueryWrapper<ItemComment>().eq(ItemComment::getParentCommentId, root.getCommentId()).isNull(ItemComment::getDeletedAt).last("LIMIT 1"));
            if (reply != null) {
                reply.setContent(replyContent);
                itemCommentMapper.updateById(reply);
            }
        }
    }

    private void localizeWantedPost(String legacyTitle, String currentTitle, String description) {
        WantedPost wantedPost = wantedPostMapper.selectOne(new LambdaQueryWrapper<WantedPost>().in(WantedPost::getTitle, List.of(legacyTitle, currentTitle)).isNull(WantedPost::getDeletedAt).last("LIMIT 1"));
        if (wantedPost == null) {
            return;
        }
        wantedPost.setTitle(currentTitle);
        wantedPost.setDescription(description);
        wantedPostMapper.updateById(wantedPost);
    }

    private void localizeAnnouncement(String legacyTitle, String currentTitle, String content) {
        Announcement announcement = announcementMapper.selectOne(new LambdaQueryWrapper<Announcement>().in(Announcement::getTitle, List.of(legacyTitle, currentTitle)).last("LIMIT 1"));
        if (announcement == null) {
            return;
        }
        announcement.setTitle(currentTitle);
        announcement.setContent(content);
        announcementMapper.updateById(announcement);
    }

    private void localizeOrder(String orderNo, String receiverName, String deliveryAddress, String buyerRemark, String sellerRemark) {
        TradeOrder order = tradeOrderMapper.selectOne(new LambdaQueryWrapper<TradeOrder>().eq(TradeOrder::getOrderNo, orderNo).last("LIMIT 1"));
        if (order == null) {
            return;
        }
        order.setReceiverName(receiverName);
        order.setDeliveryAddress(deliveryAddress);
        order.setBuyerRemark(buyerRemark);
        order.setSellerRemark(sellerRemark);
        tradeOrderMapper.updateById(order);
    }

    private void localizeSearchKeyword(String oldKeyword, String newKeyword) {
        List<SearchHistory> histories = searchHistoryMapper.selectList(new LambdaQueryWrapper<SearchHistory>().eq(SearchHistory::getKeyword, oldKeyword));
        for (SearchHistory history : histories) {
            history.setKeyword(newKeyword);
            searchHistoryMapper.updateById(history);
        }
    }

    private void localizeNotification(String legacyTitle, String currentTitle, String content) {
        Notification notification = notificationMapper.selectOne(new LambdaQueryWrapper<Notification>().in(Notification::getTitle, List.of(legacyTitle, currentTitle)).last("LIMIT 1"));
        if (notification == null) {
            return;
        }
        notification.setTitle(currentTitle);
        notification.setContent(content);
        notificationMapper.updateById(notification);
    }

    private void rewriteDemoSvg(String fileKey, String title, String subtitle, String colorFrom, String colorTo) {
        Path path = Path.of(storageProperties.getRootDir()).resolve(fileKey.replace('/', java.io.File.separatorChar));
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, buildDemoSvg(title, subtitle, colorFrom, colorTo), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new BusinessException(50096, HttpStatus.INTERNAL_SERVER_ERROR, "Failed to rewrite demo placeholder image");
        }
    }
    private User ensureDemoUser(Counter created, String studentNo, String email, String realName, Gender gender, String phone,
                                String qqNo, String wechatNo, String collegeName, String majorName, String className,
                                String dormitoryAddress, LocalDateTime createdAt) {
        User existing = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getStudentNo, studentNo)
                .isNull(User::getDeletedAt)
                .last("LIMIT 1"));
        if (existing != null) {
            return existing;
        }
        User user = User.builder()
                .studentNo(studentNo)
                .email(email)
                .passwordHash(passwordEncoder.encode(DEFAULT_DEMO_PASSWORD))
                .realName(realName)
                .gender(gender)
                .phone(phone)
                .qqNo(qqNo)
                .wechatNo(wechatNo)
                .collegeName(collegeName)
                .majorName(majorName)
                .className(className)
                .dormitoryAddress(dormitoryAddress)
                .accountStatus(UserAccountStatus.ACTIVE)
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .build();
        userMapper.insert(user);
        created.users++;
        return user;
    }

    private RegistrationApplication ensureRegistrationApplication(Counter created, String applicationNo, String studentNo,
                                                                  String realName, Gender gender, String email, String phone,
                                                                  String collegeName, String majorName, String className,
                                                                  Long studentCardFileId, LocalDateTime submittedAt) {
        RegistrationApplication existing = registrationApplicationMapper.selectOne(new LambdaQueryWrapper<RegistrationApplication>()
                .eq(RegistrationApplication::getApplicationNo, applicationNo)
                .last("LIMIT 1"));
        if (existing != null) {
            return existing;
        }
        RegistrationApplication application = RegistrationApplication.builder()
                .applicationNo(applicationNo)
                .studentNo(studentNo)
                .realName(realName)
                .gender(gender)
                .email(email)
                .phone(phone)
                .passwordHash(passwordEncoder.encode(DEFAULT_DEMO_PASSWORD))
                .collegeName(collegeName)
                .majorName(majorName)
                .className(className)
                .studentCardFileId(studentCardFileId)
                .status(RegistrationStatus.PENDING)
                .submittedAt(submittedAt)
                .updatedAt(submittedAt)
                .build();
        registrationApplicationMapper.insert(application);
        created.pendingRegistrations++;
        return application;
    }

    private Item ensureItem(Counter created, User seller, Long categoryId, String title, String brand, String model,
                            String description, ItemConditionLevel conditionLevel, BigDecimal price, BigDecimal originalPrice,
                            Integer stock, ItemTradeMode tradeMode, boolean negotiable, String contactPhone, String contactQq,
                            String contactWechat, String pickupAddress, ItemStatus status, LocalDateTime publishedAt,
                            LocalDateTime soldAt, int viewCount, int commentCount, LocalDateTime createdAt) {
        Item existing = itemMapper.selectOne(new LambdaQueryWrapper<Item>()
                .eq(Item::getTitle, title)
                .isNull(Item::getDeletedAt)
                .last("LIMIT 1"));
        if (existing != null) {
            return existing;
        }
        Item item = Item.builder()
                .sellerUserId(seller.getUserId())
                .categoryId(categoryId)
                .title(title)
                .brand(brand)
                .model(model)
                .description(description)
                .conditionLevel(conditionLevel)
                .price(price)
                .originalPrice(originalPrice)
                .stock(stock)
                .tradeMode(tradeMode)
                .negotiable(negotiable ? 1 : 0)
                .contactPhone(contactPhone)
                .contactQq(contactQq)
                .contactWechat(contactWechat)
                .pickupAddress(pickupAddress)
                .status(status)
                .publishedAt(publishedAt)
                .soldAt(soldAt)
                .viewCount(viewCount)
                .commentCount(commentCount)
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .build();
        itemMapper.insert(item);
        created.items++;
        return item;
    }

    private void attachDemoItemImage(Item item, Long adminId, String fileKey, String originalName, String title, String subtitle) {
        MediaFile mediaFile = ensureSvgMedia(fileKey, originalName, "image", adminId, title, subtitle, "#1f4d46", "#d07a39");
        ItemImage existing = itemImageMapper.selectOne(new LambdaQueryWrapper<ItemImage>()
                .eq(ItemImage::getItemId, item.getItemId())
                .eq(ItemImage::getFileId, mediaFile.getFileId())
                .last("LIMIT 1"));
        if (existing != null) {
            return;
        }
        boolean hasCover = zeroSafe(itemImageMapper.selectCount(new LambdaQueryWrapper<ItemImage>()
                .eq(ItemImage::getItemId, item.getItemId())
                .eq(ItemImage::getIsCover, 1))) > 0;
        itemImageMapper.insert(ItemImage.builder()
                .itemId(item.getItemId())
                .fileId(mediaFile.getFileId())
                .sortOrder(1)
                .isCover(hasCover ? 0 : 1)
                .createdAt(LocalDateTime.now())
                .build());
    }

    private void ensureComments(Item item, User commenter, User seller, LocalDateTime createdAt) {
        String rootContent = item.getTitle().contains("Keyboard") ? "Can this connect to both tablet and phone?" : "Any visible scratches on the racket frame?";
        String replyContent = item.getTitle().contains("Keyboard") ? "Yes, the Bluetooth switch works well with both tablet and laptop." : "Only light marks from normal use. The grip was replaced recently.";
        ItemComment root = itemCommentMapper.selectOne(new LambdaQueryWrapper<ItemComment>()
                .eq(ItemComment::getItemId, item.getItemId())
                .eq(ItemComment::getCommenterUserId, commenter.getUserId())
                .isNull(ItemComment::getParentCommentId)
                .eq(ItemComment::getContent, rootContent)
                .isNull(ItemComment::getDeletedAt)
                .last("LIMIT 1"));
        if (root == null) {
            root = ItemComment.builder()
                    .itemId(item.getItemId())
                    .commenterUserId(commenter.getUserId())
                    .content(rootContent)
                    .status(ItemCommentStatus.VISIBLE)
                    .createdAt(createdAt)
                    .updatedAt(createdAt)
                    .build();
            itemCommentMapper.insert(root);
        }
        ItemComment reply = itemCommentMapper.selectOne(new LambdaQueryWrapper<ItemComment>()
                .eq(ItemComment::getItemId, item.getItemId())
                .eq(ItemComment::getParentCommentId, root.getCommentId())
                .eq(ItemComment::getCommenterUserId, seller.getUserId())
                .eq(ItemComment::getContent, replyContent)
                .isNull(ItemComment::getDeletedAt)
                .last("LIMIT 1"));
        if (reply == null) {
            itemCommentMapper.insert(ItemComment.builder()
                    .itemId(item.getItemId())
                    .commenterUserId(seller.getUserId())
                    .parentCommentId(root.getCommentId())
                    .replyToUserId(commenter.getUserId())
                    .content(replyContent)
                    .status(ItemCommentStatus.VISIBLE)
                    .createdAt(createdAt.plusHours(2))
                    .updatedAt(createdAt.plusHours(2))
                    .build());
        }
    }

    private WantedPost ensureWantedPost(Counter created, Long requesterUserId, Long categoryId, String title, String brand,
                                        String model, String description, BigDecimal minPrice, BigDecimal maxPrice,
                                        String contactPhone, WantedPostStatus status, LocalDateTime expiresAt,
                                        LocalDateTime createdAt) {
        WantedPost existing = wantedPostMapper.selectOne(new LambdaQueryWrapper<WantedPost>()
                .eq(WantedPost::getTitle, title)
                .isNull(WantedPost::getDeletedAt)
                .last("LIMIT 1"));
        if (existing != null) {
            return existing;
        }
        WantedPost wantedPost = WantedPost.builder()
                .requesterUserId(requesterUserId)
                .categoryId(categoryId)
                .title(title)
                .brand(brand)
                .model(model)
                .description(description)
                .expectedPriceMin(minPrice)
                .expectedPriceMax(maxPrice)
                .contactPhone(contactPhone)
                .status(status)
                .expiresAt(expiresAt)
                .viewCount(15)
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .build();
        wantedPostMapper.insert(wantedPost);
        created.wantedPosts++;
        return wantedPost;
    }

    private Announcement ensureAnnouncement(Counter created, Long publisherAdminId, String title, String content, int isPinned,
                                            AnnouncementPublishStatus publishStatus, LocalDateTime publishedAt,
                                            LocalDateTime expireAt, LocalDateTime createdAt) {
        Announcement existing = announcementMapper.selectOne(new LambdaQueryWrapper<Announcement>()
                .eq(Announcement::getTitle, title)
                .last("LIMIT 1"));
        if (existing != null) {
            return existing;
        }
        Announcement announcement = Announcement.builder()
                .publisherAdminId(publisherAdminId)
                .title(title)
                .content(content)
                .isPinned(isPinned)
                .publishStatus(publishStatus)
                .publishedAt(publishedAt)
                .expireAt(expireAt)
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .build();
        announcementMapper.insert(announcement);
        created.announcements++;
        return announcement;
    }    private TradeOrder ensureOrder(Counter created, String orderNo, User buyer, User seller, Item item, OrderStatus orderStatus,
                                   PaymentStatus paymentStatus, DeliveryType deliveryType, String receiverName,
                                   String receiverPhone, String deliveryAddress, String buyerRemark, String sellerRemark,
                                   LocalDateTime createdAt, LocalDateTime confirmedAt, LocalDateTime deliveredAt,
                                   LocalDateTime completedAt, LocalDateTime cancelledAt, String cancelReason,
                                   List<OrderLogSeed> logSeeds) {
        TradeOrder existing = tradeOrderMapper.selectOne(new LambdaQueryWrapper<TradeOrder>()
                .eq(TradeOrder::getOrderNo, orderNo)
                .last("LIMIT 1"));
        if (existing != null) {
            return existing;
        }
        TradeOrder order = TradeOrder.builder()
                .orderNo(orderNo)
                .buyerUserId(buyer.getUserId())
                .sellerUserId(seller.getUserId())
                .orderType(OrderType.ONLINE_COD)
                .paymentMethod(PaymentMethod.COD)
                .paymentStatus(paymentStatus)
                .orderStatus(orderStatus)
                .deliveryType(deliveryType)
                .receiverName(receiverName)
                .receiverPhone(receiverPhone)
                .deliveryAddress(deliveryAddress)
                .totalAmount(item.getPrice())
                .buyerRemark(buyerRemark)
                .sellerRemark(sellerRemark)
                .cancelReason(cancelReason)
                .confirmedAt(confirmedAt)
                .deliveredAt(deliveredAt)
                .completedAt(completedAt)
                .cancelledAt(cancelledAt)
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .build();
        tradeOrderMapper.insert(order);

        orderItemMapper.insert(OrderItem.builder()
                .orderId(order.getOrderId())
                .itemId(item.getItemId())
                .itemTitleSnapshot(item.getTitle())
                .itemPriceSnapshot(item.getPrice())
                .quantity(1)
                .subtotalAmount(item.getPrice())
                .createdAt(createdAt)
                .build());

        for (OrderLogSeed logSeed : logSeeds) {
            orderStatusLogMapper.insert(OrderStatusLog.builder()
                    .orderId(order.getOrderId())
                    .operatorType(logSeed.operatorType())
                    .operatorId(logSeed.operatorId())
                    .fromStatus(logSeed.fromStatus())
                    .toStatus(logSeed.toStatus())
                    .actionNote(logSeed.actionNote())
                    .createdAt(logSeed.createdAt())
                    .build());
        }
        created.orders++;
        return order;
    }

    private void ensureSearchHistory(Long userId, String keyword, Long categoryId, SearchSortType sortType, LocalDateTime searchedAt) {
        SearchHistory existing = searchHistoryMapper.selectOne(new LambdaQueryWrapper<SearchHistory>()
                .eq(SearchHistory::getUserId, userId)
                .eq(SearchHistory::getKeyword, keyword)
                .eq(SearchHistory::getSearchedAt, searchedAt)
                .last("LIMIT 1"));
        if (existing != null) {
            return;
        }
        searchHistoryMapper.insert(SearchHistory.builder()
                .userId(userId)
                .keyword(keyword)
                .categoryId(categoryId)
                .sortType(sortType)
                .searchedAt(searchedAt)
                .build());
    }

    private void ensureNotification(Long receiverUserId, String receiverEmail, Long senderAdminId, NotificationChannel channel,
                                    NotificationBusinessType businessType, Long businessId, String title, String content,
                                    NotificationSendStatus sendStatus, LocalDateTime sentAt, LocalDateTime readAt) {
        Notification existing = notificationMapper.selectOne(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getReceiverUserId, receiverUserId)
                .eq(Notification::getTitle, title)
                .last("LIMIT 1"));
        if (existing != null) {
            return;
        }
        notificationMapper.insert(Notification.builder()
                .receiverUserId(receiverUserId)
                .receiverEmail(receiverEmail)
                .senderAdminId(senderAdminId)
                .channel(channel)
                .businessType(businessType)
                .businessId(businessId)
                .title(title)
                .content(content)
                .sendStatus(sendStatus)
                .sentAt(sentAt)
                .readAt(readAt)
                .createdAt(sentAt != null ? sentAt : LocalDateTime.now())
                .build());
    }

    private void ensureRecommendation(Long userId, Long itemId, BigDecimal score, RecommendationReasonCode reasonCode, LocalDateTime generatedAt) {
        UserRecommendation existing = userRecommendationMapper.selectOne(new LambdaQueryWrapper<UserRecommendation>()
                .eq(UserRecommendation::getUserId, userId)
                .eq(UserRecommendation::getItemId, itemId)
                .last("LIMIT 1"));
        if (existing != null) {
            return;
        }
        userRecommendationMapper.insert(UserRecommendation.builder()
                .userId(userId)
                .itemId(itemId)
                .recommendScore(score)
                .reasonCode(reasonCode)
                .isClicked(0)
                .generatedAt(generatedAt)
                .expiresAt(generatedAt.plusDays(7))
                .build());
    }

    private void ensureAdminOperationLog(Long adminId, String targetType, Long targetId, String operationType, String detail, LocalDateTime createdAt) {
        AdminOperationLog existing = adminOperationLogMapper.selectOne(new LambdaQueryWrapper<AdminOperationLog>()
                .eq(AdminOperationLog::getAdminId, adminId)
                .eq(AdminOperationLog::getTargetType, targetType)
                .eq(AdminOperationLog::getTargetId, targetId)
                .eq(AdminOperationLog::getOperationType, operationType)
                .last("LIMIT 1"));
        if (existing != null) {
            return;
        }
        adminOperationLogMapper.insert(AdminOperationLog.builder()
                .adminId(adminId)
                .targetType(targetType)
                .targetId(targetId)
                .operationType(operationType)
                .operationDetail(detail)
                .createdAt(createdAt)
                .build());
    }

    private MediaFile ensureSvgMedia(String fileKey, String originalName, String fileCategory, Long uploaderRefId,
                                     String title, String subtitle, String colorFrom, String colorTo) {
        MediaFile existing = mediaFileMapper.selectOne(new LambdaQueryWrapper<MediaFile>()
                .eq(MediaFile::getFileKey, fileKey)
                .last("LIMIT 1"));
        if (existing != null) {
            return existing;
        }
        Path path = Path.of(storageProperties.getRootDir()).resolve(fileKey.replace('/', java.io.File.separatorChar));
        try {
            Files.createDirectories(path.getParent());
            if (!Files.exists(path)) {
                Files.writeString(path, buildDemoSvg(title, subtitle, colorFrom, colorTo), StandardCharsets.UTF_8);
            }
        } catch (IOException ex) {
            throw new BusinessException(50095, HttpStatus.INTERNAL_SERVER_ERROR, "Failed to generate demo placeholder image");
        }

        MediaFile mediaFile = MediaFile.builder()
                .storageProvider("local")
                .bucketName("local")
                .fileKey(fileKey)
                .originalName(originalName)
                .fileUrl(buildFileUrl(fileKey))
                .mimeType("image/svg+xml")
                .fileSize(path.toFile().length())
                .fileExt("svg")
                .fileCategory(fileCategory)
                .uploaderRole("admin")
                .uploaderRefId(uploaderRefId)
                .createdAt(LocalDateTime.now())
                .build();
        mediaFileMapper.insert(mediaFile);
        return mediaFile;
    }

    private Map<String, Long> loadCategoryIds() {
        return itemCategoryMapper.selectList(new LambdaQueryWrapper<ItemCategory>().eq(ItemCategory::getIsEnabled, 1))
                .stream()
                .filter(category -> category.getCategoryCode() != null)
                .collect(Collectors.toMap(ItemCategory::getCategoryCode, ItemCategory::getCategoryId, (left, right) -> left));
    }

    private Long requireCategory(Map<String, Long> categoryIdMap, String categoryCode) {
        Long categoryId = categoryIdMap.get(categoryCode);
        if (categoryId == null) {
            throw new BusinessException(40995, HttpStatus.CONFLICT, "Required category is missing: " + categoryCode);
        }
        return categoryId;
    }

    private boolean readBooleanSetting(String key, boolean defaultValue) {
        SystemSetting setting = systemSettingMapper.selectOne(new LambdaQueryWrapper<SystemSetting>().eq(SystemSetting::getSettingKey, key).last("LIMIT 1"));
        if (setting == null || setting.getSettingValue() == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(setting.getSettingValue());
    }

    private LocalDateTime readDateTimeSetting(String key) {
        SystemSetting setting = systemSettingMapper.selectOne(new LambdaQueryWrapper<SystemSetting>().eq(SystemSetting::getSettingKey, key).last("LIMIT 1"));
        if (setting == null || !StringUtils.hasText(setting.getSettingValue())) {
            return null;
        }
        return LocalDateTime.parse(setting.getSettingValue(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    private void upsertSetting(String key, String value, String valueType, Long adminId) {
        SystemSetting existing = systemSettingMapper.selectOne(new LambdaQueryWrapper<SystemSetting>().eq(SystemSetting::getSettingKey, key).last("LIMIT 1"));
        if (existing == null) {
            systemSettingMapper.insert(SystemSetting.builder()
                    .settingKey(key)
                    .settingValue(value)
                    .valueType(valueType)
                    .updatedByAdminId(adminId)
                    .build());
            return;
        }
        existing.setSettingValue(value);
        existing.setValueType(valueType);
        existing.setUpdatedByAdminId(adminId);
        systemSettingMapper.updateById(existing);
    }

    private String buildFileUrl(String fileKey) {
        String baseUrl = storageProperties.getPublicBaseUrl();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl + "/" + fileKey;
    }

    private long zeroSafe(Long value) {
        return value == null ? 0L : value;
    }

    private String buildDemoSvg(String title, String subtitle, String accentFrom, String accentTo) {
        return String.format(Locale.ROOT, """
                <svg xmlns="http://www.w3.org/2000/svg" width="1200" height="900" viewBox="0 0 1200 900">
                  <defs>
                    <linearGradient id="bg" x1="0" y1="0" x2="1" y2="1">
                      <stop offset="0%%" stop-color="%s"/>
                      <stop offset="100%%" stop-color="%s"/>
                    </linearGradient>
                  </defs>
                  <rect width="1200" height="900" rx="48" fill="url(#bg)"/>
                  <rect x="64" y="64" width="1072" height="772" rx="36" fill="rgba(255,255,255,0.12)" stroke="rgba(255,255,255,0.36)"/>
                  <text x="96" y="170" fill="#f8f5ef" font-size="54" font-family="Georgia, serif">%s</text>
                  <text x="96" y="236" fill="#eef2f2" font-size="28" font-family="Segoe UI, Arial, sans-serif">%s</text>
                  <circle cx="952" cy="220" r="118" fill="rgba(255,255,255,0.18)"/>
                  <circle cx="1006" cy="286" r="56" fill="rgba(255,255,255,0.24)"/>
                  <text x="96" y="770" fill="#fff2e6" font-size="26" font-family="Segoe UI, Arial, sans-serif">Campus Secondhand Demo Asset</text>
                </svg>
                """, accentFrom, accentTo, escapeXml(title), escapeXml(subtitle));
    }

    private String escapeXml(String value) {
        return Objects.toString(value, "")
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private record OrderLogSeed(OrderOperatorType operatorType, Long operatorId, String fromStatus, String toStatus,
                                String actionNote, LocalDateTime createdAt) {
    }

    private static final class Counter {
        private long users;
        private long items;
        private long orders;
        private long wantedPosts;
        private long announcements;
        private long pendingRegistrations;

        private DemoDataSummaryResponse toSummary() {
            return new DemoDataSummaryResponse(users, items, orders, wantedPosts, announcements, pendingRegistrations);
        }
    }
}