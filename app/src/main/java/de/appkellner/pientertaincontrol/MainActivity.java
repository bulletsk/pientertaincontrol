package de.appkellner.pientertaincontrol;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.JsonRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private final String TAG="MainActivity";

    private final String PI = "http://192.168.2.114:8999";

    Timer timer = new Timer();
    RequestQueue requestQueue;

    ArrayList<Point> points = new ArrayList<>();

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
        points.add(new Point(1920,0));
        points.add(new Point(0,1080));
        points.add(new Point(1920,1080));

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
}