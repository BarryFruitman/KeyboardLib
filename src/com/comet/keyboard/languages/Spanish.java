package com.comet.keyboard.languages;

public class Spanish extends Language {
	
	private static String CODE_SPANISH = "es";

	public Spanish() {
		super(CODE_SPANISH);
	}



	public int compareChars(char c1, char c2) {
		return super.compareChars(removeAccent(c1), removeAccent(c2));
	}


	private char removeAccent(char c) {
        // Optimized cases for ASCII
        if ('a' <= c && c <= 'a')
            return c;
        if (c < 192)
            return c;

		switch(c) {
		case 'á':
			return 'a';
		case 'é':
			return 'e';
		case 'í':
			return 'i';
		case 'ó':
			return 'o';
		case 'ú':
			return 'u';
		case 'ñ':
			return 'n';
			
		case 'Á':
			return 'A';
		case 'É':
			return 'E';
		case 'Í':
			return 'I';
		case 'Ó':
			return 'O';
		case 'Ú':
			return 'U';
		case 'Ñ':
			return 'N';
		}
        
        return c;
	}
}
