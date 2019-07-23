package com.acentria.benslist.elements;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RadioButton;

import com.acentria.benslist.Lang;

public class FlyRadioButton extends RadioButton {
	
	public FlyRadioButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		String text = (String) this.getText();
		this.setText(Lang.get(text));
	}
}