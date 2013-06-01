package com.comet.keyboard.dictionary.radixtrie;


/**
 * @author Barry Fruitman
 */
public class Node {
	char[] mValue;
	Node[] mChildren;
	Node mParent;
	int mCount;


	private static int mNodeCount = 0;
	Node(char value[], int count) {
		mValue = value;
		mCount = count;
		mChildren = new Node[0];
		
		mNodeCount++;
	}
	
	
	public int getCount() {
		return mCount;
	}
	
	
	public void setCount(int count) {
		mCount = count;
	}


	public void removeEntry() {
		mCount = 0;
	}

	
	public static int getNodeCount() {
		return mNodeCount;
	}


	public char[] getValue() {
		return mValue;
	}
	
	
	protected Node getChild(char[] c){
		int i = findChild(c);
		if(i >= 0)
			return mChildren[i];
		
		return null;
	}
	
	
	protected void replaceChild(Node oldChild, Node newChild) {
		newChild.mParent = this;
		int i = findChild(oldChild.mValue);
		mChildren[i] = newChild;
	}


	protected int findChild(char[] c){
		for(int i = 0; i < mChildren.length; i++) {
			Node child = mChildren[i];
			if(child.mValue == c)
				return i;
		}
		
		return -1;
	}


	public Node[] getChildren() {
		return mChildren;
	}
	
	
	protected void addChild(Node child) {
		child.mParent = this;
		mChildren = Node.copyOf(mChildren, mChildren.length+1);
		mChildren[mChildren.length-1] = child;
	}
	
	
	protected void setValue(char[] value) {
		mValue = value;
	}


	public boolean isEntry() {
		return mCount > 0;
	}


	protected boolean startsWith(char[] otherValue) {
		if(otherValue.length > mValue.length)
			return false;
		
		for(int i = 0; i < otherValue.length; i++)
			if(Character.toLowerCase(mValue[i]) != Character.toLowerCase(otherValue[i]))
				return false;
		
		return true;
	}



	protected boolean isPrefixOf(char[] otherValue) {
		if(otherValue.length < mValue.length)
			return false;

		for(int i = 0; i < mValue.length; i++)
			if(Character.toLowerCase(mValue[i]) != Character.toLowerCase(otherValue[i]))
				return false;
		
		return true;
	}



	public static char[] copyOf(char[] original, int newLength) {
		return copyOfRange(original, 0, newLength);
	}



	public static char[] copyOfRange(char[] original, int start, int end) {
		int newLength = end - start;
		char newArray[] = new char[newLength];

		for(int i = 0; i < newLength && i < original.length; i++)
			newArray[i] = original[start + i];
		
		return newArray;
	}



	protected static Node[] copyOf(Node[] original, int newLength) {
		return copyOfRange(original, 0, newLength);
	}



	protected static Node[] copyOfRange(Node[] original, int start, int end) {
		int newLength = end - start;
		Node newArray[] = new Node[newLength];

		for(int i = 0; i < newLength && i < original.length; i++)
			newArray[i] = original[start + i];
		
		return newArray;
	}


	// Rebuild the word by following the parent path
	public String getWord() {
		StringBuilder word = new StringBuilder();
		Node node = this;
		while(node.mParent != null) {
			word.insert(0, node.mValue);
			node = node.mParent;
		}
		
		return word.toString();
	}



	@Override
	public String toString() {
		if(mCount > 0)
			return "Node(" + String.valueOf(mValue) + ",(\"" + getWord() + "\"," + mCount + "))";
		else
			return "Node(" + String.valueOf(mValue) + ")";
	}
}