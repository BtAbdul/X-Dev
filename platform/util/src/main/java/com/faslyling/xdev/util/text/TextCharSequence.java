package com.faslyling.xdev.util.text;

import androidx.annotation.NonNull;

public class TextCharSequence implements CharSequence {
	private final char[] data;
	private final int offset;
	private final int len;
	private final TextCharSequence next;
	
	public TextCharSequence(char[] data, int offset, int len) {
		this(data, offset, len, null);
	}
	
	public TextCharSequence(char[] data, int offset, int len, TextCharSequence next) {
		this.data = data;
		this.offset = offset;
		this.len = len;
		this.next = next;
	}
	
	@Override
	public int length() {
		return len + ((next != null) ? next.length() : 0);
	}
	
	@Override
	public char charAt(int index) {
		if (index < len)
			return data[offset + index];
		else if (next != null)
			return next.charAt(index - len);
		else
			throw new ArrayIndexOutOfBoundsException(index);
	}
	
	@NonNull
	@Override
	public CharSequence subSequence(int start, int end) {
		return subSegment(start, end);
	}
	
	@NonNull
	public String toString() {
		StringBuilder sb = new StringBuilder();
		toString(sb);
		return sb.toString();
	}
	
	private void toString(@NonNull StringBuilder sb) {
		sb.append(data, offset, len);
		if (next != null)
			next.toString(sb);
	}
	
	@NonNull
	private TextCharSequence subSegment(int start, int end) {
		if (0 <= start && start <= end)
			if (end <= len)
				return new TextCharSequence(data, offset + start, end - start);
			else if (next != null)
				if (start < len)
					return new TextCharSequence(data, offset + start, len - start, next.subSegment(0, end - len));
				else
					return next.subSegment(start - len, end - len);
			else
				throw new ArrayIndexOutOfBoundsException();
		else
			throw new ArrayIndexOutOfBoundsException();
	}
}


