package com.geil.myapplication.activity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by CaptainStosha on 8/19/2017.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageModel {

    public MessageModel(){}

    @JsonProperty(value ="title")
    private String title;

    @JsonProperty(value ="message")
    private String message;

    @JsonProperty(value ="id")
    private String id;

    @JsonProperty(value ="isBackground")
    private boolean isBackground;

    @JsonProperty(value ="imageUrl")
    private String imageUrl;

    @JsonProperty(value ="timeStamp")
    private String timeStamp;

    @JsonProperty(value ="uniqueId")
    private String uniqueId;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isBackground() {
        return isBackground;
    }

    public void setBackground(boolean background) {
        isBackground = background;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }
}
