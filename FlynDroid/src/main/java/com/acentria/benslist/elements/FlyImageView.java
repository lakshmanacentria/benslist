package com.acentria.benslist.elements;

import android.content.Context;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ImageView;

public class FlyImageView extends ImageView
{
    public FlyImageView(Context context)
    {
        super(context);
    }

    public FlyImageView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    public FlyImageView(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        if ( event.getAction() == MotionEvent.ACTION_DOWN && isEnabled() )
            setColorFilter(0xFFC6C6C6, PorterDuff.Mode.MULTIPLY);

        if ( event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL )
            setColorFilter(null);

        return super.onTouchEvent(event);
    }
}