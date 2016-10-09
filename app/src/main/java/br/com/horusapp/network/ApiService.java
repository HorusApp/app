package br.com.horusapp.network;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.DELETE;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface ApiService {

    @FormUrlEncoded
    @POST("users")
    Call<ResponseBody> signUp(@Field("name") String name,
                              @Field("password") String password,
                              @Field("email") String email);

    @FormUrlEncoded
    @POST("login")
    Call<ResponseBody> login(@Field("email") String email,
                             @Field("password") String password);

    @FormUrlEncoded
    @POST("videos")
    Call<ResponseBody> sendVideo(@Header("email") String email,
                                 @Header("token") String token,
                                 @Field("user_id") int userId,
                                 @Field("url") String url,
                                 @Field("title") String title,
                                 @Field("location") String location);

    @DELETE("users/{id}")
    Call<ResponseBody> deleteUser(@Header("email") String email,
                                  @Header("token") String token,
                                  @Path("id") int id);


    Retrofit retrofit = new Retrofit.Builder()
            .baseUrl("https://fiap-horusapp.herokuapp.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build();
}
