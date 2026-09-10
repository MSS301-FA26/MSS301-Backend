package com.sba301.cinemaai.movie;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class MovieCatalogEndpointInventoryTests {

    @Test
    void adminMovieControllerShouldExposePhase3MovieManagementEndpoints() throws IOException {
        String controller = readSource("controller/AdminMovieController.java");

        List<String> endpoints = List.of(
                "@requestmapping(\"/api/v1/admin/movies\")",
                "@getmapping",
                "@getmapping(\"/{movieid}\")",
                "@postmapping",
                "@putmapping(\"/{movieid}\")",
                "@patchmapping(\"/{movieid}/status\")",
                "@deletemapping(\"/{movieid}\")"
        );

        for (String endpoint : endpoints) {
            assertTrue(controller.contains(endpoint), "AdminMovieController must expose " + endpoint);
        }
    }

    @Test
    void publicMovieAndActorControllersShouldExposeCatalogEndpoints() throws IOException {
        String movieController = readSource("controller/MovieController.java");
        assertTrue(movieController.contains("@requestmapping(\"/api/v1/movies\")"));
        assertTrue(movieController.contains("@getmapping"));
        assertTrue(movieController.contains("@getmapping(\"/{movieid}\")"));

        String actorController = readSource("controller/ActorController.java");
        assertTrue(actorController.contains("@requestmapping(\"/api/v1/actors\")"));
        assertTrue(actorController.contains("@getmapping"));
        assertTrue(actorController.contains("@getmapping(\"/{actorid}\")"));
        assertTrue(actorController.contains("@getmapping(\"/{actorid}/movies\")"));
    }

    @Test
    void adminGenreAndActorControllersShouldExposeCrudEndpoints() throws IOException {
        String genreController = readSource("controller/AdminGenreController.java");
        assertTrue(genreController.contains("@requestmapping(\"/api/v1/admin/genres\")"));
        assertTrue(genreController.contains("@postmapping"));
        assertTrue(genreController.contains("@putmapping(\"/{genreid}\")"));
        assertTrue(genreController.contains("@deletemapping(\"/{genreid}\")"));

        String actorController = readSource("controller/AdminActorController.java");
        assertTrue(actorController.contains("@requestmapping(\"/api/v1/admin/actors\")"));
        assertTrue(actorController.contains("@getmapping"));
        assertTrue(actorController.contains("@postmapping"));
        assertTrue(actorController.contains("@putmapping(\"/{actorid}\")"));
        assertTrue(actorController.contains("@deletemapping(\"/{actorid}\")"));
    }

    @Test
    void movieRequestsShouldRequireActorIdsAndMainActorIds() throws IOException {
        String createRequest = readSource("dto/request/movie/MovieCreateRequest.java");
        String updateRequest = readSource("dto/request/movie/MovieUpdateRequest.java");

        for (String request : List.of(createRequest, updateRequest)) {
            assertTrue(request.contains("list<long> genreids"));
            assertTrue(request.contains("@notempty(message = \"actor ids are required\")"));
            assertTrue(request.contains("list<long> actorids"));
            assertTrue(request.contains("@notempty(message = \"main actor ids are required\")"));
            assertTrue(request.contains("list<long> mainactorids"));
        }
    }

    @Test
    void actorAndMovieStatusConstraintsShouldMatchPhase3Rules() throws IOException {
        String actorRequest = readSource("dto/request/movie/ActorRequest.java");
        assertTrue(actorRequest.contains("@size(max = 50"));

        String movieStatus = readSource("enums/MovieStatus.java");
        for (String status : List.of("upcoming", "now_showing", "ended", "inactive")) {
            assertTrue(movieStatus.contains(status), "MovieStatus must include " + status);
        }
    }

    private String readSource(String relativePath) throws IOException {
        return Files.readString(Path.of("src/main/java/com/sba301/cinemaai").resolve(relativePath)).toLowerCase();
    }
}
