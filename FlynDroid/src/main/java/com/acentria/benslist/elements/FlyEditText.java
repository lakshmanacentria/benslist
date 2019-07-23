package com.acentria.benslist.elements;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.EditText;

import com.acentria.benslist.Lang;

public class FlyEditText extends EditText {
	
	public FlyEditText(Context context, AttributeSet attrs) {
		super(context, attrs);

		String hint = (String) this.getHint();
		this.setHint(Lang.get(hint));
	}
}