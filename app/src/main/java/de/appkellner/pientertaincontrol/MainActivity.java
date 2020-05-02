package de.appkellner.pientertaincontrol;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private final String TAG="MainActivity";

//    private String PI = "http://192.168.2.114:8999";
    private String PI = "";

    Timer timer = new Timer();
    RequestQueue requestQueue;

    ArrayList<Point> points = new ArrayList<>();

    JSONObject camSettings = new JSONObject();

    boolean pointsUpdated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

            StrictMode.ThreadPolicy policy = new
                    StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);

        requestQueue = Volley.newRequestQueue(this);


        points.add(new Point(0,0));
        points.add(new Point(640,0));
        points.add(new Point(0,480));
        points.add(new Point(640,480));

        AreaOverlay overlay = findViewById(R.id.overlay);
        overlay.setPoints(points);

        Button b = findViewById(R.id.button_image);
        b.setOnClickListener(this);
        b = findViewById(R.id.button_sendcorners);
        b.setOnClickListener(this);
        b = findViewById(R.id.button_start);
        b.setOnClickListener(this);
        b = findViewById(R.id.button_stop);
        b.setOnClickListener(this);
        b = findViewById(R.id.button_shutdown);
        b.setOnClickListener(this);

        TimerTask updateStatus = new UpdateOnTimerTask();
        timer.scheduleAtFixedRate(updateStatus, 0, 2000);

        readSettings();


    }

    protected void readSettings() {
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        PI = sharedPref.getString("piip", "");
    }

    protected void writeSettings() {
        if (PI.length() == 0) {
            return;
        }
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("piip", PI);
        editor.apply();
    }

    @Override
    protected void onStop () {
        super.onStop();
        if (requestQueue != null) {
            requestQueue.cancelAll(TAG);
        }
        timer.cancel();
    }

    void setPoints(ArrayList<Point> points) {
        this.points = points;
    }

    @Override
    public void onClick(View v) {

        if (PI.length()==0) return;

        switch (v.getId()) {
            case R.id.button_image:

                ImageRequest imageRequest = new ImageRequest(
                        PI+"/image", // Image URL
                        new Response.Listener<Bitmap>() { // Bitmap listener
                            @Override
                            public void onResponse(Bitmap response) {
                                ((ImageView)findViewById(R.id.imageView)).setImageBitmap(response);

                                AreaOverlay overlay = findViewById(R.id.overlay);
                                overlay.setImageSize(response.getWidth(), response.getHeight());
                            }
                        },
                        0, // Image width
                        0, // Image height
                        ImageView.ScaleType.CENTER_INSIDE, // Image scale type
                        Bitmap.Config.RGB_565, //Image decode configuration
                        new Response.ErrorListener() { // Error listener
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                ((TextView)findViewById(R.id.status_video)).setText("image not loaded, try again");
                            }
                        }
                );
                imageRequest.setTag(TAG);
                requestQueue.add(imageRequest);
                break;
            case R.id.button_start: {
                JsonObjectRequest jsonreq = new JsonObjectRequest(Request.Method.GET, PI + "/start",
                        null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                    }
                },
                        new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                ((TextView) findViewById(R.id.status_bridge)).setText("can not connect to pi");
                            }
                        });
                jsonreq.setTag(TAG);
                requestQueue.add(jsonreq);
            }break;
            case R.id.button_stop: {
                JsonObjectRequest jsonreq = new JsonObjectRequest(Request.Method.GET, PI + "/stop",
                        null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                    }
                },
                        new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                ((TextView) findViewById(R.id.status_bridge)).setText("can not connect to pi");
                            }
                        });
                jsonreq.setTag(TAG);
                requestQueue.add(jsonreq);
            }break;
            case R.id.button_shutdown: {
                JsonObjectRequest jsonreq = new JsonObjectRequest(Request.Method.GET, PI + "/shutdown",
                        null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                    }
                },
                        new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                ((TextView) findViewById(R.id.status_bridge)).setText("can not connect to pi");
                            }
                        });
                jsonreq.setTag(TAG);
                requestQueue.add(jsonreq);
            } break;
            case R.id.button_sendcorners: {
                JSONArray arr = new JSONArray();
                if (points.size()==4) {
                    try {
                        for (int i = 0; i < points.size(); i++) {
                            Point p = points.get(i);
                            JSONObject obj = new JSONObject();
                            obj.put("x", p.x);
                            obj.put("y", p.y);
                            arr.put(obj);
                            Log.d("log", p.x + " " + p.y);
                        }
                    } catch (JSONException e) {
                        return;
                    }
                } else {
                    return;
                }

                JsonObjectRequestStringbody jsonreq = new JsonObjectRequestStringbody (Request.Method.PUT, PI + "/corners",
                        arr.toString(), new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                    }
                },
                        new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                ((TextView) findViewById(R.id.status_bridge)).setText("can not connect to pi");
                            }
                        });
                jsonreq.setTag(TAG);
                requestQueue.add(jsonreq);
            } break;
            default:
                break;
        }
    }

    private class UpdateOnTimerTask extends TimerTask {

        private int frame = 0;

        @Override
        public void run() {
            if (PI.length()==0) return;
            frame++;
            if (pointsUpdated || (frame % 2 == 0)) {
                JsonObjectRequest jsonreq = new JsonObjectRequest(Request.Method.GET, PI+"/status",
                        null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            ((TextView)findViewById(R.id.status_bridge)).setText(response.get("bridge").toString());
                            ((TextView)findViewById(R.id.status_stream)).setText(response.get("stream").toString());
                            ((TextView)findViewById(R.id.status_video)).setText(response.get("video").toString());
                            camSettings = response.getJSONObject("camerasettings");
                        } catch (JSONException e) {
                            ((TextView)findViewById(R.id.status_bridge)).setText("can not parse reply from pi");
                        }
                    }

                },
                        new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        ((TextView) findViewById(R.id.status_bridge)).setText("can not connect to pi");
                    }
                });
                jsonreq.setTag(TAG);
                requestQueue.add(jsonreq);
            } else {
                JsonArrayRequest jsonreq = new JsonArrayRequest(Request.Method.GET, PI+"/corners",
                        null, new Response.Listener<JSONArray>() {

                    @Override
                    public void onResponse(JSONArray response) {
                        try {
                            if (response.length()!=4) {
                                return;
                            }

                            ArrayList<Point> newpoints = new ArrayList<>();
                            for (int i=0;i<4;i++) {
                                JSONObject obj = response.getJSONObject(i);
                                Point p = new Point();
                                p.set( obj.getInt("x"), obj.getInt("y"));
                                newpoints.add(p);
                            }

                            points = newpoints;
                            pointsUpdated = true;
                            AreaOverlay overlay = findViewById(R.id.overlay);
                            overlay.setPoints(points);

                        } catch (JSONException e) {
                            ((TextView)findViewById(R.id.status_bridge)).setText("can not parse reply from pi");
                        }
                    }

                },
                        new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                ((TextView) findViewById(R.id.status_bridge)).setText("can not connect to pi");
                            }
                        });
                jsonreq.setTag(TAG);
                requestQueue.add(jsonreq);
            }

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.settings_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // action with ID action_refresh was selected
            case R.id.action_settings:
                final Dialog dialog = new Dialog(this);
                dialog.setContentView(R.layout.settings);
                dialog.setTitle("Settings");

                try {
                    sliderFromSettings( (SeekBar) dialog.findViewById(R.id.brightness), "brightness" );
                    sliderFromSettings( (SeekBar) dialog.findViewById(R.id.saturation), "saturation" );
                    sliderFromSettings( (SeekBar) dialog.findViewById(R.id.contrast), "contrast" );
                    sliderFromSettings( (SeekBar) dialog.findViewById(R.id.whitebalance_r), "whitebalance_r" );
                    sliderFromSettings( (SeekBar) dialog.findViewById(R.id.whitebalance_b), "whitebalance_b" );
                    sliderFromSettings( (SeekBar) dialog.findViewById(R.id.iso), "iso" );
                    sliderFromSettings( (SeekBar) dialog.findViewById(R.id.shutter), "shutter" );
                    sliderFromSettings( (SeekBar) dialog.findViewById(R.id.area), "area" );
                    sliderFromSettings( (SeekBar) dialog.findViewById(R.id.smooth), "smooth" );
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                final EditText editText = dialog.findViewById(R.id.textInputEditText);
                if (PI.length()>0) {
                    editText.setText(PI);
                }
                Button saveButton = dialog.findViewById(R.id.buttonSave);
                saveButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String value = editText.getText().toString();
                        if (!value.startsWith("http")) {
                            value = "http://" + value;
                        }
                        if (value.lastIndexOf(':') < 5) {
                            value = value + ":8999";
                        }
                        if (value.compareTo(PI)!=0) {
                            PI = value;
                            writeSettings();
                        }
                        try {
                            JSONObject sendsettings = new JSONObject();

                            setValueIfExists( (SeekBar) dialog.findViewById(R.id.brightness), "brightness", sendsettings);
                            setValueIfExists( (SeekBar) dialog.findViewById(R.id.saturation), "saturation", sendsettings);
                            setValueIfExists( (SeekBar) dialog.findViewById(R.id.contrast), "contrast", sendsettings);
                            setValueIfExists( (SeekBar) dialog.findViewById(R.id.whitebalance_r), "whitebalance_r", sendsettings);
                            setValueIfExists( (SeekBar) dialog.findViewById(R.id.whitebalance_b), "whitebalance_b", sendsettings);
                            setValueIfExists( (SeekBar) dialog.findViewById(R.id.iso), "iso", sendsettings);
                            setValueIfExists( (SeekBar) dialog.findViewById(R.id.shutter), "shutter", sendsettings);
                            setValueIfExists( (SeekBar) dialog.findViewById(R.id.area), "area", sendsettings);
                            setValueIfExists( (SeekBar) dialog.findViewById(R.id.smooth), "smooth", sendsettings);

                            if (sendsettings.has("whitebalance_r")) {
                                setValueIfExists( (SeekBar) dialog.findViewById(R.id.whitebalance_b), "whitebalance_b", sendsettings, true);
                            } else if (sendsettings.has("whitebalance_b")) {
                                setValueIfExists( (SeekBar) dialog.findViewById(R.id.whitebalance_r), "whitebalance_r", sendsettings, true);
                            }

                            if (sendsettings.length()!=0) {
                                JsonObjectRequest jsonreq = new JsonObjectRequest (Request.Method.PUT, PI + "/camsettings",
                                        sendsettings, new Response.Listener<JSONObject>() {
                                    @Override
                                    public void onResponse(JSONObject response) {
                                    }
                                },
                                        new Response.ErrorListener() {
                                            @Override
                                            public void onErrorResponse(VolleyError error) {
                                                ((TextView) findViewById(R.id.status_bridge)).setText("can not connect to pi");
                                            }
                                        });
                                jsonreq.setTag(TAG);
                                requestQueue.add(jsonreq);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        dialog.dismiss();
                    }
                });
                Button cancelButton = dialog.findViewById(R.id.buttonCancel);
                cancelButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                });

                dialog.show();
                break;
            default:
                break;
        }

        return true;
    }

    private Pair<Integer,Integer> minMax(int Id) {
        Pair<Integer, Integer> p = null;
        switch (Id) {
            case R.id.brightness:
                p = new Pair<>(0,100);
                break;
            case R.id.saturation:
                p = new Pair<>(-100,100);
                break;
            case R.id.contrast:
                p = new Pair<>(-100,100);
                break;
            case R.id.whitebalance_r:
                p = new Pair<>(0,8);
                break;
            case R.id.whitebalance_b:
                p = new Pair<>(0,8);
                break;
            case R.id.iso:
                p = new Pair<>(100,800);
                break;
            case R.id.shutter:
                p = new Pair<>(0,330000);
                break;
            case R.id.area:
                p = new Pair<>(5,200);
                break;
            case R.id.smooth:
                p = new Pair<>(0,100);
                break;
            default:
                p = new Pair<>(0,100);
            break;
        }
        return p;
    }

    private int defaultValue(int Id) {
        int ret = 0;
        switch (Id) {
            case R.id.brightness:
                ret = 50;
                break;
            case R.id.iso:
                ret = 400;
                break;
            case R.id.shutter:
                ret = 40000;
                break;
            case R.id.whitebalance_r:
            case R.id.whitebalance_b:
                ret = 2;
                break;
            case R.id.area:
                ret = 20;
                break;
            case R.id.smooth:
                ret = 0;
                break;
            default:
                break;
        }
        return ret;
    }

    private void setValueIfExists( SeekBar b, String key, JSONObject target ) throws JSONException {
        setValueIfExists(b,key, target, false);
    }

    private void setValueIfExists( SeekBar b, String key, JSONObject target, boolean force ) throws JSONException {
        int oldvalue = -100000;
        if (camSettings.has(key)) {
            oldvalue = camSettings.getInt(key);
        }
        int newvalueSlider = b.getProgress();

        Pair<Integer, Integer> p = minMax(b.getId());
            int newvalue;
        switch (b.getId()) {
            case R.id.whitebalance_r:
            case R.id.whitebalance_b:
                newvalue = newvalueSlider;
                break;
            default:
                newvalue = Math.round (((float)newvalueSlider / 1000.0f) * (float)(p.second-p.first)) + p.first;
                break;
        }

        if (oldvalue != newvalue || force) {
            target.put(key, newvalue);
            camSettings.put(key, newvalue);
        }
    }

    private void sliderFromSettings(SeekBar seekbar, String key) throws JSONException {
        int value = 0;
        if (!camSettings.has(key)) {
            value = defaultValue(seekbar.getId());
        } else {
            value = camSettings.getInt(key);
        }
        Pair<Integer, Integer> p = minMax(seekbar.getId());
        int slidervalue;

        switch (seekbar.getId()) {
            case R.id.whitebalance_r:
            case R.id.whitebalance_b:
                slidervalue = value;
                break;
            default:
                slidervalue = Math.round(((float) value - p.first) / (float) (p.second - p.first) * 1000.0f);
                break;
        }
        seekbar.setProgress(slidervalue);
    }


}
