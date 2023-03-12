package com.faslyling.xdev.util.text;

import androidx.annotation.NonNull;

import com.faslyling.xdev.util.IntArrayList;

public class DummyFoldHandler implements FoldHandler{
	@NonNull
	@Override
	public String getName() {
		return "NONE";
	}
	
	@Override
	public int getFoldLevel(@NonNull TextModel model, int line, @NonNull Segment segment) {
		return 0;
	}
	
	@Override
	public IntArrayList getPrecedingFoldLevels(@NonNull TextModel model, int line, @NonNull Segment segment, int lineLevel) {
		return null;
	}
}
