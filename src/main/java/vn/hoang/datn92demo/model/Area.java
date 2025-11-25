package vn.hoang.datn92demo.model;

import jakarta.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "areas")
public class Area {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String code;

    private String name;

    @Column(columnDefinition = "text")
    private String description;

    // getters / setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Area)) return false;
        Area area = (Area) o;
        return Objects.equals(id, area.id);
    }
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
