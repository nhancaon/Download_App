package com.example.dowloadfile.Adapter;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.example.dowloadfile.Fragment.AddFragment;
import com.example.dowloadfile.Fragment.DownloadFragment;
import com.example.dowloadfile.Fragment.UploadFragment;

public class ViewPagerAdapter extends FragmentPagerAdapter {
    private Context context;
    private String[] tabTitles;
    public ViewPagerAdapter(@NonNull FragmentManager fm, int behavior, Context context, String[] tabTitles) {
        super(fm, behavior);
        this.context = context;
        this.tabTitles = tabTitles;
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                return new AddFragment(tabTitles);
            case 1:
                return new AddFragment(tabTitles);
            case 2:
                return new DownloadFragment();
            case 3:
                return new UploadFragment();
            default:
                return new AddFragment(tabTitles);
        }
    }

    @Override
    public int getCount() {
        return 4;
    }
}