package com.comet.keyboard.dictionary;

import com.comet.keyboard.layouts.KeyboardLayout;

public final class EditDistance {
	private static final int DELETE = 1;	// Missing keys
	private static final int INSERT = 1;		// Extra keys
	private static final int SUBSTITUTE = 1;	// Adjacent key
	private static final int TRANS = 1;		// Transposed keys
	private static final int JOINED = 1;	// Conjoined bigram


	public static int getMaxEditDistance(CharSequence prefix) {
		if(prefix.length() <= 4)
			return 1;

		return 2;
	}
	
	
	public static int getDelete() {
		return DELETE;
	}
	
	
	public static int getInsert() {
		return INSERT;
	}


	public static int getSubstitute() {
		return KeyboardLayout.getCurrentLayout().getSubstituteEditDistance();
	}
	
	
	public static int getTranspose() {
		return TRANS;
	}
	
	
	public static int getJoined() {
		return JOINED;
	}
	
	
	public static int getDefaultSubstitue() {
		return SUBSTITUTE;
	}
}
