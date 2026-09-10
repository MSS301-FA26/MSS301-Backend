package com.sba301.cinemaai.service;

import com.sba301.cinemaai.dto.request.wishlist.WishlistCreateRequest;
import com.sba301.cinemaai.dto.response.wishlist.WishlistResponse;
import java.util.List;

public interface WishlistService {

        public WishlistResponse addToWishlist(String email, WishlistCreateRequest request);

        public void removeFromWishlist(String email, Long movieId);

        public List<WishlistResponse> getWishlist(String email);
}
