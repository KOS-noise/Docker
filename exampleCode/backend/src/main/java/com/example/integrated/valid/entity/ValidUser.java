package com.example.integrated.valid.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "valid_users")
@Getter
@NoArgsConstructor
public class ValidUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private String email;

    @Column
    private Integer age;

    @Builder
    public ValidUser(String username, String password, String email, Integer age) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.age = age;
    }
}
