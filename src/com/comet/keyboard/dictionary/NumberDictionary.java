package com.comet.keyboard.dictionary;

import java.util.regex.Pattern;

import com.comet.keyboard.Suggestor.Suggestion;
import com.comet.keyboard.Suggestor.Suggestions;

public class NumberDictionary implements Dictionary {

	private final Pattern mNumberPattern, mTeenPattern;
	private final String NUMBER_REGEX = "[\\d,]+";
	private final String TEEN_REGEX = "[\\d]*1[\\d]";

	public NumberDictionary() {
		mNumberPattern = Pattern.compile(NUMBER_REGEX);
		mTeenPattern = Pattern.compile(TEEN_REGEX);
	}



	@Override
	public Suggestions getSuggestions(Suggestions suggestions) {
		String prefix = suggestions.getComposing();
		if(!mNumberPattern.matcher(prefix).matches())
			return suggestions;

		suggestions.add(new NumberSuggestion(prefix.toString(), NumberSuggestion.NumberType.NORMAL));

		// Add commaed number
		StringBuilder commaed = new StringBuilder();
		if(prefix.length() > 3) {
			commaed.append(prefix);
			for(int iComma = prefix.length() - 3; iComma > 0; iComma-= 3)
				commaed.insert(iComma, ',');
			suggestions.add(new NumberSuggestion(commaed.toString(), NumberSuggestion.NumberType.COMMAED));
		}
		
		// Add ordinal
		StringBuilder ordinal = new StringBuilder();
		if(commaed.length() > 0)
			ordinal.append(commaed);
		else
			ordinal.append(prefix);

		if(mTeenPattern.matcher(prefix).matches())
			ordinal.append("th");
		else if(prefix.endsWith("1"))
			ordinal.append("st");
		else if(prefix.endsWith("2"))
			ordinal.append("nd");
		else if(prefix.endsWith("3"))
			ordinal.append("rd");
		else 
			ordinal.append("th");
		suggestions.add(new NumberSuggestion(ordinal.toString(), NumberSuggestion.NumberType.ORDINAL));

		return suggestions;
	}



	public static class NumberSuggestion extends Suggestion {
		
		enum NumberType {
			NORMAL,
			ORDINAL,
			COMMAED
		}
		private final NumberType mType;
		
		public NumberSuggestion(String ordinal, NumberType type) {
			super(ordinal, 3);
			mType = type;
		}

		@Override
		protected int compareTo(Suggestion suggestion, String prefix) {
			NumberSuggestion numSuggestion = (NumberSuggestion) suggestion;
			
			if(mType == NumberType.NORMAL)
				return -1;
			else if(numSuggestion.mType == NumberType.NORMAL)
				return 1;

			if(mType == NumberType.COMMAED)
				return -1;
			else if(numSuggestion.mType == NumberType.COMMAED)
				return 1;
			
			return 0;
		}
	}



	@Override
	public boolean contains(String word) {
		if(mNumberPattern.matcher(word).matches())
			return true;

		return false;
	}



	@Override
	public boolean matches(String word) {
		return false;
	}
}
