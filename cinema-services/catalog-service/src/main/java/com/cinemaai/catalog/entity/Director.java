package com.cinemaai.catalog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Entity
@Table(name = "directors")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Director extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @Column(nullable = false, unique = true, length = 255)
    private String name;

    @Setter
    @Column(columnDefinition = "TEXT")
    private String biography;

    @Setter
    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    public Director(String name, String biography, String avatarUrl) {
        this.name = name;
        this.biography = biography;
        this.avatarUrl = avatarUrl;
    }

    public Director(Long id, String name, String biography, String avatarUrl) {
        this.id = id;
        this.name = name;
        this.biography = biography;
        this.avatarUrl = avatarUrl;
    }
}
