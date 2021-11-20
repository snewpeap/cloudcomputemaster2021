package edu.nju.practice.util;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.springframework.stereotype.Component;

import edu.nju.practice.vo.Movie;
import edu.nju.practice.vo.MovieList;

@Component
public class QueueUtil implements Serializable
{
	private Queue<List<Movie>> movieQueue=new LinkedList<List<Movie>>();
	private Queue<MovieList> movieListQueue=new LinkedList<MovieList>();
	
	public synchronized boolean push(List<Movie> movies)
	{
		return movieQueue.offer(movies);
	}
	
	public synchronized List<Movie> pop()
	{
		return movieQueue.poll();
	}

	public synchronized boolean empty(){
		return movieQueue.isEmpty();
	}
	
	public synchronized boolean pushList(MovieList movieList)
	{
		return movieListQueue.offer(movieList);
	}
	
	public synchronized MovieList popList()
	{
		return movieListQueue.poll();
	}

	public synchronized boolean listEmpty(){
		return movieListQueue.isEmpty();
	}
}
