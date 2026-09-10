package com.sba301.cinemaai.repository.projection;

import com.sba301.cinemaai.entity.Actor;

public interface ActorMovieCountProjection {

    Actor getActor();

    long getMovieCount();
}
