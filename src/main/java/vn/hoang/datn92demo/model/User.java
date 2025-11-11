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

    @Column(nullable = false, unique = true)
    private String username;

    private String fullName;

    private String phone;

    private String password;

    private String email;


    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Role role; // USER hoặc ADMIN

    public enum Role {
        USER,
        ADMIN
    }
}
