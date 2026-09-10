package com.sba301.cinemaai.dto.response.report;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TopMovieResponse {

    private Long movieId;
    private String movieTitle;
    private long ticketsSold;
    private BigDecimal revenue;
}
