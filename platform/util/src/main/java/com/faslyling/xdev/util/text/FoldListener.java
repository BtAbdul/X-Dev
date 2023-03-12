package com.faslyling.xdev.util.text;

import androidx.annotation.NonNull;

import java.util.EventListener;

public interface FoldListener extends EventListener {
	void foldLevelChanged(@NonNull TextModel model, int startLine, int endLine);
	
	void foldHandlerChanged(@NonNull TextModel model);
	
}
