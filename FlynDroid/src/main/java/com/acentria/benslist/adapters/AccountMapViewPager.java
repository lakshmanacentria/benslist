package com.acentria.benslist.adapters;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.acentria.benslist.AccountDetailsActivity;

public class AccountMapViewPager extends ViewPager {

    public AccountMapViewPager(Context context) {
        super(context);
    }

    public AccountMapViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if ( AccountDetailsActivity.TAB_KEYS.get(getCurrentItem()).equals("map") ) {
            if ( event.getAction() == MotionEvent.ACTION_DOWN && event.getX() > (getWidth() / 16)) {
            	// don't allow swiping to switch if not right on the edge
                return false;
            }
        }

        return super.onInterceptTouchEvent(event);
    }
}