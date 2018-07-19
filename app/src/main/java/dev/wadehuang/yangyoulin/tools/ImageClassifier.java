package dev.wadehuang.yangyoulin.tools;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import android.content.Context;
import android.graphics.Bitmap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Vector;

public class ImageClassifier {
    public static final int INPUT_SIZE = 224;

    /**
     * 最小识别百分比
     */
    private static final float THRESHOLD = 0.1f;

    /**
     * 最大结果条数
     */
    private static final int MAX_RESULTS = 3;

    /**
     * 模型文件
     */
    private static final String MODEL_FILE = "mobilenet_v1.pb";

    private static final String LABEL_FILE = "labels.txt";
    /**
     * label条数
     */
    private static final int CLASS_SIZE = 1001;
    /**
     * 输入层名称
     */
    private static final String INPUT_NAME = "input";
    /**
     * 输出层名称
     */
    private static final String OUTPUT_NAME = "MobilenetV1/Predictions/Reshape_1";
    private static final String[] OUTPUT_NAMES = {OUTPUT_NAME};

    private Context context;
    private TensorFlowInferenceInterface tfInterface;
    private Vector<String> labels;

    public ImageClassifier(Context context) {
        this.context = context;

        this.tfInterface = new TensorFlowInferenceInterface(context.getAssets(), MODEL_FILE);

        InitLabels();
    }

    /**
     * 读取labels数据
     */
    private void InitLabels(){
        /*Vector > 线程安全的动态数组*/
        labels = new Vector<>(CLASS_SIZE);
        try {
            BufferedReader br = null;
            InputStream stream  = context.getAssets().open(LABEL_FILE);
            br = new BufferedReader(new InputStreamReader(stream));
            String line;
            while ((line = br.readLine()) != null) {
                labels.add(line);
            }
            br.close();
        } catch (IOException e) {
            throw new RuntimeException("Problem reading label file!" , e);
        }
    }

    /**
     * 识别图片
     */
    public List<Recognition> recognizeImage(final Bitmap bitmap) {
        return  recognizeImage(ImageHelper.bitmapToFloat(bitmap));
    }

    private List<Recognition> recognizeImage(final float[] imageFloats) {

        this.tfInterface.feed(INPUT_NAME, imageFloats, 1, INPUT_SIZE, INPUT_SIZE, 3);

        this.tfInterface.run(OUTPUT_NAMES, false);

        float[] outputs = new float[CLASS_SIZE];
        this.tfInterface.fetch(OUTPUT_NAME, outputs);

        PriorityQueue<Recognition> pq = new PriorityQueue<>(
                        3,
                        new Comparator<Recognition>() {
                            @Override
                            public int compare(Recognition lhs, Recognition rhs) {
                                return Float.compare(rhs.getConfidence(), lhs.getConfidence());
                            }
                        });

        for (int i = 0; i < outputs.length; ++i) {
            /*获取大于最小百分比的内容*/
            if (outputs[i] > THRESHOLD) {
                pq.add(new Recognition("" + i, labels.get(i), outputs[i], null));
            }
        }

        final ArrayList<Recognition> recognitions = new ArrayList<>();

        int recognitionsSize = Math.min(pq.size(), MAX_RESULTS);
        for (int i = 0; i < recognitionsSize; ++i) {
            recognitions.add(pq.poll());
        }

        return recognitions;
    }

}
