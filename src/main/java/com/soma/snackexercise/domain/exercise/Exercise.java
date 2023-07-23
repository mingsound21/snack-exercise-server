package com.soma.snackexercise.domain.exercise;

import com.soma.snackexercise.domain.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Entity
public class Exercise extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private ExerciseCategory exerciseCategory;

    private String videoLink;

    private String description;

    private Integer minPerKcal;

    @Builder
    public Exercise(String name, ExerciseCategory exerciseCategory, String videoLink, String description, Integer minPerKcal) {
        this.name = name;
        this.exerciseCategory = exerciseCategory;
        this.videoLink = videoLink;
        this.description = description;
        this.minPerKcal = minPerKcal;
        active();
    }
}