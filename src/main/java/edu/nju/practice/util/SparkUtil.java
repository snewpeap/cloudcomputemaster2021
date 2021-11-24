package edu.nju.practice.util;

import com.google.gson.Gson;
import edu.nju.practice.config.SparkConfig;
import edu.nju.practice.vo.Movie;
import edu.nju.practice.vo.MovieList;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.*;
import org.apache.spark.broadcast.Broadcast;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import scala.Tuple2;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.annotation.PostConstruct;

@Component
public class SparkUtil implements Serializable
{
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
	
	private int count;
	
	transient private JavaStreamingContext javaStreamingContext;
	
	private static final String hostname="localhost";
	
	// 疫情相关的城市
	private static final List<String> filterCities=Arrays.asList("南京市", "郑州市", "张家界市", "上海市", "广州市");
	private static Broadcast<List<String>> filterCitiesBroadcast;

	@PostConstruct
	public void postConstruct() {
		javaStreamingContext = sparkConfig.getStreamingContext();
		filterCitiesBroadcast = javaStreamingContext.sparkContext().broadcast(filterCities);
		startMonitorHdfs();
	}

	public void startMonitorHdfs()
	{
		count=0;
		
		new Thread(() -> {
			JavaDStream<String> lines = javaStreamingContext.textFileStream(directory);
			//JavaDStream<String> lines=javaStreamingContext.socketTextStream(hostname, port);

			// 将一行json字符串映射为一个movie对象
			JavaDStream<Movie> movieDStream = lines.map(new Function<String, Movie>() {
				@Override
				public Movie call(String line) throws Exception {
					return new Gson().fromJson(line, Movie.class);
				}
			});

			movieDStream.foreachRDD(new VoidFunction<JavaRDD<Movie>>() {
				@Override
				public void call(JavaRDD<Movie> javaRDD) throws Exception {
					ExecutorService streamingJobThreadPool = Executors.newFixedThreadPool(3);
					Future<List<Movie>> dailyBoxOfficeTask = streamingJobThreadPool.submit(new Callable<List<Movie>>() {
						@Override
						public List<Movie> call() throws Exception {
							return SparkUtil.this.computeMovieDailyBoxOffice(javaRDD);
						}
					});
					Future<List<Movie>> genreTask = streamingJobThreadPool.submit(new Callable<List<Movie>>() {
						@Override
						public List<Movie> call() throws Exception {
							return SparkUtil.this.computeByGenre(javaRDD);
						}
					});
					Future<List<Movie>> cityTask = streamingJobThreadPool.submit(new Callable<List<Movie>>() {
						@Override
						public List<Movie> call() throws Exception {
							return SparkUtil.this.computeByCity(javaRDD, filterCitiesBroadcast);
						}
					});
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
					// if (movies.size() == 0 && genreMovies.size() == 0) // 超时
					// 	SparkUtil.this.checkTimeout();
				}
			});

			// 启动StreamingContext
			SparkUtil.this.startStreamingContext();
		}).start();
	}

	private List<Movie> computeMovieDailyBoxOffice(JavaRDD<Movie> moviesRDD) {
		Collection<Movie> movies =
				moviesRDD
						// 按电影ID和movie对象映射为pair
						.mapToPair(new PairFunction<Movie, Integer, Movie>() {
							@Override
							public Tuple2<Integer, Movie> call(Movie movie) throws Exception {
								return new Tuple2<>(movie.getMovieID(), movie);
							}
						})
						// 按电影名对两个movie对象求票房的和
						.reduceByKey(new Function2<Movie, Movie, Movie>() {
							@Override
							public Movie call(Movie movie1, Movie movie2) throws Exception {
								Movie movie = new Movie();
								movie.setDate(movie1.getDate());
								movie.setMovieName(movie1.getMovieName());
								movie.setBoxOffice(movie1.getBoxOffice() + movie2.getBoxOffice());
								return movie;
							}
						})
						// 取tuple的第二个参数，映射为movie对象
						.map(new Function<Tuple2<Integer, Movie>, Movie>() {
							@Override
							public Movie call(Tuple2<Integer, Movie> tuple) throws Exception {
								return tuple._2();
							}
						}).collect();
		return new ArrayList<>(movies);
	}
	
	private List<Movie> computeByGenre(JavaRDD<Movie> javaRDD)
	{
		Collection<Movie> movies=
			javaRDD
			// 按电影类型列表扩充movie的RDD
			.flatMap(new FlatMapFunction<Movie, Movie>() {
				@Override
				public Iterator<Movie> call(Movie movie) throws Exception {
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
				}
			})
			// 按电影类型和movie对象映射为pair
			.mapToPair(new PairFunction<Movie, String, Movie>() {
				@Override
				public Tuple2<String, Movie> call(Movie movie) throws Exception {
					return new Tuple2<>(movie.getGenre().get(0), movie);
				}
			})
			// 按电影类型对两个movie对象求观众数的和
			.reduceByKey(new Function2<Movie, Movie, Movie>() {
				@Override
				public Movie call(Movie movie1, Movie movie2) throws Exception {
					Movie movie = new Movie();
					movie.setDate(movie1.getDate());
					movie.setGenre(movie1.getGenre());
					movie.setAudience(movie1.getAudience() + movie2.getAudience());
					return movie;
				}
			})
			// // 取tuple的第二个参数，映射为movie对象
			.map(new Function<Tuple2<String, Movie>, Movie>() {
				@Override
				public Movie call(Tuple2<String, Movie> tuple) throws Exception {
					return tuple._2();
				}
			}).collect();
		
		return new ArrayList<>(movies);
	}
	
	private List<Movie> computeByCity(JavaRDD<Movie> javaRDD, final Broadcast<List<String>> filterCitiesBroadcast)
	{
		Collection<Movie> movies=
			// 将一行json字符串映射为一个movie对象
			javaRDD
			// 按疫情相关的城市过滤
			.filter(new Function<Movie, Boolean>() {
				@Override
				public Boolean call(Movie movie) throws Exception {
					return filterCitiesBroadcast.getValue().contains(movie.getCity());
				}
			})
			// 按城市和movie对象映射为pair
			.mapToPair(new PairFunction<Movie, String, Movie>() {
				@Override
				public Tuple2<String, Movie> call(Movie movie) throws Exception {
					return new Tuple2<>(movie.getCity(), movie);
				}
			})
			// 按城市对两个movie对象求观众数的和
			.reduceByKey(new Function2<Movie, Movie, Movie>() {
				@Override
				public Movie call(Movie movie1, Movie movie2) throws Exception {
					Movie movie = new Movie();
					movie.setDate(movie1.getDate());
					movie.setCity(movie1.getCity());
					movie.setAudience(movie1.getAudience() + movie2.getAudience());
					return movie;
				}
			})
			// 取tuple的第二个参数，映射为movie对象
			.map(new Function<Tuple2<String, Movie>, Movie>() {
				@Override
				public Movie call(Tuple2<String, Movie> tuple) throws Exception {
					return tuple._2();
				}
			}).collect();
		
		return new ArrayList<>(movies);
	}
	
	private void checkTimeout()
	{
		// 数据为空超过timeoutSecond，则停止Streaming上下文
		count++;
		System.out.println(count+", "+timeoutSecond);
		if(count>timeoutSecond)
		{
			System.out.println(count+", "+timeoutSecond);
			javaStreamingContext.stop(false);
		}
	}
	
	private void startStreamingContext()
	{
		javaStreamingContext.start();
		try {
			javaStreamingContext.awaitTerminationOrTimeout(timeoutSecond * 1000L);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private static final long serialVersionUID = 999L;
}
