package be.birbbro.java;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import java.util.List;

public class ViewPagerAdapter extends FragmentStatePagerAdapter {

    private List<Double> timestamps;

    public ViewPagerAdapter(FragmentManager fm, List<Double> timestampsList) {
        super(fm);
        this.timestamps = timestampsList;
    }

    @Override
    public Fragment getItem(int position) {
        return PageFragment.getInstance(timestamps.get(position));
    }

    @Override
    public int getCount() {
        return timestamps.size();
    }
}
