package com.acentria.benslist.controllers;


import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;

import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;

import com.acentria.benslist.Config;
import com.acentria.benslist.R;
import com.acentria.benslist.Utils;
import com.acentria.benslist.fragments.CreatCharityFrag;
import com.acentria.benslist.fragments.DonateCharityFrag;


public class CharityActivity extends AppCompatActivity {
    private static final String TAG = "CharityActivity=>";
    private LinearLayout ll_container;
    private TabLayout tabLayout;



    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Create Charity");
        setContentView(R.layout.activity_charity_layout);
        ll_container = (LinearLayout) findViewById(R.id.ll_container);
        tabLayout = (TabLayout) findViewById(R.id.tabLayout);

        /* enable back action */
        ActionBar actionbar = getSupportActionBar();
        ((ActionBar) actionbar).setDisplayHomeAsUpEnabled(true);

        tabLayout.addTab(tabLayout.newTab().setText("Create Charity"));
        tabLayout.addTab(tabLayout.newTab().setText("Donate charity"));
        openfragment(new CreatCharityFrag());

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0:
                        openfragment(new CreatCharityFrag());
                        break;
                    case 1:
                        openfragment(new DonateCharityFrag());
                        break;
                }
                Log.e(TAG, "onTabSelected=>" + tab.getPosition() + " " + tab.getText());

            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                Log.e(TAG, "onTabUnselected=>" + tab.getPosition() + " " + tab.getText());
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                Log.e(TAG, "onTabReselected=>" + tab.getPosition() + " " + tab.getText());
            }
        });
    }


    @Override
    public void onBackPressed() {
//        super.onBackPressed();
        Utils.showContent();
        finish();
        Log.e(TAG, "onBackPressed");
    }


    private void openfragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.ll_container, fragment)
                /* .addToBackStack(null)*/.commit();
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressMethod();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    private void onBackPressMethod() {
        Utils.showContent();
        finish();
    }
}
