package com.example.meetingrooms.repo;

import com.example.meetingrooms.domain.Booking;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.OffsetDateTime;
import java.util.*;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    Optional<Booking> findByIdempotencyKey(String key);

    @Query("select count(b)>0 from Booking b where b.room.id=:roomId and b.status='CONFIRMED' and b.startTime < :end and b.endTime > :start")
    boolean hasOverlap(@Param("roomId") Long roomId, @Param("start") OffsetDateTime start,
            @Param("end") OffsetDateTime end);

    @Query(value = "select * from bookings b where (:roomId is null or b.room_id=:roomId) and (:fromTime is null or b.end_time>:fromTime) and (:toTime is null or b.start_time<:toTime) order by b.start_time limit :lim offset :off", nativeQuery = true)
    List<Booking> page(@Param("roomId") Long roomId, @Param("fromTime") OffsetDateTime from,
            @Param("toTime") OffsetDateTime to, @Param("lim") int limit, @Param("off") int offset);

    @Query("select count(b) from Booking b where (:roomId is null or b.room.id=:roomId) and (:fromTime is null or b.endTime>:fromTime) and (:toTime is null or b.startTime<:toTime)")
    long countPage(@Param("roomId") Long roomId, @Param("fromTime") OffsetDateTime from,
            @Param("toTime") OffsetDateTime to);

    @Query("select b from Booking b where b.room.id=:roomId and b.status='CONFIRMED' and b.startTime<:to and b.endTime>:from")
    List<Booking> confirmedOverlappingRange(@Param("roomId") Long roomId, @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to);
}
