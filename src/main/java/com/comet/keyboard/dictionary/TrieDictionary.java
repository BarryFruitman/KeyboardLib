package com.comet.keyboard.dictionary;

import android.content.Context;
import android.util.Log;

import com.comet.keyboard.KeyboardApp;
import com.comet.keyboard.Suggestor.SuggestionsExpiredException;
import com.comet.keyboard.Suggestor.Suggestions;
import com.comet.keyboard.dictionary.radixtrie.Node;
import com.comet.keyboard.dictionary.radixtrie.RadixTrie;

public abstract class TrieDictionary implements LearningDictionary {

	protected final KeyCollator mCollator;
	protected final Context mContext;
	private final RadixTrie mTrie = new RadixTrie();
	private boolean mCancelled = false;


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
	public boolean matches(final String word) {
		return mTrie.findNode(word, mCollator) != null;
	}


	@Override
	public boolean contains(final String word) {
		final Node node = mTrie.findNode(word, mCollator);
		
		return node != null 
				&& node.isEntry()
				&& node.getWord().length() == word.length();
	}


	public int getCount(final String word) {
		Node node = findEntry(word);
		if(node == null) {
			return -1;
		}
		
		return node.getCount();
	}

	
	private Node findEntry(final String word) {
		return mTrie.findNode(word, mCollator);
	}


	@Override
	public Suggestions getSuggestions(final Suggestions suggestions) {
		return findSuggestionsInTrie(suggestions, mTrie.getRoot(), 1);
	}


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
			final int editDistance,
			final int maxEditDistance) {

		if(editDistance > maxEditDistance) {
			return;
		}

		if(suggestions.isExpired()) {
			throw new SuggestionsExpiredException();
		}

		final char[] value = node.getValue();

		if(iPrefix >= prefix.length()) {
			// End of prefix. Look for suggestions below this node and add them.
			prefix.append(Node.copyOfRange(value, iNodeValue, value.length));
			addSuggestions(node, prefix, suggestions, editDistance);
			prefix.setLength(prefix.length() - (value.length - iNodeValue));
			return;
		}

		if(iNodeValue >= value.length) {
			// End of this node's key. Traverse children.
			for(Node child : node.getChildren()) {
				findSuggestionsInTrie(prefix, suggestions, iPrefix, child, 0, editDistance, maxEditDistance);
			}
			return;
		}

		// Skip non-letter characters
		if(!Character.isLetter(value[iNodeValue])) {
			prefix.insert(iPrefix, value[iNodeValue]);
			findSuggestionsInTrie(prefix, suggestions, iPrefix + 1, node, iNodeValue + 1, editDistance, maxEditDistance);
			prefix.deleteCharAt(iPrefix);
			return;
		}

		final char keyStroke = prefix.charAt(iPrefix);

		// Compare the keystroke to the next character in the trie traversal
		final int iKeyDistance = mCollator.compareCharToKey(value[iNodeValue], keyStroke);
		if(iKeyDistance >= 0 && iKeyDistance + editDistance <= maxEditDistance) {
			// Matched key. Follow this node, then return.
			prefix.setCharAt(iPrefix, value[iNodeValue]);
			findSuggestionsInTrie(prefix, suggestions, iPrefix + 1, node, iNodeValue + 1, iKeyDistance == 0 ? editDistance : editDistance + EditDistance.getSubstitute(), maxEditDistance);
			prefix.setCharAt(iPrefix, keyStroke);
			if(iKeyDistance == 0) {
				return;
			}
		}

		// Assume this prefix is missing a keystroke. Insert missing char and follow node.
		prefix.insert(iPrefix, value[iNodeValue]);
		findSuggestionsInTrie(prefix, suggestions, iPrefix + 1, node, iNodeValue + 1, editDistance + EditDistance.getDelete(), maxEditDistance);
		prefix.deleteCharAt(iPrefix);

		// Is this the same key as the last one?
		if(iPrefix > 1 && prefix.charAt(iPrefix-1) == keyStroke) {
			// Assume it is a double-tap. Delete it and follow node.
			final char deleted = prefix.charAt(iPrefix);
			prefix.deleteCharAt(iPrefix);
			findSuggestionsInTrie(prefix, suggestions, iPrefix, node, iNodeValue, editDistance + EditDistance.getInsert(), maxEditDistance);
			prefix.insert(iPrefix, deleted);
		}
	}


	public void addSuggestions(final Node node, final StringBuilder prefix, final Suggestions suggestions, final int editDistance) {
		// Add this node if it's a leaf
		if(node.isEntry()) {
			addSuggestion(suggestions, prefix.toString(), node.getCount(), editDistance);
		}

		// Recursively traverse all children
		for(Node child : node.getChildren()) {
			prefix.append(child.getValue());
			addSuggestions(child, prefix, suggestions, editDistance);
			prefix.setLength(prefix.length()-child.getValue().length);
		}
	}


	protected abstract void addSuggestion(Suggestions suggestions, String word, int count, int editDistance);


	/**
	 * Adds a word to the dictionary with desired count, or increments its count by 1
	 * @param word		The word to learn
	 * @param countIncrement		The default count for new words
	 */
	protected int learn(String word, int countIncrement) {
		final String lower = word.toLowerCase();
		final int count;
		if(contains(lower) && !contains(word)) {
			// This word exists in the main dictionary, but only in lower case.
			word = lower;
		}

		final Node node = findEntry(word);
		if(node != null) {
			// Update trie entry
			count = node.getCount() + countIncrement;
			node.setCount(count);
		} else {
			// Insert into trie
			count = countIncrement;
			insert(word, count);
		}

		return count;
	}


	@Override
	public boolean forget(String word) {
		Node node = findEntry(word);
		
		if(node == null || node.getCount() == 0)
			return false;
		
		node.removeEntry();
		
		return true;
	}

	
	private static class DictionaryCancelledException extends RuntimeException {
		private static final long serialVersionUID = -141336718087069436L;
	}
}
