package com.roomchores.model;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * A person sharing the room. Capacity is a percentage (0-100) that scales their fair share
 * of the monthly workload, so someone with limited availability can be set to e.g. 60.
 */
@Entity
@Table(name = "roommates")
public class Roommate extends PanacheEntityBase {

    @Id
    public UUID id = UUID.randomUUID();

    @Column(nullable = false)
    public String name;

    @Column(nullable = false)
    public boolean active = true;

    @Column(name = "capacity_percent", nullable = false)
    public int capacityPercent = 100;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "roommate_skills", joinColumns = @JoinColumn(name = "roommate_id"))
    @Column(name = "skill")
    public Set<String> skills = new HashSet<>();
}
