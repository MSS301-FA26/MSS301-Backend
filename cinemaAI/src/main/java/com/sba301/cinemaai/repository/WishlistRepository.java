package com.sba301.cinemaai.repository;

import com.sba301.cinemaai.entity.Movie;
import com.sba301.cinemaai.entity.User;
import com.sba301.cinemaai.entity.Wishlist;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    List<Wishlist> findByUser(User user);

    Optional<Wishlist> findByUserAndMovie(User user, Movie movie);

    boolean existsByUserAndMovie(User user, Movie movie);

    void deleteByUserAndMovie(User user, Movie movie);
}
