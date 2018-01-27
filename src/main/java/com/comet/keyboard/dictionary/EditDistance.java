package com.comet.keyboard.dictionary;

import com.comet.keyboard.layouts.KeyboardLayout;

public final class EditDistance {
	public static final int DELETE = 1;		// Missing keys
	public static final int INSERT = 1;		// Extra keys
	public static final int TRANS = 1;		// Transposed keys
	public static final int SUBSTITUTE = 1;	// Adjacent key
	public static final int JOINED = 1;		// Conjoined bigram


	public static int getMaxEditDistance(CharSequence prefix) {
		if(prefix.length() <= 4) {
			return 2;
		} else if(prefix.length() <= 8) {
			return 3;
		}

		return 4;
	}
}
