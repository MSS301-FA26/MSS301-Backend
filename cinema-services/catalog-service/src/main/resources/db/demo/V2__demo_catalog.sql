-- Optional development fixtures. Exclude classpath:db/demo in production.
INSERT INTO cinemas(name,address,city,phone,status,created_at,updated_at)
 VALUES ('CinemaAI Central','1 Cinema Street','Ho Chi Minh City','0900000000','ACTIVE',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP);
INSERT INTO genres(name,description,created_at,updated_at) VALUES ('Adventure','Demo genre',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP);
INSERT INTO actors(name,biography,created_at,updated_at) VALUES ('Demo Actor','Sample actor for local development',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP);
INSERT INTO movies(title,description,duration_minutes,release_date,end_date,status,age_rating,created_at,updated_at)
 VALUES ('CinemaAI Demo','Sample movie for Catalog acceptance testing',100,CURRENT_DATE,CURRENT_DATE + 365,'NOW_SHOWING','P',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP);
INSERT INTO movie_genres(movie_id,genre_id,created_at,updated_at)
 SELECT m.id,g.id,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP FROM movies m,genres g WHERE m.title='CinemaAI Demo' AND g.name='Adventure';
INSERT INTO movie_actors(movie_id,actor_id,is_main_actor,created_at,updated_at)
 SELECT m.id,a.id,true,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP FROM movies m,actors a WHERE m.title='CinemaAI Demo' AND a.name='Demo Actor';
INSERT INTO rooms(cinema_id,name,room_type,row_count,column_count,status,created_at,updated_at)
 SELECT id,'Hall 1','STANDARD',2,4,'ACTIVE',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP FROM cinemas WHERE name='CinemaAI Central';
INSERT INTO seat_rows(room_id,row_label,display_order,start_column,row_type,created_at,updated_at)
 SELECT id,'A',1,1,'STANDARD',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP FROM rooms WHERE name='Hall 1';
INSERT INTO seat_rows(room_id,row_label,display_order,start_column,row_type,created_at,updated_at)
 SELECT id,'B',2,1,'VIP',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP FROM rooms WHERE name='Hall 1';
INSERT INTO seats(room_id,seat_row_id,row_label,seat_number,display_column,seat_type,status,created_at,updated_at)
 SELECT r.room_id,r.id,r.row_label,n.n,n.n,r.row_type,'AVAILABLE',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP
 FROM seat_rows r CROSS JOIN (VALUES(1),(2),(3),(4)) n(n);
INSERT INTO showtimes(movie_id,room_id,start_time,end_time,base_price,vip_price,couple_price,
 adult_standard_price,child_standard_price,student_standard_price,adult_vip_price,child_vip_price,student_vip_price,
 weekend_surcharge,holiday_surcharge,late_night_surcharge_amount,status,created_at,updated_at)
 SELECT m.id,r.id,CAST(CURRENT_DATE + 1 AS TIMESTAMP) + INTERVAL '18' HOUR,CAST(CURRENT_DATE + 1 AS TIMESTAMP) + INTERVAL '20' HOUR,
 90000,110000,120000,90000,60000,70000,110000,80000,90000,false,false,20000,'OPEN',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP
 FROM movies m,rooms r WHERE m.title='CinemaAI Demo' AND r.name='Hall 1';
INSERT INTO food_items(name,description,price,status,created_at,updated_at)
 VALUES ('Sweet popcorn','Large popcorn',45000,'ACTIVE',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP);
INSERT INTO food_combos(name,description,price,status,created_at,updated_at)
 VALUES ('Popcorn and drinks','One large popcorn and two drinks',95000,'ACTIVE',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP);
