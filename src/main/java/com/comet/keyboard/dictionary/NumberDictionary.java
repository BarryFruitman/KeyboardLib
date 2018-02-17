package com.comet.keyboard.dictionary;

import java.util.Comparator;
import java.util.regex.Pattern;

import com.comet.keyboard.KeyboardService;
import com.comet.keyboard.R;

public class NumberDictionary implements Dictionary {

	private final Pattern mNumberPattern;
	private final Pattern mTeenPattern;
	private final String NUMBER_REGEX = "[\\d]+";
	private final String TEEN_REGEX = "[\\d]*1[\\d]";

	NumberDictionary() {
		mNumberPattern = Pattern.compile(NUMBER_REGEX);
		mTeenPattern = Pattern.compile(TEEN_REGEX);
	}


	@Override
	public Suggestions getSuggestions(SuggestionsRequest request) {
		final Suggestions suggestions = new NumberSuggestions(request);
		String composing = suggestions.getComposing();
		if(!mNumberPattern.matcher(composing).matches())
			return suggestions;

		suggestions.add(new NumberSuggestion(composing.toString(), NumberSuggestion.NumberType.NORMAL));

		final int value = Integer.parseInt(composing);
		if(value >= 0 && value <=20) {
			// Add word
			String[] words = KeyboardService.getIME().getResources().getStringArray(R.array.number_words);
			suggestions.add(new NumberSuggestion(words[value], NumberSuggestion.NumberType.WORD));
		}

		// Add commaed number
		StringBuilder commaed = new StringBuilder();
		if(composing.length() > 3) {
			commaed.append(composing);
			for(int iComma = composing.length() - 3; iComma > 0; iComma-= 3)
				commaed.insert(iComma, ',');
			suggestions.add(new NumberSuggestion(commaed.toString(), NumberSuggestion.NumberType.COMMAED));
		}
		
		// Add ordinal
		StringBuilder ordinal = new StringBuilder();
		if(commaed.length() > 0)
			ordinal.append(commaed);
		else
			ordinal.append(composing);

		if(mTeenPattern.matcher(composing).matches())
			ordinal.append("th");
		else if(composing.endsWith("1"))
			ordinal.append("st");
		else if(composing.endsWith("2"))
			ordinal.append("nd");
		else if(composing.endsWith("3"))
			ordinal.append("rd");
		else 
			ordinal.append("th");
		suggestions.add(new NumberSuggestion(ordinal.toString(), NumberSuggestion.NumberType.ORDINAL));

		return suggestions;
	}


	private static final Comparator<NumberSuggestion> mComparator = new Comparator<NumberSuggestion>() {
		@Override
		public int compare(NumberSuggestion o1, NumberSuggestion o2) {
			return o1.mType.compareTo(o2.mType);
		}
	};


	private static class NumberSuggestions extends SortedSuggestions {
		NumberSuggestions(SuggestionsRequest request) {
			super(request, mComparator);
		}
	}


	private static class NumberSuggestion extends Suggestion {
		enum NumberType {
			NORMAL,
			WORD,
			COMMAED,
			ORDINAL
		}
		private final NumberType mType;
		
		public NumberSuggestion(String cardinal, NumberType type) {
			super(cardinal);
			mType = type;
		}
	}


	@Override
	public boolean contains(String word) {
		if(mNumberPattern.matcher(word).matches())
			return true;

		return false;
	}
}
