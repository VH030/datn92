package vn.hoang.datn92demo.dto.response;

public class AreaSubscriptionResponseDTO {

    private Long id;           // ID của subscription
    private Long areaId;       // ID khu vực
    private String areaName;   // Tên khu vực
    private Long userId;       // ID user
    private String userPhone;  // Số điện thoại user

    public AreaSubscriptionResponseDTO() {}

    public AreaSubscriptionResponseDTO(Long id,
                                       Long areaId,
                                       String areaName,
                                       Long userId,
                                       String userPhone) {
        this.id = id;
        this.areaId = areaId;
        this.areaName = areaName;
        this.userId = userId;
        this.userPhone = userPhone;
    }

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    public Long getAreaId() {
        return areaId;
    }
    public void setAreaId(Long areaId) {
        this.areaId = areaId;
    }

    public String getAreaName() {
        return areaName;
    }
    public void setAreaName(String areaName) {
        this.areaName = areaName;
    }

    public Long getUserId() {
        return userId;
    }
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserPhone() {
        return userPhone;
    }
    public void setUserPhone(String userPhone) {
        this.userPhone = userPhone;
    }
}
