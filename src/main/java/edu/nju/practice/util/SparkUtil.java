package edu.nju.practice.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.annotation.PostConstruct;

import com.google.gson.Gson;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.broadcast.Broadcast;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import edu.nju.practice.config.SparkConfig;
import edu.nju.practice.vo.Movie;
import edu.nju.practice.vo.MovieList;
import scala.Tuple2;

@Component
public class SparkUtil implements Serializable {
	@Value("${socket.port}")
	private int port;

	@Value("${hdfs.directory}")
	private String directory;

	@Value("${spark.timeoutSecond}")
	private int timeoutSecond;

	@Autowired
	private QueueUtil queueUtil;

	@Autowired
	transient private SparkConfig sparkConfig;

	transient private JavaStreamingContext javaStreamingContext;

	// 疫情相关的城市
	private static final List<String> filterCities = Arrays.asList("南京市", "郑州市", "张家界市", "上海市", "广州市");
	private static Broadcast<List<String>> filterCitiesBroadcast;

	@PostConstruct
	public void postConstruct() {
		javaStreamingContext = sparkConfig.getStreamingContext();
		filterCitiesBroadcast = javaStreamingContext.sparkContext().broadcast(filterCities);
		startMonitorHdfs();
	}

	public void startMonitorHdfs() {
		new Thread(() -> {
			JavaDStream<String> lines = javaStreamingContext.textFileStream(directory);

			// 将一行json字符串映射为一个movie对象
			JavaDStream<Movie> movieDStream = lines.map(line -> new Gson().fromJson(line, Movie.class));

			movieDStream.foreachRDD(javaRDD -> {
				ExecutorService streamingJobThreadPool = Executors.newFixedThreadPool(3);
				Future<List<Movie>> dailyBoxOfficeTask = streamingJobThreadPool
						.submit(() -> SparkUtil.this.computeMovieDailyBoxOffice(javaRDD));
				Future<List<Movie>> genreTask = streamingJobThreadPool
						.submit(() -> SparkUtil.this.computeByGenre(javaRDD));
				Future<List<Movie>> cityTask = streamingJobThreadPool
						.submit(() -> SparkUtil.this.computeByCity(javaRDD, filterCitiesBroadcast));
				List<Movie> movies = dailyBoxOfficeTask.get();
				// 电影类型
				List<Movie> genreMovies = genreTask.get();
				// 观影地区
				List<Movie> cityMovies = cityTask.get();

				streamingJobThreadPool.shutdown();

				if (genreMovies.size() > 0)
					queueUtil.pushList(new MovieList(genreMovies, new ArrayList<>(), cityMovies));
				if (movies.size() > 0)
					queueUtil.push(new ArrayList<>(movies));
			});

			// 启动StreamingContext
			SparkUtil.this.startStreamingContext();
		}).start();
	}

	private List<Movie> computeMovieDailyBoxOffice(JavaRDD<Movie> moviesRDD) {
		Collection<Movie> movies = moviesRDD
				// 按电影ID和movie对象映射为pair
				.mapToPair(movie -> new Tuple2<>(movie.getMovieID(), movie))
				// 按电影名对两个movie对象求票房的和
				.reduceByKey((movie1, movie2) -> {
					Movie movie = new Movie();
					movie.setDate(movie1.getDate());
					movie.setMovieName(movie1.getMovieName());
					movie.setBoxOffice(movie1.getBoxOffice() + movie2.getBoxOffice());
					return movie;
				})
				// 取tuple的第二个参数，映射为movie对象
				.map(tuple -> tuple._2()).collect();
		return new ArrayList<>(movies);
	}

	private List<Movie> computeByGenre(JavaRDD<Movie> javaRDD) {
		Collection<Movie> movies = javaRDD
				// 按电影类型列表扩充movie的RDD
				.flatMap(movie -> {
					List<String> movieGenres = movie.getGenre();
					if (movieGenres == null) {
						Collections.emptyIterator();
					}
					ArrayList<Movie> genreMappedMovies = new ArrayList<>(movieGenres.size());
					for (String genre : movieGenres) {
						Movie genreMappedMovie = new Movie();
						genreMappedMovie.setMovieName(movie.getMovieName());
						genreMappedMovie.setAudience(movie.getAudience());
						genreMappedMovie.setDate(movie.getDate());
						genreMappedMovie.setGenre(Arrays.asList(genre));
						genreMappedMovies.add(genreMappedMovie);
					}
					return genreMappedMovies.iterator();
				})
				// 按电影类型和movie对象映射为pair
				.mapToPair(movie -> new Tuple2<>(movie.getGenre().get(0), movie))
				// 按电影类型对两个movie对象求观众数的和
				.reduceByKey((movie1, movie2) -> {
					Movie movie = new Movie();
					movie.setDate(movie1.getDate());
					movie.setGenre(movie1.getGenre());
					movie.setAudience(movie1.getAudience() + movie2.getAudience());
					return movie;
				})
				// // 取tuple的第二个参数，映射为movie对象
				.map(tuple -> tuple._2()).collect();

		return new ArrayList<>(movies);
	}

	private List<Movie> computeByCity(JavaRDD<Movie> javaRDD, final Broadcast<List<String>> filterCitiesBroadcast) {
		Collection<Movie> movies = javaRDD
				// 按疫情相关的城市过滤
				.filter(movie -> filterCitiesBroadcast.getValue().contains(movie.getCity()))
				// 按城市和movie对象映射为pair
				.mapToPair(movie -> new Tuple2<>(movie.getCity(), movie))
				// 按城市对两个movie对象求观众数的和
				.reduceByKey((movie1, movie2) -> {
					Movie movie = new Movie();
					movie.setDate(movie1.getDate());
					movie.setCity(movie1.getCity());
					movie.setAudience(movie1.getAudience() + movie2.getAudience());
					return movie;
				})
				// 取tuple的第二个参数，映射为movie对象
				.map(tuple -> tuple._2()).collect();

		return new ArrayList<>(movies);
	}

	private void startStreamingContext() {
		javaStreamingContext.start();
		try {
			javaStreamingContext.awaitTerminationOrTimeout(timeoutSecond * 1000L);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private static final long serialVersionUID = 999L;
}
