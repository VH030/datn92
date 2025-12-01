package vn.hoang.datn92demo.service;

import com.fazecast.jSerialComm.SerialPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
public class OtpService {

    @Value("${modem.port}")
    private String modemPort;

    @Value("${modem.baudRate:115200}")
    private int baudRate;

    @Value("${modem.openTimeoutMs:2000}")
    private int openTimeoutMs;

    @Value("${modem.writeDelayMs:500}")
    private int writeDelayMs;

    // Lưu OTP tạm thời trong bộ nhớ (RAM) — dùng số đã chuẩn hóa làm key
    private final Map<String, OtpData> otpStorage = new HashMap<>();

    // Thời gian hết hạn OTP (5 phút)
    private static final long EXPIRE_MILLIS = 5 * 60 * 1000;

    // Gửi OTP đến số điện thoại (chấp nhận 09..., +849..., 849..., v.v.)
    public void sendOtp(String rawPhoneNumber) {
        String phoneNumber = normalizePhone(rawPhoneNumber);
        String otp = String.format("%06d", new Random().nextInt(1_000_000));

        // Lưu OTP và thời gian hết hạn dùng phoneNumber đã chuẩn hóa
        otpStorage.put(phoneNumber, new OtpData(otp, Instant.now().plusMillis(EXPIRE_MILLIS)));

        String message = "OTP cua ban la: " + otp + " (hieu luc 5 phut)";

        try {
            boolean sent = sendSmsViaModem(phoneNumber, message);
            if (sent) {
                System.out.println("Đã gửi OTP qua modem đến " + phoneNumber + ": " + otp);
            } else {
                System.err.println("Không thể gửi SMS qua modem, OTP (fallback): " + otp);
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi gửi SMS qua modem: " + e.getMessage());
            e.printStackTrace();
            System.err.println("OTP fallback: " + otp);
        }
    }

    // Xác thực OTP (input phone có thể ở dạng 09..., +84...)
    public boolean verifyOtp(String rawPhoneNumber, String inputOtp) {
        String phoneNumber = normalizePhone(rawPhoneNumber);
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

    // --- Helpers để gửi SMS qua modem bằng AT command ---
    private boolean sendSmsViaModem(String phone, String text) throws Exception {
        SerialPort port = SerialPort.getCommPort(modemPort);
        port.setBaudRate(baudRate);
        port.setNumDataBits(8);
        port.setNumStopBits(SerialPort.ONE_STOP_BIT);
        port.setParity(SerialPort.NO_PARITY);
        // non-blocking, ta tự loop chờ
        port.setComPortTimeouts(SerialPort.TIMEOUT_NONBLOCKING, 0, 0);

        if (!port.openPort()) {
            System.err.println("Không mở được cổng " + modemPort);
            return false;
        }

        try {
            OutputStream out = port.getOutputStream();
            InputStream in = port.getInputStream();

            // 1. Khởi tạo modem
            sendRaw(out, "AT\r");
            Thread.sleep(200);
            System.out.println("RESP AT: " + readAll(in));

            sendRaw(out, "ATE0\r"); // tắt echo
            Thread.sleep(200);
            System.out.println("RESP ATE0: " + readAll(in));

            sendRaw(out, "AT+CMGF=1\r"); // text mode
            Thread.sleep(300);
            System.out.println("RESP CMGF: " + readAll(in));

            // ⚠️ QUAN TRỌNG: set SMS params (DCS) để tránh CMS ERROR 500
            sendRaw(out, "AT+CSMP=17,167,0,0\r");
            Thread.sleep(300);
            System.out.println("RESP CSMP: " + readAll(in));

            // Kiểm tra SMSC (chỉ để debug/log)
            sendRaw(out, "AT+CSCA?\r");
            Thread.sleep(300);
            System.out.println("RESP CSCA?: " + readAll(in));

            // 2. Gửi lệnh CMGS và chờ prompt '>'
            sendRaw(out, "AT+CMGS=\"" + phone + "\"\r");

            final int promptTimeoutMs = 8000;
            int waited = 0;
            boolean gotPrompt = false;
            while (waited < promptTimeoutMs) {
                String r = readAll(in);
                if (r != null && !r.isEmpty()) {
                    System.out.println("RESP waiting for > : " + r);
                    if (r.contains(">")) { // modem sẵn sàng nhận nội dung
                        gotPrompt = true;
                        break;
                    }
                    if (r.contains("ERROR") || r.contains("CMS ERROR") || r.contains("NO CARRIER")) {
                        System.err.println("Lỗi trước prompt: " + r);
                        return false;
                    }
                }
                Thread.sleep(200);
                waited += 200;
            }

            if (!gotPrompt) {
                System.err.println("Không nhận được '>' prompt từ modem (timeout). Snapshot: " + readAll(in));
                return false;
            }

            // 3. Gửi nội dung + Ctrl+Z
            sendRaw(out, text + "\u001A");
            System.out.println("Đã gửi payload, chờ kết quả...");

            // 4. Chờ +CMGS / CMS ERROR
            final int resultTimeoutMs = 30000;
            waited = 0;
            StringBuilder accum = new StringBuilder();
            while (waited < resultTimeoutMs) {
                String r = readAll(in);
                if (r != null && !r.isEmpty()) {
                    accum.append(r);
                    System.out.println("RESP chunk: " + r);
                    if (accum.indexOf("+CMGS:") >= 0 || accum.indexOf("OK") >= 0) {
                        System.out.println("SMS gửi thành công, response: " + accum);
                        return true;
                    }
                    if (accum.indexOf("CMS ERROR") >= 0 || accum.indexOf("ERROR") >= 0) {
                        System.err.println("Modem trả lỗi khi gửi SMS: " + accum);
                        return false;
                    }
                }
                Thread.sleep(500);
                waited += 500;
            }

            System.err.println("Hết timeout chờ kết quả. Accumulated response: " + accum);
            return false;
        } finally {
            if (port.isOpen()) port.closePort();
        }
    }

    private void sendRaw(OutputStream out, String payload) throws Exception {
        out.write(payload.getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    private String readAll(InputStream in) throws Exception {
        StringBuilder sb = new StringBuilder();
        byte[] buffer = new byte[1024];
        int available;
        while ((available = in.available()) > 0) {
            int r = in.read(buffer, 0, Math.min(buffer.length, available));
            if (r > 0) {
                sb.append(new String(buffer, 0, r, StandardCharsets.UTF_8));
                Thread.sleep(50);
            } else {
                break;
            }
        }
        String s = sb.length() == 0 ? null : sb.toString();
        return s;
    }

    // --- Chuẩn hóa số điện thoại ---
    /**
     * Chuẩn hóa số điện thoại đầu vào:
     * - Nếu bắt đầu bằng 0 -> chuyển thành +84...
     * - Nếu bắt đầu bằng 84 (vd "849...") -> thêm +
     * - Nếu bắt đầu bằng +84 -> giữ nguyên
     * - Nếu chứa ký tự khác (space, -, .) sẽ được loại bỏ
     * - Nếu không thể nhận dạng, trả về input đã trim
     */
    private String normalizePhone(String raw) {
        if (raw == null) return null;
        String s = raw.trim();

        // Remove spaces, dashes, dots, parentheses
        s = s.replaceAll("[\\s\\-\\.\\(\\)]", "");

        if (s.isEmpty()) return s;

        // If already starts with +, keep plus sign
        if (s.startsWith("+")) {
            return s;
        }

        // If starts with 0 -> +84 + substring(1)
        if (s.startsWith("0")) {
            return "+84" + s.substring(1);
        }

        // If starts with 84 (no plus) -> add +
        if (s.startsWith("84")) {
            return "+" + s;
        }

        // If all digits but no leading 0/84, assume local and add +84
        if (s.matches("^\\d+$")) {
            return "+84" + s;
        }

        // fallback: return trimmed raw
        return s;
    }

    // Lớp lưu OTP và thời gian hết hạn
    private record OtpData(String otp, Instant expireAt) {}
}
