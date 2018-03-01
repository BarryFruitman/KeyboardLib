package com.comet.keyboard.dictionary;

import android.content.Context;
import android.util.Log;

import com.comet.keyboard.KeyboardApp;
import com.comet.keyboard.dictionary.Suggestor.SuggestionsExpiredException;
import com.comet.keyboard.dictionary.radixtrie.Node;
import com.comet.keyboard.dictionary.radixtrie.RadixTrie;

import java.util.Comparator;

abstract class TrieDictionary<S extends Suggestion, R extends SuggestionsRequest>
		 implements LearningDictionary<S, R> {

	private static final int MAX_DELETABLE_COUNT = 100;
	static final int COUNT_INCREMENT = 10;
	private final int MIN_COUNT = 2; // Count threshold for suggestions
	final KeyCollator mCollator;
	protected final Context mContext;
	private final RadixTrie mTrie = new RadixTrie();
	private boolean mCancelled = false;
	private int mCountSum;


	TrieDictionary(final Context context, final KeyCollator collator) {
		mContext = context;
		mCollator = collator;

		// Load lexicon in a background thread
		final Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					loadDictionary();
				} catch (DictionaryCancelledException dce) {
					Log.i(KeyboardApp.LOG_TAG, "Cancelled loading " + mCollator.getLanguage() + " dictionary");
				}
			}
		});
		thread.start();
	}


	void insert(final String word, final int count) {
		mTrie.insert(word, count);
	}


	protected final void cancel() {
		mCancelled = true;
	}

	
	final boolean isCancelled() {
		if(mCancelled) {
			throw new DictionaryCancelledException();
		}

		return false;
	}


	protected abstract void loadDictionary();


	@Override
	public boolean contains(final String word) {
		final Node node = mTrie.findNode(word, mExactCharComparator);

		return node != null
				&& node.isEntry()
				&& node.getWord().length() == word.length();
	}


	int getCount(final String word) {
		Node node = mTrie.findNode(word, mExactCharComparator);
		if(node == null || !node.isEntry() || node.getWord().length() != word.length()) {
			return -1;
		}

		return node.getCount();
	}


	@Override
	public Suggestions<S> getSuggestions(final R request) {
		final Suggestions<S> suggestions
				= new SortedSuggestions<>(request, getComparator());
		findSuggestionsInTrie(
				"",
				new StringBuilder(suggestions.getComposing()),
				suggestions,
				0,
				mTrie.getRoot(),
				1,
				getCountSum(),
				0,
				EditDistance.getMaxEditDistance(suggestions.getComposing()));

		return suggestions;
	}


	Suggestions<S> getMatches(final String word) {
		final Suggestions<S> suggestions =
				new SortedSuggestions<>(new SuggestionsRequest(word), getComparator());

		findSuggestionsInTrie(
				"",
				new StringBuilder(suggestions.getComposing()),
				suggestions,
				0,
				mTrie.getRoot(),
				1,
				getCountSum(),
				0,
				0);

		return suggestions;
	}


	Suggestions<S> getSuggestionsAfterPrefix(
			final Suggestions<S> suggestions,
			String prefix,
			final int countSum) {
		final Node node = mTrie.findNode(prefix, mExactCharComparator);
		if(node != null) {
//			final int iNodeValue = node.getWord().length() - prefix.length();
			final String composing = suggestions.getComposing();
			findSuggestionsInTrie(
					prefix,
					new StringBuilder(composing),
					suggestions,
					0,
					node,
					1,
					countSum,
					0,
					composing.length() == 0 ? 8 : EditDistance.getMaxEditDistance(composing));
		}

		return suggestions;
	}


	private void findSuggestionsInTrie(
			final String prefix,
			final StringBuilder composing,
			final Suggestions<S> suggestions,
			final int iComposing,
			final Node node,
			final int iNodeValue,
			final int countSum,
			final double editDistance,
			final double maxEditDistance) {

		if(editDistance > maxEditDistance) {
			return;
		}

		if(suggestions.isExpired()) {
			throw new SuggestionsExpiredException();
		}

		final char[] value = node.getValue();

		if(iComposing >= composing.length()) {
			// End of composing. Look for suggestions below this node and add them.
			final double trailingEditDistance = node.getWord().length() - prefix.length() - composing.length();
			composing.append(Node.copyOfRange(value, iNodeValue, value.length));
			addSuggestions(
					node,
					composing,
					suggestions,
					countSum,
					editDistance + trailingEditDistance,
					maxEditDistance);
			composing.setLength(composing.length() - (value.length - iNodeValue));

			return;
		}

		if(iNodeValue >= value.length) {
			// End of this node's key. Traverse children.
			for(Node child : node.getChildren()) {
				findSuggestionsInTrie(
						prefix,
						composing,
						suggestions,
						iComposing,
						child,
						0,
						countSum,
						editDistance,
						maxEditDistance);
			}

			return;
		}


		// Skip non-letter characters
		if(!Character.isLetter(value[iNodeValue])
				&& !Character.isSpaceChar(value[iNodeValue])) {
			composing.insert(iComposing, value[iNodeValue]);
			findSuggestionsInTrie(
					prefix,
					composing,
					suggestions,
					iComposing + 1,
					node,
					iNodeValue + 1,
					countSum,
					editDistance,
					maxEditDistance);
			composing.deleteCharAt(iComposing);

			return;
		}

		final char keyStroke = composing.charAt(iComposing);

		// Compare the keystroke to the next character in the trie traversal
		int iKeyDistance = mCollator.compareCharToKey(value[iNodeValue], keyStroke);
		if(iKeyDistance >= 0 && iKeyDistance + editDistance <= maxEditDistance) {
			// Matched key. Follow this node, then return.
			composing.setCharAt(iComposing, value[iNodeValue]);
			findSuggestionsInTrie(
					prefix,
					composing,
					suggestions,
					iComposing + 1,
					node,
					iNodeValue + 1,
					countSum,
					iKeyDistance == 0 ? editDistance : editDistance + EditDistance.SUBSTITUTE,
					maxEditDistance);
			composing.setCharAt(iComposing, keyStroke);
		}

		// Assume this composing is missing a keystroke. Insert missing char and follow node.
		composing.insert(iComposing, value[iNodeValue]);
		findSuggestionsInTrie(
				prefix,
				composing,
				suggestions,
				iComposing + 1,
				node,
				iNodeValue + 1,
				countSum,
				editDistance + EditDistance.DELETE,
				maxEditDistance);
		composing.deleteCharAt(iComposing);

		// Is this key adjacent the next keystroke?
		if(iComposing < composing.length() - 1) {
			iKeyDistance = mCollator.compareCharToKey(composing.charAt(iComposing +1), keyStroke);
			if(iKeyDistance >= 0) {
				// Assume it is a double-tap. Delete it and follow node.
				final char deleted = composing.charAt(iComposing);
				composing.deleteCharAt(iComposing);

				findSuggestionsInTrie(
						prefix,
						composing,
						suggestions,
						iComposing,
						node,
						iNodeValue,
						countSum,
						editDistance + EditDistance.INSERT,
						maxEditDistance);

				composing.insert(iComposing, deleted);
			}
		}
	}


	private void addSuggestions(
			final Node node,
			final StringBuilder prefix,
			final Suggestions<S> suggestions,
			final int countSum,
			final double editDistance,
			final double maxEditDistance) {

		if(editDistance > maxEditDistance) {
			return;
		}

		// Add this node if it's an entry
		if(node.isEntry()) {
			addSuggestion(suggestions, prefix.toString(), node.getCount(), countSum, editDistance);
		}


		// Recursively traverse all children
		for(Node child : node.getChildren()) {
			prefix.append(child.getValue());
			addSuggestions(
					child,
					prefix,
					suggestions,
					countSum,
					editDistance + child.getValue().length,
					maxEditDistance);
			prefix.setLength(prefix.length() - child.getValue().length);
		}
	}


	protected abstract void addSuggestion(
			Suggestions<S> suggestions,
			String word,
			int count,
			int countSum,
			double editDistance);


	abstract protected Comparator<S> getComparator();


	/**
	 * Adds a word to the dictionary with desired count, or increments its count by 1
	 * @param word		The word to learn
	 * @param countIncrement		The default count for new words
	 */
	int learn(String word, int countIncrement) {
		final int count;
		final Node node = mTrie.findNode(word, mExactCharComparator);
		if(node != null
				&& node.isEntry() && node.getWord().equals(word)) {
			// Update trie entry
			count = node.getCount() + countIncrement;
			node.setCount(count);
		} else {
			// Insert into trie
			count = countIncrement;
			insert(word, count);
		}

		mCountSum += countIncrement;

		// Write to db
		addToDB(word, count);

		return count;
	}


	private final RadixTrie.CharComparator mExactCharComparator = new RadixTrie.CharComparator() {
		@Override
		public int compareChars(char c1, char c2) {
			return c1 - c2;
		}
	};


	/**
	 * Adds a word to the dictionary with count = 1, or increments its count by 1
	 * @param word		The word to learn
	 */
	public boolean learn(String word) {
		learn(word, COUNT_INCREMENT);

		return true;
	}


	@Override
	public boolean forget(String word) {
		Node node = mTrie.findNode(word, mExactCharComparator);

		if(node == null || node.getCount() == 0 || node.getCount() > MAX_DELETABLE_COUNT) {
			return false;
		}

		node.removeEntry();
		deleteFromDB(word);

		return !contains(word);
	}


	@Override
	public boolean remember(String word) {
		if(isRemembered(word)) {
			return false;
		}

		learn(word, MIN_COUNT);

		return true;
	}


	abstract void addToDB(String word, int count);


	abstract void deleteFromDB(String word);


	/**
	 * @param word	The word to check.
	 * @return		Returns true if the word can appear in suggestions.
	 */


	private boolean isRemembered(String word) {
		return getCount(word) >= MIN_COUNT;
	}


	final void setCountSum(int countSum) {
		mCountSum = countSum;
	}


	final int getCountSum() {
		return mCountSum;
	}


	private static class DictionaryCancelledException extends RuntimeException {
		private static final long serialVersionUID = -141336718087069436L;
	}
}
