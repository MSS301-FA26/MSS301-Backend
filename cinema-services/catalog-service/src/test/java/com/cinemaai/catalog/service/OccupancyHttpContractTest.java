package com.cinemaai.catalog.service;

import com.cinemaai.catalog.client.BookingClient;
import com.cinemaai.catalog.controller.ShowtimeController;
import com.cinemaai.catalog.entity.*;
import com.cinemaai.catalog.enums.*;
import com.cinemaai.catalog.exception.GlobalExceptionHandler;
import com.cinemaai.catalog.mapper.CinemaMapper;
import com.cinemaai.catalog.repository.*;
import com.cinemaai.catalog.service.impl.ShowtimeServiceImpl;
import com.cinemaai.catalog.dto.response.cinema.ShowtimeSeatResponse;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/** Real RestClient + HTTP fault server; repositories mocked, no claim of deployed E2E. */
class OccupancyHttpContractTest {
    HttpServer upstream;
    java.util.concurrent.ExecutorService executor;
    MockMvc mvc;
    volatile int status=200;
    volatile String body="{\"success\":true,\"data\":[]}";
    volatile long delay;

    @BeforeEach void setup() throws Exception {
        upstream=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        executor=Executors.newCachedThreadPool(); upstream.setExecutor(executor);
        upstream.createContext("/", exchange -> {
            try {
                Thread.sleep(delay);
                byte[] bytes=body.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type","application/json");
                exchange.sendResponseHeaders(status,bytes.length);
                exchange.getResponseBody().write(bytes);
            } catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
            finally { exchange.close(); }
        });
        upstream.start();
        var shows=mock(ShowtimeRepository.class); var seats=mock(SeatRepository.class);
        var mapper=mock(CinemaMapper.class); var show=mock(Showtime.class);
        var room=mock(Room.class); var movie=mock(Movie.class); var seat=mock(Seat.class);
        when(show.getId()).thenReturn(1L); when(show.getRoom()).thenReturn(room); when(show.getMovie()).thenReturn(movie);
        when(show.getStatus()).thenReturn(ShowtimeStatus.OPEN);
        when(show.getStartTime()).thenReturn(LocalDateTime.now().plusDays(1));
        when(show.getEndTime()).thenReturn(LocalDateTime.now().plusDays(1).plusHours(2));
        when(room.getId()).thenReturn(1L); when(room.getStatus()).thenReturn(RoomStatus.ACTIVE);
        when(room.getRoomType()).thenReturn(RoomType.STANDARD);
        when(movie.getId()).thenReturn(1L); when(movie.getStatus()).thenReturn(MovieStatus.NOW_SHOWING);
        when(seat.getId()).thenReturn(1L); when(seat.getRowLabel()).thenReturn("A");
        when(seat.getStatus()).thenReturn(SeatStatus.AVAILABLE);
        when(shows.findWithDetailsById(1L)).thenReturn(Optional.of(show));
        when(shows.findCustomerCandidateShowtimes(any(),any(),any())).thenReturn(List.of(show));
        when(seats.findByRoom(room)).thenReturn(List.of(seat));
        when(mapper.toShowtimeSeatResponse(eq(seat),anyString(),any(),eq(show))).thenAnswer(inv ->
                new ShowtimeSeatResponse(1L,1L,"A",1,1,1,1,SeatType.STANDARD,SeatStatus.AVAILABLE,inv.getArgument(1),null,BigDecimal.TEN));
        var client=new BookingClient("http://127.0.0.1:"+upstream.getAddress().getPort(),"private-test-secret");
        var service=new ShowtimeServiceImpl(shows,mock(MovieRepository.class),seats,mock(MovieGenreRepository.class),
                mock(RoomService.class),mapper,mock(AuditLogService.class),client,mock(CinemaAudiencePriceRepository.class),mock(CinemaService.class));
        mvc=MockMvcBuilders.standaloneSetup(new ShowtimeController(service)).setControllerAdvice(new GlobalExceptionHandler()).build();
    }
    @AfterEach void cleanup(){ upstream.stop(0); executor.shutdownNow(); }
    @Test void validEmptyOccupancyAllowsAvailability() throws Exception {
        mvc.perform(get("/api/v1/showtimes/1/seat-map")).andExpect(status().isOk()).andExpect(jsonPath("$.data.seats[0].runtimeStatus").value("AVAILABLE"));
        mvc.perform(get("/api/v1/showtimes/customer-schedule")).andExpect(status().isOk()).andExpect(jsonPath("$.data[0].availableSeats").value(1));
        mvc.perform(get("/api/v1/showtimes/1/resolve")).andExpect(status().isOk()).andExpect(jsonPath("$.data.availableSeats").value(1));
    }
    @Test void occupiedSeatNeverBecomesAvailable() throws Exception {
        for(String state:List.of("BOOKED","HOLDING","UNAVAILABLE")) {
            body="{\"success\":true,\"data\":[{\"seatId\":1,\"runtimeStatus\":\""+state+"\"}]}";
            mvc.perform(get("/api/v1/showtimes/1/seat-map")).andExpect(status().isOk()).andExpect(jsonPath("$.data.seats[0].runtimeStatus").value(state));
            mvc.perform(get("/api/v1/showtimes/customer-schedule")).andExpect(status().isOk()).andExpect(jsonPath("$.data",hasSize(0)));
        }
    }
    void assertUnavailable() throws Exception {
        for(String path:List.of("/api/v1/showtimes/1/seat-map","/api/v1/showtimes/customer-schedule","/api/v1/showtimes/1/resolve")) {
            mvc.perform(get(path)).andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Seat availability is temporarily unavailable"))
                    .andExpect(content().string(not(containsString("private-test-secret"))))
                    .andExpect(jsonPath("$.data").doesNotExist());
        }
    }
    @Test void refusedConnectionFailsClosedAcrossAllAvailabilityPaths() throws Exception { upstream.stop(0); assertUnavailable(); }
    @Test void upstream403And5xxFailClosed() throws Exception { for(int code:List.of(403,500,503)){status=code; assertUnavailable();} }
    @Test void malformedOrIncompletePayloadFailsClosed() throws Exception {
        for(String payload:List.of("invalid","null","{}","{\"success\":true}","{\"success\":false,\"data\":[]}",
                "{\"success\":true,\"data\":[null]}","{\"success\":true,\"data\":[{\"seatId\":1}]}",
                "{\"success\":true,\"data\":[{\"seatId\":1,\"runtimeStatus\":\"AVAILABLE\"}]}")) {body=payload; assertUnavailable();}
    }
    @Test void readTimeoutFailsClosed() throws Exception { delay=4000; assertUnavailable(); }
}
