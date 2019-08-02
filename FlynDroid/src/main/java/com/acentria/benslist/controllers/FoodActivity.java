package com.acentria.benslist.controllers;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;

import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;

import com.acentria.benslist.R;

import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.view.KeyEvent;
import android.view.LayoutInflater;


import com.acentria.benslist.Config;
import com.acentria.benslist.SwipeMenu;
import com.acentria.benslist.Utils;
import com.acentria.benslist.fragments.CreatCharityFrag;
import com.acentria.benslist.fragments.CreateFoodFrag;
import com.acentria.benslist.fragments.DonateCharityFrag;
import com.acentria.benslist.fragments.DonateFoodFrag;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;
import com.jeremyfeinstein.slidingmenu.lib.app.SlidingActivity;
import com.jeremyfeinstein.slidingmenu.lib.app.SlidingActivityHelper;

public class FoodActivity extends AppCompatActivity {
    Activity mActivity;
    private SlidingActivityHelper mHelper;
    private SlidingMenu mSlidingMenu;
    /*.............1st approch for implements charity food*/
    private LinearLayout ll_container;
    private TabLayout tabLayout;
    private String TAG = "CreatFoodActivity=>";


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_food_layout);
        mHelper = new SlidingActivityHelper(this);
        mActivity = this;
//        ActionBar actionBar = Config.context.getSupportActionBar();
//        actionBar.show();
        intiWigits();

    }

    /*Initiate weights*/
    private void intiWigits() {
        ll_container = findViewById(R.id.ll_container);
        tabLayout = findViewById(R.id.tabLayout);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("Create Food");
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        tabLayout.addTab(tabLayout.newTab().setText("Create Food Charity"));
        tabLayout.addTab(tabLayout.newTab().setText("Donate Food charity"));

        openfragment(new CreateFoodFrag());
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0:
                        openfragment(new CreateFoodFrag());
                        break;
                    case 1:
                        openfragment(new DonateFoodFrag());
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


    private void openfragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.ll_container, fragment)
                /*.addToBackStack(null)*/.commit();
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



    /*Try to implement Approch on 28july2019*/


    public void testMethod(View view) {
        // startActivity(new Intent(this,CharityActivity.class));
        mSlidingMenu.toggle();
    }

    public void showContent() {
        mSlidingMenu.getMenu().setVisibility(View.GONE);
    }

    private void backButton() {
        onBackPressed();
    }

    @Override
    public void onBackPressed() {
//        super.onBackPressed();
        Utils.showContent();
        finish();
        Log.e(TAG, "onBackPressed");
    }


/* public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && mSlidingMenu.isMenuShowing()) {
           // showContent();
            return true;
        }
        return false;
    }*/

   /* @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {


        new SlidingActivity().onKeyUp(keyCode,event);
        return super.onKeyUp(keyCode, event);
    }*/
}
