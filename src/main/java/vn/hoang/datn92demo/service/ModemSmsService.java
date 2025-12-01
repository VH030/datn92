package vn.hoang.datn92demo.service;

import com.fazecast.jSerialComm.SerialPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ModemSmsService implements NotificationService {

    @Value("${modem.port}")
    private String modemPort;

    @Value("${modem.baudRate}")
    private int baudRate;

    private SerialPort serialPort;

    private void openPort() {
        if (serialPort != null && serialPort.isOpen()) return;

        serialPort = SerialPort.getCommPort(modemPort);
        serialPort.setBaudRate(baudRate);
        serialPort.setNumDataBits(8);
        serialPort.setNumStopBits(1);
        serialPort.setParity(SerialPort.NO_PARITY);
        serialPort.openPort();
    }

    private void sendAT(String command, int delay) throws Exception {
        serialPort.getOutputStream().write((command + "\r").getBytes());
        serialPort.getOutputStream().flush();
        Thread.sleep(delay);
    }

    @Override
    public void sendSms(String phone, String text) {
        try {
            openPort();

            sendAT("AT", 500);
            sendAT("AT+CMGF=1", 500); // text mode
            sendAT("AT+CSCS=\"GSM\"", 500); // charset
            sendAT("AT+CMGS=\"" + phone + "\"", 500);

            serialPort.getOutputStream().write((text + "\u001A").getBytes()); // CTRL+Z
            serialPort.getOutputStream().flush();

            System.out.println("SMS sent to " + phone);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
