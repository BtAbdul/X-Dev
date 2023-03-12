package com.faslyling.xdev.util.text;

import androidx.annotation.NonNull;

import java.util.EventListener;

public interface ModelListener extends FoldListener, EventListener {
	
	void textSet(@NonNull TextModel model);
	
	void textPreInserted(@NonNull TextModel model, int startLine, int start, int length, int numLines);
	
	void textInserted(@NonNull TextModel model, int startLine, int start, int length, int numLines);
	
	void textPreDeleted(@NonNull TextModel model, int startLine, int start, int length, int numLines);
	
	void textDeleted(@NonNull TextModel model, int startLine, int start, int length, int numLines);
	
	void transactionComplete(@NonNull TextModel model);
	
}
