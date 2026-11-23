package com.example.meetingrooms.api;

import com.example.meetingrooms.api.ApiDtos.*;
import com.example.meetingrooms.domain.Room;
import com.example.meetingrooms.repo.RoomRepository;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@RestController
@RequestMapping("/rooms")
public class RoomController {
    private final RoomRepository rooms;

    public RoomController(RoomRepository rooms) {
        this.rooms = rooms;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RoomResponse create(@Valid @RequestBody CreateRoomRequest req) {
        String name = req.name().trim();
        if (rooms.existsByNameKey(name.toLowerCase(Locale.ROOT)))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Room name must be unique (case-insensitive)");
        try {
            return RoomResponse.from(rooms.saveAndFlush(new Room(name, req.capacity(), req.floor(),
                    new HashSet<>(req.amenities() == null ? List.of() : req.amenities()))));
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Room name must be unique (case-insensitive)");
        }
    }

    @GetMapping
    public List<RoomResponse> list(@RequestParam(required = false) Integer minCapacity,
            @RequestParam(required = false) String amenity) {
        if (minCapacity != null && minCapacity < 1)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "minCapacity must be at least 1");
        return rooms.search(minCapacity, amenity == null || amenity.isBlank() ? null : amenity.trim()).stream()
                .map(RoomResponse::from).toList();
    }
}
