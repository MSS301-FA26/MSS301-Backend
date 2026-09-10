package com.sba301.cinemaai.entity;

import com.sba301.cinemaai.enums.UploadedFileStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Entity
@Table(name = "uploaded_files")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UploadedFile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @Column(name = "stored_filename", nullable = false)
    private String storedFilename;

    @Column(nullable = false, length = 100)
    private String provider;

    @Column(nullable = false, length = 500)
    private String url;

    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by_user_id")
    private User uploadedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Setter
    private UploadedFileStatus status = UploadedFileStatus.ACTIVE;

    public UploadedFile(
            String originalFilename,
            String storedFilename,
            String provider,
            String url,
            String mimeType,
            long fileSize,
            User uploadedBy
    ) {
        this.originalFilename = originalFilename;
        this.storedFilename = storedFilename;
        this.provider = provider;
        this.url = url;
        this.mimeType = mimeType;
        this.fileSize = fileSize;
        this.uploadedBy = uploadedBy;
    }

}
