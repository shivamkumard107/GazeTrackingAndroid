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
package com.google.firebase.samples.apps.mlkit

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Camera
import android.hardware.Camera.PictureCallback
import android.media.CamcorderProfile
import android.media.MediaRecorder
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.OnRequestPermissionsResultCallback
import androidx.core.content.ContextCompat
import com.google.android.gms.common.annotation.KeepName
import pl.droidsonroids.gif.GifImageView
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

//import com.google.firebase.ml.common.FirebaseMLException;
@KeepName
class LivePreviewActivity : AppCompatActivity(), OnRequestPermissionsResultCallback, CompoundButton.OnCheckedChangeListener {
    private var cameraSource: CameraSource? = null
    private var preview: CameraSourcePreview? = null
    private var graphicOverlay: GraphicOverlay? = null
    private var GIFimg: GifImageView? = null

    //    public static final String BASE_URL = "https://shrouded-lake-86672.herokuapp.com/";
    private var fileString = ""

    //    private var retrofit: Retrofit? = null
//    private var service: API? = null
    private var uiChange = true
    private var isRunning = true
    private val mTimeLeftInMillis: Long = 60000
    private var countDownTimer: CountDownTimer? = null
    private var scoreList: ArrayList<Int>? = null
    private var dot: ImageView? = null
    private var recordBtn: Button? = null
    private var chunk_count = 0

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
        setContentView(R.layout.activity_live_preview)
        Toast.makeText(this, intent.extras!!.getString("ip"), Toast.LENGTH_SHORT).show()
        preview = findViewById<View>(R.id.inside_fire_preview) as CameraSourcePreview
        GIFimg = findViewById(R.id.outside_gif)
        dot = findViewById(R.id.dot)
        if (preview == null) {
            Log.d(TAG, "Preview is null")
        }
        graphicOverlay = findViewById<View>(R.id.fireFaceOverlay) as GraphicOverlay
        if (graphicOverlay == null) {
            Log.d(TAG, "graphicOverlay is null")
        }
        val spinner = findViewById<View>(R.id.spinner) as Spinner
        spinner.visibility = View.GONE
        val facingSwitch = findViewById<View>(R.id.facingswitch) as ToggleButton
        facingSwitch.setOnCheckedChangeListener(this)
        val captureBtn = findViewById<Button>(R.id.captureBtn)
        recordBtn = findViewById(R.id.recordBtn)
//        val okHttpClient = OkHttpClient.Builder()
//                .readTimeout(60, TimeUnit.SECONDS)
//                .connectTimeout(60, TimeUnit.SECONDS)
//                .build()
//        retrofit = Retrofit.Builder()
//                .client(okHttpClient)
//                .baseUrl(intent.extras!!.getString("ip"))
//                .addConverterFactory(GsonConverterFactory.create())
//                .build()
//        service = retrofit!!.create(API::class.java)
        val recordTV = findViewById<TextView>(R.id.record_msg_tv)
        val tv3sec = findViewById<TextView>(R.id.tv_3sec)
        val start_inforLL = findViewById<LinearLayout>(R.id.start_infoTV)
        val threeSecTimer: CountDownTimer = object : CountDownTimer(5000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = millisUntilFinished.toInt() / 1000
                tv3sec.text = seconds.toString()
            }

            override fun onFinish() {
                Toast.makeText(applicationContext, "Countdown Over", Toast.LENGTH_SHORT).show()
                start_inforLL.visibility = View.GONE
                captureBtn.isEnabled = true
                recordBtn!!.callOnClick()
            }
        }
        captureBtn.setOnClickListener { //                cameraSource.camera.takePicture(null, null, mPicture);
            scoreList = ArrayList()
            chunk_count = 1
            start_inforLL.visibility = View.VISIBLE
            preview!!.visibility = View.GONE
            GIFimg!!.visibility = View.VISIBLE
            dot!!.visibility = View.VISIBLE
            recordTV.text = "Don't move your head and focus on candle"
            captureBtn.isEnabled = false
            threeSecTimer.start()
        }
        val timeleftTV = findViewById<TextView>(R.id.time_left_tv)
        val alertDialog = AlertDialog.Builder(this@LivePreviewActivity)
                .setMessage("To get accurate results, kindly please adjust your head and keep your eyes inside the overlay")
                .setPositiveButton("Ok") { dialog, which -> dialog.dismiss() }
                .setCancelable(true)
                .create()
        alertDialog.show()
        recordBtn!!.setOnClickListener(View.OnClickListener {
            if (isRecording) {
                // stop recording and release camera
                if (countDownTimer != null) countDownTimer!!.cancel()
                if (uiChange) {
                    recordBtn!!.background = getDrawable(R.drawable.circle)
                    recordTV.text = "Please adjust your eyes and tap to start"
                    timeleftTV.visibility = View.GONE
                    preview!!.visibility = View.VISIBLE
                    GIFimg!!.visibility = View.GONE
                    dot!!.visibility = View.GONE
                    isRunning = false
                    recordBtn!!.visibility = View.GONE
                    captureBtn.visibility = View.VISIBLE
                }
                mediaRecorder!!.stop() // stop the recording
                releaseMediaRecorder() // release the MediaRecorder object
                mCamera!!.lock() // take camera access back from MediaRecorder
                isRecording = false
            } else {
                // initialize video camera
                if (uiChange) {
//                        timeleftTV.setVisibility(View.VISIBLE);
                    captureBtn.visibility = View.GONE
                    recordBtn!!.visibility = View.VISIBLE
                    recordBtn!!.background = getDrawable(R.drawable.ic_pause_circle_outline_black_24dp)
                    isRunning = true
                }
                if (prepareVideoRecorder()) {
                    // Camera is available and unlocked, MediaRecorder is prepared,
                    // now you can start recording
                    mediaRecorder!!.start()

                    // inform the user that recording has started
//                        recordBtn.setText("Stop");
                    isRecording = true
                } else {
                    // prepare didn't work, release the camera
                    releaseMediaRecorder()
                    // inform user
                }
                countDownTimer = object : CountDownTimer(mTimeLeftInMillis, 1000) {
                    override fun onTick(millisUntilFinished: Long) {
                        val mins = millisUntilFinished.toInt() / (60 * 1000)
                        val seconds = (millisUntilFinished / 1000) - mins * 60
                        val format = DecimalFormat("00")
                        val timeleftStr = format.format(mins.toLong()) + ":" + format.format(seconds.toLong())
                        timeleftTV.text = timeleftStr
                    }

                    override fun onFinish() {
                        Toast.makeText(applicationContext, "Countdown Over", Toast.LENGTH_SHORT).show()
                        chunk_count++
                        uiChange = false
                        recordBtn!!.callOnClick()
                        recordBtn!!.callOnClick()
                        uiChange = true
                    }
                }
                countDownTimer!!.start()
            }
        })
        if (allPermissionsGranted()) {
            createCameraSource()
        } else {
            runtimePermissions
        }
    }

    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        Log.d(TAG, "Set facing")
        if (cameraSource != null) {
            if (!isChecked) {
                cameraSource!!.setFacing(CameraSource.CAMERA_FACING_FRONT)
            } else {
                cameraSource!!.setFacing(CameraSource.CAMERA_FACING_BACK)
            }
        }
        preview!!.stop()
        startCameraSource()
    }

    private fun createCameraSource() {
        // If there's no existing cameraSource, create one.
        if (cameraSource == null) {
            cameraSource = CameraSource(this, graphicOverlay)
            cameraSource!!.setFacing(CameraSource.CAMERA_FACING_FRONT)
        }
    }

    /**
     * Starts or restarts the camera source, if it exists. If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    private fun startCameraSource() {
        if (cameraSource != null) {
            try {
                if (preview == null) {
                    Log.d(TAG, "resume: Preview is null")
                }
                if (graphicOverlay == null) {
                    Log.d(TAG, "resume: graphOverlay is null")
                }
                preview!!.start(cameraSource, graphicOverlay)
            } catch (e: IOException) {
                Log.e(TAG, "Unable to start camera source.", e)
                cameraSource!!.release()
                cameraSource = null
            }
        }
    }

    public override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume")
        startCameraSource()
    }

    /**
     * Stops the camera.
     */
    override fun onPause() {
        super.onPause()
        releaseMediaRecorder()
        preview!!.stop()
    }

    public override fun onDestroy() {
        super.onDestroy()
        if (cameraSource != null) {
            cameraSource!!.release()
        }
    }

    private val requiredPermissions: Array<String?>
        private get() = try {
            val info = this.packageManager
                    .getPackageInfo(this.packageName, PackageManager.GET_PERMISSIONS)
            val ps = info.requestedPermissions
            if (ps != null && ps.size > 0) {
                ps
            } else {
                arrayOfNulls(0)
            }
        } catch (e: Exception) {
            arrayOfNulls(0)
        }

    private fun allPermissionsGranted(): Boolean {
        for (permission in requiredPermissions) {
            if (!isPermissionGranted(this, permission)) {
                return false
            }
        }
        return true
    }

    private val runtimePermissions: Unit
        private get() {
            val allNeededPermissions: MutableList<String?> = ArrayList()
            for (permission in requiredPermissions) {
                if (!isPermissionGranted(this, permission)) {
                    allNeededPermissions.add(permission)
                }
            }
            if (!allNeededPermissions.isEmpty()) {
                ActivityCompat.requestPermissions(
                        this, allNeededPermissions.toTypedArray(), PERMISSION_REQUESTS)
            }
        }

    override fun onRequestPermissionsResult(
            requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        Log.i(TAG, "Permission granted!")
        if (allPermissionsGranted()) {
            createCameraSource()
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private val mPicture = PictureCallback { data, camera ->
        val pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE)
        if (pictureFile == null) {
            Log.d(TAG, "Error creating media file, check storage permissions")
            return@PictureCallback
        }
        try {
            preview!!.stop()
            val fos = FileOutputStream(pictureFile)
            fos.write(data)
            fos.close()
            Log.d(TAG, "onPictureTaken: Picture saved successfully")
            //                upload_image(pictureFile.toString());
            Toast.makeText(applicationContext, "Image saved successfully", Toast.LENGTH_SHORT).show()
            startCameraSource()
        } catch (e: FileNotFoundException) {
            Log.d(TAG, "File not found: " + e.message)
        } catch (e: IOException) {
            Log.d(TAG, "Error accessing file: " + e.message)
        }
    }
    private var mCamera: Camera? = null
    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    private fun prepareVideoRecorder(): Boolean {
        mCamera = cameraSource!!.camera
        mediaRecorder = MediaRecorder()
        val params = mCamera!!.parameters
        params.setRotation(270)
        //    mCamera.setDisplayOrientation(90);
        mediaRecorder!!.setOrientationHint(270)
        mCamera!!.parameters = params

        // Step 1: Unlock and set camera to MediaRecorder
        mCamera!!.unlock()
        mediaRecorder!!.setCamera(mCamera)

        // Step 2: Set sources
        mediaRecorder!!.setAudioSource(MediaRecorder.AudioSource.CAMCORDER)
        mediaRecorder!!.setVideoSource(MediaRecorder.VideoSource.CAMERA)
        //    mediaRecorder.setOrientationHint(90);

        // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
//    mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_LOW));
        mediaRecorder!!.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_480P))


        // Step 4: Set output file
        fileString = getOutputMediaFile(MEDIA_TYPE_VIDEO).toString()
        mediaRecorder!!.setOutputFile(fileString)

        // Step 5: Set the preview output
        mediaRecorder!!.setPreviewDisplay(preview!!.surfaceView.holder.surface)

        // Step 6: Prepare configured MediaRecorder
        try {
            mediaRecorder!!.prepare()
        } catch (e: IllegalStateException) {
            Log.d(TAG, "IllegalStateException preparing MediaRecorder: " + e.message)
            releaseMediaRecorder()
            return false
        } catch (e: IOException) {
            Log.d(TAG, "IOException preparing MediaRecorder: " + e.message)
            releaseMediaRecorder()
            return false
        }
        return true
    }

    private fun releaseMediaRecorder() {
        if (mediaRecorder != null) {
            mediaRecorder!!.reset() // clear recorder configuration
            mediaRecorder!!.release() // release the recorder object
            mediaRecorder = null
            mCamera!!.lock() // lock camera for later use
//            if (fileString != "") {
//                upload_to_firebase()
//                Toast.makeText(applicationContext, "Uploading Video to server ...", Toast.LENGTH_LONG).show()
//            }
        }
    }

//    private fun upload_to_firebase() {
//        val progressDialog = ProgressDialog(this@LivePreviewActivity)
//        if (uiChange) {
//            progressDialog.setMessage("Uploading to server...")
//            progressDialog.setCanceledOnTouchOutside(false)
//            progressDialog.setCancelable(false)
//            progressDialog.show()
//        } else {
//            Toast.makeText(applicationContext, "Sending chunk data...", Toast.LENGTH_SHORT).show()
//        }
//        val mStorageRef = FirebaseStorage.getInstance().reference
//        val videoFile = File(fileString)
//        val file = Uri.fromFile(videoFile)
//        fileString = ""
//        val ref = mStorageRef.child("images").child(videoFile.name)
//        val uploadTask = ref.putFile(file)
//        val urlTask = uploadTask.continueWithTask { task ->
//            if (!task.isSuccessful) {
//                throw task.exception!!
//            }
//
//            // Continue with the task to get the download URL
//            ref.downloadUrl
//        }.addOnCompleteListener { task ->
//            if (task.isSuccessful) {
//                val downloadUri = task.result
//                //                    Toast.makeText(getApplicationContext(), downloadUri.toString(), Toast.LENGTH_LONG).show();
////                send_url_to_server(downloadUri.toString())
//                if (uiChange) progressDialog.dismiss()
//            } else {
//                // Handle failures
//                // ...
//            }
//        }
//    }

//    private fun send_url_to_server(url: String) {
//        val progressDialog = ProgressDialog(this@LivePreviewActivity)
//        if (!isRunning) {
//            progressDialog.setMessage("Please wait while we are calculating results...")
//            progressDialog.setCancelable(false)
//            progressDialog.setCanceledOnTouchOutside(false)
//            progressDialog.show()
//        }
//        val api = retrofit!!.create(API::class.java)
//        //        Call<Response> response = api.send_url(url);
//        val param = Param(url)
//        val response = api.send_url(param)
//        response.enqueue(object : Callback<JsonResponse?> {
//            override fun onResponse(call: Call<JsonResponse?>, response: retrofit2.Response<JsonResponse?>) {
//
////                Toast.makeText(getApplicationContext(), response + "", Toast.LENGTH_LONG).show();
//                if (response.isSuccessful) {
//                    Log.d("LivePreview Activity : ", response.toString() + "")
//                    assert(response.body() != null)
//                    val score = response.body()!!.message
//                    scoreList!!.add((score * 100).toInt())
//                    Toast.makeText(applicationContext, "You're " + (score * 100).toInt() + "% focused", Toast.LENGTH_LONG).show()
//                    if (!isRunning && chunk_count == scoreList!!.size) {
//                        var totalScore = 0
//                        for (i in scoreList!!.indices) totalScore += scoreList!![i]
//                        if (scoreList!!.size > 0) totalScore = totalScore / scoreList!!.size
//                        if (totalScore < 0) totalScore = 0
//                        progressDialog.dismiss()
//                        AlertDialog.Builder(this@LivePreviewActivity)
//                                .setTitle("Your overall focus")
//                                .setMessage("$totalScore%") // Specifying a listener allows you to take an action before dismissing the dialog.
//                                // The dialog is automatically dismissed when a dialog button is clicked.
//                                .setPositiveButton(android.R.string.yes) { dialog, which ->
//                                    // Continue with delete operation
//                                } // A null listener allows the button to dismiss the dialog and take no further action.
//                                .setIcon(android.R.drawable.ic_dialog_info)
//                                .show()
//                    }
//                }
//                Log.d("LivePreview Activity : ", "SUCESS")
//                if (progressDialog.isShowing) progressDialog.dismiss()
//            }
//
//            override fun onFailure(call: Call<JsonResponse?>, t: Throwable) {
//                if (progressDialog.isShowing) progressDialog.dismiss()
//                Toast.makeText(applicationContext, t.message + "", Toast.LENGTH_LONG).show()
//            }
//        })

//    }

//    private fun upload_image(imgString: String) {
//        val dialog = ProgressDialog(this@LivePreviewActivity)
//        dialog.setMessage("Please wait while we'are checking whether your eyes are locked or not!!!")
//        dialog.show()
//        val file = File(imgString)
//        val requestFile = RequestBody.create(MediaType.parse("multipart/form-data"), file)
//        val body = MultipartBody.Part.createFormData("image", file.name, requestFile)
//        val api = retrofit!!.create(API::class.java)
//        val response = api.upload_image(body)
//        response.enqueue(object : Callback<Response?> {
//            override fun onResponse(call: Call<Response?>, response: retrofit2.Response<Response?>) {
//                Log.d("LivePreviewActivity", "onResponse called")
//                if (response.isSuccessful) {
//                    if (BuildConfig.DEBUG && response.body() == null) {
//                        error("Assertion failed")
//                    }
//                    val focus = response.body()!!.focused
//                    if (!focus) recordBtn!!.callOnClick()
//                    Toast.makeText(applicationContext, "Eyes are locked : " + response.body()!!.isFocused, Toast.LENGTH_SHORT).show()
//                }
//                dialog.dismiss()
//            }
//
//            override fun onFailure(call: Call<Response?>, t: Throwable) {
//                dialog.cancel()
//            }
//        })
//    }

    companion object {
        private const val TAG = "LivePreviewActivity"
        private const val PERMISSION_REQUESTS = 1
        const val MEDIA_TYPE_IMAGE = 1
        const val MEDIA_TYPE_VIDEO = 2
        private fun isPermissionGranted(context: Context, permission: String?): Boolean {
            if (ContextCompat.checkSelfPermission(context, permission!!)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "Permission granted: $permission")
                return true
            }
            Log.i(TAG, "Permission NOT granted: $permission")
            return false
        }

        private fun getOutputMediaFile(type: Int): File? {
            // To be safe, you should check that the SDCard is mounted
            // using Environment.getExternalStorageState() before doing this.
            val mediaStorageDir = File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES), "MyCameraApp")
            // This location works best if you want the created images to be shared
            // between applications and persist after your app has been uninstalled.

            // Create the storage directory if it does not exist
            if (!mediaStorageDir.exists()) {
                if (!mediaStorageDir.mkdirs()) {
                    Log.d("MyCameraApp", "failed to create directory")
                    return null
                }
            }

            // Create a media file name
            @SuppressLint("SimpleDateFormat") val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
            val mediaFile: File
            mediaFile = when (type) {
                MEDIA_TYPE_IMAGE -> {
                    File(mediaStorageDir.path + File.separator +
                            "IMG_" + timeStamp + ".jpg")
                }
                MEDIA_TYPE_VIDEO -> {
                    File(mediaStorageDir.path + File.separator +
                            "VID_" + timeStamp + ".mp4")
                }
                else -> {
                    return null
                }
            }
            return mediaFile
        }
    }
}