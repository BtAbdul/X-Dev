package com.faslyling.xdev.util.text;

import androidx.annotation.NonNull;

import com.faslyling.xdev.util.IntArrayList;

public interface FoldHandler {
	
	@NonNull
	String getName();
	
	int getFoldLevel(@NonNull TextModel model,int line,@NonNull Segment segment);
	
	IntArrayList getPrecedingFoldLevels(@NonNull TextModel model,int line,@NonNull Segment segment,int lineLevel);
}
