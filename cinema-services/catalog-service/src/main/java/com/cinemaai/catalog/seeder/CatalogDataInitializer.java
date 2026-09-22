package com.cinemaai.catalog.seeder;

import com.cinemaai.catalog.entity.Actor;
import com.cinemaai.catalog.entity.Genre;
import com.cinemaai.catalog.entity.Movie;
import com.cinemaai.catalog.entity.MovieActor;
import com.cinemaai.catalog.entity.MovieGenre;
import com.cinemaai.catalog.enums.AgeRating;
import com.cinemaai.catalog.enums.MovieStatus;
import com.cinemaai.catalog.repository.ActorRepository;
import com.cinemaai.catalog.repository.GenreRepository;
import com.cinemaai.catalog.repository.MovieActorRepository;
import com.cinemaai.catalog.repository.MovieGenreRepository;
import com.cinemaai.catalog.repository.MovieRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tự động nạp dữ liệu mẫu ban đầu cho catalog-service:
 * - 10 Thể loại phim
 * - 15 Diễn viên nổi tiếng
 * - 12 Phim bom tấn chất lượng cao (đang chiếu và sắp chiếu)
 * Idempotent: Kiểm tra theo tên, không chèn trùng lặp khi khởi động lại.
 */
@Slf4j
@Component
@Profile("!test")
@Order(Ordered.LOWEST_PRECEDENCE)
@RequiredArgsConstructor
public class CatalogDataInitializer implements CommandLineRunner {

    private final GenreRepository genreRepository;
    private final ActorRepository actorRepository;
    private final MovieRepository movieRepository;
    private final MovieGenreRepository movieGenreRepository;
    private final MovieActorRepository movieActorRepository;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Bắt đầu kiểm tra và khởi tạo dữ liệu mẫu Catalog Service...");
        Map<String, Genre> genreMap = seedGenres();
        Map<String, Actor> actorMap = seedActors();
        seedMovies(genreMap, actorMap);
        log.info("Hoàn tất khởi tạo dữ liệu mẫu Catalog Service thành công! Tổng số phim hiện tại: {}", movieRepository.count());
    }

    private Map<String, Genre> seedGenres() {
        Map<String, Genre> map = new HashMap<>();
        List<String[]> genreDefs = List.of(
                new String[]{"Hành động", "Những pha hành động nghẹt thở, rượt đuổi kịch tính và kỹ xảo đỉnh cao"},
                new String[]{"Phiêu lưu", "Những chuyến hành trình khám phá thế giới mới mẻ đầy bất ngờ"},
                new String[]{"Hoạt hình", "Phim hoạt hình dành cho mọi lứa tuổi với hình ảnh tuyệt mỹ"},
                new String[]{"Kinh dị", "Những câu chuyện rùng rợn, giật gân thách thức sự can đảm"},
                new String[]{"Tình cảm", "Những câu chuyện tình lãng mạn, sâu lắng và giàu cảm xúc"},
                new String[]{"Khoa học viễn tưởng", "Khám phá tương lai, công nghệ và vũ trụ vô tận"},
                new String[]{"Hài hước", "Những tình huống dí dỏm mang lại tiếng cười sảng khoái"},
                new String[]{"Tâm lý - Kịch tính", "Đào sâu vào nội tâm con người và những nút thắt éo le"},
                new String[]{"Gia đình", "Tác phẩm ấm áp gắn kết tình cảm giữa các thành viên trong gia đình"},
                new String[]{"Sử thi", "Những bối cảnh lịch sử hoành tráng tái hiện các thời kỳ hào hùng"}
        );

        for (String[] def : genreDefs) {
            String name = def[0];
            String desc = def[1];
            Genre genre = genreRepository.findByName(name)
                    .orElseGet(() -> genreRepository.save(new Genre(name, desc)));
            map.put(name, genre);
        }
        return map;
    }

    private Map<String, Actor> seedActors() {
        Map<String, Actor> map = new HashMap<>();
        List<String[]> actorDefs = List.of(
                new String[]{"Trấn Thành", "Đạo diễn kiêm diễn viên đa tài hàng đầu điện ảnh Việt Nam", "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?auto=format&fit=crop&w=600&q=80"},
                new String[]{"Phương Anh Đào", "Ngọc nữ sáng giá của màn ảnh rộng Việt Nam", "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=600&q=80"},
                new String[]{"Tuấn Trần", "Nam diễn viên triển vọng với nhiều vai diễn ấn tượng", "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=600&q=80"},
                new String[]{"Lý Hải", "Đạo diễn kiêm nhà sản xuất chuỗi phim ăn khách Lật Mặt", "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=600&q=80"},
                new String[]{"Cillian Murphy", "Nam diễn viên đoạt giải Oscar với vai Oppenheimer", "https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?auto=format&fit=crop&w=600&q=80"},
                new String[]{"Robert Downey Jr.", "Tài tử Hollywood lừng danh và biểu tượng Iron Man", "https://images.unsplash.com/photo-1522075469751-3a6694fb2f61?auto=format&fit=crop&w=600&q=80"},
                new String[]{"Timothée Chalamet", "Ngôi sao điện ảnh toàn cầu với loạt tác phẩm đình đám", "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?auto=format&fit=crop&w=600&q=80"},
                new String[]{"Zendaya", "Nữ diễn viên tài năng và biểu tượng thời trang quốc tế", "https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&w=600&q=80"},
                new String[]{"Ryan Reynolds", "Nam diễn viên quyến rũ và hóm hỉnh với vai Deadpool", "https://images.unsplash.com/photo-1492562080023-ab3db95bfbce?auto=format&fit=crop&w=600&q=80"},
                new String[]{"Hugh Jackman", "Huyền thoại Wolverine bất hủ trong lòng người hâm mộ", "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=600&q=80"},
                new String[]{"Amy Poehler", "Nữ diễn viên kỳ cựu lồng tiếng cho nhân vật Joy", "https://images.unsplash.com/photo-1544005313-94ddf0286df2?auto=format&fit=crop&w=600&q=80"},
                new String[]{"Maya Hawke", "Nữ diễn viên thế hệ mới xuất sắc với vai Anxiety", "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=600&q=80"},
                new String[]{"Sam Worthington", "Nam chính dũng cảm Jake Sully của vũ trụ Avatar", "https://images.unsplash.com/photo-1560250097-0b93528c311a?auto=format&fit=crop&w=600&q=80"},
                new String[]{"Zoe Saldana", "Nữ diễn viên của những siêu phẩm điện ảnh tỷ đô", "https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?auto=format&fit=crop&w=600&q=80"},
                new String[]{"Matthew McConaughey", "Chủ nhân tượng vàng Oscar với siêu phẩm Interstellar", "https://images.unsplash.com/photo-1508214751196-bcfd4ca60f91?auto=format&fit=crop&w=600&q=80"}
        );

        for (String[] def : actorDefs) {
            String name = def[0];
            String bio = def[1];
            String avatar = def[2];
            Actor actor = actorRepository.findByNameIgnoreCase(name)
                    .map(existing -> {
                        if (existing.getAvatarUrl() == null || existing.getAvatarUrl().contains("dmcodhbcc") || existing.getAvatarUrl().isBlank()) {
                            existing.setAvatarUrl(avatar);
                            return actorRepository.save(existing);
                        }
                        return existing;
                    })
                    .orElseGet(() -> actorRepository.save(new Actor(name, bio, avatar)));
            map.put(name, actor);
        }
        return map;
    }

    private void seedMovies(Map<String, Genre> genres, Map<String, Actor> actors) {
        LocalDate today = LocalDate.now();

        List<SeedMovieItem> items = List.of(
                new SeedMovieItem(
                        "Mai",
                        "Câu chuyện về Mai, một phụ nữ làm nghề massage trị liệu luôn khao khát hạnh phúc đích thực nhưng vướng phải những định kiến khắt khe của cuộc đời.",
                        131,
                        today.minusDays(15),
                        today.plusDays(45),
                        MovieStatus.NOW_SHOWING,
                        AgeRating.T18,
                        "Trấn Thành",
                        "https://m.media-amazon.com/images/M/MV5BMGUyMjM0MDctZDE2OS00MzNmLTk5OTAtZTBmNjI1ZDY4OTU3XkEyXkFqcGc@._V1_FMjpg_UX1000_.jpg",
                        "https://m.media-amazon.com/images/M/MV5BMGUyMjM0MDctZDE2OS00MzNmLTk5OTAtZTBmNjI1ZDY4OTU3XkEyXkFqcGc@._V1_FMjpg_UX1000_.jpg",
                        "https://www.youtube.com/watch?v=F3Q7dIu7j7I",
                        "Tiếng Việt",
                        "Tiếng Anh",
                        List.of("Tình cảm", "Tâm lý - Kịch tính"),
                        List.of("Phương Anh Đào", "Tuấn Trần", "Trấn Thành"),
                        List.of("Phương Anh Đào", "Tuấn Trần")
                ),
                new SeedMovieItem(
                        "Lật Mặt 7: Một Điều Ước",
                        "Câu chuyện gia đình xúc động về bà Hai 73 tuổi cùng 5 người con trưởng thành lập nghiệp nơi phương xa, khơi dậy bài học quý giá về tình mẫu tử.",
                        138,
                        today.minusDays(10),
                        today.plusDays(50),
                        MovieStatus.NOW_SHOWING,
                        AgeRating.K,
                        "Lý Hải",
                        "https://m.media-amazon.com/images/M/MV5BYzA2ZDczMjctZWE0OC00Y2ZhLWEwM2UtNTMyN2JmNGUzNmRhXkEyXkFqcGc@._V1_.jpg",
                        "https://m.media-amazon.com/images/M/MV5BYzA2ZDczMjctZWE0OC00Y2ZhLWEwM2UtNTMyN2JmNGUzNmRhXkEyXkFqcGc@._V1_.jpg",
                        "https://www.youtube.com/watch?v=d_kH1Bv8QoQ",
                        "Tiếng Việt",
                        "Tiếng Anh",
                        List.of("Gia đình", "Tâm lý - Kịch tính", "Hài hước"),
                        List.of("Lý Hải"),
                        List.of("Lý Hải")
                ),
                new SeedMovieItem(
                        "Dune: Hành Tinh Cát - Phần 2",
                        "Paul Atreides hợp lực cùng Chani và người Fremen trên hành trình báo thù những kẻ đã hủy diệt gia tộc mình, gánh vác sứ mệnh cứu rỗi toàn vũ trụ.",
                        166,
                        today.minusDays(20),
                        today.plusDays(30),
                        MovieStatus.NOW_SHOWING,
                        AgeRating.T16,
                        "Denis Villeneuve",
                        "https://m.media-amazon.com/images/M/MV5BN2QyZGU4ZDctOWMzMy00NTc5LThlOGQtODhmNDI1NmY5YzAwXkEyXkFqcGc@._V1_.jpg",
                        "https://m.media-amazon.com/images/M/MV5BN2QyZGU4ZDctOWMzMy00NTc5LThlOGQtODhmNDI1NmY5YzAwXkEyXkFqcGc@._V1_.jpg",
                        "https://www.youtube.com/watch?v=Way9Dexny3w",
                        "Tiếng Anh",
                        "Tiếng Việt",
                        List.of("Khoa học viễn tưởng", "Phiêu lưu", "Hành động"),
                        List.of("Timothée Chalamet", "Zendaya"),
                        List.of("Timothée Chalamet", "Zendaya")
                ),
                new SeedMovieItem(
                        "Oppenheimer",
                        "Kiệt tác tiểu sử lịch sử tái hiện cuộc đời nhà vật lý lý thuyết J. Robert Oppenheimer - cha đẻ của bom nguyên tử trong Dự án Manhattan.",
                        180,
                        today.minusDays(25),
                        today.plusDays(20),
                        MovieStatus.NOW_SHOWING,
                        AgeRating.T18,
                        "Christopher Nolan",
                        "https://m.media-amazon.com/images/M/MV5BN2JkMDc5MGQtZjg3YS00NmFiLWIyZmQtZTJmNTM5MjVmYTQ4XkEyXkFqcGc@._V1_.jpg",
                        "https://m.media-amazon.com/images/M/MV5BN2JkMDc5MGQtZjg3YS00NmFiLWIyZmQtZTJmNTM5MjVmYTQ4XkEyXkFqcGc@._V1_.jpg",
                        "https://www.youtube.com/watch?v=uYPbbksJxIg",
                        "Tiếng Anh",
                        "Tiếng Việt",
                        List.of("Tâm lý - Kịch tính", "Sử thi"),
                        List.of("Cillian Murphy", "Robert Downey Jr."),
                        List.of("Cillian Murphy", "Robert Downey Jr.")
                ),
                new SeedMovieItem(
                        "Deadpool & Wolverine",
                        "Wade Wilson tái xuất giang hồ và bất đắc dĩ phải hợp tác cùng một Wolverine đầy dằn vặt để giải cứu dòng thời gian và toàn bộ vũ trụ.",
                        128,
                        today.minusDays(5),
                        today.plusDays(60),
                        MovieStatus.NOW_SHOWING,
                        AgeRating.T18,
                        "Shawn Levy",
                        "https://m.media-amazon.com/images/M/MV5BNzRiMjg0MzUtNTQ1Mi00Y2Q5LWEwM2MtMzUwZDU5NmVjN2NkXkEyXkFqcGc@._V1_.jpg",
                        "https://m.media-amazon.com/images/M/MV5BNzRiMjg0MzUtNTQ1Mi00Y2Q5LWEwM2MtMzUwZDU5NmVjN2NkXkEyXkFqcGc@._V1_.jpg",
                        "https://www.youtube.com/watch?v=73_1biulkYk",
                        "Tiếng Anh",
                        "Tiếng Việt",
                        List.of("Hành động", "Hài hước", "Khoa học viễn tưởng"),
                        List.of("Ryan Reynolds", "Hugh Jackman"),
                        List.of("Ryan Reynolds", "Hugh Jackman")
                ),
                new SeedMovieItem(
                        "Inside Out 2 (Những Mảnh Ghép Cảm Xúc 2)",
                        "Bộ chỉ huy tâm trí cô bé Riley bước vào tuổi dậy thì với sự xuất hiện của những cảm xúc mới toanh: Lo Âu, Ganh Tị, Xấu Hổ và Chán Chường.",
                        96,
                        today.minusDays(12),
                        today.plusDays(40),
                        MovieStatus.NOW_SHOWING,
                        AgeRating.P,
                        "Kelsey Mann",
                        "https://m.media-amazon.com/images/M/MV5BYWY3ODE4NDQtOGY5NS00MDZhLTlhZmMtODc3ZWM4Y2UwZjYxXkEyXkFqcGc@._V1_.jpg",
                        "https://m.media-amazon.com/images/M/MV5BYWY3ODE4NDQtOGY5NS00MDZhLTlhZmMtODc3ZWM4Y2UwZjYxXkEyXkFqcGc@._V1_.jpg",
                        "https://www.youtube.com/watch?v=LEjhY15eCx0",
                        "Tiếng Anh",
                        "Tiếng Việt",
                        List.of("Hoạt hình", "Gia đình", "Hài hước"),
                        List.of("Amy Poehler", "Maya Hawke"),
                        List.of("Amy Poehler", "Maya Hawke")
                ),
                new SeedMovieItem(
                        "Avatar: Fire and Ash",
                        "Phần 3 của thiên anh hùng ca Pandora khám phá tộc người Ash People - một bộ tộc Na'vi bí ẩn đầy giận dữ gắn liền với nguyên tố lửa.",
                        190,
                        today.plusDays(90),
                        today.plusDays(180),
                        MovieStatus.UPCOMING,
                        AgeRating.T13,
                        "James Cameron",
                        "https://m.media-amazon.com/images/M/MV5BYjhiNjBlODctY2ZiOC00YjVlLWFlNzAtNTVhNzM1YjI1NzMxXkEyXkFqcGc@._V1_.jpg",
                        "https://m.media-amazon.com/images/M/MV5BYjhiNjBlODctY2ZiOC00YjVlLWFlNzAtNTVhNzM1YjI1NzMxXkEyXkFqcGc@._V1_.jpg",
                        "https://www.youtube.com/watch?v=d9MyW72ELq0",
                        "Tiếng Anh",
                        "Tiếng Việt",
                        List.of("Khoa học viễn tưởng", "Phiêu lưu", "Hành động"),
                        List.of("Sam Worthington", "Zoe Saldana"),
                        List.of("Sam Worthington", "Zoe Saldana")
                ),
                new SeedMovieItem(
                        "Spider-Man: Beyond the Spider-Verse",
                        "Chương kết định mệnh của Miles Morales khi phải vượt qua đa vũ trụ để giải cứu người cha thân yêu và định nghĩa lại số phận Người Nhện.",
                        140,
                        today.plusDays(60),
                        today.plusDays(150),
                        MovieStatus.UPCOMING,
                        AgeRating.P,
                        "Joaquim Dos Santos",
                        "https://m.media-amazon.com/images/M/MV5BNzA1Njg4NzYxOV5BMl5BanBnXkFtZTgwODk5NjU3MzI@._V1_.jpg",
                        "https://m.media-amazon.com/images/M/MV5BNzA1Njg4NzYxOV5BMl5BanBnXkFtZTgwODk5NjU3MzI@._V1_.jpg",
                        "https://www.youtube.com/watch?v=cqGjhVJWtEg",
                        "Tiếng Anh",
                        "Tiếng Việt",
                        List.of("Hoạt hình", "Hành động", "Khoa học viễn tưởng"),
                        List.of("Timothée Chalamet", "Zendaya"),
                        List.of("Timothée Chalamet")
                ),
                new SeedMovieItem(
                        "Joker: Điên Có Đôi (Joker: Folie à Deux)",
                        "Arthur Fleck bị giam giữ tại viện tâm thần Arkham đang chờ xét xử thì tình cờ gặp gỡ người phụ nữ định mệnh thắp sáng ảo vọng điên rồ của mình.",
                        138,
                        today.plusDays(30),
                        today.plusDays(100),
                        MovieStatus.UPCOMING,
                        AgeRating.T18,
                        "Todd Phillips",
                        "https://m.media-amazon.com/images/M/MV5BNTRlYjM0ZTYtYzM5Ny00YmU2LTg4NDEtNDU0ZTFhNDM3OGY0XkEyXkFqcGc@._V1_.jpg",
                        "https://m.media-amazon.com/images/M/MV5BNTRlYjM0ZTYtYzM5Ny00YmU2LTg4NDEtNDU0ZTFhNDM3OGY0XkEyXkFqcGc@._V1_.jpg",
                        "https://www.youtube.com/watch?v=_OKAwz2NiOI",
                        "Tiếng Anh",
                        "Tiếng Việt",
                        List.of("Tâm lý - Kịch tính", "Kinh dị"),
                        List.of("Cillian Murphy", "Robert Downey Jr."),
                        List.of("Cillian Murphy")
                ),
                new SeedMovieItem(
                        "Võ Sĩ Giác Đấu II (Gladiator II)",
                        "Nhiều năm sau cái chết anh dũng của Maximus, Lucius buộc phải bước vào đấu trường Colosseum để giành lại tự do và vinh quang cho La Mã.",
                        148,
                        today.plusDays(45),
                        today.plusDays(120),
                        MovieStatus.UPCOMING,
                        AgeRating.T18,
                        "Ridley Scott",
                        "https://m.media-amazon.com/images/M/MV5BMDY3Y2E1Y2QtZGVjOS00MWZhLWI4N2YtYjBmOGNhMGQ3OTI0XkEyXkFqcGc@._V1_.jpg",
                        "https://m.media-amazon.com/images/M/MV5BMDY3Y2E1Y2QtZGVjOS00MWZhLWI4N2YtYjBmOGNhMGQ3OTI0XkEyXkFqcGc@._V1_.jpg",
                        "https://www.youtube.com/watch?v=4rgYUipGJNo",
                        "Tiếng Anh",
                        "Tiếng Việt",
                        List.of("Hành động", "Sử thi", "Phiêu lưu"),
                        List.of("Hugh Jackman", "Ryan Reynolds"),
                        List.of("Hugh Jackman")
                ),
                new SeedMovieItem(
                        "Hố Đen Tử Thần (Interstellar)",
                        "Trong bối cảnh Trái Đất cạn kiệt sự sống, một nhóm nhà thám hiểm dũng cảm đi xuyên qua lỗ sâu vũ trụ để tìm kiếm mái nhà mới cho nhân loại.",
                        169,
                        today.minusDays(30),
                        today.plusDays(30),
                        MovieStatus.NOW_SHOWING,
                        AgeRating.T13,
                        "Christopher Nolan",
                        "https://m.media-amazon.com/images/M/MV5BYzdjMDAxZGItMjI2My00ODA1LTlkNzItOWFjMDU5ZDJlYWY3XkEyXkFqcGc@._V1_.jpg",
                        "https://m.media-amazon.com/images/M/MV5BYzdjMDAxZGItMjI2My00ODA1LTlkNzItOWFjMDU5ZDJlYWY3XkEyXkFqcGc@._V1_.jpg",
                        "https://www.youtube.com/watch?v=zSWdZVtXT7E",
                        "Tiếng Anh",
                        "Tiếng Việt",
                        List.of("Khoa học viễn tưởng", "Phiêu lưu", "Tâm lý - Kịch tính"),
                        List.of("Matthew McConaughey", "Zendaya"),
                        List.of("Matthew McConaughey")
                ),
                new SeedMovieItem(
                        "Avengers: Secret Wars",
                        "Sự kiện giao thoa đa vũ trụ lớn nhất lịch sử MCU tập hợp các siêu anh hùng từ mọi dòng thời gian trong trận chiến sinh tồn cuối cùng.",
                        175,
                        today.plusDays(180),
                        today.plusDays(270),
                        MovieStatus.UPCOMING,
                        AgeRating.T13,
                        "Russo Brothers",
                        "https://m.media-amazon.com/images/M/MV5BNDYxNjQyMjAtNTdiOS00NGYwLWFmNTAtNThmYjU5ZGI2YTI1XkEyXkFqcGc@._V1_.jpg",
                        "https://m.media-amazon.com/images/M/MV5BNDYxNjQyMjAtNTdiOS00NGYwLWFmNTAtNThmYjU5ZGI2YTI1XkEyXkFqcGc@._V1_.jpg",
                        "https://www.youtube.com/watch?v=6ZfuNTqbHE8",
                        "Tiếng Anh",
                        "Tiếng Việt",
                        List.of("Hành động", "Khoa học viễn tưởng", "Phiêu lưu"),
                        List.of("Robert Downey Jr.", "Ryan Reynolds", "Hugh Jackman"),
                        List.of("Robert Downey Jr.", "Ryan Reynolds")
                )
        );

        for (SeedMovieItem item : items) {
            if (movieRepository.existsByTitle(item.title())) {
                continue;
            }

            Movie movie = new Movie(item.title(), item.durationMinutes(), item.status());
            movie.setDescription(item.description());
            movie.setReleaseDate(item.releaseDate());
            movie.setEndDate(item.endDate());
            movie.setTrailerUrl(item.trailerUrl());
            movie.setPosterUrl(item.posterUrl());
            movie.setAvatarUrl(item.avatarUrl());
            movie.setLanguage(item.language());
            movie.setSubtitleLanguage(item.subtitleLanguage());
            movie.setAgeRating(item.ageRating());
            movie.setDirector(item.director());
            movie.setMainActors(String.join(", ", item.mainActors()));
            movie.setCastList(String.join(", ", item.actors()));

            Movie saved = movieRepository.save(movie);

            for (String genreName : item.genres()) {
                Genre g = genres.get(genreName);
                if (g != null) {
                    movieGenreRepository.save(new MovieGenre(saved, g));
                }
            }

            for (String actorName : item.actors()) {
                Actor a = actors.get(actorName);
                if (a != null) {
                    boolean isMain = item.mainActors().contains(actorName);
                    movieActorRepository.save(new MovieActor(saved, a, isMain));
                }
            }

            log.info("Đã khởi tạo phim mẫu thành công: [{}] - Trạng thái: {}", saved.getTitle(), saved.getStatus());
        }
    }

    private record SeedMovieItem(
            String title,
            String description,
            int durationMinutes,
            LocalDate releaseDate,
            LocalDate endDate,
            MovieStatus status,
            AgeRating ageRating,
            String director,
            String posterUrl,
            String avatarUrl,
            String trailerUrl,
            String language,
            String subtitleLanguage,
            List<String> genres,
            List<String> actors,
            List<String> mainActors
    ) {
    }
}
