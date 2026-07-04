package com.example.scaffold.user;

import java.sql.Date;

import com.example.scaffold.common.Gender;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String name;
    @Column(name="dob", nullable=false)
    private Date birthDate;
    @Enumerated(EnumType.STRING)
    private Gender gender;
}
