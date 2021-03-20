package be.birbbro.java;

import android.content.res.Resources;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.List;

import be.birbbro.R;

public class ViewPagerAdapter extends FragmentStatePagerAdapter {

    private List<Double> timestamps;
    private HashMap<String, Integer> predictedImages =new HashMap<String, Integer>();
    String defaultClassName;

    public ViewPagerAdapter(FragmentManager fm, List<Double> timestamps, HashMap<String, Integer> predictedImages, String defaultClassName) {
        super(fm);
        this.timestamps = timestamps;
        this.predictedImages = predictedImages;
        this.defaultClassName = defaultClassName;
    }

    @Override
    public Fragment getItem(int position) {
        String className = defaultClassName;
        String key = String.format("%.0f", timestamps.get(position));
        if(predictedImages.containsKey(key)){
            Integer classIndex = predictedImages.get(key);
            className = Constants.BIRBBROML_CLASSES[classIndex];
        }
        return PageFragment.getInstance(timestamps.get(position), className);
    }

    public String getTimestamp(int position) {
        String key = String.format("%.0f", timestamps.get(position));
        return key;
    }

    @Override
    public int getCount() {
        return timestamps.size();
    }
}
