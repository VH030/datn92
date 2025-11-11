package vn.hoang.datn92demo.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String phone;

    private String password;

    private String email;

    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Role role; // USER hoặc ADMIN

    public enum Role {
        USER,
        ADMIN
    }
}
