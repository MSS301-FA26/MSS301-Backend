package com.sba301.cinemaai.repository;

import com.sba301.cinemaai.entity.Actor;
import com.sba301.cinemaai.repository.projection.ActorMovieCountProjection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ActorRepository extends JpaRepository<Actor, Long> {

    Optional<Actor> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    List<Actor> findByNameContainingIgnoreCaseOrderByNameAsc(String keyword, Pageable pageable);

    @Query("""
            select actor as actor, count(movieActor) as movieCount
            from Actor actor
            left join MovieActor movieActor on movieActor.actor = actor
            group by actor
            order by lower(actor.name)
            """)
    Page<ActorMovieCountProjection> findAdminWithMovieCount(Pageable pageable);

    @Query("""
            select actor as actor, count(movieActor) as movieCount
            from Actor actor
            left join MovieActor movieActor on movieActor.actor = actor
            where lower(actor.name) like lower(concat('%', :keyword, '%'))
            group by actor
            order by lower(actor.name)
            """)
    Page<ActorMovieCountProjection> searchAdminWithMovieCount(
            @Param("keyword") String keyword,
            Pageable pageable
    );

    @Query("""
            select actor as actor, count(movieActor) as movieCount
            from Actor actor
            join MovieActor movieActor on movieActor.actor = actor
            where movieActor.movie.status <> com.sba301.cinemaai.enums.MovieStatus.INACTIVE
            group by actor
            order by lower(actor.name)
            """)
    Page<ActorMovieCountProjection> findPublicWithMovieCount(Pageable pageable);

    @Query("""
            select actor as actor, count(movieActor) as movieCount
            from Actor actor
            join MovieActor movieActor on movieActor.actor = actor
            where movieActor.movie.status <> com.sba301.cinemaai.enums.MovieStatus.INACTIVE
              and lower(actor.name) like lower(concat('%', :keyword, '%'))
            group by actor
            order by lower(actor.name)
            """)
    Page<ActorMovieCountProjection> searchPublicWithMovieCount(
            @Param("keyword") String keyword,
            Pageable pageable
    );

    @Query("""
            select actor as actor, count(movieActor) as movieCount
            from Actor actor
            join MovieActor movieActor on movieActor.actor = actor
            where actor.id = :actorId
              and movieActor.movie.status <> com.sba301.cinemaai.enums.MovieStatus.INACTIVE
            group by actor
            """)
    Optional<ActorMovieCountProjection> findPublicWithMovieCountById(@Param("actorId") Long actorId);

    @Query("""
            select actor as actor, count(movieActor) as movieCount
            from Actor actor
            left join MovieActor movieActor on movieActor.actor = actor
            where actor.id in :actorIds
            group by actor
            """)
    List<ActorMovieCountProjection> findWithMovieCountByIdIn(@Param("actorIds") List<Long> actorIds);
}
