package com.example.meetingrooms.domain;

import jakarta.persistence.*;
import java.util.*;

@Entity
@Table(name = "rooms", uniqueConstraints = @UniqueConstraint(name = "uk_room_name", columnNames = "name_key"))
public class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String name;
    @Column(name = "name_key", nullable = false)
    private String nameKey;
    @Column(nullable = false)
    private int capacity;
    @Column(nullable = false)
    private int floor;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "room_amenities", joinColumns = @JoinColumn(name = "room_id"))
    @Column(name = "amenity")
    private Set<String> amenities = new HashSet<>();

    protected Room() {
    }

    public Room(String name, int capacity, int floor, Set<String> amenities) {
        this.name = name;
        this.nameKey = name.trim().toLowerCase(Locale.ROOT);
        this.capacity = capacity;
        this.floor = floor;
        this.amenities = amenities == null ? new HashSet<>() : new HashSet<>(amenities);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getCapacity() {
        return capacity;
    }

    public int getFloor() {
        return floor;
    }

    public Set<String> getAmenities() {
        return Collections.unmodifiableSet(amenities);
    }
}
