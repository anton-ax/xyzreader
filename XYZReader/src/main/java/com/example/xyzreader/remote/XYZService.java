package com.example.xyzreader.remote;

import com.example.xyzreader.model.Article;

import java.util.List;

import retrofit.http.GET;

/**
 * Created by Anton on 12/17/2015.
 */
public interface XYZService {

    @GET("/u/231329/xyzreader_data/data.json")
    List<Article> readData();
}
