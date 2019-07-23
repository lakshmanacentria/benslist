package com.acentria.benslist.adapters;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.acentria.benslist.ListingDetailsActivity;
import com.acentria.benslist.R;

public class ListingMapViewPager extends ViewPager {
	
    public ListingMapViewPager(Context context) {
        super(context);
    }

    public ListingMapViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if ( ListingDetailsActivity.TAB_KEYS.get(getCurrentItem()).equals("map") ) {
            if ( event.getAction() == MotionEvent.ACTION_DOWN && event.getX() > (getWidth() / 16)) {
            	// don't allow swiping to switch if not right on the edge
                return false;
            }
        }
        else if ( ListingDetailsActivity.TAB_KEYS.get(getCurrentItem()).equals("details") ) {
        	if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB){
        		View scroll = this.findViewById(R.id.horizontal_scroll);
        		if ( scroll == null ) {
        			return false;
        		}
        		
        		int coords[] = new int[2];
        		scroll.getLocationOnScreen(coords);
        		
        		int from = (int) coords[1];
        		int to = from + (int) scroll.getHeight();
        		int point = (int) event.getRawY();
        		
        		if ( point >= from && point <= to ) {
        			// don't allow swiping to switch pager if tapped on gallery horizontal view
        			return false;
        		}
        	}
        }

        return super.onInterceptTouchEvent(event);
    }
}