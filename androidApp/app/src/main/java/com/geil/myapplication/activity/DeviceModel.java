package com.geil.myapplication.activity;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.lang.ref.WeakReference;

/**
 * Created by CaptainStosha on 8/25/2017.
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class DeviceModel {

    @JsonProperty("Model")
    private String model;

    @JsonProperty("Brand")
    private String brand;

    @JsonProperty("ClientId")
    private String clientId;

    @JsonProperty("Number")
    private String Number;

    @JsonProperty("Name")
    private String name;

    private WeakReference<Context> contextRef;

    public DeviceModel(String clientID, Context context){
        this.clientId = clientID;
        brand = Build.BRAND.toUpperCase();
        model = Build.MODEL;
        name = Settings.Secure.getString(context.getContentResolver(), "bluetooth_name"); // this gets the user-entered device name
        contextRef = new WeakReference<>(context);
    }

    public String getModel() {
        return model;
    }

    @SuppressWarnings("all")
    public String getNumber() {
        try{
            TelephonyManager tMgr = (TelephonyManager)contextRef.get().getSystemService(Context.TELEPHONY_SERVICE);
            return tMgr.getLine1Number();
        }catch (Exception e){}

        return "";
    }

    public void setModel(String model) {
        this.model = model;
    }
    public void setNumber(String Number) {
        this.Number = Number;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }
    public String getBrand() {
        return brand;
    }

    public void setName(String name) {
        this.name = name;
    }
    public String getName() {
        return name;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }
    public String getClientId() {
        return clientId;
    }
}
