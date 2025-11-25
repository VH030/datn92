package vn.hoang.datn92demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.hoang.datn92demo.dto.request.WaterLevelRequestDTO;
import vn.hoang.datn92demo.model.WaterLevel;
import vn.hoang.datn92demo.repository.WaterLevelRepository;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class WaterLevelService {

    private static final Logger logger = LoggerFactory.getLogger(WaterLevelService.class);

    private final WaterLevelRepository repository;

    public WaterLevelService(WaterLevelRepository repository) {
        this.repository = repository;
    }

    /**
     * Cho phép các kiểu alert dạng mở rộng:
     * NORMAL, NORMAL_PERIODIC,
     * WARNING_ENTER, WARNING_EXIT, WARNING_UP, WARNING_DOWN, WARNING_PERIODIC,
     * ALARM_ENTER, ALARM_EXIT, ALARM_UP, ALARM_DOWN, ALARM_PERIODIC.
     *
     * Regex gồm:
     *    - 3 trạng thái chính: NORMAL | WARNING | ALARM
     *    - Các hậu tố mở rộng: _ENTER, _EXIT, _UP, _DOWN, _PERIODIC (tuỳ chọn)
     */
    private static final Pattern ALERT_PATTERN =
            Pattern.compile("^(NORMAL|WARNING|ALARM)(?:_(ENTER|EXIT|UP|DOWN|PERIODIC))?$",
                    Pattern.CASE_INSENSITIVE);

    @Transactional
    public WaterLevel save(WaterLevelRequestDTO dto) {

        WaterLevel wl = new WaterLevel();
        wl.setLevel(dto.getLevel());
        wl.setTimestamp(dto.getTimestamp());
        wl.setDeviceId(dto.getDeviceId());

        // --- Xử lý alertType ---
        String at = dto.getAlertType();

        // Trường hợp thiết bị không gửi loại cảnh báo → mặc định NORMAL
        if (at == null || at.isBlank()) {
            wl.setAlertType("NORMAL");
        } else {
            String normalized = at.trim().toUpperCase();

            // Nếu alertType khớp mẫu hợp lệ thì chấp nhận
            if (ALERT_PATTERN.matcher(normalized).matches()) {
                wl.setAlertType(normalized);
            } else {
                // Nếu giá trị lạ → vẫn lưu để dễ debug nhưng ghi log cảnh báo
                logger.warn("Giá trị alertType không xác định '{}' từ thiết bị. Hệ thống sẽ lưu nguyên dạng.", at);
                wl.setAlertType(normalized);
            }
        }

        // Lưu DB
        WaterLevel saved = repository.save(wl);

        logger.debug("Đã lưu bản ghi: id={} | deviceId={} | level={} | alertType={}",
                saved.getId(), saved.getDeviceId(), saved.getLevel(), saved.getAlertType());

        return saved;
    }

    public List<WaterLevel> getAll() {
        return repository.findAll();
    }

    // Hàm hỗ trợ format ngày giờ (giữ lại để dùng khi cần)
    private String formatDate(Date d) {
        if (d == null) return new Date().toString();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(d);
    }
}
