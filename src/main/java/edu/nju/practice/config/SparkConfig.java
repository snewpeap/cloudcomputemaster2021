package edu.nju.practice.config;

import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SparkConfig 
{
	@Value("${spark.appName}")
	private String appName;
	
	@Value("${spark.master}")
	private String master;
	
	@Value("${socket.second}")
	private int second;
	
	public SparkSession getSession()
	{
		SparkSession sparkSession = SparkSession
			.builder()
			.appName(appName)
			.master(master)
			.config("spark.streaming.fileStream.minRememberDuration", "10s")
			// .config("spark.locality.wait", "2s")
			.config("spark.jars", "hdfs://master:9000/user/spark/streaming.jar")
			.config("spark.scheduler.mode", "FAIR")
			.getOrCreate();
		
		sparkSession.sparkContext().setLogLevel("WARN");
		
		return sparkSession;
	}
	
	public JavaStreamingContext getStreamingContext()
	{
		SparkSession sparkSession=this.getSession();
		JavaSparkContext javaSparkContext=new JavaSparkContext(sparkSession.sparkContext());
		JavaStreamingContext javaStreamingContext=new JavaStreamingContext(javaSparkContext, 
			Durations.seconds(second));
		javaStreamingContext.sparkContext().setLogLevel("WARN");
		
		return javaStreamingContext;
	}
}
