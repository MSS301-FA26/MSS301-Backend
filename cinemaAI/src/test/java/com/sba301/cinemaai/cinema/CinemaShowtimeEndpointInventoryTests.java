package com.sba301.cinemaai.cinema;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class CinemaShowtimeEndpointInventoryTests {

    @Test
    void cinemaControllersShouldExposeSingleCinemaEndpoints() throws IOException {
        String adminCinema = readSource("controller/AdminCinemaController.java");
        for (String endpoint : List.of(
                "@getmapping(\"/api/v1/admin/cinema\")",
                "@postmapping(\"/api/v1/admin/cinema\")",
                "@putmapping(\"/api/v1/admin/cinema\")",
                "@patchmapping(\"/api/v1/admin/cinema/status\")",
                "@deletemapping(\"/api/v1/admin/cinema\")"
        )) {
            assertTrue(adminCinema.contains(endpoint), "AdminCinemaController must expose " + endpoint);
        }

        String publicCinema = readSource("controller/CinemaController.java");
        assertTrue(publicCinema.contains("@getmapping({\"/api/v1/cinema\", \"/api/v1/cinemas\"})"));
        assertTrue(publicCinema.contains("@getmapping(\"/api/v1/cinema/rooms\")"));
        assertTrue(publicCinema.contains("@getmapping(\"/api/v1/cinemas/{cinemaid}/rooms\")"));
    }

    @Test
    void roomControllerShouldExposeRoomAndSeatLayoutEndpoints() throws IOException {
        String controller = readSource("controller/AdminRoomController.java");
        for (String endpoint : List.of(
                "@requestmapping(\"/api/v1/admin/rooms\")",
                "@getmapping",
                "@getmapping(\"/{roomid}\")",
                "@getmapping(\"/{roomid}/seats\")",
                "@getmapping(\"/seats/{seatid}\")",
                "@postmapping",
                "@putmapping(\"/{roomid}\")",
                "@patchmapping(\"/{roomid}/status\")",
                "@postmapping(\"/{roomid}/seats/generate\")",
                "@putmapping(\"/{roomid}/seats\")",
                "@putmapping(\"/seats/{seatid}\")",
                "@deletemapping(\"/seats/{seatid}\")"
        )) {
            assertTrue(controller.contains(endpoint), "AdminRoomController must expose " + endpoint);
        }
    }

    @Test
    void showtimeControllersShouldExposeAdminAndPublicEndpoints() throws IOException {
        String adminShowtime = readSource("controller/AdminShowtimeController.java");
        for (String endpoint : List.of(
                "@requestmapping(\"/api/v1/admin/showtimes\")",
                "@getmapping",
                "@getmapping(\"/{showtimeid}\")",
                "@getmapping(\"/{showtimeid}/seat-map\")",
                "@postmapping",
                "@postmapping(\"/bulk\")",
                "@putmapping(\"/{showtimeid}\")",
                "@patchmapping(\"/{showtimeid}/status\")",
                "@deletemapping(\"/{showtimeid}\")"
        )) {
            assertTrue(adminShowtime.contains(endpoint), "AdminShowtimeController must expose " + endpoint);
        }

        String publicShowtime = readSource("controller/ShowtimeController.java");
        assertTrue(publicShowtime.contains("@requestmapping(\"/api/v1/showtimes\")"));
        assertTrue(publicShowtime.contains("@getmapping"));
        assertTrue(publicShowtime.contains("@getmapping(\"/{showtimeid}\")"));
        assertTrue(publicShowtime.contains("@getmapping(\"/{showtimeid}/seat-map\")"));
    }

    @Test
    void seatAndShowtimeRequestsShouldContainPhase4Fields() throws IOException {
        String seatLayout = readSource("dto/request/cinema/SeatLayoutRequest.java");
        assertTrue(seatLayout.contains("seattype defaultseattype"));
        assertTrue(seatLayout.contains("seatrowgenerationrequest"));

        String seatRow = readSource("dto/request/cinema/SeatRowGenerationRequest.java");
        for (String field : List.of("string rowlabel", "int displayorder", "int startcolumn", "seattype seattype", "list<@min")) {
            assertTrue(seatRow.contains(field), "SeatRowGenerationRequest must include " + field);
        }

        String showtime = readSource("dto/request/cinema/ShowtimeRequest.java");
        for (String field : List.of(
                "long movieid",
                "long roomid",
                "localdatetime starttime",
                "bigdecimal baseprice",
                "showtimestatus status"
        )) {
            assertTrue(showtime.contains(field), "ShowtimeRequest must include " + field);
        }

        String bulkShowtime = readSource("dto/request/cinema/BulkShowtimeRequest.java");
        assertTrue(bulkShowtime.contains("list<slot> slots"));
        assertTrue(bulkShowtime.contains("showtimestatus defaultstatus"));
    }

    private String readSource(String relativePath) throws IOException {
        return Files.readString(Path.of("src/main/java/com/sba301/cinemaai").resolve(relativePath)).toLowerCase();
    }
}
