/*
 * Copyright (C) The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package devilhawks.hackduke.hackdukeproject;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

/**
 * Activity for the face tracker app.  This app detects faces with the rear facing camera, and draws
 * overlay graphics to indicate the position, size, and ID of each face.
 */
public final class FaceTrackerActivity extends AppCompatActivity {

    /**
     * File type for JSON storage.
     */
    public static final String TEXT_FILE_TYPE = ".txt";
    private static final String TAG = "FaceTracker";

    public static final String JSON_BLINKS_KEY = "blinksKey";
    public static final String JSON_TILTS_KEY = "tiltsKey";
    public static final String JSON_TURNS_KEY = "turnsKey";
    public static final String JSON_PAUSES_KEY = "pausesKey";
    public static final String JSON_PAUSETIMES_KEY = "pauseTimesKey";
    public static final String JSON_PAUSEAVG_KEY = "pauseAvgKey";
    public static final String JSON_DURATION_KEY = "durationKey";
    public static final String JSON_DATE_KEY = "dateKey";

    private CameraSource mCameraSource = null;

    private CameraSourcePreview mPreview;
    private GraphicOverlay mGraphicOverlay;
    private SpeechRecognizer mSpeechRecognizer;
    private FloatingActionButton fab;
    private TextView textView;
    private Intent intent = null;

    private static final int RC_HANDLE_GMS = 9001;
    // permission request codes need to be < 256
    private static final int RC_HANDLE_CAMERA_PERM = 2;
    private static final float BLINK_THRESHOLD = .3f;
    private static final float Y_THRESHOLD = 13.0f;
    private static final float Z_THRESHOLD = 13.0f;

    private boolean isRecording = false;
    private float lastLeftEyeProbability = 0.0f;
    private float lastRightEyeProbability = 0.0f;
    private int blinks = 0;
    private int tilts = 0;
    private int turns = 0;
    private int pauses = 0;
    private boolean isBlinking = false;
    private boolean isTilting = false;
    private boolean isTurning = false;
    private long timeOfLastPause = 0;
    ArrayList<Float> pauseLengths = new ArrayList<>();
    float averagePause = 0.0f;
    long startTime = 0L;
    long endTime = 0L;

    //==============================================================================================
    // Activity Methods
    //==============================================================================================

    /**
     * Initializes the UI and initiates the creation of a face detector.
     */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.activity_face_tracker);

        mPreview = (CameraSourcePreview) findViewById(R.id.preview);
        textView = (TextView) findViewById(R.id.textView);
        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!isRecording) {
                    isRecording = true;
                    fab.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.paleBlue)));
                    startTime = System.currentTimeMillis();
                    if (mSpeechRecognizer == null) {
                        mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(FaceTrackerActivity.this);
                    }
                    if (intent == null) {
                        intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
                        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2500);
                    }
                    mSpeechRecognizer.setRecognitionListener(new Listener());
                    mSpeechRecognizer.startListening(intent);

                    blinks = 0;
                    turns = 0;
                    tilts = 0;
                    pauses = 0;
                    averagePause = 0;
                    showStats();
                } else {
                    isRecording = false;
                    endTime = System.currentTimeMillis();
                    fab.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.colorAccent)));
                    AlertDialog.Builder builder = new AlertDialog.Builder(FaceTrackerActivity.this);
                    builder.setMessage("Save session?").setPositiveButton("Save", new DialogInterface.OnClickListener(){
                        public void onClick(DialogInterface dialog, int id){
                            FaceTrackerActivity.this.attemptSaveToFile();
                        }
                    }).setNegativeButton("Cancel", new DialogInterface.OnClickListener(){
                        public void onClick(DialogInterface dialogInterface, int id){
                            blinks = 0;
                            turns = 0;
                            tilts = 0;
                            pauses = 0;
                            startTime = 0L;
                            endTime = 0L;
                            showStats();
                        }
                    }).create().show();
                }
            }
        });
        mGraphicOverlay = (GraphicOverlay) findViewById(R.id.faceOverlay);

        // Check for the camera permission before accessing the camera.  If the
        // permission is not granted yet, request permission.
        int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (rc == PackageManager.PERMISSION_GRANTED) {
            createCameraSource();
        } else {
            requestCameraPermission();
        }
    }

    private void showStats(){
        Handler mainHandler = new Handler(FaceTrackerActivity.this.getMainLooper());

        Runnable myRunnable = new Runnable() {
            @Override
            public void run() {
                textView.setText("Blinks: " + blinks + " Turns: " + turns + " Tilts: " + tilts + " Pauses: " + pauses);
            }
        };
        mainHandler.post(myRunnable);
    }

    /**
     * Handles the requesting of the camera permission.  This includes
     * showing a "Snackbar" message of why the permission is needed then
     * sending the request.
     */
    private void requestCameraPermission() {
        Log.w(TAG, "Camera permission is not granted. Requesting permission");

        final String[] permissions = new String[]{Manifest.permission.CAMERA};

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM);
            return;
        }

        final Activity thisActivity = this;

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityCompat.requestPermissions(thisActivity, permissions,
                        RC_HANDLE_CAMERA_PERM);
            }
        };

        Snackbar.make(mGraphicOverlay, R.string.permission_camera_rationale,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.ok, listener)
                .show();
    }

    /**
     * Creates and starts the camera.  Note that this uses a higher resolution in comparison
     * to other detection examples to enable the barcode detector to detect small barcodes
     * at long distances.
     */
    private void createCameraSource() {

        Context context = getApplicationContext();
        FaceDetector detector = new FaceDetector.Builder(context)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .setProminentFaceOnly(true)
                .setMode(FaceDetector.ACCURATE_MODE)
                .setTrackingEnabled(true)
                .build();

        detector.setProcessor(
                new MultiProcessor.Builder<>(new GraphicFaceTrackerFactory())
                        .build());

        if (!detector.isOperational()) {
            // Note: The first time that an app using face API is installed on a device, GMS will
            // download a native library to the device in order to do detection.  Usually this
            // completes before the app is run for the first time.  But if that download has not yet
            // completed, then the above call will not detect any faces.
            //
            // isOperational() can be used to check if the required native library is currently
            // available.  The detector will automatically become operational once the library
            // download completes on device.
            Log.w(TAG, "Face detector dependencies are not yet available.");
        }

        mCameraSource = new CameraSource.Builder(context, detector)
                .setRequestedPreviewSize(640, 480)
                .setFacing(CameraSource.CAMERA_FACING_FRONT)
                .setRequestedFps(30.0f)
                .build();
    }

    /**
     * Restarts the camera.
     */
    @Override
    protected void onResume() {
        super.onResume();

        startCameraSource();
    }

    /**
     * Stops the camera.
     */
    @Override
    protected void onPause() {
        super.onPause();
        mPreview.stop();
    }

    /**
     * Releases the resources associated with the camera source, the associated detector, and the
     * rest of the processing pipeline.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCameraSource != null) {
            mCameraSource.release();
        }
        if(mSpeechRecognizer!=null){
            mSpeechRecognizer.stopListening();
            mSpeechRecognizer.destroy();
            mSpeechRecognizer = null;
        }
    }

    /**
     * Callback for the result from requesting permissions. This method
     * is invoked for every call on {@link #requestPermissions(String[], int)}.
     * <p>
     * <strong>Note:</strong> It is possible that the permissions request interaction
     * with the user is interrupted. In this case you will receive empty permissions
     * and results arrays which should be treated as a cancellation.
     * </p>
     *
     * @param requestCode  The request code passed in {@link #requestPermissions(String[], int)}.
     * @param permissions  The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *                     which is either {@link PackageManager#PERMISSION_GRANTED}
     *                     or {@link PackageManager#PERMISSION_DENIED}. Never null.
     * @see #requestPermissions(String[], int)
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode != RC_HANDLE_CAMERA_PERM) {
            Log.d(TAG, "Got unexpected permission result: " + requestCode);
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Camera permission granted - initialize the camera source");
            // we have permission, so create the camerasource
            createCameraSource();
            return;
        }

        Log.e(TAG, "Permission not granted: results len = " + grantResults.length +
                " Result code = " + (grantResults.length > 0 ? grantResults[0] : "(empty)"));

        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Face Tracker sample")
                .setMessage(R.string.no_camera_permission)
                .setPositiveButton(R.string.ok, listener)
                .show();
    }


    /**
     * Generates a unique word directory name to save to.
     * @return - A uniquely named file folder to store word information in.
     */
    private File generateFileName(){
        int n=0;
        Random random = new Random();
        String fileName;
        File file;
        // Guarantee a unique folder for new character
        do {
            n += random.nextInt(100);
            fileName = "Session" + n + TEXT_FILE_TYPE ;
            file = new File(getExternalFilesDir(
                    Environment.DIRECTORY_DOCUMENTS), fileName);
        } while (file.exists());

        return file;
    }

    private void attemptSaveToFile(){
        try {
            saveToFile();
        }catch (Exception e){
            Toast.makeText(this, "Could not save", Toast.LENGTH_SHORT).show();
            blinks = 0;
            turns = 0;
            tilts = 0;
            pauses = 0;
            startTime = 0L;
            endTime = 0L;
            showStats();
        }
    }

    /**
     * Saves word information to a new directory in the file system.
     * @throws JSONException
     * @throws IOException
     */
    private void saveToFile() throws JSONException, IOException {
        File file = generateFileName();

        JSONObject jsonObject = new JSONObject();
        /*JSONArray pauseTimes = new JSONArray();
        for (float pause: pauseLengths) {
            pauseTimes.put(pause);
        }*/

        jsonObject.put(JSON_BLINKS_KEY, blinks);
        jsonObject.put(JSON_TILTS_KEY, tilts);
        jsonObject.put(JSON_TURNS_KEY, turns);
        jsonObject.put(JSON_PAUSES_KEY, pauses);
        jsonObject.put(JSON_PAUSEAVG_KEY, averagePause);
        jsonObject.put(JSON_DURATION_KEY, endTime - startTime);
        jsonObject.put(JSON_DATE_KEY, endTime);
        //jsonObject.put(MainActivity.JSON_PAUSETIMES_KEY, pauseTimes);

        // Write to file
        FileWriter fileWriter = new FileWriter(file);
        try {
            fileWriter.write(jsonObject.toString());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            fileWriter.flush();
            fileWriter.close();
        }
    }

    //==============================================================================================
    // Camera Source Preview
    //==============================================================================================

    /**
     * Starts or restarts the camera source, if it exists.  If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    private void startCameraSource() {

        // check that the device has play services available.
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                getApplicationContext());
        if (code != ConnectionResult.SUCCESS) {
            Dialog dlg =
                    GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS);
            dlg.show();
        }

        if (mCameraSource != null) {
            try {
                mPreview.start(mCameraSource, mGraphicOverlay);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                mCameraSource.release();
                mCameraSource = null;
            }
        }
    }

    //==============================================================================================
    // Graphic Face Tracker
    //==============================================================================================

    /**
     * Factory for creating a face tracker to be associated with a new face.  The multiprocessor
     * uses this factory to create face trackers as needed -- one for each individual.
     */
    private class GraphicFaceTrackerFactory implements MultiProcessor.Factory<Face> {
        @Override
        public Tracker<Face> create(Face face) {
            return new GraphicFaceTracker(mGraphicOverlay);
        }
    }

    /**
     * Face tracker for each detected individual. This maintains a face graphic within the app's
     * associated face overlay.
     */
    private class GraphicFaceTracker extends Tracker<Face> {
        private GraphicOverlay mOverlay;
        private FaceGraphic mFaceGraphic;

        GraphicFaceTracker(GraphicOverlay overlay) {
            mOverlay = overlay;
            mFaceGraphic = new FaceGraphic(overlay);
        }

        /**
         * Start tracking the detected face instance within the face overlay.
         */
        @Override
        public void onNewItem(int faceId, Face item) {
            mFaceGraphic.setId(faceId);
        }

        /**
         * Update the position/characteristics of the face within the overlay.
         */
        @Override
        public void onUpdate(FaceDetector.Detections<Face> detectionResults, Face face) {
            if(isRecording) {
                boolean changed = false;
                mOverlay.add(mFaceGraphic);
                mFaceGraphic.updateFace(face);
                if(lastLeftEyeProbability - face.getIsLeftEyeOpenProbability() > BLINK_THRESHOLD && !isBlinking){
                    isBlinking = true;
                    blinks++;
                    changed = true;
                } else if (face.getIsLeftEyeOpenProbability() - lastLeftEyeProbability > BLINK_THRESHOLD && isBlinking) {
                    isBlinking = false;
                }
                lastLeftEyeProbability = face.getIsLeftEyeOpenProbability();
                lastRightEyeProbability = face.getIsRightEyeOpenProbability();

                if( Math.abs(face.getEulerZ()) > Z_THRESHOLD && !isTilting){
                    isTilting = true;
                    tilts++;
                    changed = true;
                } else if (Math.abs(face.getEulerZ()) < Z_THRESHOLD && isTilting){
                    isTilting = false;
                }
                if(Math.abs(face.getEulerY()) > Y_THRESHOLD && !isTurning){
                    isTurning = true;
                    turns++;
                    changed = true;
                } else if (Math.abs(face.getEulerY()) < Y_THRESHOLD && isTurning){
                    isTurning = false;
                }
                if(changed){
                    FaceTrackerActivity.this.showStats();
                }
            }
        }

        /**
         * Hide the graphic when the corresponding face was not detected.  This can happen for
         * intermediate frames temporarily (e.g., if the face was momentarily blocked from
         * view).
         */
        @Override
        public void onMissing(FaceDetector.Detections<Face> detectionResults) {
            mOverlay.remove(mFaceGraphic);
        }

        /**
         * Called when the face is assumed to be gone for good. Remove the graphic annotation from
         * the overlay.
         */
        @Override
        public void onDone() {
            mOverlay.remove(mFaceGraphic);
        }
    }

    class Listener implements RecognitionListener {

        @Override
        public void onReadyForSpeech(Bundle params){

        }

        @Override
        public void onBeginningOfSpeech() {
            if(timeOfLastPause > 0){
                long currentTime = System.currentTimeMillis();
                float lastTime = (float)(currentTime - timeOfLastPause)/1000;
                //pauses++;
                //textView.setText("Blinks: " + blinks + " Turns: " + turns + " Tilts: " + tilts + " Pauses: " + pauses);
                int size = pauseLengths.size();
                averagePause = size > 0 ? (size*averagePause + lastTime)/(size + 1) : lastTime;
                pauseLengths.add(lastTime);
                textView.setText("Pauses: " + size + " Last Time: " + lastTime + " Average: " + averagePause);
            }
        }

        @Override
        public void onRmsChanged(float rmsdB) {

        }

        @Override
        public void onBufferReceived(byte[] buffer) {

        }

        @Override
        public void onEndOfSpeech() {
            timeOfLastPause = System.currentTimeMillis();
        }

        @Override
        public void onError(int error) {

        }

        @Override
        public void onResults(Bundle results) {
            mSpeechRecognizer.setRecognitionListener(new Listener());
            mSpeechRecognizer.startListening(intent);
        }

        @Override
        public void onPartialResults(Bundle partialResults) {
        }

        @Override
        public void onEvent(int eventType, Bundle params) {

        }
    }
}
