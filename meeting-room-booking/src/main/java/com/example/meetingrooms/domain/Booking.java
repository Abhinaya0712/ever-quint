package com.example.meetingrooms.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity @Table(name="bookings", indexes=@Index(name="idx_booking_room_time",columnList="room_id,start_time,end_time"), uniqueConstraints=@UniqueConstraint(name="uk_booking_idempotency",columnNames="idempotency_key"))
public class Booking {
    public enum Status { CONFIRMED, CANCELLED }
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.EAGER,optional=false) @JoinColumn(name="room_id",nullable=false) private Room room;
    @Column(nullable=false) private String title;
    @Column(name="organizer_email",nullable=false) private String organizerEmail;
    @Column(name="start_time",nullable=false) private OffsetDateTime startTime;
    @Column(name="end_time",nullable=false) private OffsetDateTime endTime;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private Status status=Status.CONFIRMED;
    @Column(name="idempotency_key",unique=true) private String idempotencyKey;
    @Column(name="request_fingerprint") private String requestFingerprint;
    protected Booking() {}
    public Booking(Room room,String title,String email,OffsetDateTime start,OffsetDateTime end,String key,String fingerprint){this.room=room;this.title=title;this.organizerEmail=email;this.startTime=start;this.endTime=end;this.idempotencyKey=key;this.requestFingerprint=fingerprint;}
    public void cancel(){status=Status.CANCELLED;}
    public Long getId(){return id;} public Room getRoom(){return room;} public String getTitle(){return title;} public String getOrganizerEmail(){return organizerEmail;} public OffsetDateTime getStartTime(){return startTime;} public OffsetDateTime getEndTime(){return endTime;} public Status getStatus(){return status;}
    public String getRequestFingerprint(){return requestFingerprint;}
}
