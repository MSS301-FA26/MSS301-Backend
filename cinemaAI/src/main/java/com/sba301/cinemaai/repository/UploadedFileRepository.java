package com.sba301.cinemaai.repository;

import com.sba301.cinemaai.entity.UploadedFile;
import com.sba301.cinemaai.enums.UploadedFileStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UploadedFileRepository extends JpaRepository<UploadedFile, Long> {

    List<UploadedFile> findByStatus(UploadedFileStatus status);

    List<UploadedFile> findByProvider(String provider);
}
