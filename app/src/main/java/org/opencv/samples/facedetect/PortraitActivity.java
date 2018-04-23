package org.opencv.samples.facedetect;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
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
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.facedetect.DetectionBasedTracker;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class PortraitActivity extends Activity implements CvCameraViewListener2
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
    private File mCascadeFile;
    private DetectionBasedTracker mNativeDetector;

    private int mDetectorType = JAVA_DETECTOR;
    private String[] mDetectorName;

    private float mRelativeFaceSize = 0.1f;
    private int mAbsoluteFaceSize = 0;

    private CameraBridgeViewBase mOpenCvCameraView;
    private Button mSwitchCameraView;
    private ImageView mImageView;

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
                    System.loadLibrary("detection_based_tracker");

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

    public PortraitActivity()
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

        setContentView(R.layout.face_detect_portrait_view);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.fd_activity_surface_view);
        mOpenCvCameraView.setVisibility(CameraBridgeViewBase.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_FRONT);

        mImageView = findViewById(R.id.fd_image_view);
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

    private int mViewWidth = 0;
    private int mViewHeight = 0;

    public void onCameraViewStarted(int width, int height)
    {
        mGray = new Mat();
        mRgba = new Mat();

        Matlin = new Mat();
        gMatlin = new Mat();
        mCacheBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        mAlphaBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        WindowManager manager = this.getWindowManager();
        DisplayMetrics outMetrics = new DisplayMetrics();
        manager.getDefaultDisplay().getMetrics(outMetrics);
        mViewWidth = outMetrics.widthPixels;
        mViewHeight = outMetrics.heightPixels;
    }

    public void onCameraViewStopped()
    {
        mGray.release();
        mRgba.release();
    }

    //    @Override
    //    public Mat onCameraFrame(CvCameraViewFrame inputFrame)
    //    {
    //        mGray = inputFrame.gray();
    //        // 0ms
    //        Utils.bitmapToMat(mAlphaBitmap, mRgba);
    //        //10-15ms
    //        //使前置的图像也是正的
    //        if (mOpenCvCameraView.getCameraIndex() == CameraBridgeViewBase.CAMERA_ID_FRONT)
    //        {
    //            // Core.flip(mRgba, mRgba, 1);
    //            // Core.flip(mGray, mGray, 1);
    //        }
    //        //40-50ms
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
    //        //40-50ms
    //
    //        MatOfRect faces = new MatOfRect();
    //        Core.rotate(mGray, gMatlin, Core.ROTATE_90_CLOCKWISE);
    //        Core.rotate(mRgba, Matlin, Core.ROTATE_90_CLOCKWISE);
    //        //150ms
    //        if (mNativeDetector != null)
    //        {
    //            mNativeDetector.detect(gMatlin, faces);
    //        }
    //        //180ms
    //
    //        Rect[] faceArray = faces.toArray();
    //        System.out.println("------>>>>" + faceArray.length);
    //        for (Rect rect : faceArray)
    //        {
    //            Imgproc.rectangle(Matlin, rect.tl(), rect.br(), new Scalar(0, 255, 0, 255), 2);
    //        }
    //        //190ms
    //        Core.rotate(Matlin, mRgba, Core.ROTATE_90_COUNTERCLOCKWISE);
    //        //250ms
    //        deliverAndDrawFrame(mRgba);
    //        //900ms
    //        return mRgba;
    //    }

    private Bitmap mCacheBitmap;
    private Bitmap mAlphaBitmap;

    protected void deliverAndDrawFrame(Mat modified)
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
        Matrix matrix = new Matrix(); // I rotate it with minimal process
        matrix.preTranslate((mViewWidth - mCacheBitmap.getWidth()) / 2, (mViewHeight - mCacheBitmap.getHeight()) / 2);
        //        matrix.postRotate(90f, (mViewWidth) / 2, (mViewHeight) / 2);
        float scale = (float) mViewWidth / (float) mCacheBitmap.getHeight();
        matrix.postScale(scale, scale, mViewWidth / 2, mViewHeight / 2);
        final Bitmap bitmap = Bitmap.createBitmap(mCacheBitmap, 0, 0, mCacheBitmap.getWidth(), mCacheBitmap.getHeight(), matrix, false);

        if (bmpValid && mCacheBitmap != null)
        {
            mHandler.post(new Runnable()
            {
                @Override
                public void run()
                {
                    //                    BitmapDrawable drawable = (BitmapDrawable) mImageView.getDrawable();
                    //                    if (drawable != null)
                    //                    {
                    //                        Bitmap bmp = drawable.getBitmap();
                    //                        if (null != bmp && !bmp.isRecycled())
                    //                        {
                    //                            bmp.recycle();
                    //                            bmp = null;
                    //                        }
                    //                    }
                    //                    mImageView.setImageBitmap(null);
                    mImageView.setImageBitmap(bitmap);
                }
            });
        }
    }

    private Handler mHandler = new Handler();

    @Override
    public Mat onCameraFrame(CvCameraViewFrame inputFrame)
    {
        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();

        if (mAbsoluteFaceSize == 0)
        {
            int height = mGray.rows();
            if (Math.round(height * mRelativeFaceSize) > 0)
            {
                mAbsoluteFaceSize = Math.round(height * mRelativeFaceSize);
            }
            mNativeDetector.setMinFaceSize(mAbsoluteFaceSize);
        }

        MatOfRect faces = new MatOfRect();

        if (mNativeDetector != null)
        {
            mNativeDetector.detect(mGray, faces);
        }

        Rect[] facesArray = faces.toArray();
        System.out.println("facesArray.length = " + facesArray.length);
        for (int i = 0; i < facesArray.length; i++)
            Imgproc.rectangle(mRgba, facesArray[i].tl(), facesArray[i].br(), FACE_RECT_COLOR, 3);
        return mRgba;
    }

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
