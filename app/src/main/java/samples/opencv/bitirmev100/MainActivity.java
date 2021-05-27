package samples.opencv.bitirmev100;

import androidx.annotation.FloatRange;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.solver.widgets.Rectangle;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.CpuUsageInfo;
import android.os.Handler;
import android.provider.ContactsContract;
import android.service.quicksettings.Tile;
import android.util.Log;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.InstallCallbackInterface;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Range;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.CLAHE;
import org.opencv.imgproc.GeneralizedHough;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.tabs.TabLayout;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EventListener;
import java.util.List;
import java.util.Random;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class MainActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2  {
    private static final String TAG = "OCVSample::Activity";

    private CameraBridgeViewBase mOpenCvCameraView;
    private boolean              mIsJavaCamera = true;
    private MenuItem             mItemSwitchCamera = null;
    File eyecascFile;
    TextView resultText;
    String result;
    SharedArea sharedArea;

    CascadeClassifier eyeDetector;
    private Mat mRgba, mGray;
    int statringFrameIndex;
    int rand = 0;
    int score = 0;
    Random randomInteger;



    private BaseLoaderCallback baseLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    InputStream is = getResources().openRawResource(R.raw.haarcascade_eye);
                    File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                    eyecascFile = new File(cascadeDir, "haarcascade_eye.xml");

                    FileOutputStream fos = null;
                    try {
                        fos = new FileOutputStream(eyecascFile);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                    byte[] buffer = new byte[4096];
                    int bytesRead = 0;

                    while (true) {
                        try {
                            if (!((bytesRead = is.read(buffer)) != -1)) break;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        try {
                            fos.write(buffer, 0, bytesRead);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    try {
                        is.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    eyeDetector = new CascadeClassifier(eyecascFile.getAbsolutePath());
                    if (eyeDetector.empty()) {
                        eyeDetector = null;
                    } else
                        cascadeDir.delete();
                    mOpenCvCameraView.enableView();
                }
                    break;

                default: {
                    super.onManagerConnected(status);
                }
                break;
            }


        }
    };

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.tutorial1_activity_java_surface_view); //activity_main (görüntü dosyasında bulunan objeyi bu değişken üzerinden tanıtıyoruz)
        mOpenCvCameraView.setCameraIndex(1); //Ön Kamera
        sharedArea = new SharedArea();
        resultText = findViewById(R.id.resultText);



        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            // Kullanıcıdan izin alma komutu burada olacak.


        } else { //eğer izin verilmişse
            Log.d("TAG : ", "Permissions granted");
            mOpenCvCameraView.setCameraPermissionGranted(); //izin verildiğini nesneye belirt.
            if(OpenCVLoader.initDebug()) {
                baseLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
            }

        }


        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);

        mOpenCvCameraView.setCvCameraViewListener(this);


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
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, baseLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            baseLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        statringFrameIndex = 0;
    }

    public void onCameraViewStopped() {
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {


                mRgba = inputFrame.rgba();
                Mat gray = inputFrame.gray(); //input frame with grayscale
                Mat threshold = new Mat();

                // gray = mRgba;
                Mat area = new Mat();
                Rect pupilRect;


                Mat eyeRectangle = new Mat(); //eye area
                Core.flip(gray.t(), gray, 0); //mRgba.t() is the transpose


        Imgproc.putText(gray,"Look at : " + ((rand==0) ? "RIGHT":"LEFT") ,new Point(gray.width()/2,gray.height()/2),Imgproc.FONT_HERSHEY_COMPLEX,1,new Scalar(255,0,0),2);



        Rect r = new Rect(new Point(gray.width()/15,(gray.height()/3)+gray.height()/15), new Point(14*gray.width()/15,gray.height()/15));
                Imgproc.rectangle(gray,r,new Scalar(255,255,255),3);
                Mat findEyeArea = gray.submat(r);

                Imgproc.medianBlur(gray,gray,5);
                // Imgproc.threshold(gray, threshold, 40,255,Imgproc.THRESH_BINARY_INV);

                MatOfRect eyeDetections = new MatOfRect();
                eyeDetector.detectMultiScale(findEyeArea,eyeDetections);

                if(eyeDetections.toArray().length !=0 ) { //eye detected, rectangle will be drawn
                    System.out.println("eyes detected.");

                    for (Rect rect : eyeDetections.toArray()) {
                        if(rect.height < 50) break;
                        //the area that will be try to find circular object
                        Imgproc.rectangle(findEyeArea, new Point(rect.x, rect.y),
                                new Point(rect.x + rect.width, rect.y + rect.height),
                                new Scalar(255, 255, 0), 5);
                        //Draw crosslines on that rectangle area.---
                        //Draw crosslines on that rectangle area.---

                        eyeRectangle = findEyeArea .submat(rect); //- Haar cascade ile bulunan gözleri içeren alan (İşlemler bu alan üzeridnen yapılacak)
                        //Imgproc.cvtColor(eyeRectangle,eyeRectangle,Imgproc.COLOR_RGBA2GRAY);

                        // Imgproc.threshold(eyeRectangle,threshold,50,255,Imgproc.THRESH_BINARY_INV); //bu alana threshold işlemi uygulanır. (Göz bebeğini, gözün yuvarlak kısmından ayırt etmek için)
                        // Imgproc.adaptiveThreshold(eyeRectangle,threshold,255,Imgproc.ADAPTIVE_THRESH_MEAN_C,Imgproc.THRESH_BINARY_INV,15,40);


                        int bestTH = findBestThreshold(eyeRectangle);
                        String thresholdString = Integer.toString(bestTH);

                        Imgproc.threshold(eyeRectangle,threshold,bestTH,255,Imgproc.THRESH_BINARY_INV);
                        Imgproc.putText(gray,thresholdString,new Point(rect.x,1.5*rect.y),Imgproc.FONT_HERSHEY_PLAIN,3,new Scalar(255,255,255),4);

                        List<MatOfPoint> contours = new ArrayList<>(); //Bu bölgeyi temsil eden bir Liste elemanı tutulur.
                        Imgproc.findContours(threshold, contours, area, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE); //değerler bu elemana aktarılır. contours değişkeni, threshold değerine bağlı olarak ayırt edilmiş alanları temsil eder.
                        //contours değeri eleman içermek zorunda değildir. Bu alan içindeki tüm pixel değerleri th değerinden küçükse veya büyükse contours değeri 0 dır.
                        //th değerinden büyük ve küçük değerler bir arada bu alan içinde ise contours değişkeni 0 dan farklıdır.

                        //Sorting Side
                        Collections.sort(contours, new Comparator<MatOfPoint>() {
                            @Override
                            public int compare(MatOfPoint o1, MatOfPoint o2) {
                                Rect rect1 = Imgproc.boundingRect(o1);
                                Rect rect2 = Imgproc.boundingRect(o2);

                                return  Integer.valueOf((int) rect2.area()).compareTo((int) rect1.area());
                            }
                        });//threshold değerinden sonra bulunan alanlar içinden en büyüğünü bulmak için sıralama kullanılır.

                        if(contours.size() != 0) { //eğer threshold değerine bağlı olarak ayırt edilebilen bir alan bulunmuşsa
                            MatOfPoint2f contour2f = new MatOfPoint2f(contours.get(0).toArray() ); //kullanılmış sıralama algoritması en büyük alanlı dikdörtgeni listenin başına getirir. Bu yüzden listenin ilk elemanı ekrana çizilir.
                            MatOfPoint points = new MatOfPoint(contour2f.toArray() );
                            pupilRect = Imgproc.boundingRect(points);
                            Imgproc.rectangle(eyeRectangle, new Point(pupilRect.x, pupilRect.y), new Point(pupilRect.x + pupilRect.width, pupilRect.y + pupilRect.height), new Scalar(255, 0, 0, 255), 1);
                            Point centerPoint = new Point((pupilRect.x+pupilRect.width)/2,(pupilRect.y+pupilRect.height)/2);

                            int yon = yonBul(centerPoint,rect);

                            switch (yon) {
                                case 0:
                                    Imgproc.putText(gray,"SAG",new Point(rect.x,rect.y),Imgproc.FONT_HERSHEY_PLAIN,3,new Scalar(255,255,255),4);
                                    break;
                                case 1:
                                    Imgproc.putText(gray,"SOL",new Point(rect.x,rect.y),Imgproc.FONT_HERSHEY_PLAIN,3,new Scalar(255,255,255),4);
                                    break;
                                case 2:
                                    Imgproc.putText(gray,"ORTA",new Point(rect.x,rect.y),Imgproc.FONT_HERSHEY_PLAIN,3,new Scalar(255,255,255),4);
                                    break;

                                default:
                            }

                            if(yon == rand)
                            {
                            randomInteger = new Random();
                            rand = randomInteger.nextInt(2);

                            Point p = new Point();
                            p.x = gray.width()/2;
                            p.y = gray.height()/2;
                                score++;
                            }

                        }
                    }
                }

                Point scorePoint = new Point();
                scorePoint.x = gray.width() / 15;
                scorePoint.y = gray.height()/10;

          Imgproc.putText(gray,"Your score : " + score,scorePoint,Imgproc.FONT_HERSHEY_COMPLEX,2,new Scalar(255,0,0),4);

        return gray;

    }

    int yonBul(Point p, Rect rect) {
        double cnt = (p.x / rect.width);
        int result;
       if(cnt < 0.2) {
            result =  0;
        }
        else if(cnt > 0.2 && cnt < 0.35){
            result = 2;
        }
        else {
            result = 1;
        }


        return result;
    }

    int findBestThreshold(Mat eyeMat) {
        Mat threshold = new Mat();
        Mat area = new Mat();
        Rect pupilRect = new Rect();

        double eyeMatArea = eyeMat.width() * eyeMat.height();

        for (int i = 0; i < 255; i++) {

            Imgproc.threshold(eyeMat, threshold, i, 255, Imgproc.THRESH_BINARY_INV);

            List<MatOfPoint> contours = new ArrayList<>(); //Bu bölgeyi temsil eden bir Liste elemanı tutulur.
            Imgproc.findContours(threshold, contours, area, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE); //değerler bu elemana aktarılır. contours değişkeni, threshold değerine bağlı olarak ayırt edilmiş alanları temsil eder.
            //contours değeri eleman içermek zorunda değildir. Bu alan içindeki tüm pixel değerleri th değerinden küçükse veya büyükse contours değeri 0 dır.
            //th değerinden büyük ve küçük değerler bir arada bu alan içinde ise contours değişkeni 0 dan farklıdır.

            if(contours.size() == 0)
                continue;

            //Sorting Side
            Collections.sort(contours, new Comparator<MatOfPoint>() {
                @Override
                public int compare(MatOfPoint o1, MatOfPoint o2) {
                    Rect rect1 = Imgproc.boundingRect(o1);
                    Rect rect2 = Imgproc.boundingRect(o2);

                    return Integer.valueOf((int) rect2.area()).compareTo((int) rect1.area());
                }
            });//threshold değerinden sonra bulunan alanlar içinden en büyüğünü bulmak için sıralama kullanılır.

            if (contours.size() != 0) { //eğer threshold değerine bağlı olarak ayırt edilebilen bir alan bulunmuşsa
                MatOfPoint2f contour2f = new MatOfPoint2f(contours.get(0).toArray()); //kullanılmış sıralama algoritması en büyük alanlı dikdörtgeni listenin başına getirir. Bu yüzden listenin ilk elemanı ekrana çizilir.
                MatOfPoint points = new MatOfPoint(contour2f.toArray());
                pupilRect = Imgproc.boundingRect(points);

                double ratio = eyeMatArea / pupilRect.area(); // bölü

                if( ratio > 5) {
                    return i;
                }

            }

        }

        return 50;
    }


}