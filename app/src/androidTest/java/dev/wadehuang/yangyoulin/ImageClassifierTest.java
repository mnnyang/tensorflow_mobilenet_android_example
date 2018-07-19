package dev.wadehuang.yangyoulin;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import dev.wadehuang.yangyoulin.tools.ImageClassifier;

@RunWith(AndroidJUnit4.class)
public class ImageClassifierTest {
    @Test
    public void recognizeImage() throws Exception {
        Context appContext = InstrumentationRegistry.getTargetContext();

        ImageClassifier imageClassifier = new ImageClassifier(appContext);

//        InputStream stream = appContext.getAssets().open("example_image.jpg");
//        Bitmap bitmap = BitmapFactory.decodeStream(stream);
//
//        List<Recognition> results = imageClassifier.recognizeImage(bitmap);
//
//        assertEquals(results.get(0).getId(), "818");
    }

}