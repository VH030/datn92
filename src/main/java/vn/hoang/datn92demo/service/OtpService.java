package vn.hoang.datn92demo.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
public class OtpService {

    private final NotificationService notificationService;

    // Lưu OTP tạm thời trong bộ nhớ (RAM) — dùng số đã chuẩn hóa làm key
    private final Map<String, OtpData> otpStorage = new HashMap<>();

    // Thời gian hết hạn OTP (5 phút)
    private static final long EXPIRE_MILLIS = 5 * 60 * 1000L;

    public OtpService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    // Gửi OTP đến số điện thoại (chấp nhận 09..., +849..., 849..., v.v.)
    public void sendOtp(String rawPhoneNumber) {
        String phoneKey = normalizePhone(rawPhoneNumber);
        String otp = String.format("%06d", new Random().nextInt(1_000_000));

        // Lưu OTP và thời gian hết hạn dùng phoneKey đã chuẩn hóa
        otpStorage.put(phoneKey, new OtpData(otp, Instant.now().plusMillis(EXPIRE_MILLIS)));

        String message =  otp ;

        try {
            // Gửi SMS qua NotificationService (đang được implement bởi ModemSmsService)
            notificationService.sendSms(rawPhoneNumber, message);
            System.out.println("Đã gửi OTP tới " + phoneKey + ": " + otp);
        } catch (Exception e) {
            System.err.println("Không gửi được OTP qua SMS. OTP test: " + otp);
            e.printStackTrace();
        }
    }

    // Xác thực OTP (input phone có thể ở dạng 09..., +84...)
    public boolean verifyOtp(String rawPhoneNumber, String inputOtp) {
        String phoneKey = normalizePhone(rawPhoneNumber);
        OtpData otpData = otpStorage.get(phoneKey);

        if (otpData == null) {
            System.out.println("Không tìm thấy OTP cho " + phoneKey);
            return false;
        }

        if (Instant.now().isAfter(otpData.expireAt())) {
            otpStorage.remove(phoneKey);
            System.out.println("OTP đã hết hạn cho " + phoneKey);
            return false;
        }

        boolean valid = otpData.otp().equals(inputOtp);
        if (valid) {
            otpStorage.remove(phoneKey); // Xóa sau khi dùng
            System.out.println("OTP hợp lệ cho " + phoneKey);
        } else {
            System.out.println("OTP sai cho " + phoneKey);
        }
        return valid;
    }

    // --- Chuẩn hóa số điện thoại dùng làm key trong map OTP ---
    /**
     * Chuẩn hóa số điện thoại đầu vào:
     * - Nếu bắt đầu bằng 0 -> chuyển thành +84...
     * - Nếu bắt đầu bằng 84 (vd "849...") -> thêm +
     * - Nếu bắt đầu bằng +84 -> giữ nguyên
     * - Nếu chứa ký tự khác (space, -, .) sẽ được loại bỏ
     */
    private String normalizePhone(String raw) {
        if (raw == null) return null;
        String s = raw.trim();

        // bỏ space, -, ., ()
        s = s.replaceAll("[\\s\\-\\.\\(\\)]", "");

        if (s.isEmpty()) return s;

        // nếu đã có +
        if (s.startsWith("+")) {
            return s;
        }

        // 0xxxx -> +84xxx
        if (s.startsWith("0")) {
            return "+84" + s.substring(1);
        }

        // 84xxxx -> +84xxxx
        if (s.startsWith("84")) {
            return "+" + s;
        }

        // toàn chữ số nhưng không có 0/84 ở đầu -> mặc định +84
        if (s.matches("^\\d+$")) {
            return "+84" + s;
        }

        return s;
    }

    // Lớp lưu OTP và thời gian hết hạn
    private record OtpData(String otp, Instant expireAt) {}
}
