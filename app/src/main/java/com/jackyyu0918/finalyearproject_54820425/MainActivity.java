package com.jackyyu0918.finalyearproject_54820425;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Rect2d;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.tracking.Tracker;
import org.opencv.tracking.TrackerBoosting;
import org.opencv.tracking.TrackerCSRT;
import org.opencv.tracking.TrackerGOTURN;
import org.opencv.tracking.TrackerKCF;
import org.opencv.tracking.TrackerMIL;
import org.opencv.tracking.TrackerMOSSE;
import org.opencv.tracking.TrackerMedianFlow;
import org.opencv.tracking.TrackerTLD;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

//TensorflowLite library

public class MainActivity extends Activity implements CvCameraViewListener2 {
    private static final String TAG = "OCVSample::Activity";

    //private CameraBridgeViewBase mOpenCvCameraView;
    private Zoomcameraview mOpenCvCameraView;
    private boolean              mIsJavaCamera = true;
    private MenuItem             mItemSwitchCamera = null;

    //--------------------My code-----------------//
    //sensor view object
    private DragRegionView DragRegionView;

    //Matrix
    private Mat mRgba;

    private Mat mGray;
    private Mat testMat;
    private Mat videoMat;

    private Mat targetObjectMat;
    private Mat zoomWindowMat;
    private Mat optimizeObjectMat;

    //Tracker
    private Tracker objectTracker;
    private boolean isInitTracker = false;

    private Rect2d roiRect2d;
    private Rect roiRect;

    //Pre-defined size
    private Point trackerCoordinate;
    private Point trackerSize;
    private Scalar greenColorOutline;


    //Mode switching
        //false = small window
    private boolean isFullScreen = false;

    //button
        // for init tracker
    private Button button_startTrack;
    private String currentTrackerType;

        // for reset tracker
    private Button button_resetTrack;

        // for full view tracking
    private Button button_fullView;

        // for start recording
    private Button button_startRecord;

    //Media recorder
    public MediaRecorder recorder = new MediaRecorder();
    private boolean isRecording = false;

    //TensorFlowLite interpreter
    private int[] rgbBytes;

    //Thread handler
    private Handler handler;
    private HandlerThread handlerThread;

    //--------------------------------------------------------------------------//

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public MainActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //Hide bar
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);


        //default setting
        /*
        mOpenCvCameraView = findViewById(R.id.main_surfaceView);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
         */

        //zoom view setting
        mOpenCvCameraView = (Zoomcameraview)findViewById(R.id.ZoomCameraView);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setZoomControl((SeekBar) findViewById(R.id.CameraZoomControls));
        mOpenCvCameraView.setCvCameraViewListener(this);

        //Grant permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.CAMERA}, 1888);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, 112);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.RECORD_AUDIO}, 120);
        }

        //Tracker section
        //roiRect2d setting

        //old square roiRect2d 1:1
        /*
        trackerCoordinate = new Point(700,200);
        trackerSize = new Point(300,300);
        greenColorOutline = new Scalar(0, 255, 0, 255);
         */


        //new 2:1 size
        /*
        trackerCoordinate = new Point(1000,200);
        trackerSize = new Point(210,300);
        greenColorOutline = new Scalar(0, 255, 0, 255);

        roiRect2d = new Rect2d(trackerCoordinate.x,trackerCoordinate.y,trackerSize.x,trackerSize.y);
        roiRect = new Rect((int)trackerCoordinate.x,(int)trackerCoordinate.y,(int)trackerSize.x,(int)trackerSize.y);
         */

        greenColorOutline = new Scalar(0, 255, 0, 255);
        trackerCoordinate = new Point();
        trackerSize = new Point();

        roiRect2d = new Rect2d();
        roiRect = new Rect();


        //tracker creation, base on drop down menu selection
        //currentTrackerType = "KCF";

        //spinner tracker selection
        final Spinner trackerSpinner  = findViewById(R.id.trackerSpinner);

        final ArrayAdapter<String> nameAdaptar = new ArrayAdapter<String>(MainActivity.this,android.R.layout.simple_expandable_list_item_1, getResources().getStringArray(R.array.trackingAlgorithmName));

        nameAdaptar.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        trackerSpinner.setAdapter(nameAdaptar);


        trackerSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(MainActivity.this, "You are choosing "+ parent.getSelectedItem().toString() + ".", Toast.LENGTH_SHORT).show() ;
                currentTrackerType = parent.getSelectedItem().toString();
                System.out.println("currentTrackerType: " + currentTrackerType);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                Toast.makeText(MainActivity.this, "Nothing is selected.", Toast.LENGTH_LONG).show();
            }
        });

        //button onClick listener
            //start button
        button_startTrack = findViewById(R.id.button_startTrack);
        button_startTrack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                //No default tracker type
                //firstTracker = TrackerKCF.create();

                //own function, create proper tracker
                if (DragRegionView.points[0] == null){
                    Toast toast1 = Toast.makeText(MainActivity.this,
                            "Please drag on target object.", Toast.LENGTH_LONG);
                    //顯示Toast
                    toast1.show();
                } else {
                    createTracker(currentTrackerType);

                    //get user drag result
                    calculateRectInfo(DragRegionView.points);

                    //tracker initialization
                    objectTracker.init(mGray, roiRect2d);
                    //System.out.println("Tracker init result: " + firstTracker.init(mGray,roiRect2d));
                    isInitTracker = true;

                    //Tracker message
                    Toast toast1 = Toast.makeText(MainActivity.this,
                            "Current tracker: " + objectTracker.getClass(), Toast.LENGTH_LONG);
                    //顯示Toast
                    toast1.show();

                    Toast toast2 = Toast.makeText(MainActivity.this,
                            "Current camera size: " + mOpenCvCameraView.getWidth() + "x" + mOpenCvCameraView.getHeight(), Toast.LENGTH_LONG);
                    //顯示Toast
                    toast2.show();

                    DragRegionView.isReset = true;
                    DragRegionView.invalidate();

                    Toast toast3 = Toast.makeText(MainActivity.this,
                            "Current tracker size: " + roiRect.width + "x" + roiRect.height, Toast.LENGTH_LONG);
                    //顯示Toast
                    toast3.show();
                }
            }
        });

            //switch mode button
        button_fullView = findViewById(R.id.button_fullView);
        button_fullView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                if(isFullScreen == false) {
                    isFullScreen = true;
                } else {
                    isFullScreen = false;
                }
                //Tracker message
                Toast toast = Toast.makeText(MainActivity.this,
                        "Full screen mode: " + isFullScreen , Toast.LENGTH_LONG);
                //顯示Toast
                toast.show();
            }
        });

            //reset button
        button_resetTrack = findViewById(R.id.button_resetTracker);
        button_resetTrack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                isInitTracker = false;
                objectTracker = null;

                //new 2:1 size
                resetTracker();
                DragRegionView.isReset = true;
                DragRegionView.invalidate();
                //testing****
                //System.out.println("Coordinate: " + DragRegionView.points[0] + DragRegionView.points[1] + DragRegionView.points[2] + DragRegionView.points[3]);
            }
        });

            //start recording button
        //start button
        button_startRecord = findViewById(R.id.button_startRecord);
        button_startRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                // prepareRecord();  <-- Moved to onCreate()

                if(isRecording == false){
                    recorder.reset();
                    prepareRecorder();

                    //Recording message
                    Toast toast = Toast.makeText(MainActivity.this,
                            "Start recording...", Toast.LENGTH_LONG);
                    //顯示Toast
                    toast.show();
                    button_startRecord.setText("STOP RECORDING");
                    startRecord();
                } else {
                    //Recording message
                    Toast toast = Toast.makeText(MainActivity.this,
                            "End recording...", Toast.LENGTH_LONG);
                    //顯示Toast
                    toast.show();
                    button_startRecord.setText("START RECORDING");

                    //empty the recorder before stop
                    mOpenCvCameraView.setRecorder(null);
                    stopRecord();
                }
            }
        });

        //Sensor View at top
        DragRegionView = (DragRegionView) findViewById(R.id.SensorView);

        //TensorFlowLite in
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }

        //Multi Thread issue -Me
        handlerThread = new HandlerThread("inference");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();

        //Media recorder;
        //recorder.release();
    }

    public void onCameraViewStarted(int width, int height) {
        mGray = new Mat();
        mRgba = new Mat();
        testMat =  new Mat();
        targetObjectMat = new Mat();
        zoomWindowMat = new Mat();
        optimizeObjectMat = new Mat();
    }

    public void onCameraViewStopped() {
        mGray.release();
        mRgba.release();
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();

        Size sizeRgba = mRgba.size();
        int rows = (int) sizeRgba.height;
        int cols = (int) sizeRgba.width;

        //tracking section
        // if initialized tracker, start update the ROI
        if(isInitTracker){

            //Pre-defined target window details: x,y,width,height
            //Assign 2d to 1d:
            // 2d: update by tracker
            // 1d: update Rec
            roiRect.x = (int) roiRect2d.x;
            roiRect.y = (int) roiRect2d.y;
            roiRect.width = (int) roiRect2d.width;
            roiRect.height = (int) roiRect2d.height;

            System.out.println("x,y,width,height: " + (int) roiRect2d.x + ", "+ (int) roiRect2d.y + ", "+ (int) roiRect2d.width + ", "+ (int) roiRect2d.height);

            //Update tracker information to roiRect2d
            System.out.println("Tracker update result: " + objectTracker.update(mGray, roiRect2d));

            //make sure target object is inside the screen
            if(roiRect2d.x+ roiRect2d.width > 3000 || roiRect2d.x < 0 || roiRect2d.y < 0 || roiRect2d.y+ roiRect2d.height > 1080) {
                System.out.println("Tracking Failed, target object fall outside screen");

            } else {
                //Target object matrix frame
                targetObjectMat = mRgba.submat((int)(roiRect2d.y), (int)(roiRect2d.y+ roiRect2d.height), (int)(roiRect2d.x), (int)(roiRect2d.x+ roiRect2d.width));

                if(roiRect2d.height >= roiRect2d.width) {
                    //Optimized aspect ratio for video recording (2:1)
                    System.out.println("Before crash: " + (int) (roiRect2d.y) + ", " + (int) (roiRect2d.y + roiRect2d.height) + ", " + (int) (roiRect2d.x + (roiRect2d.width/2) - roiRect2d.height) + ", " + (int) (roiRect2d.x + (roiRect2d.width/2) + roiRect2d.height));
                    optimizeObjectMat = mRgba.submat((int) (roiRect2d.y), (int) (roiRect2d.y + roiRect2d.height), (int) (roiRect2d.x + (roiRect2d.width/2) - roiRect2d.height), (int) (roiRect2d.x + (roiRect2d.width/2) + roiRect2d.height));
                } else if(roiRect2d.height < roiRect2d.width){
                    //Optimized aspect ratio for video recording (2:1)
                    optimizeObjectMat = mRgba.submat((int)(roiRect2d.y + (roiRect2d.height/2) - roiRect2d.width/4), (int)((roiRect2d.y + (roiRect2d.height/2) - roiRect2d.width/4) + roiRect2d.width/2), (int)(roiRect2d.x), (int)(roiRect2d.x+ roiRect2d.width));
                }

                    // Small window preview mode
                if(isFullScreen == false){
                    //top-left corner of mRgba
                    zoomWindowMat = mRgba.submat(0, rows / 2 - rows / 10, 0, cols / 2 - cols / 10);

                    //show target matrix at the top-left corner
                    //Imgproc.resize(targetObjectMat, zoomWindowMat, zoomWindowMat.size(), 0, 0, Imgproc.INTER_LINEAR_EXACT);
                    Imgproc.resize(optimizeObjectMat, zoomWindowMat, zoomWindowMat.size(), 0, 0, Imgproc.INTER_LINEAR_EXACT);

                // Full screen mode (for video recording)
                } else {
                    mRgba.copyTo(testMat);

                    //full the screen with target matrix
                    //Imgproc.resize(targetObjectMat, testMat, mRgba.size(), 0, 0, Imgproc.INTER_LINEAR_EXACT);
                    Imgproc.resize(optimizeObjectMat, testMat, mRgba.size(), 0, 0, Imgproc.INTER_LINEAR_EXACT);
                    //draw on full screen
                    Imgproc.rectangle(testMat,roiRect,greenColorOutline,2);
                    return testMat;
                }
            }

            Imgproc.rectangle(mRgba,roiRect,greenColorOutline,2);
        }

        //draw rectangle using Rect roiRect
        //Imgproc.rectangle(mRgba,roiRect,greenColorOutline,2);

        //========Object detection=========//
        rgbBytes = new int[rows*cols];

        //Decided the previewSize
        //onPreviewSizeChosen(new android.util.Size(cols, rows), 270);

        return mRgba;
    }

    //my own function
    public void createTracker(String trackerType){
        switch (trackerType){
            case "KCF":
                System.out.println("KCF case.");
                objectTracker = TrackerKCF.create();
                break;
            case "MedianFlow":
                System.out.println("MedianFlow case.");
                objectTracker = TrackerMedianFlow.create();
                break;
            case "TLD":
                System.out.println("TLD case.");
                objectTracker = TrackerTLD.create();
                break;
            case "Boosting":
                System.out.println("Boosting case.");
                objectTracker = TrackerBoosting.create();
                break;
            case "CSRT":
                System.out.println("CSRT case.");
                objectTracker = TrackerCSRT.create();
                break;
            case "GOTURN":
                System.out.println("GOTURN case.");
                objectTracker = TrackerGOTURN.create();
                break;
            case "MIL":
                System.out.println("MIL case.");
                objectTracker = TrackerMIL.create();
                break;
            case "MOSSE":
                System.out.println("MOSSE case.");
                objectTracker = TrackerMOSSE.create();
                break;
            default:
                break;
        }
    }

    public void prepareRecorder(){
        //Success for start,but shut down on stop

        //Video source is from the surface
        //Everything draw on the surface will be recorder by recorder
        recorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);

        //Store the video with time stamp
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());

        String currentDateandTime = sdf.format(new Date());

        recorder.setOutputFile(Environment.getExternalStorageDirectory().getAbsoluteFile()+File.separator+"/FYP/" + currentDateandTime + ".mp4");
        recorder.setVideoEncodingBitRate(1000000);
        recorder.setVideoFrameRate(60);
        recorder.setVideoSize(mOpenCvCameraView.getWidth(), mOpenCvCameraView.getHeight());
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);

        try {
            recorder.prepare();
            System.out.println("success prepared media recorder!!");

        } catch (IllegalStateException e) {
            Log.e("debug mediarecorder", "not prepare");
        } catch (IOException e) {
            Log.e("debug mediarecorder", "not prepare IOException");
        }

        //Initialized the mRecorder in CameraBridgeViewBase and make a new surface for recording!
        //Everything draw on that
        mOpenCvCameraView.setRecorder(recorder);

        //Only Audio
        //Success!
            /*
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            recorder.setOutputFile(Environment.getExternalStorageDirectory().getAbsoluteFile()+File.separator+"outputAudio.3gp");
             */
    }

    public void startRecord() {
        recorder.start();
        isRecording = true;
    }

    public void stopRecord(){
        //testing add codes
        /* ignore
        recorder.setOnErrorListener(null);
        recorder.setOnInfoListener(null);
        recorder.setPreviewDisplay(null);
         */

        try{
            recorder.stop();
            isRecording = false;
        }catch(RuntimeException stopException){
            System.out.println("RuntimeException occurred!");
        }
    }

    public void resetTracker(){
        roiRect.x = (int)trackerCoordinate.x;
        roiRect.y = (int)trackerCoordinate.y;
        roiRect.width = (int)trackerSize.x;
        roiRect.height = (int)trackerSize.y;

        roiRect2d.x = trackerCoordinate.x;
        roiRect2d.y = trackerCoordinate.y;
        roiRect2d.width = trackerSize.x;
        roiRect2d.height = trackerSize.y;


        //without value, blank rect
        //roiRect = null;
        //roiRect2d = null;
    }
    /*
    //Top view for sensoring
    private View.OnTouchListener handleDragTouch = new View.OnTouchListener() {

        @Override
        public boolean onTouch(View v, MotionEvent event) {

            int touch_x = (int) event.getX();
            int touch_y = (int) event.getY();

            Toast toast1 = Toast.makeText(MainActivity.this,
                    "X: " + touch_x + ", Y: " + touch_y, Toast.LENGTH_LONG);
            //顯示Toast
            toast1.show();

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    Log.i("TAG", "touched down");
                    break;
                case MotionEvent.ACTION_MOVE:
                    Log.i("TAG", "moving: (" + touch_x + ", " + touch_y + ")");
                    break;
                case MotionEvent.ACTION_UP:
                    Log.i("TAG", "touched up");
                    break;
            }

            return true;
        }
    };

     */

    //Set rectangle info from drag class
    public void calculateRectInfo(android.graphics.Point[] points){
            /*
            trackerCoordinate = new Point(points[0].x+40,points[0].y+20);
            trackerSize = new Point((points[2].x+40) - (points[0].x+40),(points[2].y+20) - (points[0].y+20));

            roiRect2d = new Rect2d(trackerCoordinate.x,trackerCoordinate.y,trackerSize.x,trackerSize.y);
            roiRect = new Rect((int)trackerCoordinate.x,(int)trackerCoordinate.y,(int)trackerSize.x,(int)trackerSize.y);
             */

            trackerCoordinate.x = points[0].x+40;
            trackerCoordinate.y = points[0].y+20;
            trackerSize.x = ((points[2].x+40) - (points[0].x+40));
            trackerSize.y = ((points[2].y+20) - (points[0].y+20));

            roiRect2d.x = trackerCoordinate.x;
            roiRect2d.y = trackerCoordinate.y;
            roiRect2d.width = trackerSize.x;
            roiRect2d.height =trackerSize.y;

            roiRect.x = (int)trackerCoordinate.x;
            roiRect.y =  (int)trackerCoordinate.y;
            roiRect.width = (int)trackerSize.x;
            roiRect.height = (int)trackerSize.y;

            Toast toast1 = Toast.makeText(MainActivity.this,
                    "Press start to track object", Toast.LENGTH_LONG);
            //顯示Toast
            toast1.show();
    }


}