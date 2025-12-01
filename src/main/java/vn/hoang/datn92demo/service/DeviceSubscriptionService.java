package vn.hoang.datn92demo.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.hoang.datn92demo.model.Device;
import vn.hoang.datn92demo.model.DeviceSubscription;
import vn.hoang.datn92demo.model.User;
import vn.hoang.datn92demo.repository.DeviceRepository;
import vn.hoang.datn92demo.repository.DeviceSubscriptionRepository;
import vn.hoang.datn92demo.repository.UserRepository;

import java.util.List;
import java.util.Optional;

@Service
public class DeviceSubscriptionService {

    private final DeviceSubscriptionRepository subscriptionRepository;
    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;

    public DeviceSubscriptionService(DeviceSubscriptionRepository subscriptionRepository,
                                     DeviceRepository deviceRepository,
                                     UserRepository userRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.deviceRepository = deviceRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public DeviceSubscription subscribe(Long userId, Long deviceId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found"));

        boolean exists = subscriptionRepository.findByUser_Id(userId).stream()
                .anyMatch(s -> s.getDevice() != null && s.getDevice().getId().equals(deviceId));
        if (exists) {
            Optional<DeviceSubscription> existing = subscriptionRepository.findByUser_Id(userId).stream()
                    .filter(s -> s.getDevice() != null && s.getDevice().getId().equals(deviceId))
                    .findFirst();
            return existing.orElseThrow();
        }

        DeviceSubscription s = new DeviceSubscription();
        s.setUser(user);
        s.setDevice(device);
        return subscriptionRepository.save(s);
    }

    @Transactional
    public void unsubscribe(Long userId, Long deviceId) {
        subscriptionRepository.findByUser_Id(userId).stream()
                .filter(s -> s.getDevice() != null && s.getDevice().getId().equals(deviceId))
                .findFirst()
                .ifPresent(subscriptionRepository::delete);
    }

    public List<DeviceSubscription> getSubscriptionsForDeviceByDeviceIdString(String deviceDeviceId) {
        return subscriptionRepository.findByDeviceDeviceIdWithUserAndDevice(deviceDeviceId);
    }

    public List<DeviceSubscription> getSubscriptionsForDevice(Long deviceId) {
        return subscriptionRepository.findByDeviceIdWithUserAndDevice(deviceId);
    }

    public List<DeviceSubscription> getSubscriptionsForUser(Long userId) {
        return subscriptionRepository.findByUserIdWithDevice(userId);
    }
}
