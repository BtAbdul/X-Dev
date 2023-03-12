package com.faslyling.xdev.util.text;

import androidx.annotation.Nullable;

public interface FoldHandlerProvider {
	@Nullable
	FoldHandler getFoldHandler(String name);
	
	String[] getFoldHandlerNames();
}
