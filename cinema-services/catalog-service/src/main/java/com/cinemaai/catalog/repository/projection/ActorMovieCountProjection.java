package com.cinemaai.catalog.repository.projection;

import com.cinemaai.catalog.entity.Actor;

public interface ActorMovieCountProjection {

    Actor getActor();

    long getMovieCount();
}
