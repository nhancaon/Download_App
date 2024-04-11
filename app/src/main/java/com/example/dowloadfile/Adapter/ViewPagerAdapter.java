package com.example.dowloadfile.Adapter;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.example.dowloadfile.Fragment.AddFragment;
import com.example.dowloadfile.Fragment.DoneFragment;
import com.example.dowloadfile.Fragment.MenuFragment;
import com.example.dowloadfile.Fragment.OtherFragment;
import com.example.dowloadfile.Fragment.QueueFragment;
import com.example.dowloadfile.Fragment.URLFragment;

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
                return new OtherFragment();
            case 2:
                return new URLFragment();
            case 3:
                return new AddFragment(tabTitles);
            case 4:
                return new QueueFragment();
            case 5:
                return new DoneFragment();
            case 6:
                return new MenuFragment();
            default:
                return new AddFragment(tabTitles);
        }
    }

    @Override
    public int getCount() {
        return 7;
    }
}