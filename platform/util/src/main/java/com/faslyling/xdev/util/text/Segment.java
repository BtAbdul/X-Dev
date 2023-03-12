package com.faslyling.xdev.util.text;


import androidx.annotation.NonNull;

import com.faslyling.xdev.util.Pools;

import java.text.CharacterIterator;
import java.util.Arrays;

public class Segment implements CharacterIterator, CharSequence {
	private static final Pools.SynchronizedPool<Segment> cachePool = new Pools.SynchronizedPool<>(10);
	public char[] array;
	public int offset;
	public int count;
	private boolean copy;
	
	private int pos;
	
	public Segment() {
		this(null, 0, 0);
	}
	
	public Segment(char[] array, int offset, int count) {
		this.array = array;
		this.offset = offset;
		this.count = count;
	}
	
	@NonNull
	public static Segment obtain() {
		Segment segment = cachePool.acquire();
		if (segment == null)
			segment = new CachedSegment();
		return segment;
	}
	
	public static void release(@NonNull Segment segment) {
		if (segment instanceof CachedSegment) {
			if (segment.copy)
				Arrays.fill(segment.array, '\u0000');
			segment.array = null;
			segment.copy = false;
			segment.count = 0;
			cachePool.release(segment);
		}
	}
	
	@Override
	public char first() {
		pos = offset;
		if (count != 0)
			return array[pos];
		
		return DONE;
	}
	
	
	@Override
	public char last() {
		pos = offset + count;
		if (count != 0) {
			pos -= 1;
			return array[pos];
		}
		return DONE;
	}
	
	@Override
	public char current() {
		if (count != 0 && pos < offset + count)
			return array[pos];
		
		return DONE;
	}
	
	@Override
	public char next() {
		pos += 1;
		int end = offset + count;
		if (pos >= end) {
			pos = end;
			return DONE;
		}
		return current();
	}
	
	@Override
	public char previous() {
		if (pos == offset)
			return DONE;
		
		pos -= 1;
		return current();
	}
	
	@Override
	public char setIndex(int position) {
		int end = offset + count;
		if ((position < offset) || (position > end))
			throw new IllegalArgumentException("bad position: " + position);
		
		pos = position;
		if ((pos != end) && (count != 0))
			return array[pos];
		
		return DONE;
	}
	
	@Override
	public int getBeginIndex() {
		return offset;
	}
	
	@Override
	public int getEndIndex() {
		return offset + count;
	}
	
	@Override
	public int getIndex() {
		return pos;
	}
	
	@NonNull
	@Override
	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}
	
	@NonNull
	public String toString() {
		if (array != null)
			return new String(array, offset, count);
		return "";
	}
	
	@Override
	public int length() {
		return count;
	}
	
	@Override
	public char charAt(int index) {
		if (index < 0 || index >= count)
			throw new StringIndexOutOfBoundsException(index);
		
		return array[offset + index];
	}
	
	@NonNull
	public CharSequence subSequence(int start, int end) {
		if (start < 0)
			throw new StringIndexOutOfBoundsException(start);
		
		if (end > count)
			throw new StringIndexOutOfBoundsException(end);
		
		if (start > end)
			throw new StringIndexOutOfBoundsException(end - start);
		
		Segment segment = new Segment();
		segment.array = this.array;
		segment.offset = this.offset + start;
		segment.count = end - start;
		return segment;
	}
	
	private static class CachedSegment extends Segment {
	}
	
}
