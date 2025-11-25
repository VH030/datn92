package vn.hoang.datn92demo.model;

import jakarta.persistence.*;
import java.util.Date;
import java.util.Objects;

@Entity
@Table(name = "area_subscriptions", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "area_id"})
})
public class AreaSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private vn.hoang.datn92demo.model.User user;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "area_id")
    private Area area;

    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt = new Date();

    // getters / setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public vn.hoang.datn92demo.model.User getUser() { return user; }
    public void setUser(vn.hoang.datn92demo.model.User user) { this.user = user; }

    public Area getArea() { return area; }
    public void setArea(Area area) { this.area = area; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AreaSubscription)) return false;
        AreaSubscription that = (AreaSubscription) o;
        return Objects.equals(id, that.id);
    }
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
