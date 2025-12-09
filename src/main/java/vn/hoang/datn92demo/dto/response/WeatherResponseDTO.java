package vn.hoang.datn92demo.dto.response;

public class WeatherResponseDTO {
    private String description;
    private Double temperature;
    private Double feelsLike;
    private Integer humidity;
    private Double windSpeed;
    private Long timestamp;
    private String provider; // optional: tên provider (openweathermap...)

    public WeatherResponseDTO() {}

    public WeatherResponseDTO(String description, Double temperature, Double feelsLike,
                              Integer humidity, Double windSpeed, Long timestamp, String provider) {
        this.description = description;
        this.temperature = temperature;
        this.feelsLike = feelsLike;
        this.humidity = humidity;
        this.windSpeed = windSpeed;
        this.timestamp = timestamp;
        this.provider = provider;
    }

    // getters / setters
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }

    public Double getFeelsLike() { return feelsLike; }
    public void setFeelsLike(Double feelsLike) { this.feelsLike = feelsLike; }

    public Integer getHumidity() { return humidity; }
    public void setHumidity(Integer humidity) { this.humidity = humidity; }

    public Double getWindSpeed() { return windSpeed; }
    public void setWindSpeed(Double windSpeed) { this.windSpeed = windSpeed; }

    public Long getTimestamp() { return timestamp; }
    public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
}
