package com.cinemaai.catalog.service.impl;

import com.cinemaai.catalog.dto.request.movie.DirectorRequest;
import com.cinemaai.catalog.dto.response.PageResponse;
import com.cinemaai.catalog.dto.response.movie.DirectorResponse;
import com.cinemaai.catalog.entity.Director;
import com.cinemaai.catalog.exception.ConflictException;
import com.cinemaai.catalog.exception.NotFoundException;
import com.cinemaai.catalog.repository.DirectorRepository;
import com.cinemaai.catalog.repository.MovieDirectorRepository;
import com.cinemaai.catalog.service.DirectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DirectorServiceImpl implements DirectorService {

    private final DirectorRepository directorRepository;
    private final MovieDirectorRepository movieDirectorRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DirectorResponse> searchDirectors(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        Page<Director> p;
        if (keyword != null && !keyword.isBlank()) {
            p = directorRepository.findByNameContainingIgnoreCaseOrderByNameAsc(keyword.trim(), pageable);
        } else {
            p = directorRepository.findAll(pageable);
        }
        return PageResponse.from(p.map(DirectorResponse::fromEntity));
    }

    @Override
    @Transactional(readOnly = true)
    public DirectorResponse getDirector(Long id) {
        return DirectorResponse.fromEntity(findById(id));
    }

    @Override
    @Transactional
    public DirectorResponse create(DirectorRequest request) {
        String name = request.name().trim();
        if (directorRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("Đạo diễn với tên '" + name + "' đã tồn tại trong hệ thống.");
        }
        Director director = new Director(name, request.biography(), request.avatarUrl());
        Director saved = directorRepository.save(director);
        log.info("Created director id={} name='{}'", saved.getId(), saved.getName());
        return DirectorResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public DirectorResponse update(Long id, DirectorRequest request) {
        Director director = findById(id);
        String name = request.name().trim();
        if (directorRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ConflictException("Đạo diễn với tên '" + name + "' đã tồn tại trong hệ thống.");
        }
        director.setName(name);
        director.setBiography(request.biography());
        director.setAvatarUrl(request.avatarUrl());
        Director updated = directorRepository.save(director);
        log.info("Updated director id={} name='{}'", updated.getId(), updated.getName());
        return DirectorResponse.fromEntity(updated);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Director director = findById(id);
        if (movieDirectorRepository.existsByDirectorId(id)) {
            throw new ConflictException("Không thể xóa đạo diễn đang được liên kết với phim.");
        }
        directorRepository.delete(director);
        log.info("Deleted director id={}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public Director findById(Long id) {
        return directorRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Đạo diễn ID #" + id + " không tồn tại"));
    }
}
