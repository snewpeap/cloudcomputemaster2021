package edu.nju.practice.util;

import java.util.List;

import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaReceiverInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;

import edu.nju.practice.vo.Movie;

@Component
public class SparkUtil 
{
	@Value("${socket.port}")
	private int port;
	
	@Value("${socket.second}")
	private int second;
	
	@Autowired
	private QueueUtil queueUtil;
	
	private int count;
	
	private static final String hostname="localhost";
	
	public SparkSession getSession()
	{
		SparkSession sparkSession = SparkSession
			.builder()
			.appName("SparkUtil")
			.master("local[*]")
			.getOrCreate();
		
		sparkSession.sparkContext().setLogLevel("WARN");
		
		return sparkSession;
	}
	
	public void startStreaming()
	{
		count=0;
		
		new Thread(()->{
			SparkSession sparkSession=this.getSession();
			JavaSparkContext javaSparkContext=new JavaSparkContext(sparkSession.sparkContext());
			JavaStreamingContext javaStreamingContext=new JavaStreamingContext(javaSparkContext, 
				Durations.seconds(second));
			
			JavaReceiverInputDStream<String> lines=javaStreamingContext
				.socketTextStream(hostname, port);
			lines.print();
			lines.foreachRDD(javaRDD->{
				List<Movie> movies=javaRDD.map(line->new Gson().fromJson(line, Movie.class)).collect();
				System.out.println("xxx: "+movies);
				System.out.println(javaRDD.collect());
				queueUtil.push(movies);
				
				// 10次movies为空，则停止Streaming上下文
				if(movies.size()==0)
				{
					count++;
					if(count>10)
						javaStreamingContext.stop(false);
				}
			});
					
			javaStreamingContext.start();
			try {
				javaStreamingContext.awaitTermination();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}).start();
	}
}
