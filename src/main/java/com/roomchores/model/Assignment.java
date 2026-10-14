package com.roomchores.model;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * One chore given to one roommate for one day. Points are only awarded on completion;
 * a missed or pending assignment earns nothing.
 */
@Entity
@Table(name = "assignments", indexes = {
        @Index(name = "idx_assignments_due_date", columnList = "due_date"),
        @Index(name = "idx_assignments_roommate", columnList = "roommate_id")
})
public class Assignment extends PanacheEntityBase {

    @Id
    public UUID id = UUID.randomUUID();

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "chore_code", nullable = false)
    public Chore chore;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "roommate_id", nullable = false)
    public Roommate roommate;

    @Column(name = "due_date", nullable = false)
    public LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public AssignmentStatus status = AssignmentStatus.PENDING;

    @Column(name = "points_awarded", nullable = false)
    public int pointsAwarded;

    @Column(name = "completed_at")
    public Instant completedAt;

    public String note;
}
