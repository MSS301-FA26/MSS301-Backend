package com.cinemaai.booking.client;

import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class CatalogClientReportingShowtimesTest {
    private final RestClient.Builder builder = RestClient.builder().baseUrl("http://catalog.test");
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final CatalogClient client = new CatalogClient("http://catalog.test", "internal-test", 1000, 1000);
    private final LocalDate day = LocalDate.of(2026, 1, 15);

    @BeforeEach
    void configureClient() {
        ReflectionTestUtils.setField(client, "restClient", builder.build());
    }

    @AfterEach
    void verifyRequests() {
        server.verify();
    }

    @Test
    void sendsDateAndCinemaFiltersWithInternalSecretAndReadsCapacity() {
        respond("&cinemaId=7", """
                {"success":true,"data":[{"showtimeId":9,"movieId":5,"movieTitle":"Movie",
                "cinemaId":7,"cinemaName":"Cinema","roomId":3,"roomName":"Room",
                "startTime":"2026-01-15T10:00:00","endTime":"2026-01-15T12:00:00","capacity":80}]}
                """);

        var shows = client.getReportingShowtimes(day, day, 7L);

        assertEquals(1, shows.size());
        assertEquals(9L, shows.getFirst().showtimeId());
        assertEquals(day.atTime(10, 0), shows.getFirst().startTime());
        assertEquals(80, shows.getFirst().capacity());
    }

    @Test
    void omitsCinemaFilterForAllCinemasAndAcceptsEmptyResults() {
        respond("", "{\"success\":true,\"data\":[]}");

        assertTrue(client.getReportingShowtimes(day, day, null).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"success\":false,\"data\":[]}", "{\"success\":true,\"data\":null}"})
    void rejectsInvalidDataInsteadOfReportingZeroCapacity(String body) {
        respond("&cinemaId=7", body);

        assertThrows(IllegalStateException.class, () -> client.getReportingShowtimes(day, day, 7L));
    }

    @Test
    void propagatesCatalogOutage() {
        server.expect(requestTo("http://catalog.test/internal/v1/catalog/showtimes/reporting?from=2026-01-15&to=2026-01-15&cinemaId=7"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThrows(HttpServerErrorException.class, () -> client.getReportingShowtimes(day, day, 7L));
    }

    private void respond(String cinemaFilter, String body) {
        server.expect(requestTo("http://catalog.test/internal/v1/catalog/showtimes/reporting?from=2026-01-15&to=2026-01-15" + cinemaFilter))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Internal-Service-Secret", "internal-test"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
    }
}
