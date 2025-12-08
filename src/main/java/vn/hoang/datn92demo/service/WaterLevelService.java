package vn.hoang.datn92demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.hoang.datn92demo.dto.request.WaterLevelRequestDTO;
import vn.hoang.datn92demo.model.Area;
import vn.hoang.datn92demo.model.Device;
import vn.hoang.datn92demo.model.DeviceSubscription;
import vn.hoang.datn92demo.model.WaterLevel;
import vn.hoang.datn92demo.repository.DeviceSubscriptionRepository;
import vn.hoang.datn92demo.repository.WaterLevelRepository;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Service
public class WaterLevelService {

    private static final Logger logger = LoggerFactory.getLogger(WaterLevelService.class);

    private final WaterLevelRepository repository;
    private final DeviceSubscriptionRepository deviceSubscriptionRepository;
    private final NotificationService notificationService;
    private final NotificationLogService notificationLogService;

    // cooldown chống spam SMS: WARNING vs ALARM
    private static final long WARNING_COOLDOWN_MS = 2 * 60 * 1000L; // 30 phút
    private static final long ALARM_COOLDOWN_MS   = 2 * 60 * 1000L; // 10 phút

    // key: userId:deviceId -> last WARNING sent
    private final Map<String, Long> lastWarningSentMap = new ConcurrentHashMap<>();

    // key: userId:deviceId -> last ALARM sent
    private final Map<String, Long> lastAlarmSentMap = new ConcurrentHashMap<>();

    public WaterLevelService(WaterLevelRepository repository,
                             DeviceSubscriptionRepository deviceSubscriptionRepository,
                             NotificationService notificationService,
                             NotificationLogService notificationLogService) {
        this.repository = repository;
        this.deviceSubscriptionRepository = deviceSubscriptionRepository;
        this.notificationService = notificationService;
        this.notificationLogService = notificationLogService;
    }

    /**
     * Hợp lệ các dạng:
     *  - NORMAL, WARNING, ALARM
     *  - NORMAL_ENTER, NORMAL_PERIODIC
     *  - WARNING_ENTER, WARNING_UP, WARNING_DOWN, WARNING_PERIODIC
     *  - ALARM_ENTER, ALARM_UP, ALARM_DOWN, ALARM_PERIODIC
     */
    private static final Pattern ALERT_PATTERN =
            Pattern.compile("^(NORMAL|WARNING|ALARM)(?:_(ENTER|EXIT|UP|DOWN|PERIODIC))?$",
                    Pattern.CASE_INSENSITIVE);

    /**
     * Chỉ gửi SMS khi BẮT ĐẦU vào vùng WARNING hoặc ALARM.
     * (WARNING_ENTER, ALARM_ENTER)
     */
    private boolean shouldNotify(String alertType) {
        if (alertType == null || alertType.isBlank()) return false;
        String t = alertType.trim().toUpperCase();
        return "WARNING_ENTER".equals(t) || "ALARM_ENTER".equals(t);
    }

    /**
     * Quyết định có tạo Notification (lưu DB, hiển thị trên UI) hay không.
     * Ở đây: log mọi NORMAL_*, WARNING_*, ALARM_*.
     */
    private boolean shouldCreateNotification(String alertType) {
        if (alertType == null || alertType.isBlank()) return false;
        String t = alertType.trim().toUpperCase();

        if (t.startsWith("NORMAL")) return true;
        if (t.startsWith("WARNING")) return true;
        if (t.startsWith("ALARM")) return true;

        return false;
    }

    /**
     * Chống spam SMS: tách riêng cooldown cho WARNING và ALARM.
     * Mỗi cặp (user, device) có 2 đồng hồ cooldown riêng:
     *  - lastWarningSentMap
     *  - lastAlarmSentMap
     */
    private boolean canSendFor(DeviceSubscription sub, WaterLevel wl) {
        if (sub.getUser() == null || sub.getUser().getId() == null
                || sub.getDevice() == null || sub.getDevice().getId() == null) {
            return true; // không đủ thông tin -> không chặn
        }

        String t = wl.getAlertType() != null ? wl.getAlertType().toUpperCase() : "";
        String key = sub.getUser().getId() + ":" + sub.getDevice().getId();
        long now = System.currentTimeMillis();

        // ALARM_* -> dùng cooldown ALARM riêng
        if (t.startsWith("ALARM")) {
            Long last = lastAlarmSentMap.get(key);
            if (last != null && now - last < ALARM_COOLDOWN_MS) {
                logger.debug("Bỏ qua SMS ALARM cho userId={} deviceId={} vì đang trong cooldown ALARM (còn {} ms)",
                        sub.getUser().getId(), sub.getDevice().getId(), (ALARM_COOLDOWN_MS - (now - last)));
                return false;
            }
            lastAlarmSentMap.put(key, now);
            return true;
        }

        // WARNING_* -> dùng cooldown WARNING riêng
        if (t.startsWith("WARNING")) {
            Long last = lastWarningSentMap.get(key);
            if (last != null && now - last < WARNING_COOLDOWN_MS) {
                logger.debug("Bỏ qua SMS WARNING cho userId={} deviceId={} vì đang trong cooldown WARNING (còn {} ms)",
                        sub.getUser().getId(), sub.getDevice().getId(), (WARNING_COOLDOWN_MS - (now - last)));
                return false;
            }
            lastWarningSentMap.put(key, now);
            return true;
        }

        // NORMAL_* hoặc loại khác -> không áp cooldown SMS
        return true;
    }

    @Transactional
    public WaterLevel save(WaterLevelRequestDTO dto) {

        WaterLevel wl = new WaterLevel();
        wl.setLevel(dto.getLevel());
        wl.setTimestamp(dto.getTimestamp());
        wl.setDeviceId(dto.getDeviceId()); // String (ESP32-01 ...)

        String at = dto.getAlertType();
        if (at == null || at.isBlank()) {
            wl.setAlertType("NORMAL");
        } else {
            String normalized = at.trim().toUpperCase();
            if (ALERT_PATTERN.matcher(normalized).matches()) {
                wl.setAlertType(normalized);
            } else {
                logger.warn("Giá trị alertType không xác định '{}' từ thiết bị. Hệ thống sẽ lưu nguyên dạng.", at);
                wl.setAlertType(normalized);
            }
        }

        WaterLevel saved = repository.save(wl);

        logger.debug("Đã lưu water level: id={} | deviceId={} | level={} | alertType={}",
                saved.getId(), saved.getDeviceId(), saved.getLevel(), saved.getAlertType());

        // --- Gửi SMS + tạo notification cho user theo dõi device ---
        try {
            String deviceIdentifier = saved.getDeviceId();
            List<DeviceSubscription> subs =
                    deviceSubscriptionRepository.findByDeviceDeviceIdWithUserAndDevice(deviceIdentifier);

            if (subs != null && !subs.isEmpty()) {
                for (DeviceSubscription s : subs) {
                    if (s.getUser() == null
                            || s.getUser().getPhone() == null
                            || s.getUser().getPhone().isBlank()) {
                        continue;
                    }

                    String phone = s.getUser().getPhone();
                    String message = buildMessageFor(saved, s);

                    // 1) GỬI SMS: chỉ khi WARNING_ENTER / ALARM_ENTER và qua được cooldown
                    if (shouldNotify(saved.getAlertType()) && canSendFor(s, saved)) {
                        try {
                            notificationService.sendSms(phone, message);
                        } catch (Exception ex) {
                            logger.error("Gửi SMS tới {} thất bại: {}", phone, ex.getMessage());
                        }
                    }

                    // 2) LƯU THÔNG BÁO TRONG DB: mọi NORMAL_*, WARNING_*, ALARM_*
                    if (shouldCreateNotification(saved.getAlertType())) {
                        try {
                            notificationLogService.createWaterAlertNotification(s, saved, message);
                        } catch (Exception ex) {
                            logger.error("Lưu notification DB thất bại cho userId={} deviceId={}: {}",
                                    s.getUser().getId(),
                                    s.getDevice() != null ? s.getDevice().getId() : null,
                                    ex.getMessage());
                        }
                    }
                }
            } else {
                logger.debug("Không tìm thấy subscribers cho deviceId={}", deviceIdentifier);
            }
        } catch (Exception e) {
            logger.error("Lỗi khi xử lý gửi SMS / tạo thông báo device subscriptions: {}", e.getMessage(), e);
        }

        return saved;
    }

    /**
     * Tạo nội dung message dùng chung cho SMS + notification.
     * (Không dấu, 1 dòng, gọn để tránh lỗi encode / bị cắt)
     */
    private String buildMessageFor(WaterLevel wl, DeviceSubscription sub) {
        Device device = sub.getDevice();

        String deviceCode = wl.getDeviceId(); // ESP32-01
        String deviceName = deviceCode;
        String areaName = "khong ro";

        if (device != null) {
            if (device.getName() != null && !device.getName().isBlank()) {
                deviceName = device.getName();
            }
            Area area = device.getArea();
            if (area != null && area.getName() != null && !area.getName().isBlank()) {
                areaName = area.getName();
            }
        }

        String statusLabel = friendlyAlertLabel(wl.getAlertType());
        String timeStr = formatDate(wl.getTimestamp());
        String levelStr = (wl.getLevel() != null)
                ? String.format("%.1f", wl.getLevel())
                : "n/a";

        // 1 dòng, các mục cách nhau bằng dấu phẩy
        String msg = String.format(
                "- WATER ALERT - Thiet bi: %s, Muc nuoc: %s cm, Trang thai: %s, Vi tri: %s, Thoi gian: %s",
                deviceCode, levelStr, statusLabel, deviceName, timeStr
        );

        return msg;
    }

    private String friendlyAlertLabel(String alertType) {
        if (alertType == null) return "KHONG RO";
        String t = alertType.toUpperCase();

        if (t.startsWith("ALARM")) return "BAO DONG";
        if (t.startsWith("WARNING")) return "CANH BAO";
        if (t.startsWith("NORMAL")) return "BINH THUONG";
        return t;
    }

    public List<WaterLevel> getAll() {
        return repository.findAll();
    }

    private String formatDate(Date d) {
        if (d == null) return new Date().toString();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(d);
    }
}
