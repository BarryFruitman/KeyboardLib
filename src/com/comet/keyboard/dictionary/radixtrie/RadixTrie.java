package com.comet.keyboard.dictionary.radixtrie;

import com.comet.keyboard.dictionary.radixtrie.Node;


public class RadixTrie {
	protected Node mRoot;
	protected int mCountSum = 0;

	public RadixTrie() {
		mRoot = new Node(new char[] {' '}, 0);
	}


	public final Node getRoot() {
		return mRoot;
	}



	public void insert(String word, int count) {
		insert(word, mRoot, word.toCharArray(), count);
		mCountSum += count;
	}
	protected void insert(String word, Node node, char[] s, int count) {
		// Scan children for matching prefixes
		Node[] children = node.getChildren();
		for(Node child : children) {
			char[] value = child.getValue();
			int nPrefix = matchingPrefixLength(value, s, mInsertComparator);

			if(nPrefix > 0) {
				// New value shares a prefix with this child

				if(nPrefix == value.length && nPrefix == s.length) {
					// Node already exists. Make it a tail
					child.setCount(count);
					return;
				}

				if(nPrefix == value.length && s.length > nPrefix) {
					// Insert suffix of new value.
					insert(word, child, Node.copyOfRange(s, nPrefix, s.length), count);
					return;
				}

				if(value.length > nPrefix) {
					// Split this child into two descendants.
					Node newChild = new Node(Node.copyOfRange(s, 0, nPrefix), nPrefix == s.length ? count : 0);
					node.replaceChild(child, newChild);

					child.setValue(Node.copyOfRange(value, nPrefix, value.length));
					newChild.addChild(child);

					if(s.length > nPrefix)
						newChild.addChild(new Node(Node.copyOfRange(s, nPrefix, s.length), count));

					return;
				}
			}
		}

		// No children have a matching prefix. Insert new node.
		node.addChild(new Node(s, count));
	}

	
	
	public final CharComparator mInsertComparator = new CharComparator() {
		@Override
		public int compareChars(char c1, char c2) {
			return c1 - c2;
		}
	};
	
	
	
	public Node findNode(CharSequence prefix, CharComparator comparator) {
		return findNode(prefix, 0, mRoot, comparator);
	}



	private Node findNode(CharSequence prefix, int iString, Node node, CharComparator comparator) {
		if(iString >= prefix.length())
			return null;

		for(Node child : node.getChildren()) {
			char[] value = child.getValue();
			int iMatches = matches(prefix, iString, value, comparator);
			if(iMatches > 0) {
				if(iString + iMatches >= prefix.length()) {
					return child;
				} else if(iMatches < value.length)
					continue;	// Try the next sibling.

				if(child.getChildren().length == 0)
					// No grandchildren
					continue;

				// Recursively search the next child node
				Node result = findNode(prefix, iString + value.length, child, comparator);
				if(result != null)
					return result;
			}
		}

		return null;
	}


	public interface CharComparator {
		public int compareChars(char c1, char c2);
	}




	/**
	 * Performs a case-insensitive match between a node value and a substring for find()
	 * @param string	The string to match
	 * @param iString	The starting index in string
	 * @param value		The node value to compare against
	 * @return			The number of matching characters
	 */
	private int matches(CharSequence string, int iString, char[] value, CharComparator comparator) {
		int iMatches = 0;
		for(int iValue = 0; iValue < value.length; iValue++) {
			if(iString + iValue >= string.length())
				return iMatches;

			if(comparator.compareChars(string.charAt(iString + iValue), value[iValue]) != 0) {
				if(!Character.isLetter(value[iValue])) {
					iString--;
					continue;
				} else	
					return iMatches;
			}
			
			iMatches++;
		}
		
		return iMatches;
	}


	
	private int matchingPrefixLength(final char[] l, final char[] r, CharComparator comparator) {
		int n = Math.min(l.length, r.length);

		for(int i = 0; i < n; i++) {
			if(comparator.compareChars(l[i], r[i]) != 0)
				return i;
		}

		return n;
	}



	public int getCountSum() {
		return mCountSum;
	}



	public void setCountSum(int countSum) {
		mCountSum = countSum;
	}



	public void clear() {
		mRoot = new Node(new char[] {' '}, 0);
		mCountSum = 0;
	}
}