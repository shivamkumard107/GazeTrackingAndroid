package com.google.firebase.samples.apps.mlkit

import android.annotation.SuppressLint
import android.app.TimePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Camera
import android.hardware.Camera.PictureCallback
import android.media.AudioManager
import android.media.CamcorderProfile
import android.media.MediaRecorder
import android.media.ToneGenerator
import android.os.*
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.OnRequestPermissionsResultCallback
import androidx.core.content.ContextCompat
import com.google.android.gms.common.annotation.KeepName
import kotlinx.android.synthetic.main.activity_live_preview.*
import pl.droidsonroids.gif.GifImageView
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

@KeepName
class LivePreviewActivity : AppCompatActivity(), OnRequestPermissionsResultCallback {
    private var cameraSource: CameraSource? = null
    private var preview: CameraSourcePreview? = null
    private var graphicOverlay: GraphicOverlay? = null
    private var GIFimg: GifImageView? = null

    private var fileString = ""

    private var uiChange = true
    private var isRunning = true
    private val mTimeLeftInMillis: Long = 60000
    private var countDownTimer: CountDownTimer? = null
    private var scoreList: ArrayList<Int>? = null
    private var dot: ImageView? = null
    private var recordBtn: Button? = null
    private var chunkCount = 0

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
        setContentView(R.layout.activity_live_preview)
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
        val captureBtn = findViewById<Button>(R.id.captureBtn)
        recordBtn = findViewById(R.id.recordBtn)
        val recordTV = findViewById<TextView>(R.id.record_msg_tv)
        val tv3sec = findViewById<TextView>(R.id.tv_3sec)
        val startInForLL = findViewById<LinearLayout>(R.id.start_infoTV)
        val threeSecTimer: CountDownTimer = object : CountDownTimer(5000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = millisUntilFinished.toInt() / 1000
                tv3sec.text = seconds.toString()
            }

            override fun onFinish() {
                Toast.makeText(applicationContext, "Countdown Over", Toast.LENGTH_SHORT).show()
                startInForLL.visibility = View.GONE
                captureBtn.isEnabled = true
                recordBtn!!.callOnClick()
            }
        }

        captureBtn.setOnClickListener {
            scoreList = ArrayList()
            chunkCount = 1
            startInForLL.visibility = View.VISIBLE
            preview!!.visibility = View.GONE
            GIFimg!!.visibility = View.VISIBLE
            dot!!.visibility = View.VISIBLE
            recordTV.text = "Don't move your head and focus on candle"
            captureBtn.isEnabled = false
            threeSecTimer.start()
            ivTimer.visibility = View.GONE
        }
        val alertDialog = AlertDialog.Builder(this@LivePreviewActivity)
                .setMessage("To get accurate results, kindly please adjust your head and keep your eyes inside the overlay")
                .setPositiveButton("Ok") { dialog, _ ->
                    run {
                        dialog.dismiss()
                        setCountDown()
                    }
                }
                .setCancelable(true)
                .create()
        alertDialog.show()
        recordBtn!!.setOnClickListener(View.OnClickListener {
            if (isRecording) {
                // stop recording and release camera
                stopTimer()
                if (countDownTimer != null) countDownTimer!!.cancel()
                if (uiChange) {
                    recordBtn!!.background = getDrawable(R.drawable.circle)
                    recordTV.text = "Please adjust your eyes and tap to start"
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
                    captureBtn.visibility = View.GONE
                    recordBtn!!.visibility = View.VISIBLE
                    recordBtn!!.background = getDrawable(R.drawable.ic_pause_circle_outline_black_24dp)
                    isRunning = true
                }
                if (prepareVideoRecorder()) {
                    countDown(mHour, mMinute)
                    // Camera is available and unlocked, MediaRecorder is prepared,
                    // now you can start recording
                    mediaRecorder!!.start()
                    // inform the user that recording has started
                    isRecording = true
                    setTimer()
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
                    }

                    override fun onFinish() {
                        Toast.makeText(applicationContext, "Countdown Over", Toast.LENGTH_SHORT).show()
                        chunkCount++
                        uiChange = false
                        recordBtn!!.callOnClick()
                        recordBtn!!.callOnClick()
                        uiChange = true
                    }
                }
                countDownTimer!!.start()
            }
        })

        ivTimer.setOnClickListener { getTimePickerDialog() }


        if (allPermissionsGranted()) {
            createCameraSource()
        } else {
            runtimePermissions
        }
    }

    private fun setCountDown() {
        AlertDialog.Builder(this)
                .setMessage("To get accurate results, kindly try not to move your head.")
                .setPositiveButton("Okay") { _, _ ->
                    getTimePickerDialog()
                    Toast.makeText(
                            this@LivePreviewActivity,
                            "Select time to meditate",
                            Toast.LENGTH_LONG
                    )
                            .show()
                }.setCancelable(false)
                .create().show()

    }


    private var mHour = 0
    private var mMinute = 0

    @SuppressLint("DefaultLocale")
    private fun getTimePickerDialog() {
        val hour = 0 //0 hours
        val minute = 0 // 3 minutes
        val mTimePicker: TimePickerDialog
        mTimePicker = TimePickerDialog(
                this,
                TimePickerDialog.OnTimeSetListener { _, selectedHour, selectedMinute ->
                    mHour = selectedHour
                    mMinute = selectedMinute
                    Toast.makeText(
                            this,
                            "Timer set for $selectedHour hour and $selectedMinute minutes",
                            Toast.LENGTH_LONG
                    ).show()
                }, hour, minute, true
        )
        mTimePicker.setTitle("Select time to meditate")
        mTimePicker.show()

    }

    private fun countDown(selectedHour: Int, selectedMinute: Int) {
        Log.d("time_info", "$selectedHour $selectedMinute")
        object : CountDownTimer(((selectedHour * 60 + selectedMinute) * 60000).toLong(), 1000) {
            @SuppressLint("SetTextI18n")
            override fun onTick(millisUntilFinished: Long) {
            }

            override fun onFinish() {
                if (isRecording) {
                    vibrateAndBeepPhone()
                    Toast.makeText(
                            this@LivePreviewActivity,
                            "Meditation time complete. You may stop recording",
                            Toast.LENGTH_LONG
                    )
                            .show()
                }
            }
        }.start()
    }


    private fun vibrateAndBeepPhone() {
        val toneGen1 = ToneGenerator(AudioManager.STREAM_MUSIC, 150)
        toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP, 200)
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= 26) {
            vibrator.vibrate(
                    VibrationEffect.createOneShot(
                            200,
                            VibrationEffect.DEFAULT_AMPLITUDE
                    )
            )
        } else {
            vibrator.vibrate(200)
        }
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
        get() = try {
            val info = this.packageManager
                    .getPackageInfo(this.packageName, PackageManager.GET_PERMISSIONS)
            val ps = info.requestedPermissions
            if (ps != null && ps.isNotEmpty()) {
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
        get() {
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

    private val mPicture = PictureCallback { data, _ ->
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
        }
    }

    private fun setTimer() {
        ivTimer.visibility = View.GONE
        tvTimer.base = SystemClock.elapsedRealtime()
        tvTimer.start()
    }

    private fun stopTimer() {
        ivTimer.visibility = View.VISIBLE
        tvTimer.stop()
    }

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