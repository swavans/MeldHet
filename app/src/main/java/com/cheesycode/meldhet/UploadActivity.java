package com.cheesycode.meldhet;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationProvider;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.github.jlmd.animatedcircleloadingview.AnimatedCircleLoadingView;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.cheesycode.meldhet.helper.ConfigHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.UUID;

public class UploadActivity extends AppCompatActivity {
    //Firebase
    FirebaseStorage storage;
    StorageReference storageReference;
    AnimatedCircleLoadingView animatedCircleLoadingView;
    private boolean skipMethod = false;
    private String imagePath;
    private FusedLocationProviderClient mFusedLocationClient;
    private static Location location = null;
    private String issueType;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        if(ContextCompat.checkSelfPermission( this, android.Manifest.permission.ACCESS_FINE_LOCATION ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission( this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {


            mFusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            // Got last known location. In some rare situations this can be null.
                            if (location != null) {
                                if(UploadActivity.location == null){
                                    UploadActivity.location = location;
                                }
                                if(UploadActivity.location.getAccuracy() > location.getAccuracy()){
                                    UploadActivity.location = location;
                                }
                            }
                        }
                    });
            IntroActivity.setWindowFlag(this, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, false);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
            animatedCircleLoadingView = findViewById(R.id.circle_loading_view);
            storage = FirebaseStorage.getInstance();
            storageReference = storage.getReference();
            Intent intent = getIntent();
            String filepath = intent.getStringExtra("filepath");
            issueType = intent.getStringExtra("issueType");
            if (!skipMethod) {
                uploadImage(filepath);
            }
        }
        //HANDLE NO PERMISSION
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus); if(!skipMethod){ animatedCircleLoadingView.startDeterminate();
        } skipMethod = true;}

    private void uploadImage(String filePath) {

        if(filePath != null)
        {
            try {
                InputStream is = new FileInputStream(filePath);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                BitmapFactory.decodeStream(is).compress(Bitmap.CompressFormat.JPEG, 50, out);
                StorageReference ref = storageReference.child("images/" + UUID.randomUUID().toString());
                imagePath = ref.getName();

                byte[] bytes = out.toByteArray();
                ref.putBytes(bytes)
                        .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                Toast.makeText(UploadActivity.this, "Uploaded", Toast.LENGTH_SHORT).show();
                                Volleypostfunc();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                animatedCircleLoadingView.stopFailure();
                                Toast.makeText(UploadActivity.this, "Failed " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        })
                        .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                                double progress = (99.0 * taskSnapshot.getBytesTransferred() / taskSnapshot
                                        .getTotalByteCount());
                                animatedCircleLoadingView.setPercent((int)Math.round(progress));
                            }
                        });
            }
            catch (FileNotFoundException e){

            }

        }
    }

    public void Volleypostfunc()
    {
        File file = new File(imagePath);
        file.delete();
        if(location == null) {
            Toast.makeText(UploadActivity.this, "Locatie niet gevonden", Toast.LENGTH_LONG).show();
            animatedCircleLoadingView.stopFailure();

            //TODO Further error handling
            return;
        }

        String postUrl = ConfigHelper.getConfigValue(this, "api_url") + "create/";
        try {
            RequestQueue requestQueue = Volley.newRequestQueue(this);

            JSONObject jsonBody = new JSONObject();
            jsonBody.put("id", MessagingService.getToken(this));
            jsonBody.put("image", imagePath);
            jsonBody.put("tag", issueType);
            jsonBody.put("lat", location.getLatitude());
            jsonBody.put("lon", location.getLongitude());
            jsonBody.put("acc", location.getAccuracy());

            final String mRequestBody = jsonBody.toString();

            StringRequest stringRequest = new StringRequest(Request.Method.POST, postUrl, new Response.Listener<String>(){
                @Override
                public void onResponse(String response) {
                    Log.i("LOG_VOLLEY", response);
                    animatedCircleLoadingView.stopOk();
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    animatedCircleLoadingView.stopFailure();
//                    Toast.makeText(UploadActivity.this, "Failed " +error, Toast.LENGTH_SHORT).show();
                    Log.e("LOG_VOLLEY", error.toString());
                }
            }) {
                @Override
                public String getBodyContentType() {
                    return "application/json; charset=utf-8";
                }

                @Override
                public byte[] getBody() throws AuthFailureError {
                    try {
                        return mRequestBody == null ? null : mRequestBody.getBytes("utf-8");
                    } catch (UnsupportedEncodingException uee) {
                        animatedCircleLoadingView.stopFailure();
                        VolleyLog.wtf("Unsupported Encoding while trying to get the bytes of %s using %s", mRequestBody, "utf-8");
                        return null;
                    }
                }

                @Override
                protected Response<String> parseNetworkResponse(NetworkResponse response) {
                    String responseString = "";
                    if (response != null) {
                        responseString = String.valueOf(response.statusCode);
                    }
                    return Response.success(responseString, HttpHeaderParser.parseCacheHeaders(response));
                }
            };

            requestQueue.add(stringRequest);
        } catch (JSONException e) {
            e.printStackTrace();
            animatedCircleLoadingView.stopFailure();
//            Toast.makeText(UploadActivity.this, "Failed " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
