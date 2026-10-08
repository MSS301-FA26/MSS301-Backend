package com.cinemaai.catalog.service;

import com.cinemaai.catalog.dto.request.movie.DirectorRequest;
import com.cinemaai.catalog.dto.response.PageResponse;
import com.cinemaai.catalog.dto.response.movie.DirectorResponse;
import com.cinemaai.catalog.entity.Director;
import com.cinemaai.catalog.exception.ConflictException;
import com.cinemaai.catalog.exception.NotFoundException;
import com.cinemaai.catalog.repository.DirectorRepository;
import com.cinemaai.catalog.repository.MovieDirectorRepository;
import com.cinemaai.catalog.service.impl.DirectorServiceImpl;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DirectorServiceTest {

    @Mock
    private DirectorRepository directorRepository;

    @Mock
    private MovieDirectorRepository movieDirectorRepository;

    private DirectorServiceImpl directorService;

    @BeforeEach
    void setUp() {
        directorService = new DirectorServiceImpl(directorRepository, movieDirectorRepository);
    }

    @Test
    void createDirector_success() {
        DirectorRequest request = new DirectorRequest("Christopher Nolan", "Famous director", "https://avatar.url");
        when(directorRepository.existsByNameIgnoreCase("Christopher Nolan")).thenReturn(false);
        when(directorRepository.save(any(Director.class))).thenAnswer(invocation -> {
            Director d = invocation.getArgument(0);
            return new Director(1L, d.getName(), d.getBiography(), d.getAvatarUrl());
        });

        DirectorResponse response = directorService.create(request);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("Christopher Nolan", response.name());
        verify(directorRepository).save(any(Director.class));
    }

    @Test
    void createDirector_duplicateName_throwsConflict() {
        DirectorRequest request = new DirectorRequest("Christopher Nolan", "Bio", null);
        when(directorRepository.existsByNameIgnoreCase("Christopher Nolan")).thenReturn(true);

        assertThrows(ConflictException.class, () -> directorService.create(request));
        verify(directorRepository, never()).save(any());
    }

    @Test
    void updateDirector_success() {
        Director existing = new Director(1L, "Old Name", "Old Bio", "old.jpg");
        when(directorRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(directorRepository.existsByNameIgnoreCaseAndIdNot("New Name", 1L)).thenReturn(false);
        when(directorRepository.save(any(Director.class))).thenReturn(existing);

        DirectorRequest request = new DirectorRequest("New Name", "New Bio", "new.jpg");
        DirectorResponse response = directorService.update(1L, request);

        assertNotNull(response);
        assertEquals("New Name", existing.getName());
        assertEquals("New Bio", existing.getBiography());
    }

    @Test
    void deleteDirector_whenLinkedToMovie_throwsConflict() {
        Director existing = new Director(1L, "Nolan", "Bio", null);
        when(directorRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(movieDirectorRepository.existsByDirectorId(1L)).thenReturn(true);

        ConflictException ex = assertThrows(ConflictException.class, () -> directorService.delete(1L));
        assertTrue(ex.getMessage().contains("đang được liên kết"));
        verify(directorRepository, never()).delete(any());
    }

    @Test
    void searchDirectors_paged() {
        Director d1 = new Director(1L, "Nolan", "Bio", null);
        Page<Director> page = new PageImpl<>(List.of(d1));
        when(directorRepository.findAll(any(Pageable.class))).thenReturn(page);

        PageResponse<DirectorResponse> result = directorService.searchDirectors(null, 0, 10);
        assertEquals(1, result.items().size());
        assertEquals("Nolan", result.items().get(0).name());
    }

    @Test
    void getDirector_notFound_throwsException() {
        when(directorRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> directorService.getDirector(999L));
    }
}
