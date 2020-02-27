package com.app.motolife.firebase;

import com.app.motolife.Notifications.MyResponse;
import com.app.motolife.Notifications.Sender;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface APIService {
    @Headers(
            {
                    "Content-Type:application/json",
                    "Authorization:key=AAAAbs7QFZY:APA91bH_5Tnd0jgt3BMCwtTrUepnB9aijn38MQKKTmPOxJJEpEAIVGsaOwFEyoWmWnMluvzDiSDFlg8N7B68ikA-3SUigCxJYDMcgcwgRAaoiCzfp_XJ28DCrYv70Z15cBU8TNOTWOpp"
            }
    )

    @POST("fcm/send")
    Call<MyResponse> sendNotification(@Body Sender body);
}
