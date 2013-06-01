package com.comet.keyboard.languages;

public class Hungarian extends Language {
	
	private static String CODE_HUNGARIAN = "hu";

	public Hungarian() {
		super(CODE_HUNGARIAN);
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
		case 'ö':
		case 'ő':
			return 'o';
		case 'ú':
		case 'ü':
		case 'ű':
			return 'u';
			
		case 'Á':
			return 'A';
		case 'É':
			return 'E';
		case 'Í':
			return 'I';
		case 'Ó':
		case 'Ö':
		case 'Ő':
			return 'O';
		case 'Ú':
		case 'Ü':
		case 'Ű':
			return 'U';
		}
        
        return c;
	}
}
