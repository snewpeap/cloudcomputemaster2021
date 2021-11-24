package edu.nju.practice.vo;

import java.io.Serializable;
import java.util.List;

public class MovieList implements Serializable {
	private List<Movie> genreMovies;
	private List<Movie> countryMovies;
	private List<Movie> cityMovies;

	public MovieList() {
		super();
	}

	public MovieList(List<Movie> genreMovies, List<Movie> countryMovies, List<Movie> cityMovies) {
		super();
		this.genreMovies = genreMovies;
		this.countryMovies = countryMovies;
		this.cityMovies = cityMovies;
	}

	public List<Movie> getGenreMovies() {
		return genreMovies;
	}

	public void setGenreMovies(List<Movie> genreMovies) {
		this.genreMovies = genreMovies;
	}

	public List<Movie> getCountryMovies() {
		return countryMovies;
	}

	public void setCountryMovies(List<Movie> countryMovies) {
		this.countryMovies = countryMovies;
	}

	public List<Movie> getCityMovies() {
		return cityMovies;
	}

	public void setCityMovies(List<Movie> cityMovies) {
		this.cityMovies = cityMovies;
	}

	public String toString() {
		return "MovieMap [genreMovies=" + genreMovies + ", countryMovies=" + countryMovies + ", cityMovies="
				+ cityMovies + "]";
	}
}
