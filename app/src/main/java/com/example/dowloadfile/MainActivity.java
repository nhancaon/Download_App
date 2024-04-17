package com.example.dowloadfile;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.example.dowloadfile.Adapter.ViewPagerAdapter;
import com.example.dowloadfile.Fragment.AddFragment;
import com.google.android.material.tabs.TabLayout;

public class MainActivity extends AppCompatActivity implements TabLayout.OnTabSelectedListener {
    private TabLayout mTabLayout;
    private ViewPager mViewPager;
    private String[] tabTitles;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTabLayout = findViewById(R.id.tab_layout);
        mViewPager = findViewById(R.id.view_pager);

        // Define an array of tab titles
        tabTitles = getResources().getStringArray(R.array.tab_titles);

        ViewPagerAdapter viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager(), FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT, this, tabTitles);
        mViewPager.setAdapter(viewPagerAdapter);
        mTabLayout.setupWithViewPager(mViewPager);

        // Set the listener for tab selection changes
        mTabLayout.addOnTabSelectedListener(this);

        TabLayout.Tab tab = mTabLayout.getTabAt(1);
        if (tab != null) {
            setTitle(tabTitles[1]);
            tab.select();
        }
    }

    private void changeHeaderTab(int selectedTabPosition){
        setTitle(tabTitles[selectedTabPosition]);
    }

    @Override
    public void onTabSelected(TabLayout.Tab tab) {
        setTitle(tabTitles[tab.getPosition()]);
    }

    @Override
    public void onTabUnselected(TabLayout.Tab tab) {
        // Do nothing
    }

    @Override
    public void onTabReselected(TabLayout.Tab tab) {
        // Do nothing
    }
}
