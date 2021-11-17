package edu.nju.practice.util;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaReceiverInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;

import edu.nju.practice.config.SparkConfig;
import edu.nju.practice.vo.Movie;
import edu.nju.practice.vo.MovieList;
import scala.Tuple2;

@Component
public class SparkUtil 
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
	private SparkConfig sparkConfig;
	
	private int count;
	
	private JavaStreamingContext javaStreamingContext;
	
	private static final String hostname="localhost";
	
	public void startStreaming()
	{
		count=0;
		
		new Thread(()->{
			this.javaStreamingContext=sparkConfig.getStreamingContext();
			
			JavaReceiverInputDStream<String> lines=javaStreamingContext
				.socketTextStream(hostname, port);
			lines.print();
			lines.foreachRDD(javaRDD->{
				// 将一行json字符串映射为一个movie对象，并收集成列表
				List<Movie> movies=javaRDD.map(line->new Gson().fromJson(line, Movie.class)).collect();
				queueUtil.push(movies);
				
				// 超时
				if(movies.size()==0)
					this.checkTimeout();
			});
			
			// 启动StreamingContext
			this.startStreamingContext();
		}).start();
	}
	
	public void startMonitorHdfs()
	{
		count=0;
		
		new Thread(()->{
			this.javaStreamingContext=sparkConfig.getStreamingContext();
			
			//JavaDStream<String> lines=javaStreamingContext.textFileStream(directory);
			JavaDStream<String> lines=javaStreamingContext.socketTextStream(hostname, port);
			lines.print();
			
			lines.foreachRDD(javaRDD->{
				// 电影类型
				List<Movie> genreMovies=this.computeByGenre(javaRDD);
				// 出品国家
				List<Movie> countryMovies=this.computeByCountry(javaRDD);
				// 观影地区
				List<Movie> cityMovies=this.computeByCity(javaRDD);
				
				queueUtil.pushList(new MovieList(genreMovies, countryMovies, cityMovies));

				// 超时
				if(genreMovies.size()==0)
					this.checkTimeout();
			});
			
			// 启动StreamingContext
			this.startStreamingContext();
		}).start();
	}
	
	private List<Movie> computeByGenre(JavaRDD<String> javaRDD)
	{
			
		List<Movie> movies=
			// 将一行json字符串映射为一个movie对象
			javaRDD.map(line->new Gson().fromJson(line, Movie.class))
			// 按电影类型列表扩充movie的RDD
			.flatMap(movie->{
				List<Movie> movieList=movie.getGenre().stream()
					.map(genre->{
						Movie newMovie=new Movie(movie);
						newMovie.setGenre(Arrays.asList(genre));
						return newMovie;
					}).collect(Collectors.toList());
				return movieList.iterator();
			})
			// 按电影类型和movie对象映射为pair
			.mapToPair(movie->new Tuple2<>(movie.getGenre().get(0),movie))
			// 按电影类型对两个movie对象求观众数的和
			.reduceByKey((movie1, movie2)->{
				Movie movie=new Movie();
				movie.setDate(movie1.getDate());
				movie.setGenre(Arrays.asList(movie1.getGenre().get(0)));
				movie.setAudience(movie1.getAudience()+movie2.getAudience());
				return movie;
			})
			// 取tuple的第二个参数，映射为movie对象
			.map(tuple->tuple._2())
			.collect();
		
		return movies;
	}
	
	private List<Movie> computeByCountry(JavaRDD<String> javaRDD)
	{
		List<Movie> movies=
			// 将一行json字符串映射为一个movie对象
			javaRDD.map(line->new Gson().fromJson(line, Movie.class))
			// 按出品国家列表扩充movie的RDD
			.flatMap(movie->{
				List<Movie> movieList=movie.getCountry().stream()
					.map(country->{
						Movie newMovie=new Movie(movie);
						newMovie.setCountry(Arrays.asList(country));
						return newMovie;
					}).collect(Collectors.toList());
				return movieList.iterator();
			})
			// 按出品国家和movie对象映射为pair
			.mapToPair(movie->new Tuple2<>(movie.getCountry().get(0), movie))
			// 按出品国家对两个movie对象求观众数的和
			.reduceByKey((movie1, movie2)->{
				Movie movie=new Movie();
				movie.setDate(movie1.getDate());
				movie.setCountry(Arrays.asList(movie1.getCountry().get(0)));
				movie.setAudience(movie1.getAudience()+movie2.getAudience());
				return movie;
			})
			// 取tuple的第二个参数，映射为movie对象
			.map(tuple->tuple._2())
			.collect();
		
		return movies;
	}
	
	private List<Movie> computeByCity(JavaRDD<String> javaRDD)
	{
		List<Movie> movies=
			// 将一行json字符串映射为一个movie对象
			javaRDD.map(line->new Gson().fromJson(line, Movie.class))
			// 按城市和movie对象映射为pair
			.mapToPair(movie->new Tuple2<>(movie.getCity(), movie))
			// 按城市对两个movie对象求观众数的和
			.reduceByKey((movie1, movie2)->{
				Movie movie=new Movie();
				movie.setDate(movie1.getDate());
				movie.setCity(movie1.getCity());
				movie.setAudience(movie1.getAudience()+movie2.getAudience());
				return movie;
			})
			// 取tuple的第二个参数，映射为movie对象
			.map(tuple->tuple._2())
			.collect();
		
		return movies;
	}
	
	private void checkTimeout()
	{
		// 数据为空超过timeoutSecond，则停止Streaming上下文
		count++;
		if(count>timeoutSecond)
			javaStreamingContext.stop(false);
	}
	
	private void startStreamingContext()
	{
		javaStreamingContext.start();
		try {
			javaStreamingContext.awaitTermination();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
