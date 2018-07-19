package dev.wadehuang.yangyoulin.tools;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.Image;

import java.nio.ByteBuffer;

/**
 * 图片编码转换辅助类
 *
 * YUV，分为三个分量，“Y”表示明亮度（Luminance或Luma）
 * ，也就是灰度值；而“U”和“V” 表示的则是色度（Chrominance或Chroma），
 * 作用是描述影像色彩及饱和度，用于指定像素的颜色。
 * 与我们熟知的RGB类似，YUV也是一种颜色编码方法，主要用于电视系统以及模拟视频领域，
 * 它将亮度信息（Y）与色彩信息（UV）分离，没有UV信息一样可以显示完整的图像，只不过是黑白的，
 * 这样的设计很好地解决了彩色电视机与黑白电视的兼容问题。
 * 并且，YUV不像RGB那样要求三个独立的视频信号同时传输，所以用YUV方式传送占用极少的频宽。
 */
public class ImageHelper {
    private static final int IMAGE_MEAN = 0;
    private static final float IMAGE_STD = 255;
    static final int kMaxChannelValue = 262143;

    /**
     * bitmap 转换为数组形式
     */
    public static float[] bitmapToFloat(Bitmap bitmap) {
        int[] intValues = new int[bitmap.getWidth() * bitmap.getHeight()];
        float[] result = new float[bitmap.getWidth() * bitmap.getHeight() * 3];

        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());


        for (int i = 0; i < intValues.length; ++i) {
            final int val = intValues[i];
            result[i * 3] = (((val >> 16) & 0xFF) - IMAGE_MEAN) / IMAGE_STD;
            result[i * 3 + 1] = (((val >> 8) & 0xFF) - IMAGE_MEAN) / IMAGE_STD;
            result[i * 3 + 2] = ((val & 0xFF) - IMAGE_MEAN) / IMAGE_STD;
        }

        return result;
    }

    /**
     * image 转换为 bitmap 节省内存
     * @param image
     * @return
     */
    public static void imageToBitmap(Image image, Bitmap bitmap) {
        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();

        int[] argb = new int[imageWidth * imageHeight];
        final Image.Plane[] planes = image.getPlanes();
        final byte[][] yuvBytes = new byte[3][];

        for (int i = 0; i < planes.length; ++i) {
            final ByteBuffer buffer = planes[i].getBuffer();
            if (yuvBytes[i] == null) {
                yuvBytes[i] = new byte[buffer.capacity()];
            }
            buffer.get(yuvBytes[i]);
        }

        final int yRowStride = planes[0].getRowStride();
        final int uvRowStride = planes[1].getRowStride();
        final int uvPixelStride = planes[1].getPixelStride();

        convertYUV420ToARGB8888(
                yuvBytes[0],
                yuvBytes[1],
                yuvBytes[2],
                imageWidth,
                imageHeight,
                yRowStride,
                uvRowStride,
                uvPixelStride,
                argb);

        bitmap.setPixels(argb, 0, imageWidth, 0, 0, imageWidth, imageHeight);
    }

    /**
     * Returns a transformation matrix from one reference frame into another.
     * Handles cropping (if maintaining aspect ratio is desired) and rotation.
     *
     * @param srcWidth Width of source frame.
     * @param srcHeight Height of source frame.
     * @param dstWidth Width of destination frame.
     * @param dstHeight Height of destination frame.
     * @param applyRotation Amount of rotation to apply from one frame to another.
     *  Must be a multiple of 90.
     * @param maintainAspectRatio If true, will ensure that scaling in x and y remains constant,
     * cropping the image if necessary.
     * @return The transformation fulfilling the desired requirements.
     */
    public static Matrix getTransformationMatrix(
            final int srcWidth,
            final int srcHeight,
            final int dstWidth,
            final int dstHeight,
            final int applyRotation,
            final boolean maintainAspectRatio) {
        final Matrix matrix = new Matrix();

        if (applyRotation != 0) {
            // Translate so center of image is at origin.
            matrix.postTranslate(-srcWidth / 2.0f, -srcHeight / 2.0f);

            // Rotate around origin.
            matrix.postRotate(applyRotation);
        }

        // Account for the already applied rotation, if any, and then determine how
        // much scaling is needed for each axis.
        final boolean transpose = (Math.abs(applyRotation) + 90) % 180 == 0;

        final int inWidth = transpose ? srcHeight : srcWidth;
        final int inHeight = transpose ? srcWidth : srcHeight;

        // Apply scaling if necessary.
        if (inWidth != dstWidth || inHeight != dstHeight) {
            final float scaleFactorX = dstWidth / (float) inWidth;
            final float scaleFactorY = dstHeight / (float) inHeight;

            if (maintainAspectRatio) {
                // Scale by minimum factor so that dst is filled completely while
                // maintaining the aspect ratio. Some image may fall off the edge.
                final float scaleFactor = Math.max(scaleFactorX, scaleFactorY);
                matrix.postScale(scaleFactor, scaleFactor);
            } else {
                // Scale exactly to fill dst from src.
                matrix.postScale(scaleFactorX, scaleFactorY);
            }
        }

        if (applyRotation != 0) {
            // Translate back from origin centered reference to destination frame.
            matrix.postTranslate(dstWidth / 2.0f, dstHeight / 2.0f);
        }

        return matrix;
    }

    private static void convertYUV420ToARGB8888(
            byte[] yData,
            byte[] uData,
            byte[] vData,
            int width,
            int height,
            int yRowStride,
            int uvRowStride,
            int uvPixelStride,
            int[] out) {

        int i = 0;
        for (int y = 0; y < height; y++) {
            int pY = yRowStride * y;
            int pUV = uvRowStride * (y >> 1);

            for (int x = 0; x < width; x++) {
                int uv_offset = pUV + (x >> 1) * uvPixelStride;
                out[i++] =
                        YUV2RGB(
                                convertByteToInt(yData, pY + x),
                                convertByteToInt(uData, uv_offset),
                                convertByteToInt(vData, uv_offset));
            }
        }
    }

    private static int convertByteToInt(byte[] arr, int pos) {
        return arr[pos] & 0xFF;
    }

    /**
     * yuv编码格式转换为rgb格式
     */
    private static int YUV2RGB(int nY, int nU, int nV) {
        nY -= 16;
        nU -= 128;
        nV -= 128;
        if (nY < 0) nY = 0;

        int nR = 1192 * nY + 1634 * nV;
        int nG = 1192 * nY - 833 * nV - 400 * nU;
        int nB = 1192 * nY + 2066 * nU;

        nR = Math.min(kMaxChannelValue, Math.max(0, nR));
        nG = Math.min(kMaxChannelValue, Math.max(0, nG));
        nB = Math.min(kMaxChannelValue, Math.max(0, nB));

        nR = (nR >> 10) & 0xff;
        nG = (nG >> 10) & 0xff;
        nB = (nB >> 10) & 0xff;

        return 0xff000000 | (nR << 16) | (nG << 8) | nB;
    }

}
