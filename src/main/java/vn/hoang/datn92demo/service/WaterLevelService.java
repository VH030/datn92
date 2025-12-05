package vn.hoang.datn92demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.hoang.datn92demo.dto.request.WaterLevelRequestDTO;
import vn.hoang.datn92demo.model.WaterLevel;
import vn.hoang.datn92demo.model.DeviceSubscription;
import vn.hoang.datn92demo.model.Device;
import vn.hoang.datn92demo.model.Area;
import vn.hoang.datn92demo.repository.WaterLevelRepository;
import vn.hoang.datn92demo.repository.DeviceSubscriptionRepository;

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

    // cooldown chống spam SMS: WARNING vs ALARM
    private static final long WARNING_COOLDOWN_MS = 30 * 60 * 1000L; // 30 phút
    private static final long ALARM_COOLDOWN_MS = 10 * 60 * 1000L;   // 10 phút

    // key: userId:deviceId -> lastSentMillis
    private final Map<String, Long> lastSentMap = new ConcurrentHashMap<>();

    public WaterLevelService(WaterLevelRepository repository,
                             DeviceSubscriptionRepository deviceSubscriptionRepository,
                             NotificationService notificationService) {
        this.repository = repository;
        this.deviceSubscriptionRepository = deviceSubscriptionRepository;
        this.notificationService = notificationService;
    }

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
     * Chống spam: chỉ cho gửi nếu đã qua cooldown cho (user, device).
     */
    private boolean canSendFor(DeviceSubscription sub, WaterLevel wl) {
        if (sub.getUser() == null || sub.getUser().getId() == null
                || sub.getDevice() == null || sub.getDevice().getId() == null) {
            return true; // không đủ thông tin, cho gửi (hoặc log cảnh báo)
        }

        String alertType = wl.getAlertType() != null ? wl.getAlertType().toUpperCase() : "";
        long cooldown = alertType.startsWith("ALARM") ? ALARM_COOLDOWN_MS : WARNING_COOLDOWN_MS;

        String key = sub.getUser().getId() + ":" + sub.getDevice().getId();
        long now = System.currentTimeMillis();
        Long last = lastSentMap.get(key);

        if (last != null && now - last < cooldown) {
            logger.debug("Bỏ qua SMS cho userId={} deviceId={} vì đang trong cooldown ({} ms còn lại)",
                    sub.getUser().getId(), sub.getDevice().getId(), (cooldown - (now - last)));
            return false;
        }

        lastSentMap.put(key, now);
        return true;
    }

    @Transactional
    public WaterLevel save(WaterLevelRequestDTO dto) {

        WaterLevel wl = new WaterLevel();
        wl.setLevel(dto.getLevel());
        wl.setTimestamp(dto.getTimestamp());
        wl.setDeviceId(dto.getDeviceId()); // String

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

        logger.debug("Đã lưu bản ghi: id={} | deviceId={} | level={} | alertType={}",
                saved.getId(), saved.getDeviceId(), saved.getLevel(), saved.getAlertType());

        // --- Gửi thông báo cho user theo dõi device (nếu cần) ---
        try {
            if (shouldNotify(saved.getAlertType())) {
                String deviceIdentifier = saved.getDeviceId();
                List<DeviceSubscription> subs =
                        deviceSubscriptionRepository.findByDeviceDeviceIdWithUserAndDevice(deviceIdentifier);

                if (subs != null && !subs.isEmpty()) {
                    for (DeviceSubscription s : subs) {
                        if (s.getUser() != null
                                && s.getUser().getPhone() != null
                                && !s.getUser().getPhone().isBlank()) {

                            // kiểm tra cooldown cho từng user-device
                            if (!canSendFor(s, saved)) {
                                continue;
                            }

                            String phone = s.getUser().getPhone();
                            String message = buildMessageFor(saved, s);

                            try {
                                notificationService.sendSms(phone, message);
                            } catch (Exception ex) {
                                logger.error("Gửi thông báo tới {} thất bại: {}", phone, ex.getMessage());
                            }
                        }
                    }
                } else {
                    logger.debug("Không tìm thấy subscribers cho deviceId={}", deviceIdentifier);
                }
            }
        } catch (Exception e) {
            logger.error("Lỗi khi gửi thông báo device subscriptions: {}", e.getMessage(), e);
        }

        return saved;
    }

    /**
     * Tạo nội dung SMS thân thiện cho từng subscriber.
     */
    private String buildMessageFor(WaterLevel wl, DeviceSubscription sub) {
        Device device = sub.getDevice();
        String deviceName = wl.getDeviceId();
        String areaName = "khong ro khu vuc";

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

        // 1 DÒNG NGẮN, KHÔNG DẤU
        String msg = String.format(
                "[CANH BAO] %s - %s: muc %scm, trang thai %s, luc %s",
                deviceName, areaName, levelStr, statusLabel, timeStr
        );

        // Cắt bớt nếu quá dài (phòng trường hợp tên thiết bị/khu vực quá dài)
        if (msg.length() > 150) {
            msg = msg.substring(0, 150);
        }

        return msg;
    }


    private String friendlyAlertLabel(String alertType) {
        if (alertType == null) return "khong ro";
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
