package br.com.horusapp.model;

import com.google.gson.annotations.SerializedName;

public class Video {

    public int id;
    public String url;
    public String title;
    public String location;

    @SerializedName("user_id")
    public String userId;
}
