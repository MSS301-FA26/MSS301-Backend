package com.sba301.cinemaai.service.impl;


import com.sba301.cinemaai.service.WishlistService;
import com.sba301.cinemaai.dto.request.wishlist.WishlistCreateRequest;
import com.sba301.cinemaai.dto.response.wishlist.WishlistResponse;
import com.sba301.cinemaai.entity.Movie;
import com.sba301.cinemaai.entity.User;
import com.sba301.cinemaai.entity.Wishlist;
import com.sba301.cinemaai.exception.ConflictException;
import com.sba301.cinemaai.exception.NotFoundException;
import com.sba301.cinemaai.repository.MovieRepository;
import com.sba301.cinemaai.repository.UserRepository;
import com.sba301.cinemaai.repository.WishlistRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WishlistServiceImpl implements WishlistService {

    private final WishlistRepository wishlistRepository;
    private final UserRepository userRepository;
    private final MovieRepository movieRepository;

    @Transactional
    public WishlistResponse addToWishlist(String email, WishlistCreateRequest request) {
        User user = resolveUser(email);
        Movie movie = resolveMovie(request.getMovieId());

        if (wishlistRepository.existsByUserAndMovie(user, movie)) {
            throw new ConflictException("Movie is already in your wishlist");
        }

        Wishlist saved = wishlistRepository.save(new Wishlist(user, movie));
        return WishlistResponse.from(saved);
    }

    @Transactional
    public void removeFromWishlist(String email, Long movieId) {
        User user = resolveUser(email);
        Movie movie = resolveMovie(movieId);

        Wishlist wishlist = wishlistRepository.findByUserAndMovie(user, movie)
                .orElseThrow(() -> new NotFoundException("Movie not found in your wishlist"));

        wishlistRepository.delete(wishlist);
    }

    @Transactional(readOnly = true)
    public List<WishlistResponse> getWishlist(String email) {
        User user = resolveUser(email);
        return wishlistRepository.findByUser(user)
                .stream()
                .map(WishlistResponse::from)
                .toList();
    }

    private User resolveUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found: " + email));
    }

    private Movie resolveMovie(Long movieId) {
        return movieRepository.findById(movieId)
                .orElseThrow(() -> new NotFoundException("Movie not found: " + movieId));
    }
}
