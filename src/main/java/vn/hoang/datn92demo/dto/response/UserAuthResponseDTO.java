package vn.hoang.datn92demo.dto.response;

public class UserAuthResponseDTO {

    private String token;
    private String type = "Bearer";

    public UserAuthResponseDTO(String token) {
        this.token = token;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getType() { return type; }
}

