package com.comet.keyboard.dictionary;

import com.comet.keyboard.layouts.KeyboardLayout;

public final class EditDistance {
	public static final double DELETE = 1;		// Missing keys
	public static final double INSERT = 1;		// Extra keys
	public static final double TRANS = 1;		// Transposed keys
	public static final double SUBSTITUTE = 1;	// Adjacent key
	public static final double JOINED = 1;		// Conjoined bigram


	public static double getMaxEditDistance(CharSequence prefix) {
		if(prefix.length() <= 4) {
			return 2;
		} else if(prefix.length() <= 8) {
			return 3;
		}

		return 4;
	}
}
