package vn.hoang.datn92demo.dto.request;

import jakarta.validation.constraints.NotBlank;

public class AreaRequestDTO {

    @NotBlank(message = "code không được để trống")
    private String code;

    @NotBlank(message = "name không được để trống")
    private String name;

    private String description;

    public AreaRequestDTO() {}

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
