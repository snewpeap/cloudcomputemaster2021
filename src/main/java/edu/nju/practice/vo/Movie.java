package edu.nju.practice.vo;

import java.io.Serializable;
import java.util.List;

public class Movie implements Serializable
{
	private static final long serialVersionUID = 1L;
	
	private int movieID;
	private int entityID;
	private String movieName;
	private List<String> genre;
	private List<String> country;
	private String date;
	private int cinemaID;
	private String cinemaName;
	private String province;
	private String city;
	private double boxOffice;
	private int audience;
	
	public Movie() {
		super();
	}
	
	public Movie(int movieID, int entityID, String movieName, List<String> genre, List<String> country, String date,
			int cinemaID, String cinemaName, String province, String city, double boxOffice, int audience) {
		super();
		this.movieID = movieID;
		this.entityID = entityID;
		this.movieName = movieName;
		this.genre = genre;
		this.country = country;
		this.date = date;
		this.cinemaID = cinemaID;
		this.cinemaName = cinemaName;
		this.province = province;
		this.city = city;
		this.boxOffice = boxOffice;
		this.audience = audience;
	}

	public int getMovieID() {
		return movieID;
	}

	public void setMovieID(int movieID) {
		this.movieID = movieID;
	}

	public int getEntityID() {
		return entityID;
	}

	public void setEntityID(int entityID) {
		this.entityID = entityID;
	}

	public String getMovieName() {
		return movieName;
	}

	public void setMovieName(String movieName) {
		this.movieName = movieName;
	}

	public List<String> getGenre() {
		return genre;
	}

	public void setGenre(List<String> genre) {
		this.genre = genre;
	}

	public List<String> getCountry() {
		return country;
	}

	public void setCountry(List<String> country) {
		this.country = country;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public int getCinemaID() {
		return cinemaID;
	}

	public void setCinemaID(int cinemaID) {
		this.cinemaID = cinemaID;
	}

	public String getCinemaName() {
		return cinemaName;
	}

	public void setCinemaName(String cinemaName) {
		this.cinemaName = cinemaName;
	}

	public String getProvince() {
		return province;
	}

	public void setProvince(String province) {
		this.province = province;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public double getBoxOffice() {
		return boxOffice;
	}

	public void setBoxOffice(double boxOffice) {
		this.boxOffice = boxOffice;
	}

	public int getAudience() {
		return audience;
	}

	public void setAudience(int audience) {
		this.audience = audience;
	}

	public String toString() {
		return "Movie [movieID=" + movieID + ", entityID=" + entityID + ", movieName=" + movieName + ", genre=" + genre
				+ ", country=" + country + ", date=" + date + ", cinemaID=" + cinemaID + ", cinemaName=" + cinemaName
				+ ", province=" + province + ", city=" + city + ", boxOffice=" + boxOffice + ", audience=" + audience
				+ "]";
	}
}
