package com.faslyling.xdev.util.text;

import androidx.annotation.NonNull;

import java.util.EventListener;

public interface UndoListener extends EventListener {
	void beginUndo(@NonNull TextModel model);
	
	void endUndo(@NonNull TextModel model);
	
	void beginRedo(@NonNull TextModel model);
	
	void endRedo(@NonNull TextModel model);
}
