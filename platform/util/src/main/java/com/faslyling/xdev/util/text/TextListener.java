package com.faslyling.xdev.util.text;

import androidx.annotation.NonNull;

import java.util.EventListener;

public interface TextListener extends EventListener {
	
	
	void textPreInserted(@NonNull TextModel model, int startLine, int start, int length, int numLines);
	
	void textInserted(@NonNull TextModel model, int startLine, int start, int length, int numLines);
	
	void textPreDeleted(@NonNull TextModel model, int startLine, int start, int length, int numLines);
	
	void textDeleted(@NonNull TextModel model, int startLine, int start, int length, int numLines);
	
}
