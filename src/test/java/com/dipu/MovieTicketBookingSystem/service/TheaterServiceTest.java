package com.dipu.MovieTicketBookingSystem.service;

import com.dipu.MovieTicketBookingSystem.dto.ScreenRequest;
import com.dipu.MovieTicketBookingSystem.dto.ScreenResponse;
import com.dipu.MovieTicketBookingSystem.dto.TheaterRequest;
import com.dipu.MovieTicketBookingSystem.dto.TheaterResponse;
import com.dipu.MovieTicketBookingSystem.model.entity.Screen;
import com.dipu.MovieTicketBookingSystem.model.entity.Seat;
import com.dipu.MovieTicketBookingSystem.model.entity.Theater;
import com.dipu.MovieTicketBookingSystem.repository.ScreenRepository;
import com.dipu.MovieTicketBookingSystem.repository.SeatRepository;
import com.dipu.MovieTicketBookingSystem.repository.TheaterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TheaterServiceTest {

    @Mock
    private TheaterRepository theaterRepository;

    @Mock
    private ScreenRepository screenRepository;

    @Mock
    private SeatRepository seatRepository;

    @InjectMocks
    private TheaterService theaterService;

    private Theater testTheater;
    private UUID theaterId;

    @BeforeEach
    void setUp() {
        theaterId = UUID.randomUUID();
        testTheater = Theater.builder()
                .id(theaterId)
                .name("AMC Times Square")
                .city("New York")
                .address("123 Broadway")
                .screens(new ArrayList<>())
                .build();
    }

    @Test
    void createTheater_Success() {
        TheaterRequest request = new TheaterRequest();
        request.setName("AMC Times Square");
        request.setCity("New York");
        request.setAddress("123 Broadway");

        when(theaterRepository.save(any(Theater.class))).thenReturn(testTheater);

        TheaterResponse response = theaterService.createTheater(request);

        assertNotNull(response);
        assertEquals(theaterId, response.getId());
        assertEquals("AMC Times Square", response.getName());
        verify(theaterRepository, times(1)).save(any(Theater.class));
    }

    @Test
    void addScreen_Success() {
        ScreenRequest request = new ScreenRequest();
        request.setTheaterId(theaterId);
        request.setName("Screen 1");
        request.setCapacity(25); // Should generate 25 seats

        when(theaterRepository.findById(theaterId)).thenReturn(Optional.of(testTheater));
        
        Screen testScreen = Screen.builder()
                .id(UUID.randomUUID())
                .theater(testTheater)
                .name("Screen 1")
                .capacity(25)
                .build();
                
        when(screenRepository.save(any(Screen.class))).thenReturn(testScreen);

        ScreenResponse response = theaterService.addScreen(request);

        assertNotNull(response);
        assertEquals("Screen 1", response.getName());
        assertEquals(25, response.getCapacity());
        
        // 25 seats should be generated and saved
        verify(seatRepository, times(25)).save(any(Seat.class));
    }

    @Test
    void addScreen_DuplicateName_ThrowsException() {
        ScreenRequest request = new ScreenRequest();
        request.setTheaterId(theaterId);
        request.setName("Screen 1");
        request.setCapacity(25);

        Screen existingScreen = Screen.builder().name("Screen 1").build();
        testTheater.getScreens().add(existingScreen);

        when(theaterRepository.findById(theaterId)).thenReturn(Optional.of(testTheater));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> theaterService.addScreen(request));
        assertEquals("Screen name already exists in this theater", exception.getMessage());
        
        verify(screenRepository, never()).save(any(Screen.class));
    }

    @Test
    void getTheaterById_Success() {
        when(theaterRepository.findById(theaterId)).thenReturn(Optional.of(testTheater));

        TheaterResponse response = theaterService.getTheaterById(theaterId);

        assertNotNull(response);
        assertEquals("AMC Times Square", response.getName());
    }

    @Test
    void getAllTheaters_Success() {
        when(theaterRepository.findAll()).thenReturn(List.of(testTheater));

        List<TheaterResponse> responses = theaterService.getAllTheaters();

        assertNotNull(responses);
        assertEquals(1, responses.size());
    }
}
