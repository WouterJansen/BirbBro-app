package be.birbbro.java;

import android.graphics.Bitmap;
import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

public class Classifier {

    Module model;
    float[] mean = {0.485f, 0.456f, 0.406f};
    float[] std = {0.229f, 0.224f, 0.225f};

    public Classifier(String modelPath){
        model = Module.load(modelPath);
    }

    public void setMeanAndStd(float[] mean, float[] std){
        this.mean = mean;
        this.std = std;
    }

    public Tensor preprocess(Bitmap bitmap, int size){
        bitmap = Bitmap.createScaledBitmap(bitmap,size,size,false);
        return TensorImageUtils.bitmapToFloat32Tensor(bitmap,this.mean,this.std);
    }

    public int argMax(float[] inputs){
        int maxIndex = -1;
        float maxvalue = 0.0f;

        for (int i = 0; i < inputs.length; i++){
            if(inputs[i] > maxvalue) {
                maxIndex = i;
                maxvalue = inputs[i];
            }
        }
        return maxIndex;
    }

    public float max(float[] inputs){
        float maxvalue = 0.0f;
        for (int i = 0; i < inputs.length; i++){
            if(inputs[i] > maxvalue) {
                maxvalue = inputs[i];
            }
        }
        return maxvalue;
    }

    public float sum(float[] values) {
        float result = 0;
        for (float value:values)
            result += value;
        return result;
    }

    public float[] softMax(float[] inputs){
        float[] outputs = new float[inputs.length];
        float ymax = max(inputs);

        for(int i = 0; i < inputs.length; i++)
            outputs[i] = (float) Math.exp(inputs[i] - ymax);

        float totalSum = sum(outputs);

        for(int i = 0; i < outputs.length; i++)
            outputs[i] = outputs[i] / totalSum;

        return outputs;
    }

    public ClassifierResult predict(Bitmap bitmap){
        Tensor tensor = preprocess(bitmap,224);
        IValue inputs = IValue.from(tensor);
        Tensor outputs = model.forward(inputs).toTensor();
        float[] scores = outputs.getDataAsFloatArray();
        float[] softmaxScores = softMax(scores);
        int index = argMax(scores);
        float percentage = softmaxScores[index];
        return new ClassifierResult(index, percentage);
    }
}

