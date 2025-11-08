package vn.hoang.datn92demo.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
public class OtpService {

    @Value("${twilio.account-sid}")
    private String accountSid;

    @Value("${twilio.auth-token}")
    private String authToken;

    @Value("${twilio.phone-number}")
    private String twilioPhoneNumber;

    // Lưu OTP tạm thời trong bộ nhớ (RAM)
    private final Map<String, OtpData> otpStorage = new HashMap<>();

    // Thời gian hết hạn OTP (5 phút)
    private static final long EXPIRE_MILLIS = 5 * 60 * 1000;

    // Gửi OTP đến số điện thoại
    public void sendOtp(String phoneNumber) {
        // Khởi tạo Twilio (chỉ cần 1 lần)
        Twilio.init(accountSid, authToken);

        String otp = String.format("%06d", new Random().nextInt(999999));

        // Lưu OTP và thời gian hết hạn
        otpStorage.put(phoneNumber, new OtpData(otp, Instant.now().plusMillis(EXPIRE_MILLIS)));

        // Gửi qua Twilio
        try {
            Message.creator(
                    new PhoneNumber(phoneNumber),
                    new PhoneNumber(twilioPhoneNumber),
                    "Mã xác thực đăng ký của bạn là: " + otp + " (hiệu lực 5 phút)"
            ).create();

            System.out.println(" Đã gửi OTP đến " + phoneNumber + ": " + otp);
        } catch (Exception e) {
            // Nếu lỗi, in OTP ra console để test (local)
            System.err.println(" Không thể gửi SMS, OTP test: " + otp);
        }
    }


    // Xác thực OTP
    public boolean verifyOtp(String phoneNumber, String inputOtp) {
        OtpData otpData = otpStorage.get(phoneNumber);

        if (otpData == null) {
            System.out.println(" Không tìm thấy OTP cho " + phoneNumber);
            return false;
        }

        if (Instant.now().isAfter(otpData.expireAt())) {
            otpStorage.remove(phoneNumber);
            System.out.println(" OTP đã hết hạn cho " + phoneNumber);
            return false;
        }

        boolean valid = otpData.otp().equals(inputOtp);
        if (valid) otpStorage.remove(phoneNumber); // Xóa sau khi dùng
        System.out.println(valid ? " OTP hợp lệ" : " OTP sai");
        return valid;
    }

    // Lớp lưu OTP và thời gian hết hạn
    private record OtpData(String otp, Instant expireAt) {}
}

