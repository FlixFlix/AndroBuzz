package com.geil.myapplication.activity;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import com.androbuzz.android.R;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.geil.myapplication.app.SharedPrefs;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import static android.support.v4.content.ContextCompat.checkSelfPermission;

/**
 * Created by CaptainStosha on 8/25/2017.
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class DeviceModel {

    @JsonProperty("Model")
    private String model;

    @JsonProperty("Brand")
    private String brand;

    @JsonProperty("regToken")
    private String regToken;

    @JsonProperty("Number")
    private String number;

    @JsonProperty("Name")
    private String name;

    @JsonProperty("Registration Date")
    private String regDate;

    @JsonProperty("Serial")
    private String serial;

    @JsonProperty("Carrier")
    private String carrier;

    private WeakReference<Context> contextRef;

    public DeviceModel(String regTokenParameter, Context context) {
        regToken = regTokenParameter;
        contextRef = new WeakReference<>(context);
        name = Settings.Secure.getString(context.getContentResolver(), "bluetooth_name"); // this gets the user-entered device name
        if (name == null || name.isEmpty()) {
            name = getBrand() + getModel();
        }
    }

    @SuppressWarnings("all")
    public String getNumber(Context context) {
        String number = SharedPrefs.getInstance().getPhoneNumber();
        if (checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            try {
                TelephonyManager tMgr = (TelephonyManager) contextRef.get().getSystemService(Context.TELEPHONY_SERVICE);
                number = tMgr.getLine1Number();
                // Log.e( TAG, String.valueOf( number ) );
                if (number == null || number.isEmpty()) {
                    number = "555-555-5555";
                }
                if (number.charAt(0) == '+') {
                    number = number.substring(1);
                }
                if (number.charAt(0) == '1') {
                    number = number.substring(1);
                }
                number = number.replaceFirst("(\\d{3})(\\d{3})(\\d+)", "$1-$2-$3");
                SharedPrefs.getInstance().savePhoneNumber(number);
            } catch (Exception e) {
                number = "555-555-EEEE";
            }

        } else {
            number = context.getString(R.string.noPermission);
        }
        return number;
    }

    public String getBrand() {
        return Build.BRAND.toUpperCase();
    }

    public String getDeviceKey() {
        return this.getName() + " (" + this.getSerial() + ")";
    }

    public String getModel() {
        return Build.MODEL;
    }

    public String getName() {
        return name;
    }

    public void setRegDate(String regDate) {
        this.regDate = regDate;
    }

    public void updateRegDate() {
        SimpleDateFormat currentTimeSdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        currentTimeSdf.setTimeZone(TimeZone.getTimeZone("America/Chicago"));
        this.regDate = currentTimeSdf.format(new Date());
    }

    public String getRegDate() {
        return regDate;
    }

    public String getSerial() {
        return Build.SERIAL;
    }

    public void setregToken(String regToken) {
        this.regToken = regToken;
    }

    public String getRegToken() {
        return regToken;
    }

    public void setCarrier(String carrier){
        this.carrier = carrier;
    }

    public String getCarrier(){
        return carrier;
    }
}
