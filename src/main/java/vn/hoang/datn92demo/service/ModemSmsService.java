//package vn.hoang.datn92demo.service;
//
//import com.fazecast.jSerialComm.SerialPort;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.scheduling.annotation.Async;
//import org.springframework.stereotype.Service;
//
//import java.io.InputStream;
//import java.io.OutputStream;
//import java.nio.charset.StandardCharsets;
//import java.text.Normalizer;
//
//@Service
//public class ModemSmsService implements NotificationService {
//
//    @Value("${modem.port}")
//    private String modemPort;
//
//    @Value("${modem.baudRate:115200}")
//    private int baudRate;
//
//    @Value("${modem.openTimeoutMs:2000}")
//    private int openTimeoutMs;
//
//    @Value("${modem.writeDelayMs:500}")
//    private int writeDelayMs;
//
//    @Override
//    @Async("smsExecutor")
//    public void sendSms(String rawPhone, String text) {
//        String phone = normalizePhone(rawPhone);
//        if (phone == null || phone.isBlank()) {
//            System.err.println("Phone number is blank, skip SMS");
//            return;
//        }
//
//        // Chuẩn hóa/bỏ dấu để tránh lỗi encode
//        String safeText = sanitizeGsmText(text);
//
//        try {
//            boolean ok = sendSmsViaModem(phone, safeText);
//            if (ok) {
//                System.out.println("SMS sent to " + phone + " | content: " + safeText);
//            } else {
//                System.err.println("Failed to send SMS to " + phone + " | content: " + safeText);
//            }
//        } catch (Exception e) {
//            System.err.println("Error when sending SMS to " + phone + ": " + e.getMessage());
//            e.printStackTrace();
//        }
//    }
//
//
//    // ====== Core modem logic (dùng chung cho mọi SMS) ======
//    private boolean sendSmsViaModem(String phone, String text) throws Exception {
//        SerialPort port = SerialPort.getCommPort(modemPort);
//        port.setBaudRate(baudRate);
//        port.setNumDataBits(8);
//        port.setNumStopBits(SerialPort.ONE_STOP_BIT);
//        port.setParity(SerialPort.NO_PARITY);
//        port.setComPortTimeouts(SerialPort.TIMEOUT_NONBLOCKING, 0, 0);
//
//        if (!port.openPort()) {
//            System.err.println("Không mở được cổng " + modemPort);
//            return false;
//        }
//
//        try {
//            OutputStream out = port.getOutputStream();
//            InputStream in = port.getInputStream();
//
//            // 1. Init modem
//            sendRaw(out, "AT\r");
//            Thread.sleep(200);
//            System.out.println("RESP AT: " + readAll(in));
//
//            sendRaw(out, "ATE0\r"); // tắt echo
//            Thread.sleep(200);
//            System.out.println("RESP ATE0: " + readAll(in));
//
//            sendRaw(out, "AT+CMGF=1\r"); // text mode
//            Thread.sleep(300);
//            System.out.println("RESP CMGF: " + readAll(in));
//
//            // Charset GSM 7-bit
//            sendRaw(out, "AT+CSCS=\"GSM\"\r");
//            Thread.sleep(300);
//            System.out.println("RESP CSCS: " + readAll(in));
//
//            // CSMP để tránh CMS ERROR 500
//            sendRaw(out, "AT+CSMP=17,167,0,0\r");
//            Thread.sleep(300);
//            System.out.println("RESP CSMP: " + readAll(in));
//
//            // check SMSC (log)
//            sendRaw(out, "AT+CSCA?\r");
//            Thread.sleep(300);
//            System.out.println("RESP CSCA?: " + readAll(in));
//
//            // 2. Gửi CMGS và đợi '>'
//            sendRaw(out, "AT+CMGS=\"" + phone + "\"\r");
//
//            final int promptTimeoutMs = 8000;
//            int waited = 0;
//            boolean gotPrompt = false;
//            while (waited < promptTimeoutMs) {
//                String r = readAll(in);
//                if (r != null && !r.isEmpty()) {
//                    System.out.println("RESP waiting for > : " + r);
//                    if (r.contains(">")) {
//                        gotPrompt = true;
//                        break;
//                    }
//                    if (r.contains("ERROR") || r.contains("CMS ERROR") || r.contains("NO CARRIER")) {
//                        System.err.println("Lỗi trước prompt: " + r);
//                        return false;
//                    }
//                }
//                Thread.sleep(200);
//                waited += 200;
//            }
//
//            if (!gotPrompt) {
//                System.err.println("Không nhận được '>' prompt từ modem (timeout). Snapshot: " + readAll(in));
//                return false;
//            }
//
//            // 3. Gửi nội dung + Ctrl+Z
//            sendRaw(out, text + "\u001A");
//            System.out.println("Đã gửi payload, chờ kết quả...");
//
//            // 4. Đợi +CMGS / CMS ERROR
//            final int resultTimeoutMs = 30000;
//            waited = 0;
//            StringBuilder accum = new StringBuilder();
//            while (waited < resultTimeoutMs) {
//                String r = readAll(in);
//                if (r != null && !r.isEmpty()) {
//                    accum.append(r);
//                    System.out.println("RESP chunk: " + r);
//                    if (accum.indexOf("+CMGS:") >= 0 || accum.indexOf("OK") >= 0) {
//                        System.out.println("SMS gửi thành công, response: " + accum);
//                        return true;
//                    }
//                    if (accum.indexOf("CMS ERROR") >= 0 || accum.indexOf("ERROR") >= 0) {
//                        System.err.println("Modem trả lỗi khi gửi SMS: " + accum);
//                        return false;
//                    }
//                }
//                Thread.sleep(500);
//                waited += 500;
//            }
//
//            System.err.println("Hết timeout chờ kết quả. Accumulated response: " + accum);
//            return false;
//        } finally {
//            if (port.isOpen()) port.closePort();
//        }
//    }
//
//    private void sendRaw(OutputStream out, String payload) throws Exception {
//        // Gửi dưới dạng US_ASCII để đảm bảo mỗi ký tự = 1 byte, không bị UTF-8 làm rối
//        out.write(payload.getBytes(StandardCharsets.US_ASCII));
//        out.flush();
//    }
//
//    private String readAll(InputStream in) throws Exception {
//        StringBuilder sb = new StringBuilder();
//        byte[] buffer = new byte[1024];
//        int available;
//        while ((available = in.available()) > 0) {
//            int r = in.read(buffer, 0, Math.min(buffer.length, available));
//            if (r > 0) {
//                sb.append(new String(buffer, 0, r, StandardCharsets.UTF_8));
//                Thread.sleep(50);
//            } else {
//                break;
//            }
//        }
//        return sb.length() == 0 ? null : sb.toString();
//    }
//
//    // Loại bỏ dấu tiếng Việt + ký tự lạ, chỉ giữ ASCII 0x20..0x7E
//    private String sanitizeGsmText(String text) {
//        if (text == null) return "";
//
//        // B1: chuẩn hóa về dạng tổ hợp (NFD), tách dấu thành ký tự riêng
//        String n = Normalizer.normalize(text, Normalizer.Form.NFD);
//        // B2: xóa các ký tự dấu (Mn = Mark, nonspacing)
//        n = n.replaceAll("\\p{M}+", "");
//
//        // B3: thay mọi ký tự ngoài ASCII 0x20..0x7E bằng space
//        n = n.replaceAll("[^\\x20-\\x7E]", " ");
//
//        // B4: cắt nếu quá dài
//        if (n.length() > 150) {
//            n = n.substring(0, 150);
//        }
//
//        return n;
//    }
//
//    // ====== Chuẩn hóa số điện thoại (áp dụng cho mọi loại SMS) ======
//    private String normalizePhone(String raw) {
//        if (raw == null) return null;
//        String s = raw.trim();
//
//        // bỏ space, -, ., ()
//        s = s.replaceAll("[\\s\\-\\.\\(\\)]", "");
//
//        if (s.isEmpty()) return s;
//
//        if (s.startsWith("+")) {
//            return s;
//        }
//
//        if (s.startsWith("0")) {
//            return "+84" + s.substring(1);
//        }
//
//        if (s.startsWith("84")) {
//            return "+" + s;
//        }
//
//        if (s.matches("^\\d+$")) {
//            return "+84" + s;
//        }
//
//        return s;
//    }
//}
