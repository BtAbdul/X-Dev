package com.faslyling.xdev.util.text;

import static java.lang.System.arraycopy;

import androidx.annotation.NonNull;

public class TextContent {
	private static final char[] EMPTY_TEXT = new char[0];
	private char[] text = EMPTY_TEXT;
	private int length;
	private int gapStart;
	private int gapLength;
	
	public void set(@NonNull char[] text, int length) {
		assert text.length >= length;
		this.text = text;
		this.gapStart = length;
		this.length = length;
		this.gapLength = 0;
	}
	
	public void insert(int start, @NonNull String text) {
		int len = text.length();
		prepareGapForInsertion(start, len);
		text.getChars(0, len, this.text, start);
		gapStart += len;
		length += len;
		gapLength = this.text.length - length;
	}
	
	
	public void insert(int start, @NonNull CharSequence text) {
		int len = text.length();
		prepareGapForInsertion(start, len);
		for (int i = 0; i < len; i++) {
			this.text[start + i] = text.charAt(i);
		}
		gapStart += len;
		length += len;
		gapLength = this.text.length - length;
	}
	
	public void insert(int start, @NonNull Segment segment) {
		prepareGapForInsertion(start, segment.count);
		System.arraycopy(segment.array, segment.offset, text, start, segment.count);
		gapStart += segment.count;
		length += segment.count;
		gapLength = text.length - length;
	}
	
	public void delete(int start, int len) {
		moveGapStart(start);
		length -= len;
		gapLength = text.length - length;
	}
	
	public int getLength() {
		return length;
	}
	
	@NonNull
	public CharSequence getSequence(int start, int length) {
		if (start >= gapStart)
			return new TextCharSequence(text, start + gapLength, length);
		else if (start + length <= gapStart)
			return new TextCharSequence(text, start, length);
		else {
			return new TextCharSequence(text, start, gapStart - start,
					new TextCharSequence(text, gapStart + gapLength, start + length - gapStart));
		}
	}
	
	@NonNull
	public String getText() {
		return getText(0, length);
	}
	
	@NonNull
	public String getText(int start, int length) {
		if (start >= gapStart)
			return new String(text, start + gapLength, length);
		else if (start + length <= gapStart)
			return new String(text, start, length);
		else {
			return new String(text, start, gapStart - start)
					.concat(new String(text, gapStart + gapLength, start + length - gapStart));
		}
	}
	
	public void getText(int start, int length, @NonNull Segment segment) {
		if (start >= gapStart) {
			segment.array = text;
			segment.offset = start + gapLength;
			segment.count = length;
		} else if (start + length <= gapStart) {
			segment.array = text;
			segment.offset = start;
			segment.count = length;
		} else {
			segment.array = new char[length];
			arraycopy(text, start, segment.array, 0, gapStart - start);
			arraycopy(text, gapStart + gapLength, segment.array, gapStart - start,
					length + start - gapStart);
			segment.offset = 0;
			segment.count = length;
		}
	}
	
	private void ensureCapacity(int capacity) {
		if (capacity >= text.length) {
			int gapEndOld = gapStart + gapLength;
			
			char[] textN = new char[capacity * 2];
			System.arraycopy(text, 0, textN, 0, text.length);
			text = textN;
			gapLength = text.length - length;
			int gapEndNew = gapStart + gapLength;
			System.arraycopy(text, gapEndOld, text, gapEndNew, text.length - gapEndNew);
		}
	}
	
	private void prepareGapForInsertion(int start, int len) {
		moveGapStart(start);
		if (gapLength < len)
			ensureCapacity(length + len);
	}
	
	private void moveGapStart(int newStart) {
		int gapEnd = gapStart + gapLength;
		int newEnd = gapEnd + (newStart - gapStart);
		
		if (newStart != gapStart) {
			if (newStart > gapStart) {
				arraycopy(text, gapEnd, text, gapStart,
						newStart - gapStart);
			} else {
				arraycopy(text, newStart, text, newEnd,
						gapStart - newStart);
			}
		}
		
		gapStart = newStart;
		gapLength = text.length - length;
	}
}
