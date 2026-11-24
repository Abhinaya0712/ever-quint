package com.example.meetingrooms.api;

import com.example.meetingrooms.repo.RoomRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RoomController.class)
class RoomControllerTest {
    @Autowired
    MockMvc mvc;
    @MockBean
    RoomRepository rooms;

    @Test
    void rejectsBlankRoomName() throws Exception {
        mvc.perform(post("/rooms")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\" \" ,\"capacity\":4,\"floor\":1,\"amenities\":[]}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(rooms);
    }

    @Test
    void rejectsZeroCapacity() throws Exception {
        mvc.perform(post("/rooms")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Board Room\",\"capacity\":0,\"floor\":1,\"amenities\":[]}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(rooms);
    }
}
