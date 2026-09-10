package com.sba301.cinemaai.seeder;

import com.sba301.cinemaai.entity.Actor;
import com.sba301.cinemaai.entity.Movie;
import com.sba301.cinemaai.entity.MovieActor;
import com.sba301.cinemaai.entity.MovieGenre;
import com.sba301.cinemaai.enums.AgeRating;
import com.sba301.cinemaai.enums.MovieStatus;
import com.sba301.cinemaai.repository.ActorRepository;
import com.sba301.cinemaai.repository.GenreRepository;
import com.sba301.cinemaai.repository.MovieActorRepository;
import com.sba301.cinemaai.repository.MovieGenreRepository;
import com.sba301.cinemaai.repository.MovieRepository;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(30)
@RequiredArgsConstructor
public class MovieSeeder implements Seeder {

    private static final String CLD_IMG = "https://res.cloudinary.com/dmcodhbcc/image/upload/";
    private static final String CLD_VID = "https://res.cloudinary.com/dmcodhbcc/video/upload/";

    private final MovieRepository movieRepository;
    private final GenreRepository genreRepository;
    private final MovieGenreRepository movieGenreRepository;
    private final ActorRepository actorRepository;
    private final MovieActorRepository movieActorRepository;

    @Override
    @Transactional
    public void seed() {
        LocalDate today = LocalDate.now();

        seedMovie(new MovieSeed(
                "Interstellar",
                "Khi Trái Đất dần trở nên không thể sống nổi, một nhóm phi hành gia du hành xuyên hố sâu không gian để tìm ngôi nhà mới cho nhân loại — và đối mặt với cái giá của thời gian, khoảng cách và tình thân.",
                169, MovieStatus.NOW_SHOWING, today.minusDays(14), today.plusDays(30),
                "English", "Vietnamese", "13+", "Christopher Nolan",
                "Sci-Fi,Adventure,Drama",
                CLD_IMG + "v1784275441/cinemams/posters/interstellar.jpg", CLD_VID + "v1784275531/cinemams/trailers/interstellar.mp4",
                List.of("Matthew McConaughey", "Anne Hathaway", "Jessica Chastain")
        ));
        seedMovie(new MovieSeed(
                "Parasite",
                "Gia đình nghèo họ Kim lần lượt len lỏi vào làm thuê cho gia đình giàu có họ Park. Một kế hoạch hoàn hảo — cho đến khi bí mật dưới căn hầm hé lộ. Phim Hàn Quốc đầu tiên thắng Oscar Phim hay nhất.",
                132, MovieStatus.NOW_SHOWING, today.minusDays(7), today.plusDays(30),
                "Korean", "Vietnamese", "16+", "Bong Joon-ho",
                "Drama,Thriller,Comedy",
                CLD_IMG + "v1784275442/cinemams/posters/parasite.jpg", CLD_VID + "v1784275537/cinemams/trailers/parasite.mp4",
                List.of("Song Kang-ho", "Lee Sun-kyun", "Cho Yeo-jeong")
        ));
        seedMovie(new MovieSeed(
                "The Grand Budapest Hotel",
                "Người gác cửa huyền thoại Gustave H. và cậu bé sảnh Zero Moustafa bị cuốn vào vụ trộm bức tranh vô giá và cuộc tranh giành gia sản khổng lồ, giữa lòng châu Âu những năm 1930 đầy biến động.",
                99, MovieStatus.NOW_SHOWING, today.minusDays(10), today.plusDays(25),
                "English", "Vietnamese", "13+", "Wes Anderson",
                "Comedy,Drama",
                CLD_IMG + "v1784275444/cinemams/posters/grand-budapest.jpg", CLD_VID + "v1784275608/cinemams/trailers/grand-budapest.mp4",
                List.of("Ralph Fiennes", "Tony Revolori", "Saoirse Ronan")
        ));
        seedMovie(new MovieSeed(
                "Spider-Man: Into the Spider-Verse",
                "Cậu học sinh Miles Morales trở thành Người Nhện của vũ trụ mình và phải hợp sức cùng các Spider-hero từ những chiều không gian khác để cứu mọi thực tại. Oscar Phim hoạt hình xuất sắc nhất.",
                117, MovieStatus.NOW_SHOWING, today.minusDays(5), today.plusDays(40),
                "English", "Vietnamese", "P", "Bob Persichetti",
                "Animation,Action,Adventure",
                CLD_IMG + "v1784275445/cinemams/posters/spider-verse.jpg", CLD_VID + "v1784275545/cinemams/trailers/spider-verse.mp4",
                List.of("Shameik Moore", "Jake Johnson", "Hailee Steinfeld")
        ));
        seedMovie(new MovieSeed(
                "The Dark Knight",
                "Batman đối đầu Joker — kẻ phản diện hỗn loạn muốn nhấn chìm Gotham. Màn trình diễn để đời của Heath Ledger trong tượng đài phim siêu anh hùng của Christopher Nolan.",
                152, MovieStatus.NOW_SHOWING, today.minusDays(12), today.plusDays(20),
                "English", "Vietnamese", "16+", "Christopher Nolan",
                "Action,Crime,Drama",
                CLD_IMG + "v1784275446/cinemams/posters/dark-knight.jpg", CLD_VID + "v1784275554/cinemams/trailers/dark-knight.mp4",
                List.of("Christian Bale", "Heath Ledger", "Aaron Eckhart")
        ));
        seedMovie(new MovieSeed(
                "Black Panther",
                "T'Challa trở về Wakanda kế vị ngai vàng, nhưng phải bảo vệ vương quốc công nghệ vibranium trước kẻ thách thức mang mối thù quá khứ.",
                134, MovieStatus.NOW_SHOWING, today.minusDays(9), today.plusDays(28),
                "English", "Vietnamese", "13+", "Ryan Coogler",
                "Action,Sci-Fi,Adventure",
                CLD_IMG + "v1784275448/cinemams/posters/black-panther.jpg", CLD_VID + "v1784275450/cinemams/trailers/black-panther.mp4",
                List.of("Chadwick Boseman", "Michael B. Jordan", "Lupita Nyong'o")
        ));
        seedMovie(new MovieSeed(
                "The Green Mile",
                "Trong khu tử tù năm 1935, cai ngục Paul Edgecomb gặp John Coffey — người tù khổng lồ mang năng lực chữa lành kỳ diệu và một bản án oan nghiệt.",
                189, MovieStatus.NOW_SHOWING, today.minusDays(6), today.plusDays(24),
                "English", "Vietnamese", "16+", "Frank Darabont",
                "Drama,Crime",
                CLD_IMG + "v1784275451/cinemams/posters/green-mile.jpg", CLD_VID + "v1784275452/cinemams/trailers/green-mile.mp4",
                List.of("Tom Hanks", "Michael Clarke Duncan", "David Morse")
        ));
        seedMovie(new MovieSeed(
                "Inception",
                "Dom Cobb — chuyên gia đánh cắp bí mật từ giấc mơ — nhận nhiệm vụ ngược đời: gieo một ý tưởng vào tâm trí người khác. Ranh giới giữa mơ và thực dần sụp đổ.",
                148, MovieStatus.NOW_SHOWING, today.minusDays(3), today.plusDays(35),
                "English", "Vietnamese", "13+", "Christopher Nolan",
                "Sci-Fi,Action,Thriller",
                CLD_IMG + "v1784275454/cinemams/posters/inception.jpg", CLD_VID + "v1784275455/cinemams/trailers/inception.mp4",
                List.of("Leonardo DiCaprio", "Joseph Gordon-Levitt", "Elliot Page")
        ));
        seedMovie(new MovieSeed(
                "Avengers: Endgame",
                "Sau cú búng tay của Thanos, các Avengers còn lại dốc toàn lực cho ván cược cuối cùng xuyên thời gian để hồi sinh một nửa vũ trụ.",
                181, MovieStatus.NOW_SHOWING, today.minusDays(2), today.plusDays(38),
                "English", "Vietnamese", "13+", "Anthony Russo, Joe Russo",
                "Action,Sci-Fi,Adventure",
                CLD_IMG + "v1784275457/cinemams/posters/endgame.jpg", CLD_VID + "v1784275459/cinemams/trailers/endgame.mp4",
                List.of("Robert Downey Jr.", "Chris Evans", "Scarlett Johansson")
        ));
        seedMovie(new MovieSeed(
                "La La Land",
                "Chàng nhạc công jazz Sebastian và nàng diễn viên đầy tham vọng Mia yêu nhau giữa Los Angeles — nơi giấc mơ và tình yêu không phải lúc nào cũng chung nhịp. 6 giải Oscar.",
                128, MovieStatus.UPCOMING, today.plusDays(7), today.plusDays(50),
                "English", "Vietnamese", "P", "Damien Chazelle",
                "Romance,Drama,Musical",
                CLD_IMG + "v1784275461/cinemams/posters/la-la-land.jpg", CLD_VID + "v1784275465/cinemams/trailers/la-la-land.mp4",
                List.of("Ryan Gosling", "Emma Stone", "John Legend")
        ));
        seedMovie(new MovieSeed(
                "Coco",
                "Cậu bé Miguel mê đàn hát lạc vào Vùng Đất Người Chết rực rỡ sắc màu, nơi cậu khám phá bí mật gia đình và ý nghĩa thực sự của việc được nhớ đến. Oscar Phim hoạt hình xuất sắc nhất.",
                105, MovieStatus.UPCOMING, today.plusDays(10), today.plusDays(55),
                "English", "Vietnamese", "P", "Lee Unkrich",
                "Animation,Family,Adventure",
                CLD_IMG + "v1784275467/cinemams/posters/coco.jpg", CLD_VID + "v1784275563/cinemams/trailers/coco.mp4",
                List.of("Anthony Gonzalez", "Gael García Bernal", "Benjamin Bratt")
        ));
        seedMovie(new MovieSeed(
                "Joker",
                "Arthur Fleck — nghệ sĩ hài thất bại bị xã hội ruồng bỏ — trượt dài vào hỗn loạn và điên loạn, trở thành biểu tượng tội phạm của Gotham. Giải Sư Tử Vàng Venice và Oscar Nam chính.",
                122, MovieStatus.UPCOMING, today.plusDays(14), today.plusDays(60),
                "English", "Vietnamese", "18+", "Todd Phillips",
                "Drama,Thriller,Crime",
                CLD_IMG + "v1784275468/cinemams/posters/joker.jpg", CLD_VID + "v1784275469/cinemams/trailers/joker.mp4",
                List.of("Joaquin Phoenix", "Robert De Niro", "Zazie Beetz")
        ));
    }

    private void seedMovie(MovieSeed seed) {
        Movie movie = movieRepository.findByTitle(seed.title())
                .orElseGet(() -> movieRepository.save(new Movie(seed.title(), seed.durationMinutes(), seed.status())));
        movie.setTitle(seed.title());
        movie.setDurationMinutes(seed.durationMinutes());
        movie.setDescription(seed.description());
        movie.setReleaseDate(seed.releaseDate());
        movie.setEndDate(seed.endDate());
        movie.setLanguage(seed.language());
        movie.setSubtitleLanguage(seed.subtitleLanguage());
        movie.setAgeRating(AgeRating.from(seed.ageRating()));
        movie.setDirector(seed.director());
        movie.setMainActors(String.join(", ", seed.actorNames()));
        movie.setCastList(String.join(", ", seed.actorNames()));
        movie.setStatus(seed.status());
        // Media fields are only defaulted when blank — admin uploads (e.g. Cloudinary trailers)
        // must survive BE restarts instead of being stomped back to the seed values.
        if (movie.getPosterUrl() == null || movie.getPosterUrl().isBlank()) {
            movie.setPosterUrl(seed.posterUrl());
        }
        if (movie.getAvatarUrl() == null || movie.getAvatarUrl().isBlank()) {
            movie.setAvatarUrl(seed.posterUrl());
        }
        if (movie.getTrailerUrl() == null || movie.getTrailerUrl().isBlank()) {
            movie.setTrailerUrl(seed.trailerUrl());
        }

        for (String genreName : seed.genreNames().split(",")) {
            genreRepository.findByName(genreName.trim()).ifPresent(genre -> {
                if (!movieGenreRepository.existsByMovieAndGenre(movie, genre)) {
                    movieGenreRepository.save(new MovieGenre(movie, genre));
                }
            });
        }

        for (String actorName : seed.actorNames()) {
            Actor actor = actorRepository.findByNameIgnoreCase(actorName)
                    .orElseGet(() -> actorRepository.save(new Actor(actorName, null, null)));
            if (!movieActorRepository.existsByMovieAndActor(movie, actor)) {
                movieActorRepository.save(new MovieActor(movie, actor, true));
            }
        }
    }

    private record MovieSeed(
            String title,
            String description,
            int durationMinutes,
            MovieStatus status,
            LocalDate releaseDate,
            LocalDate endDate,
            String language,
            String subtitleLanguage,
            String ageRating,
            String director,
            String genreNames,
            String posterUrl,
            String trailerUrl,
            List<String> actorNames
    ) {
    }
}
