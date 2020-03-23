// Copyright 2018 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.google.firebase.samples.apps.mlkit;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.CameraProfile;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityCompat.OnRequestPermissionsResultCallback;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.common.annotation.KeepName;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
//import com.google.firebase.ml.common.FirebaseMLException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Parameter;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import pl.droidsonroids.gif.GifImageView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;

/**
 * Demo app showing the various features of ML Kit for Firebase. This class is used to
 * set up continuous frame processing on frames from a camera source.
 */
@KeepName
public final class LivePreviewActivity extends AppCompatActivity
        implements OnRequestPermissionsResultCallback,
        CompoundButton.OnCheckedChangeListener {
    private static final String TAG = "LivePreviewActivity";
    private static final int PERMISSION_REQUESTS = 1;

    private CameraSource cameraSource = null;
    private CameraSourcePreview preview;
    private GraphicOverlay graphicOverlay;
    private GifImageView GIFimg;

    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;
//    public static final String BASE_URL = "https://shrouded-lake-86672.herokuapp.com/";

    private String fileString = "";
    private Retrofit retrofit;
    private API service;
    private boolean uiChange = true, isRunning = true;

    private long mTimeLeftInMillis = 60000;
    private CountDownTimer countDownTimer;
    private ArrayList<Integer> scoreList;
    private ImageView dot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");

        setContentView(R.layout.activity_live_preview);
        Toast.makeText(this, getIntent().getExtras().getString("ip"), Toast.LENGTH_SHORT).show();
        scoreList = new ArrayList<>();
        preview = (CameraSourcePreview) findViewById(R.id.inside_fire_preview);
        GIFimg = findViewById(R.id.outside_gif);
        dot = findViewById(R.id.dot);
        if (preview == null) {
            Log.d(TAG, "Preview is null");
        }
        graphicOverlay = (GraphicOverlay) findViewById(R.id.fireFaceOverlay);
        if (graphicOverlay == null) {
            Log.d(TAG, "graphicOverlay is null");
        }

        Spinner spinner = (Spinner) findViewById(R.id.spinner);
        spinner.setVisibility(View.GONE);
//    List<String> options = new ArrayList<>();
//    options.add(FACE_DETECTION);
//    options.add(TEXT_DETECTION);
//    options.add(BARCODE_DETECTION);
//    options.add(IMAGE_LABEL_DETECTION);
//    options.add(CLASSIFICATION);
        // Creating adapter for spinner
//    ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(this, R.layout.spinner_style, options);
        // Drop down layout style - list view with radio button
//    dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // attaching data adapter to spinner
//    spinner.setAdapter(dataAdapter);
//    spinner.setOnItemSelectedListener(this);

        ToggleButton facingSwitch = (ToggleButton) findViewById(R.id.facingswitch);
        facingSwitch.setOnCheckedChangeListener(this);

        Button captureBtn = findViewById(R.id.captureBtn);
        final Button recordBtn = findViewById(R.id.recordBtn);

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .readTimeout(60, TimeUnit.SECONDS)
                .connectTimeout(60, TimeUnit.SECONDS)
                .build();

        retrofit = new Retrofit.Builder()
                .client(okHttpClient)
                .baseUrl(Objects.requireNonNull(getIntent().getExtras().getString("ip")))
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        service = retrofit.create(API.class);

        captureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cameraSource.camera.takePicture(null, null, mPicture);
            }
        });

        final TextView recordTV = findViewById(R.id.record_msg_tv);
        final TextView timeleftTV = findViewById(R.id.time_left_tv);


        recordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (isRecording) {
                    // stop recording and release camera
                    if (countDownTimer != null) countDownTimer.cancel();
                    if (uiChange) {
                        recordBtn.setBackground(getDrawable(R.drawable.circle));
                        recordTV.setText("Please adjust your eyes and tap to start");
                        timeleftTV.setVisibility(View.GONE);
                        preview.setVisibility(View.VISIBLE);
                        GIFimg.setVisibility(View.GONE);
                        dot.setVisibility(View.GONE);
                        isRunning = false;
                    }

                    mediaRecorder.stop();  // stop the recording
                    releaseMediaRecorder(); // release the MediaRecorder object
                    mCamera.lock();         // take camera access back from MediaRecorder

                    // inform the user that recording has stopped
//                    recordBtn.setText("Cap");
                    isRecording = false;

                } else {
                    // initialize video camera
                    if (uiChange) {
//                        timeleftTV.setVisibility(View.VISIBLE);
                        recordBtn.setBackground(getDrawable(R.drawable.ic_pause_circle_outline_black_24dp));
                        recordTV.setText("Don't move your head and mlkit on candle");
                        preview.setVisibility(View.GONE);
                        GIFimg.setVisibility(View.VISIBLE);
                        dot.setVisibility(View.VISIBLE);
                        isRunning = true;
                    }

                    if (prepareVideoRecorder()) {
                        // Camera is available and unlocked, MediaRecorder is prepared,
                        // now you can start recording
                        mediaRecorder.start();

                        // inform the user that recording has started
//                        recordBtn.setText("Stop");
                        isRecording = true;
                    } else {
                        // prepare didn't work, release the camera
                        releaseMediaRecorder();
                        // inform user
                    }


                    countDownTimer = new CountDownTimer(mTimeLeftInMillis, 1000) {
                        @Override
                        public void onTick(long millisUntilFinished) {
                            int mins = (int) millisUntilFinished / (60 * 1000);
                            int seconds = ((int) (millisUntilFinished / 1000) - (mins * 60));
                            DecimalFormat format = new DecimalFormat("00");
                            String timeleftStr = format.format(mins) + ":" + format.format(seconds);
                            timeleftTV.setText(timeleftStr);
                        }

                        @Override
                        public void onFinish() {
                            Toast.makeText(getApplicationContext(), "Countdown Over", Toast.LENGTH_SHORT).show();
                            uiChange = false;
                            recordBtn.callOnClick();

                            recordBtn.callOnClick();
                            uiChange = true;
                        }
                    };
                    countDownTimer.start();
                }

            }
        });

        if (allPermissionsGranted()) {
            createCameraSource();
        } else {
            getRuntimePermissions();
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        Log.d(TAG, "Set facing");
        if (cameraSource != null) {
            if (!isChecked) {
                cameraSource.setFacing(CameraSource.CAMERA_FACING_FRONT);
            } else {
                cameraSource.setFacing(CameraSource.CAMERA_FACING_BACK);
            }
        }
        preview.stop();
        startCameraSource();
    }

    private void createCameraSource() {
        // If there's no existing cameraSource, create one.
        if (cameraSource == null) {
            cameraSource = new CameraSource(this, graphicOverlay);
            cameraSource.setFacing(CameraSource.CAMERA_FACING_FRONT);
        }
    }

    /**
     * Starts or restarts the camera source, if it exists. If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    private void startCameraSource() {
        if (cameraSource != null) {
            try {
                if (preview == null) {
                    Log.d(TAG, "resume: Preview is null");
                }
                if (graphicOverlay == null) {
                    Log.d(TAG, "resume: graphOverlay is null");
                }
                preview.start(cameraSource, graphicOverlay);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                cameraSource.release();
                cameraSource = null;
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        startCameraSource();
    }

    /**
     * Stops the camera.
     */
    @Override
    protected void onPause() {
        super.onPause();
        releaseMediaRecorder();
        preview.stop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (cameraSource != null) {
            cameraSource.release();
        }
    }

    private String[] getRequiredPermissions() {
        try {
            PackageInfo info =
                    this.getPackageManager()
                            .getPackageInfo(this.getPackageName(), PackageManager.GET_PERMISSIONS);
            String[] ps = info.requestedPermissions;
            if (ps != null && ps.length > 0) {
                return ps;
            } else {
                return new String[0];
            }
        } catch (Exception e) {
            return new String[0];
        }
    }

    private boolean allPermissionsGranted() {
        for (String permission : getRequiredPermissions()) {
            if (!isPermissionGranted(this, permission)) {
                return false;
            }
        }
        return true;
    }

    private void getRuntimePermissions() {
        List<String> allNeededPermissions = new ArrayList<>();
        for (String permission : getRequiredPermissions()) {
            if (!isPermissionGranted(this, permission)) {
                allNeededPermissions.add(permission);
            }
        }

        if (!allNeededPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(
                    this, allNeededPermissions.toArray(new String[0]), PERMISSION_REQUESTS);
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        Log.i(TAG, "Permission granted!");
        if (allPermissionsGranted()) {
            createCameraSource();
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private static boolean isPermissionGranted(Context context, String permission) {
        if (ContextCompat.checkSelfPermission(context, permission)
                == PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "Permission granted: " + permission);
            return true;
        }
        Log.i(TAG, "Permission NOT granted: " + permission);
        return false;
    }


    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
            if (pictureFile == null) {
                Log.d(TAG, "Error creating media file, check storage permissions");
                return;
            }

            try {
                preview.stop();
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();
                Log.d(TAG, "onPictureTaken: Picture saved successfully");
                startCameraSource();
            } catch (FileNotFoundException e) {
                Log.d(TAG, "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d(TAG, "Error accessing file: " + e.getMessage());
            }
        }
    };

    private static File getOutputMediaFile(int type) {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "MyCameraApp");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_" + timeStamp + ".jpg");
        } else if (type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_" + timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }

    private Camera mCamera;
    private MediaRecorder mediaRecorder;
    private boolean isRecording = false;

    private boolean prepareVideoRecorder() {

        mCamera = cameraSource.camera;
        mediaRecorder = new MediaRecorder();

        Camera.Parameters params = mCamera.getParameters();
        params.setRotation(270);
//    mCamera.setDisplayOrientation(90);
        mediaRecorder.setOrientationHint(270);
        mCamera.setParameters(params);

        // Step 1: Unlock and set camera to MediaRecorder
        mCamera.unlock();
        mediaRecorder.setCamera(mCamera);

        // Step 2: Set sources
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
//    mediaRecorder.setOrientationHint(90);

        // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
//    mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_LOW));
        mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_480P));


        // Step 4: Set output file
        fileString = getOutputMediaFile(MEDIA_TYPE_VIDEO).toString();
        mediaRecorder.setOutputFile(fileString);

        // Step 5: Set the preview output
        mediaRecorder.setPreviewDisplay(preview.surfaceView.getHolder().getSurface());

        // Step 6: Prepare configured MediaRecorder
        try {
            mediaRecorder.prepare();
        } catch (IllegalStateException e) {
            Log.d(TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
            Log.d(TAG, "IOException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        }
        return true;
    }

    private void releaseMediaRecorder() {
        if (mediaRecorder != null) {
            mediaRecorder.reset();   // clear recorder configuration
            mediaRecorder.release(); // release the recorder object
            mediaRecorder = null;
            mCamera.lock(); // lock camera for later use

            if (!fileString.equals("")) {
                upload_to_firebase();
                Toast.makeText(getApplicationContext(), "Uploading Video to server ...", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void upload_to_firebase() {

        final ProgressDialog progressDialog = new ProgressDialog(LivePreviewActivity.this);
        if (uiChange) {
            progressDialog.setMessage("Uploading to server...");
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.setCancelable(false);
            progressDialog.show();
        } else {
            Toast.makeText(getApplicationContext(), "Sending chunk data...", Toast.LENGTH_SHORT).show();
        }
        StorageReference mStorageRef = FirebaseStorage.getInstance().getReference();
        File videoFile = new File(fileString);
        Uri file = Uri.fromFile(videoFile);
        fileString = "";
        final StorageReference ref = mStorageRef.child("images").child(videoFile.getName());
        UploadTask uploadTask = ref.putFile(file);

        Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
            @Override
            public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }

                // Continue with the task to get the download URL
                return ref.getDownloadUrl();
            }
        }).addOnCompleteListener(new OnCompleteListener<Uri>() {
            @Override
            public void onComplete(@NonNull Task<Uri> task) {
                if (task.isSuccessful()) {
                    Uri downloadUri = task.getResult();
                    Toast.makeText(getApplicationContext(), downloadUri.toString(), Toast.LENGTH_LONG).show();
                    send_url_to_server(downloadUri.toString());
                    if (uiChange) progressDialog.dismiss();
                } else {
                    // Handle failures
                    // ...
                }
            }
        });

    }

    private void send_url_to_server(String url) {
        final ProgressDialog progressDialog = new ProgressDialog(LivePreviewActivity.this);
        if (!isRunning) {
            progressDialog.setMessage("Please wait while we are calculating results...");
            progressDialog.setCancelable(false);
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.show();
        }
        API api = retrofit.create(API.class);
//        Call<Response> response = api.send_url(url);
        Param param = new Param(url);
        Call<JsonResponse> response = api.send_url(param);
        response.enqueue(new Callback<JsonResponse>() {
            @Override
            public void onResponse(Call<JsonResponse> call, retrofit2.Response<JsonResponse> response) {

//                Toast.makeText(getApplicationContext(), response + "", Toast.LENGTH_LONG).show();
                if (response.isSuccessful()) {
                    Log.d("LivePreview Activity : ", response + "");

                    Float score = response.body().getMessage();
                    if (score != -1) {
                        if (!isRunning) {
                            int totalScore = (int) (score * 100);
                            for (int i = 0; i < scoreList.size(); i++)
                                totalScore += scoreList.get(i);
                            if (scoreList.size() > 0)
                                totalScore = totalScore / scoreList.size();
                            scoreList.clear();
                            progressDialog.dismiss();
                            Toast.makeText(getApplicationContext(), "You're " + totalScore + "% focused", Toast.LENGTH_LONG).show();
                            new AlertDialog.Builder(LivePreviewActivity.this)
                                    .setTitle("Your overall focus")
                                    .setMessage(totalScore + "%")

                                    // Specifying a listener allows you to take an action before dismissing the dialog.
                                    // The dialog is automatically dismissed when a dialog button is clicked.
                                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            // Continue with delete operation
                                        }
                                    })

                                    // A null listener allows the button to dismiss the dialog and take no further action.
                                    .setNegativeButton(android.R.string.ok, null)
                                    .setIcon(android.R.drawable.ic_dialog_info)
                                    .show();
                        } else
                            scoreList.add((int) (score * 100));

                    }

                }
                Log.d("LivePreview Activity : ", "SUCESS");
                if (progressDialog.isShowing()) progressDialog.dismiss();
            }

            @Override
            public void onFailure(Call<JsonResponse> call, Throwable t) {
                if (progressDialog.isShowing()) progressDialog.dismiss();
                Toast.makeText(getApplicationContext(), t.getMessage() + "", Toast.LENGTH_LONG).show();

            }
        });
//        response.enqueue(new Callback<Response>() {
//            @Override
//            public void onResponse(Call<Response> call, retrofit2.Response<Response> response) {
//                if (response.isSuccessful()) {
//                    Log.d("LivePreview Activity : ", response+"");
//                    Integer score = response.body().getScore();
//                    if (score != -1){
//                        if(!isRunning){
//                            int totalScore = score;
//                            for (int i=0; i<scoreList.size(); i++)
//                                totalScore += scoreList.get(i);
//                            totalScore = totalScore/scoreList.size();
//                            scoreList.clear();
//                            progressDialog.dismiss();
//                            if (totalScore > 70){
//                                Toast.makeText(getApplicationContext(), "You're " + totalScore + "% focussed", Toast.LENGTH_LONG).show();
//                            }
//
//                        }else
//                            scoreList.add(score);
//
//                    }
//
//                }
//                Log.d("LivePreview Activity : ", "SUCESS");
//                if(progressDialog.isShowing()) progressDialog.dismiss();
//            }
//
//            @Override
//            public void onFailure(Call<Response> call, Throwable t) {
//                progressDialog.cancel();
//                Toast.makeText(getApplicationContext(), t.getMessage(), Toast.LENGTH_LONG).show();
//            }
//        });
    }

    private void upload_video() {

        File file = new File(fileString);
        RequestBody requestFile =
                RequestBody.create(MediaType.parse("multipart/form-data"), file);

// MultipartBody.Part is used to send also the actual file name
        MultipartBody.Part body =
                MultipartBody.Part.createFormData("file", file.getName(), requestFile);
        fileString = "";
        API api = retrofit.create(API.class);
        Call<Response> response = api.upload(body);

        response.enqueue(new Callback<Response>() {
            @Override
            public void onResponse(Call<Response> call, retrofit2.Response<Response> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(getApplicationContext(), response + "", Toast.LENGTH_LONG).show();
                    Log.d("LivePreview Activity : ", "SUCESS");
                }
                Log.d("LivePreview Activity : ", "DONE");
            }

            @Override
            public void onFailure(Call<Response> call, Throwable t) {

            }
        });

    }

}