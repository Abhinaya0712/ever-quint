package com.example.meetingrooms.repo;
import com.example.meetingrooms.domain.Room;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;
public interface RoomRepository extends JpaRepository<Room,Long> {
    boolean existsByNameKey(String nameKey);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select r from Room r where r.id=:id") Optional<Room> findByIdForUpdate(@Param("id") Long id);
    @Query("select distinct r from Room r left join r.amenities a where (:capacity is null or r.capacity >= :capacity) and (:amenity is null or lower(a)=lower(:amenity))") List<Room> search(@Param("capacity") Integer capacity,@Param("amenity") String amenity);
}
