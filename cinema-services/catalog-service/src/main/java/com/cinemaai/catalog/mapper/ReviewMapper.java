package com.cinemaai.catalog.mapper;

import com.cinemaai.catalog.dto.response.ReviewReportResponse;
import com.cinemaai.catalog.dto.response.ReviewResponse;
import com.cinemaai.catalog.entity.Review;
import com.cinemaai.catalog.entity.ReviewReport;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReviewMapper {

    @Mapping(target = "movieId", source = "movie.id")
    @Mapping(target = "movieTitle", source = "movie.title")
    @Mapping(target = "moviePosterUrl", source = "movie.posterUrl")
    @Mapping(target = "status", expression = "java(review.getStatus() != null ? review.getStatus().name() : null)")
    ReviewResponse toResponse(Review review);

    @Mapping(target = "reviewId", source = "review.id")
    @Mapping(target = "reason", expression = "java(report.getReason() != null ? report.getReason().name() : null)")
    @Mapping(target = "reasonLabel", expression = "java(report.getReason() != null ? report.getReason().getDescriptionVi() : null)")
    @Mapping(target = "status", expression = "java(report.getStatus() != null ? report.getStatus().name() : null)")
    ReviewReportResponse toReportResponse(ReviewReport report);
}
