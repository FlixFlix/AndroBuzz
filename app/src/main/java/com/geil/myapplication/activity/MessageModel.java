package com.geil.myapplication.activity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by CaptainStosha on 8/19/2017.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageModel {

    @JsonProperty(value = "title")
    private String title;
    @JsonProperty(value = "command")
    private String command;
    @JsonProperty(value = "id")
    private String id;
    @JsonProperty(value = "batteryLevel")
    private String batteryLevel;
    @JsonProperty(value = "timeStamp")
    private String timeStamp;
    @JsonProperty(value = "extras")
    private String extras;
    @JsonProperty(value = "signal")
    private String signal;
    @JsonProperty("timeDelivered")
    private String timeDelivered;
    @JsonProperty(value = "signalInfo")
    private String signalInfo;


    // @JsonProperty( value = "messageDbKey" )
    // private String messageDbKey;
    // public String getmessageDbKey() {
    //     return messageDbKey;
    // }
    //
    // public void setmessageDbKey( String messageDbKey ) {
    //     this.messageDbKey = messageDbKey;
    // }

    public MessageModel() {
    }

    public void setExtras(String extras) {
        this.extras = extras;
    }

    public String getSignal() {
        return signal;
    }

    public void setSignal(String signal) {
        this.signal = signal;
    }

    public String getSignalInfo() {
        return signalInfo;
    }

    public void setSignalInfo(String signalInfo) {
        this.signalInfo = signalInfo;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getbatteryLevel() {
        return batteryLevel;
    }

    public void setBatteryLevel(String batteryLevel) {
        this.batteryLevel = batteryLevel;
    }

    public String getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
    }

    public void setTimeDelivered(String timeDelivered){
        this.timeDelivered = timeDelivered;
    }

    public String getTimeDelivered() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ").format(new Date());
    }
}
