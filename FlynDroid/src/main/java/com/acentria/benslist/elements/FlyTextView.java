package com.acentria.benslist.elements;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

import com.acentria.benslist.Lang;

public class FlyTextView extends TextView {
	
	public FlyTextView(Context context, AttributeSet attrs) {
		super(context, attrs);		
		
		String text = (String) this.getText();
		this.setText(Lang.get(text));
	}
}