package com.roomchores.model;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Configurable chore definition. Points describe the effort of ONE completion;
 * how often it happens is expressed by {@link #frequency}.
 */
@Entity
@Table(name = "chores")
public class Chore extends PanacheEntityBase {

    @Id
    @Column(length = 64)
    public String code;

    @Column(nullable = false)
    public String name;

    @Column(nullable = false)
    public String category;

    @Column(nullable = false)
    public int points;

    @Column(name = "estimated_minutes", nullable = false)
    public int estimatedMinutes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public Frequency frequency = Frequency.AS_NEEDED;

    @Column(name = "required_skill")
    public String requiredSkill;

    public String location;

    @Column(name = "parent_code")
    public String parentCode;

    @Column(nullable = false)
    public boolean enabled = true;
}
