package org.opencv.samples.facedetect;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;

import com.domo.network.opencvj.R;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.facedetect.DetectionBasedTracker;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class FdActivity extends Activity implements CvCameraViewListener2
{

    private static final String TAG = "OCVSample::Activity";
    private static final Scalar FACE_RECT_COLOR = new Scalar(0, 255, 0, 255);
    public static final int JAVA_DETECTOR = 0;
    public static final int NATIVE_DETECTOR = 1;

    private MenuItem mItemFace50;
    private MenuItem mItemFace40;
    private MenuItem mItemFace30;
    private MenuItem mItemFace20;
    private MenuItem mItemType;

    private Mat mRgba;
    private Mat mGray;
    private Mat Matlin;
    private Mat gMatlin;
    private Size mNarrowSize;
    private Mat mCache90Mat;
    private File mCascadeFile;
    private DetectionBasedTracker mNativeDetector;

    private int mDetectorType = JAVA_DETECTOR;
    private String[] mDetectorName;

    private float mRelativeFaceSize = 0.1f;
    private int mAbsoluteFaceSize = 0;

    private CameraBridgeViewBase mOpenCvCameraView;
    private Button mSwitchCameraView;
    private RectBitmap mImageView;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this)
    {
        @Override
        public void onManagerConnected(int status)
        {
            switch (status)
            {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    System.loadLibrary("opencv_java3");
                    // Load native library after(!) OpenCV initialization
                    System.loadLibrary("FaceDetected");

                    try
                    {
                        // load cascade file from application resources
                        InputStream is = getResources().openRawResource(R.raw.lbpcascade_frontalface);
                        File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                        mCascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
                        FileOutputStream os = new FileOutputStream(mCascadeFile);

                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1)
                        {
                            os.write(buffer, 0, bytesRead);
                        }
                        is.close();
                        os.close();

                        mNativeDetector = new DetectionBasedTracker(mCascadeFile.getAbsolutePath(), 0);
                        setDetectorType(NATIVE_DETECTOR);
                        cascadeDir.delete();
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                        Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
                    }

                    mOpenCvCameraView.enableView();
                    mOpenCvCameraView.enableFpsMeter();
                }
                break;
                default:
                {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    public FdActivity()
    {
        mDetectorName = new String[2];
        mDetectorName[JAVA_DETECTOR] = "Java";
        mDetectorName[NATIVE_DETECTOR] = "Native (tracking)";

        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.face_detect_surface_view);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.fd_activity_surface_view);
        mOpenCvCameraView.setVisibility(CameraBridgeViewBase.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_BACK);
        mOpenCvCameraView.setWindowManager(getWindowManager());

        mImageView = findViewById(R.id.fd_image_view);
        mImageView.setScaleType(ImageView.ScaleType.FIT_XY);
        mSwitchCameraView = findViewById(R.id.fd_switch_camera_view);
        mSwitchCameraView.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (mOpenCvCameraView.getCameraIndex() == CameraBridgeViewBase.CAMERA_ID_FRONT)
                {
                    mOpenCvCameraView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_BACK);
                }
                else
                {
                    mOpenCvCameraView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_FRONT);
                }
                mOpenCvCameraView.disableView();
                mOpenCvCameraView.enableView();
            }
        });

        // First check android version
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1)
        {
            //Check if permission is already granted
            //thisActivity is your activity. (e.g.: MainActivity.this)
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            {
                // Give first an explanation, if needed.
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA))
                {
                    // Show an explanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.
                }
                else
                {
                    // No explanation needed, we can request the permission.
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
                }
            }
        }
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
        {
            mOpenCvCameraView.disableView();
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug())
        {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            // OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        }
        else
        {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy()
    {
        super.onDestroy();
        mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height)
    {
        mGray = new Mat();
        mRgba = new Mat();
        mCache90Mat = new Mat();

        gMatlin = new Mat();
        Matlin = new Mat();

        // height = height - getStausBarHeight(this) - getVirtualBarHeight(this);

        float mScale = (float) height / (float) 540;
        mCacheBitmap = Bitmap.createBitmap((int) ((float) width / mScale), (int) ((float) height / mScale), Bitmap.Config.ARGB_4444);
        Utils.bitmapToMat(mCacheBitmap, mCache90Mat);
        Core.rotate(mCache90Mat, mCache90Mat, Core.ROTATE_90_CLOCKWISE);

        System.out.println("mScale = " + mScale + "，width = " + mCacheBitmap.getWidth() + "，height = " + mCacheBitmap.getHeight());
        mNarrowSize = new Size(mCacheBitmap.getWidth(), mCacheBitmap.getHeight()); // 设置新图片的大小
    }

    public void onCameraViewStopped()
    {
        mGray.release();
        mRgba.release();
    }

    @Override
    public Mat onCameraFrame(CvCameraViewFrame inputFrame)
    {
        mGray = inputFrame.gray();
        Mat narrowMat = new Mat(mNarrowSize, CvType.CV_8UC1);
        Imgproc.resize(mGray, narrowMat, mNarrowSize);
        mCache90Mat.copyTo(Matlin);
        //使前置的图像也是正的
        if (mOpenCvCameraView.getCameraIndex() == CameraBridgeViewBase.CAMERA_ID_FRONT)
        {
            Core.flip(narrowMat, narrowMat, 1);
        }
        if (mAbsoluteFaceSize == 0)
        {
            int height = narrowMat.rows();
            if (Math.round(height * mRelativeFaceSize) > 0)
            {
                mAbsoluteFaceSize = Math.round(height * mRelativeFaceSize);
            }
            if (mNativeDetector != null)
            {
                mNativeDetector.setMinFaceSize(mAbsoluteFaceSize);
            }
        }

        MatOfRect faces = new MatOfRect();
        if (mOpenCvCameraView.getDisplayOrientation() == 270)
        {
            Core.rotate(narrowMat, gMatlin, Core.ROTATE_90_COUNTERCLOCKWISE);
        }
        else
        {
            Core.rotate(narrowMat, gMatlin, Core.ROTATE_90_CLOCKWISE);
        }
        if (mNativeDetector != null)
        {
            mNativeDetector.detect(gMatlin, faces);
        }
        Rect[] faceArray = faces.toArray();
        for (Rect rect : faceArray)
        {
            Imgproc.rectangle(Matlin, rect.tl(), rect.br(), new Scalar(0, 255, 0, 255), 1);
        }
        Core.rotate(Matlin, mRgba, Core.ROTATE_90_COUNTERCLOCKWISE);

        deliverAndDrawFrame(faceArray.length, mRgba);
        return mRgba;
    }

    private Bitmap mCacheBitmap;

    protected void deliverAndDrawFrame(int count, Mat modified)
    {

        boolean bmpValid = true;
        if (modified != null)
        {
            try
            {
                Utils.matToBitmap(modified, mCacheBitmap, true);
            }
            catch (Exception e)
            {
                Log.e(TAG, "Mat type: " + modified);
                Log.e(TAG, "Bitmap type: " + mCacheBitmap.getWidth() + "*" + mCacheBitmap.getHeight());
                Log.e(TAG, "Utils.matToBitmap() throws an exception: " + e.getMessage());
                bmpValid = false;
            }
        }
        if (count > 0 && bmpValid && mCacheBitmap != null)
        {
            mImageView.updateRect(mCacheBitmap);
        }
        else
        {
            mImageView.updateRect(null);
        }
    }

    //    @Override
    //    public Mat onCameraFrame(CvCameraViewFrame inputFrame)
    //    {
    //        mGray = inputFrame.gray();
    //        mRgba = inputFrame.rgba();
    //        //使前置的图像也是正的
    //        if (mOpenCvCameraView.getCameraIndex() == CameraBridgeViewBase.CAMERA_ID_FRONT)
    //        {
    //            Core.flip(mRgba, mRgba, 1);
    //            Core.flip(mGray, mGray, 1);
    //        }
    //        if (mAbsoluteFaceSize == 0)
    //        {
    //            int height = mGray.rows();
    //            if (Math.round(height * mRelativeFaceSize) > 0)
    //            {
    //                mAbsoluteFaceSize = Math.round(height * mRelativeFaceSize);
    //            }
    //            if (mNativeDetector != null)
    //            {
    //                mNativeDetector.setMinFaceSize(mAbsoluteFaceSize);
    //            }
    //        }
    //
    //        MatOfRect faces = new MatOfRect();
    //        Core.rotate(mGray, gMatlin, Core.ROTATE_90_CLOCKWISE);
    //        Core.rotate(mRgba, Matlin, Core.ROTATE_90_CLOCKWISE);
    //        if (mNativeDetector != null)
    //        {
    //            mNativeDetector.detect(gMatlin, faces);
    //        }
    //        Rect[] faceArray = faces.toArray();
    //        for (Rect rect : faceArray)
    //            Imgproc.rectangle(Matlin, rect.tl(), rect.br(), new Scalar(0, 255, 0, 255), 2);
    //        Core.rotate(Matlin, mRgba, Core.ROTATE_90_COUNTERCLOCKWISE);
    //        return mRgba;
    //    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        Log.i(TAG, "called onCreateOptionsMenu");
        mItemFace50 = menu.add("Face size 50%");
        mItemFace40 = menu.add("Face size 40%");
        mItemFace30 = menu.add("Face size 30%");
        mItemFace20 = menu.add("Face size 20%");
        mItemType = menu.add(mDetectorName[mDetectorType]);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);
        if (item == mItemFace50)
        {
            setMinFaceSize(0.5f);
        }
        else if (item == mItemFace40)
        {
            setMinFaceSize(0.4f);
        }
        else if (item == mItemFace30)
        {
            setMinFaceSize(0.3f);
        }
        else if (item == mItemFace20)
        {
            setMinFaceSize(0.2f);
        }
        else if (item == mItemType)
        {
            int tmpDetectorType = (mDetectorType + 1) % mDetectorName.length;
            item.setTitle(mDetectorName[tmpDetectorType]);
            setDetectorType(tmpDetectorType);
        }
        return true;
    }

    private void setMinFaceSize(float faceSize)
    {
        mRelativeFaceSize = faceSize;
        mAbsoluteFaceSize = 0;
    }

    private void setDetectorType(int type)
    {
        if (mDetectorType != type)
        {
            mDetectorType = type;

            if (type == NATIVE_DETECTOR)
            {
                Log.i(TAG, "Detection Based Tracker enabled");
                mNativeDetector.start();
            }
            else
            {
                Log.i(TAG, "Cascade detector enabled");
                mNativeDetector.stop();
            }
        }
    }
}
