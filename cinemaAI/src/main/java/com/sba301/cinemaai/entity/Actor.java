package com.sba301.cinemaai.entity;

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
@Table(name = "actors")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Actor extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Setter
    @Column(length = 1000)
    private String biography;

    @Setter
    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    public Actor(String name, String biography, String avatarUrl) {
        this.name = name;
        this.biography = biography;
        this.avatarUrl = avatarUrl;
    }
}
