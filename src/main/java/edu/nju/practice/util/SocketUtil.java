package edu.nju.practice.util;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;

import edu.nju.practice.vo.Movie;

@Component
public class SocketUtil 
{
	@Value("classpath:static/2021-11-11.json")
	private Resource resource;
	
	@Value("${socket.port}")
	private int port;
	
	@Value("${socket.second}")
	private int second;
	
	private int index;
	
	public int startSocket()
	{
		new Thread(()->{
			try {
				// 读取json文件
				String json=IOUtils.toString(resource.getInputStream(), Charset.forName("UTF-8"));
				String[] lines=json.split("\n");
				
				// 获取键值为date的映射
				Map<String, List<Movie>> movieMap=Arrays.stream(lines)
					.map(line->new Gson().fromJson(line, Movie.class))
					.collect(Collectors.groupingBy(Movie::getDate));
					//这里可以放到spark内部进行groupByKey
				List<String> dates=movieMap.keySet().stream().sorted().collect(Collectors.toList());
				
				// 启动Socket
				ServerSocket serverSocket=new ServerSocket(port);
				Socket socket=serverSocket.accept();
				OutputStream outputStream=socket.getOutputStream();
				
				index=0;
				
				new Timer().schedule(new TimerTask() {
					public void run() {
						try {
							if(index<dates.size())
							{
								// 按日期依次输出json数据
								String date=dates.get(index);
								List<Movie> movies=movieMap.get(date);
								for(int i=0; i<movies.size(); i++) 
								{
									String text=new Gson().toJson(movies.get(i))+"\n";
									outputStream.write(text.getBytes());
								}
								
								index++;
							}
							else
							{
								// 关闭输出流和Socket
								outputStream.close();
								socket.close();
								serverSocket.close();
								this.cancel();
							}
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}, 0, 1000*second);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}).start();
		
		return 1;
	}
}
