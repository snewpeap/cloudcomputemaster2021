package edu.nju.practice.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.streaming.api.java.JavaDStream;
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
			
			JavaDStream<String> lines=javaStreamingContext.textFileStream(directory);
			//JavaDStream<String> lines=javaStreamingContext.socketTextStream(hostname, port);
			lines.print();
			
			lines.foreachRDD(javaRDD->{
				List<Movie> movies=
					// 将一行json字符串映射为一个movie对象
					javaRDD.map(line->new Gson().fromJson(line, Movie.class))
					// 按电影名和movie对象映射为pair
					.mapToPair(movie->new Tuple2<>(movie.getMovieName(), movie))
					// 按电影名对两个movie对象求票房的和
					.reduceByKey((movie1, movie2)->{
						Movie movie=new Movie();
						movie.setDate(movie1.getDate());
						movie.setMovieName(movie1.getMovieName());
						movie.setBoxOffice(movie1.getBoxOffice()+movie2.getBoxOffice());
						return movie;
					})
					// 取tuple的第二个参数，映射为movie对象
					.map(tuple->tuple._2())
					.collect();
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
			
			JavaDStream<String> lines=javaStreamingContext.textFileStream(directory);
			//JavaDStream<String> lines=javaStreamingContext.socketTextStream(hostname, port);
			lines.print();
			
			lines.foreachRDD(javaRDD->{
				// 电影类型
				List<Movie> genreMovies=this.computeByGenre(javaRDD);
				// 出品国家
				List<Movie> countryMovies=this.computeByCountry(javaRDD);
				// 观影地区
				List<Movie> cityMovies=this.computeByCity(javaRDD);
				
				System.out.println("xxx:"+genreMovies.size());
				
				if(genreMovies.size()>0)
					queueUtil.pushList(new MovieList(genreMovies, countryMovies, cityMovies));
				else // 超时
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
		// 疫情相关的城市
		List<String> citys=new ArrayList<String>(Arrays.asList("南京市", "郑州市", "张家界市", "上海市", "广州市"));
		
		List<Movie> movies=
			// 将一行json字符串映射为一个movie对象
			javaRDD.map(line->new Gson().fromJson(line, Movie.class))
			// 按疫情相关的城市过滤
			.filter(movie->citys.contains(movie.getCity()))
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
			//javaStreamingContext.awaitTermination();
			javaStreamingContext.awaitTerminationOrTimeout(timeoutSecond*1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
