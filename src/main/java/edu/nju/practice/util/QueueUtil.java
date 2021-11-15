package edu.nju.practice.util;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.springframework.stereotype.Component;

import edu.nju.practice.vo.Movie;

@Component
public class QueueUtil 
{
	private Queue<List<Movie>> queue=new LinkedList<List<Movie>>();
	
	public synchronized boolean push(List<Movie> movies)
	{
		return queue.offer(movies);
	}
	
	public synchronized List<Movie> pop()
	{
		return queue.poll();
	}
}
