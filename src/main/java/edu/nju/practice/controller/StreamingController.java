package edu.nju.practice.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.nju.practice.util.HdfsComponent;
import edu.nju.practice.util.QueueUtil;
import edu.nju.practice.util.SocketUtil;
import edu.nju.practice.util.SparkUtil;
import edu.nju.practice.vo.Movie;
import edu.nju.practice.vo.MovieList;

@RestController
@RequestMapping("/api")
public class StreamingController 
{
	@Autowired
	private SocketUtil socketUtil;
	
	@Autowired
	private SparkUtil sparkUtil;
	
	@Autowired
	private QueueUtil queueUtil;
	
	@Autowired
	private HdfsComponent hdfsComponent;

	
	@RequestMapping("/startSocket")
	public int startSocket()
	{
		//socketUtil.startSocket();
		if(sparkUtil.notStarted() && queueUtil.empty()) {
			sparkUtil.startMonitorHdfs();
			hdfsComponent.modifyFile();
		}
		
		return 1;
	}
	
	@RequestMapping("/getStreamingData")
	public List<Movie> getStreamingData()
	{	
		return queueUtil.pop();
	}
	
	@RequestMapping("/startMonitor")
	public int startMonitor()
	{
		if(sparkUtil.notStarted() && queueUtil.listEmpty()) {
			sparkUtil.startMonitorHdfs();
			hdfsComponent.modifyFile();
		}
		
		return 1;
	}
	
	@RequestMapping("/getStreamingDataList")
	public MovieList getStreamingDataList()
	{	
		return queueUtil.popList();
	}
}
