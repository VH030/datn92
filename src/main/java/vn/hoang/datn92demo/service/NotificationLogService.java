package vn.hoang.datn92demo.service;

import org.springframework.stereotype.Service;
import vn.hoang.datn92demo.model.DeviceSubscription;
import vn.hoang.datn92demo.model.Notification;
import vn.hoang.datn92demo.model.WaterLevel;
import vn.hoang.datn92demo.repository.NotificationRepository;
import java.util.Date;

@Service
public class NotificationLogService {

    private final NotificationRepository notificationRepository;

    public NotificationLogService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    /**
     * Lưu một notification cảnh báo mực nước cho user tương ứng với DeviceSubscription.
     */
    public void createWaterAlertNotification(DeviceSubscription sub,
                                             WaterLevel wl,
                                             String message) {
        if (sub.getUser() == null) {
            return;
        }

        Notification n = new Notification();
        n.setUser(sub.getUser());
        n.setDevice(sub.getDevice());
        n.setType("WATER_ALERT");
        n.setAlertType(wl.getAlertType());
        n.setLevelValue(wl.getLevel());
        n.setMessage(message);
        n.setIsRead(false);
        n.setCreatedAt(new Date());

        notificationRepository.save(n);
    }
}
