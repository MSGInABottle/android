package com.example.andy.messageinabottle;

/**
 * Created by andy on 2016-09-17.
 */
public class Message {
    public String text;
    public double lng;
    public double lat;

    Message(String t, String lg, String lt) {
        text = t;
        lng = Double.parseDouble(lg);
        lat = Double.parseDouble(lt);
    }
}
