package edu.nju.practice.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.nju.practice.util.HdfsComponent;
import edu.nju.practice.util.QueueUtil;
import edu.nju.practice.vo.Movie;
import edu.nju.practice.vo.MovieList;

@RestController
@RequestMapping("/api")
public class StreamingController {
	@Autowired
	private QueueUtil queueUtil;

	@Autowired
	private HdfsComponent hdfsComponent;

	@RequestMapping("/startSocket")
	public int startSocket() {
		if (hdfsComponent.notStarted()) {
			hdfsComponent.modifyFile();
		}

		return 1;
	}

	@RequestMapping("/getStreamingData")
	public List<Movie> getStreamingData() {
		return queueUtil.pop();
	}

	@RequestMapping("/startMonitor")
	public int startMonitor() {
		if (hdfsComponent.notStarted()) {
			hdfsComponent.modifyFile();
		}

		return 1;
	}

	@RequestMapping("/getStreamingDataList")
	public MovieList getStreamingDataList() {
		return queueUtil.popList();
	}
}
