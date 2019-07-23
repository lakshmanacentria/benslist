package com.acentria.benslist.elements;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

public class ExpandableTextView extends TextView {
    public ExpandableTextView(Context context) {
        this(context, null);
    }
 
    public ExpandableTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        
        setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				
			}
        });
    }
    
    @Override
    public void setText(CharSequence text, BufferType type) {
    	super.setText(text, type);
    }
}