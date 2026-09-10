package com.sba301.cinemaai.dto.response.wishlist;

import com.sba301.cinemaai.entity.Wishlist;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class WishlistResponse {

    private Long id;
    private Long movieId;
    private String movieTitle;
    private String posterUrl;
    private LocalDateTime createdAt;

    public static WishlistResponse from(Wishlist wishlist) {
        return new WishlistResponse(
                wishlist.getId(),
                wishlist.getMovie().getId(),
                wishlist.getMovie().getTitle(),
                wishlist.getMovie().getPosterUrl(),
                wishlist.getCreatedAt()
        );
    }
}
