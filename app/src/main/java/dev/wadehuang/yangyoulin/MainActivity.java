package dev.wadehuang.yangyoulin;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Size;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import java.util.List;

import dev.wadehuang.yangyoulin.tools.ImageClassifier;
import dev.wadehuang.yangyoulin.tools.ImageHelper;
import dev.wadehuang.yangyoulin.tools.Recognition;
import dev.wadehuang.yangyoulin.camera.CameraPreviewFragment;


public class MainActivity extends AppCompatActivity
        implements CameraPreviewFragment.CameraPreviewListener {

    private static final String TAG = "MainActivity";
    private static final String PERMISSION_CAMERA = Manifest.permission.CAMERA;
    private static final int PERMISSIONS_REQUEST = 1;
    private static final boolean DEBUG_MODE = false;

    private ListView resultListView;
    private ImageView previewImageView;

    private ArrayAdapter<Recognition> resultListAdapter;

    private boolean computing;
    private ImageClassifier imageClassifier;
    private Bitmap bitmap;
    /**
     * 剪切图
     */
    private Bitmap croppedBitmap;
    /**
     * 图像矩阵 剪切使用
     */
    private Matrix frameToCropTransform;
    private List<Recognition> resultList;

    private Runnable updateResult = new Runnable() {
        @Override
        public void run() {
            resultListAdapter.clear();
            resultListAdapter.addAll(resultList);
            computing = false;
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (hasPermission()) {
            if (null == savedInstanceState) {
                init();
            }
        } else {
            requestPermission();
        }
    }

    private void init() {
        resultListView = (ListView) findViewById(R.id.resultList);
//        previewImageView = (ImageView) findViewById(R.id.previewImage);

        resultListAdapter = new ArrayAdapter<>(this, R.layout.item_recogition);
        resultListView.setAdapter(this.resultListAdapter);

        imageClassifier = new ImageClassifier(this);

        getFragmentManager().beginTransaction()
                .replace(R.id.container, CameraPreviewFragment.newInstance(this))
                .commit();

    }

    /**
     * 摄像头准备预览就绪回调
     */
    @Override
    public void onPreviewReadied(Size size, int cameraRotation) {
        bitmap = Bitmap.createBitmap(size.getWidth(), size.getHeight(), Bitmap.Config.ARGB_8888);
        croppedBitmap = Bitmap.createBitmap(ImageClassifier.INPUT_SIZE, ImageClassifier.INPUT_SIZE, Bitmap.Config.ARGB_8888);

        final Display display = getWindowManager().getDefaultDisplay();
        final int screenOrientation = display.getRotation();

        int sensorOrientation = cameraRotation + screenOrientation;
        Log.e(TAG,"sensorOrientation -> "+ sensorOrientation);

        frameToCropTransform =
                ImageHelper.getTransformationMatrix(
                        size.getHeight(), size.getWidth(),
                        ImageClassifier.INPUT_SIZE, ImageClassifier.INPUT_SIZE,
                        sensorOrientation, true);
    }

    /**
     * 摄像头最新图像回调
     */
    @Override
    public void onImageAvailable(ImageReader reader) {
        /**
         * 计算中则放弃本次图像的识别
         */
        if (computing)
            return;
        Image image = null;

        try {
            /*获取最新图片*/
            image = reader.acquireLatestImage();

            if (image == null) {
                return;
            }

            computing = true;

            /*image 转换为bitmap*/
            ImageHelper.imageToBitmap(image, bitmap);

            /*剪切图片*/
            final Canvas canvas = new Canvas(croppedBitmap);
            canvas.drawBitmap(bitmap, frameToCropTransform, null);
            image.close();

            /*识别图像*/
            resultList = imageClassifier.recognizeImage(croppedBitmap);

            runOnUiThread(updateResult);
        } catch (final Exception e) {
            Log.e(TAG, "recognizeImage", e);
        } finally {
            if (image != null) {
                image.close();
            }

            computing = false;
        }
    }

    /**
     * 判断权限
     */
    private boolean hasPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return checkSelfPermission(PERMISSION_CAMERA) == PackageManager.PERMISSION_GRANTED;
        } else {
            return true;
        }
    }

    /**
     * 申请权限
     */
    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (shouldShowRequestPermissionRationale(PERMISSION_CAMERA)) {
                Toast.makeText(MainActivity.this, "Camera permission is required for this example", Toast.LENGTH_LONG).show();
            }

            requestPermissions(new String[]{PERMISSION_CAMERA}, PERMISSIONS_REQUEST);
        }
    }

    /**
     * 申请权限回调
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                init();
            } else {
                requestPermission();
            }
        }
    }
    //endregion
}
