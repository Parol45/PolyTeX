package ru.test.restservice.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.util.UUID;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue
    final private UUID id = null;

    @Column(nullable = false, unique = true)
    public String email;

    public String password;

    @Column(nullable = false)
    public String role;

    public Boolean banned;

    @Override
    public String toString() {
        return email;
    }

}