package vn.hoang.datn92demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.hoang.datn92demo.dto.request.WaterLevelRequestDTO;
import vn.hoang.datn92demo.model.WaterLevel;
import vn.hoang.datn92demo.model.DeviceSubscription;
import vn.hoang.datn92demo.repository.WaterLevelRepository;
import vn.hoang.datn92demo.repository.DeviceSubscriptionRepository;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class WaterLevelService {

    private static final Logger logger = LoggerFactory.getLogger(WaterLevelService.class);

    private final WaterLevelRepository repository;
    private final DeviceSubscriptionRepository deviceSubscriptionRepository;
    private final NotificationService notificationService;

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
            // chỉ notify khi alertType khác NORMAL (tuỳ policy bạn có thể notify cả NORMAL_PERIODIC nếu muốn)
            if (!"NORMAL".equalsIgnoreCase(saved.getAlertType())) {
                String deviceIdentifier = saved.getDeviceId();
                List<DeviceSubscription> subs = deviceSubscriptionRepository.findByDeviceDeviceIdWithUserAndDevice(deviceIdentifier);

                if (subs != null && !subs.isEmpty()) {
                    String message = buildMessageFor(saved);
                    for (DeviceSubscription s : subs) {
                        if (s.getUser() != null && s.getUser().getPhone() != null && !s.getUser().getPhone().isBlank()) {
                            String phone = s.getUser().getPhone();
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

    private String buildMessageFor(WaterLevel wl) {
        StringBuilder sb = new StringBuilder();
        sb.append("Cảnh báo mực nước - thiết bị ").append(wl.getDeviceId())
                .append(" | mực=").append(wl.getLevel())
                .append(" | loại=").append(wl.getAlertType())
                .append(" | lúc=").append(formatDate(wl.getTimestamp()));
        return sb.toString();
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
