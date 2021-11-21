package edu.nju.practice.test;

import java.lang.reflect.Method;
import java.util.List;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.SparkSession;

import edu.nju.practice.util.SparkUtil;
import edu.nju.practice.vo.Movie;

public class SparkStreamingTest
{
	private static final String path="file:/D:/云计算/作业/实践SparkStreaming/软件系统/StreamingPractice/src/main/resources/static/2021-10-04.jl";
	
	public static void main(String[] args) throws Exception
	{
		long ft;
		
		// 1
		ft=System.currentTimeMillis();
		
		SparkSession sparkSession=SparkSession.builder().appName("").master("local[*]").getOrCreate();
		JavaSparkContext javaSparkContext=new JavaSparkContext(sparkSession.sparkContext());
		javaSparkContext.setLogLevel("WARN");
		
		System.out.println("1.用时："+(System.currentTimeMillis()-ft)+"ms");
		
		// 2
		ft=System.currentTimeMillis();
		
		JavaRDD<String> javaRDD=javaSparkContext.textFile(path);
		
		System.out.println("2.用时："+(System.currentTimeMillis()-ft)+"ms");
		
		Class<SparkUtil> clazz=SparkUtil.class;
		Method method=clazz.getDeclaredMethod("computeByGenre", javaRDD.getClass());
		method.setAccessible(true);
		
		// 3
		ft=System.currentTimeMillis();
		
		List<Movie> movies=(List<Movie>) method.invoke(new SparkUtil(), javaRDD);
		
		System.out.println("3.用时："+(System.currentTimeMillis()-ft)+"ms");
		
		movies.stream().forEach(System.out::println);
	}
}
