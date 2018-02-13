package com.comet.keyboard.dictionary;

import android.content.Context;
import android.util.Log;

import com.comet.keyboard.KeyboardApp;
import com.comet.keyboard.dictionary.Suggestor.SuggestionsExpiredException;
import com.comet.keyboard.dictionary.radixtrie.Node;
import com.comet.keyboard.dictionary.radixtrie.RadixTrie;

/*package*/ abstract class TrieDictionary implements LearningDictionary {

	private static final int MAX_DELETABLE_COUNT = 100;
	protected static final int COUNT_INCREMENT = 10;
	protected final int MIN_COUNT = 2; // Count threshold for suggestions
	protected final KeyCollator mCollator;
	protected final Context mContext;
	private final RadixTrie mTrie = new RadixTrie();
	private boolean mCancelled = false;
	private int mCountSum;


	public TrieDictionary(final Context context, final KeyCollator collator) {
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

	
	protected final boolean isCancelled() {
		if(mCancelled) {
			throw new DictionaryCancelledException();
		}

		return mCancelled;
	}


	protected abstract void loadDictionary();


	@Override
	public boolean contains(final String word) {
		final Node node = mTrie.findNode(word, mExactCharComparator);

		return node != null
				&& node.isEntry()
				&& node.getWord().length() == word.length();
	}


	public int getCount(final String word) {
		Node node = mTrie.findNode(word, mExactCharComparator);
		if(node == null || !node.isEntry() || node.getWord().length() != word.length()) {
			return -1;
		}

		return node.getCount();
	}

	
	@Override
	public Suggestions getSuggestions(final Suggestions suggestions) {
		return findSuggestionsInTrie(suggestions, mTrie.getRoot(), 1);
	}


	// TODO: THIS METHOD IS HACKY
	protected Suggestions getSuggestionsWithPrefix(Suggestions suggestions, final String prefix) {
		final Node node = mTrie.findNode(prefix, mCollator);
		if(node != null) {
			suggestions = findSuggestionsInTrie(suggestions, node, 1);
		}
		
		return suggestions;
	}


	private Suggestions findSuggestionsInTrie(final Suggestions suggestions, final Node node, final int iNodeValue) {
		findSuggestionsInTrie(
				new StringBuilder(suggestions.getComposing()),
				suggestions,
				0,
				node,
				iNodeValue,
				0,
				EditDistance.getMaxEditDistance(suggestions.getComposing()));

		return suggestions;
	}


	private void findSuggestionsInTrie(
			final StringBuilder prefix,
			final Suggestions suggestions,
			final int iPrefix,
			final Node node,
			final int iNodeValue,
			final double editDistance,
			final double maxEditDistance) {

		if(editDistance > maxEditDistance) {
			return;
		}

		if(suggestions.isExpired()) {
			throw new SuggestionsExpiredException();
		}

		final char[] value = node.getValue();

		if(iPrefix >= prefix.length()) {
			// End of prefix. Look for suggestions below this node and add them.
			final double trailingEditDistance = node.getWord().length() - prefix.length();
			prefix.append(Node.copyOfRange(value, iNodeValue, value.length));
			addSuggestions(node, prefix, suggestions, editDistance + trailingEditDistance);
			prefix.setLength(prefix.length() - (value.length - iNodeValue));

			return;
		}

		if(iNodeValue >= value.length) {
			// End of this node's key. Traverse children.
			for(Node child : node.getChildren()) {
				findSuggestionsInTrie(
						prefix,
						suggestions,
						iPrefix,
						child,
						0,
						editDistance,
						maxEditDistance);
			}

			return;
		}

		// Skip non-letter characters
		if(!Character.isLetter(value[iNodeValue])) {
			prefix.insert(iPrefix, value[iNodeValue]);
			findSuggestionsInTrie(
					prefix,
					suggestions,
					iPrefix + 1,
					node,
					iNodeValue + 1,
					editDistance,
					maxEditDistance);
			prefix.deleteCharAt(iPrefix);

			return;
		}

		final char keyStroke = prefix.charAt(iPrefix);

		// Compare the keystroke to the next character in the trie traversal
		int iKeyDistance = mCollator.compareCharToKey(value[iNodeValue], keyStroke);
		if(iKeyDistance >= 0 && iKeyDistance + editDistance <= maxEditDistance) {
			// Matched key. Follow this node, then return.
			prefix.setCharAt(iPrefix, value[iNodeValue]);
			findSuggestionsInTrie(
					prefix,
					suggestions,
					iPrefix + 1,
					node,
					iNodeValue + 1,
					iKeyDistance == 0 ? editDistance : editDistance + EditDistance.SUBSTITUTE,
					maxEditDistance);
			prefix.setCharAt(iPrefix, keyStroke);
		}

		// Assume this prefix is missing a keystroke. Insert missing char and follow node.
		prefix.insert(iPrefix, value[iNodeValue]);
		findSuggestionsInTrie(
				prefix,
				suggestions,
				iPrefix + 1,
				node,
				iNodeValue + 1,
				editDistance + EditDistance.DELETE,
				maxEditDistance);
		prefix.deleteCharAt(iPrefix);

		// Is this key adjacent the next keystroke?
		if(iPrefix < prefix.length() - 1) {
			iKeyDistance = mCollator.compareCharToKey(prefix.charAt(iPrefix +1), keyStroke);
			if(iKeyDistance >= 0) {
				// Assume it is a double-tap. Delete it and follow node.
				final char deleted = prefix.charAt(iPrefix);
				prefix.deleteCharAt(iPrefix);

				findSuggestionsInTrie(
						prefix,
						suggestions,
						iPrefix,
						node,
						iNodeValue	,
						editDistance + EditDistance.INSERT,
						maxEditDistance);

				prefix.insert(iPrefix, deleted);
			}
		}
	}


	private void addSuggestions(
			final Node node,
			final StringBuilder prefix,
			final Suggestions suggestions,
			final double editDistance) {

		// Add this node if it's a leaf
		if(node.isEntry()) {
			addSuggestion(suggestions, prefix.toString(), node.getCount(), editDistance);
		}


		// Recursively traverse all children
		for(Node child : node.getChildren()) {
			prefix.append(child.getValue());
			addSuggestions(child, prefix, suggestions, editDistance + child.getValue().length);
			prefix.setLength(prefix.length() - child.getValue().length);
		}
	}


	protected abstract void addSuggestion(
			Suggestions suggestions,
			String word,
			int count,
			double editDistance);


	/**
	 * Adds a word to the dictionary with desired count, or increments its count by 1
	 * @param word		The word to learn
	 * @param countIncrement		The default count for new words
	 */
	protected int learn(String word, int countIncrement) {
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


	public final RadixTrie.CharComparator mExactCharComparator = new RadixTrie.CharComparator() {
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
	 * Returns true if the word can appear in suggestions.
	 * @param word	The word to check.
	 * @return
	 */


	private boolean isRemembered(String word) {
		if(getCount(word) >= MIN_COUNT) {
			return true;
		}

		return false;
	}


	final protected void setCountSum(int countSum) {
		mCountSum = countSum;
	}


	final protected int getCountSum() {
		return mCountSum;
	}


	private static class DictionaryCancelledException extends RuntimeException {
		private static final long serialVersionUID = -141336718087069436L;
	}
}
