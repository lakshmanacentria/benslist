package com.acentria.benslist.elements;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;

import com.acentria.benslist.Lang;

public class FlyButton extends Button {
	
	public FlyButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		String text = (String) this.getText();
		this.setText(Lang.get(text));
	}
}