package com.faslyling.xdev.util.text;

import androidx.annotation.NonNull;

import com.faslyling.xdev.util.IntArrayList;
import com.faslyling.xdev.util.SafeListenerList;

import java.util.concurrent.locks.ReentrantReadWriteLock;

public class TextModel {
	private final SafeListenerList<ModelListener> modelListeners = new SafeListenerList<>();
	private final SafeListenerList<UndoListener> undoListeners = new SafeListenerList<>();
	private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
	private boolean editable;
	private boolean readOnly;
	private boolean readOnlyOverride;
	private final TextContent content;
	private boolean transaction;
	
	private UndoManager undoManager;
	private boolean undoInProgress;
	private boolean dirty;
	
	private LineManager lineManager;
	private IntArrayList intArrayList;
	private FoldHandler foldHandler;
	
	public TextModel() {
		content = new TextContent();
		intArrayList=new IntArrayList();
		undoManager = new UndoManager(this);
		lineManager = new LineManager();
	}
	
	
	public void setReadOnly(boolean readOnly) {
		readOnlyOverride = readOnly;
	}
	
	public boolean isReadOnly() {
		return readOnly || readOnlyOverride;
	}
	
	public void setEditable(boolean editable) {
		this.editable = editable;
	}
	
	public boolean isEditable() {
		return editable;
	}
	
	public void writeLock() {
		lock.writeLock().lock();
	}
	
	public void writeUnlock() {
		lock.writeLock().unlock();
	}
	
	public void readLock() {
		lock.readLock().lock();
	}
	
	public void readUnlock() {
		lock.readLock().unlock();
	}
	
	public void addModelListener(@NonNull ModelListener listener) {
		modelListeners.add(listener);
	}
	
	public void removeModelListener(@NonNull ModelListener listener) {
		modelListeners.remove(listener);
	}
	
	
	public void insert(int offset, @NonNull String text) {
		insert(offset, (CharSequence) text);
	}
	
	public void insert(int offset, @NonNull Segment text) {
		insert(offset, (CharSequence) text);
	}
	
	public void insert(int offset, @NonNull CharSequence text) {
		int len = text.length();
		if (len == 0)
			return;
		try {
			writeLock();
			
			if (offset < 0 || offset > content.getLength())
				throw new ArrayIndexOutOfBoundsException(offset);
			
			content.insert(offset, text);
			
			intArrayList.clear();
			
			for (int i = 0; i < len; i++) {
				if (text.charAt(i) == '\n')
					intArrayList.add(i + 1);
			}
			
			if (!undoInProgress)
				undoManager.contentInserted(offset, text.toString(), !dirty);
			
			internalInserted(offset, len, intArrayList);
		} finally {
			writeUnlock();
		}
	}
	
	private void internalInserted(int offset, int length, @NonNull IntArrayList endOffsets) {
		try {
			transaction = true;
			
			int startLine = lineManager.getLineOfOffset(offset);
			int numLines = endOffsets.size();
			
			fireTextPreInserted(startLine, offset, length, numLines);
			
			lineManager.contentInserted(startLine, offset, length, numLines, endOffsets);
			
			setDirty(true);
			
			fireTextInserted(startLine, offset, length, numLines);
			if (!undoInProgress && !isCompoundEdit())
				fireTransactionComplete();
		} finally {
			transaction = false;
		}
	}
	
	public void delete(int offset, int length) {
		if (length == 0)
			return;
		
		try {
			transaction = true;
			writeLock();
			
			if (offset < 0 || length < 0 || offset + length > content.getLength())
				throw new ArrayIndexOutOfBoundsException(offset + ":" + length);
			
			int startLine = lineManager.getLineOfOffset(offset);
			int endLine = lineManager.getLineOfOffset(offset + length);
			
			int numLines = endLine - startLine;
			
			if (!undoInProgress ) {
				undoManager.contentRemoved(offset, length, getText(offset, length),
						!dirty);
			}
			
			fireTextPreDeleted(startLine,offset,length,numLines);
		
			content.delete(offset, length);
			lineManager.contentRemoved(startLine, offset, length, numLines);
		
			setDirty(true);
			fireTextDeleted(startLine,offset,length,numLines);
			
			if (!undoInProgress && !isCompoundEdit())
				fireTransactionComplete();
		} finally {
			transaction = false;
			writeUnlock();
		}
	}
	
	
	public void getText(int start, int length, Segment seg) {
		try {
			readLock();
			
			if (start < 0 || length < 0 || start + length > content.getLength())
				throw new ArrayIndexOutOfBoundsException(start + ":" + length);
			
			content.getText(start, length, seg);
		} finally {
			readUnlock();
		}
	}
	
	public String getText(int start, int length) {
		try {
			readLock();
			
			if (start < 0 || length < 0 || start + length > content.getLength())
				throw new ArrayIndexOutOfBoundsException(start + ":" + length);
			
			return content.getText(start, length);
		} finally {
			readUnlock();
		}
	}
	public String getText() {
		try {
			readLock();
			return content.getText();
		} finally {
			readUnlock();
		}
	}
	
	public CharSequence getSequence(int start, int length) {
		try {
			readLock();
			
			if (start < 0 || length < 0 || start + length > content.getLength())
				throw new ArrayIndexOutOfBoundsException(start + ":" + length);
			
			return content.getSequence(start, length);
		} finally {
			readUnlock();
		}
	}
	public int getLength() {
		return content.getLength();
	}
	
	public int getLineCount(){
		return lineManager.getLineCount();
	}
	
	public CharSequence getLineSequence(int line) {
		if (line < 0 || line >= lineManager.getLineCount())
			throw new ArrayIndexOutOfBoundsException(line);
		
		try {
			readLock();
			
			int start = line == 0 ? 0 : lineManager.getLineEndOffset(line - 1);
			int end = lineManager.getLineEndOffset(line);
			
			return getSequence(start, end - start - 1);
		} finally {
			readUnlock();
		}
	}
	
	public int getLineOfOffset(int offset) {
		try {
			readLock();
			
			if (offset < 0 || offset > getLength())
				throw new ArrayIndexOutOfBoundsException(offset);
			
			return lineManager.getLineOfOffset(offset);
		} finally {
			readUnlock();
		}
	}
	
	public int getLineStartOffset(int line) {
		try {
			readLock();
			
			if (line < 0 || line >= lineManager.getLineCount())
				throw new ArrayIndexOutOfBoundsException(line);
			else if (line == 0)
				return 0;
			
			return lineManager.getLineEndOffset(line - 1);
		} finally {
			readUnlock();
		}
	}
	
	public int getLineEndOffset(int line) {
		try {
			readLock();
			
			if (line < 0 || line >= lineManager.getLineCount())
				throw new ArrayIndexOutOfBoundsException(line);
			
			return lineManager.getLineEndOffset(line);
		} finally {
			readUnlock();
		}
	}
	
	public int getLineLength(int line) {
		try {
			readLock();
			return getLineEndOffset(line) - getLineStartOffset(line) - 1;
		} finally {
			readUnlock();
		}
	}
	
	public String getLineText(int line) {
		if (line < 0 || line >= lineManager.getLineCount())
			throw new ArrayIndexOutOfBoundsException(line);
		
		try {
			readLock();
			
			int start = line == 0 ? 0 : lineManager.getLineEndOffset(line - 1);
			int end = lineManager.getLineEndOffset(line);
			
			return getText(start, end - start - 1);
		} finally {
			readUnlock();
		}
	}
	
	public void getLineText(int line, Segment segment) {
		getLineText(line, 0, segment);
	}
	
	public void getLineText(int line, int relativeStartOffset, Segment segment) {
		if (line < 0 || line >= lineManager.getLineCount())
			throw new ArrayIndexOutOfBoundsException(line);
		
		try {
			readLock();
			
			int start = (line == 0 ? 0 : lineManager.getLineEndOffset(line - 1));
			int end = lineManager.getLineEndOffset(line);
			if ((start + relativeStartOffset) > end) {
				throw new IllegalArgumentException("This index is outside the line length (start+relativeOffset):" + start + " + " + relativeStartOffset + " > " + "endffset:" + end);
			} else {
				getText(start + relativeStartOffset, end - start - relativeStartOffset - 1, segment);
			}
		} finally {
			readUnlock();
		}
	}
	private void fireTextSet() {
		for (ModelListener listener : modelListeners) {
			listener.textSet(this);
		}
	}
	
	private void fireTextPreInserted(int startLine, int start, int length, int numLines) {
		for (ModelListener listener : modelListeners) {
			listener.textPreInserted(this, startLine, start, length, numLines);
		}
	}
	
	private void fireTextInserted(int startLine, int start, int length, int numLines) {
		for (ModelListener listener : modelListeners) {
			listener.textInserted(this, startLine, start, length, numLines);
		}
	}
	
	private void fireTextPreDeleted(int startLine, int start, int length, int numLines) {
		for (ModelListener listener : modelListeners) {
			listener.textPreDeleted(this, startLine, start, length, numLines);
		}
	}
	
	private void fireTextDeleted(int startLine, int start, int length, int numLines) {
		for (ModelListener listener : modelListeners) {
			listener.textDeleted(this, startLine, start, length, numLines);
		}
	}
	
	private void fireTransactionComplete() {
		for (ModelListener listener : modelListeners) {
			listener.transactionComplete(this);
		}
	}
	
	
	public void addUndoListener(@NonNull UndoListener listener) {
		undoListeners.add(listener);
	}
	
	public void removeUndoListener(@NonNull UndoListener listener) {
		undoListeners.remove(listener);
	}
	
	private void fireBeginUndo() {
		for (UndoListener listener : undoListeners) {
			listener.beginUndo(this);
		}
	}
	
	private void fireEndUndo() {
		for (UndoListener listener : undoListeners) {
			listener.endUndo(this);
		}
	}
	
	private void fireBeginRedo() {
		for (UndoListener listener : undoListeners) {
			listener.beginRedo(this);
		}
	}
	
	private void fireEndRedo() {
		for (UndoListener listener : undoListeners) {
			listener.endRedo(this);
		}
	}
	
	
	public void setUndoLimit(int limit) {
		if (undoManager == null) return;
		undoManager.setLimit(limit);
	}
	
	public boolean canUndo() {
		return undoManager != null && undoManager.canUndo();
	}
	
	public boolean canRedo() {
		return undoManager != null && undoManager.canRedo();
	}
	
	public void undo() {
		if (undoManager == null) return;
		try {
			writeLock();
			undoInProgress = true;
			fireBeginUndo();
			undoManager.undo();
			fireEndUndo();
			fireTransactionComplete();
		} finally {
			undoInProgress = false;
			writeUnlock();
		}
	}
	
	public void redo() {
		if (undoManager == null) return;
		if (!isEditable()) return;
		
		try {
			writeLock();
			undoInProgress = true;
			fireBeginRedo();
			undoManager.redo();
			fireEndRedo();
			fireTransactionComplete();
		} finally {
			undoInProgress = false;
			writeUnlock();
		}
	}
	
	public void beginCompoundEdit() {
		try {
			writeLock();
			if (undoManager != null)
				undoManager.beginCompoundEdit();
		} finally {
			writeUnlock();
		}
		
	}
	
	public void endCompoundEdit() {
		try {
			writeLock();
			
			if (undoManager != null)
				undoManager.endCompoundEdit();
			
			if (!isCompoundEdit())
				fireTransactionComplete();
		} finally {
			writeUnlock();
		}
		
	}
	
	public boolean isCompoundEdit() {
		return undoManager != null && undoManager.insideCompoundEdit();
	}
	
	protected boolean isDirty() {
		return dirty;
	}
	
	protected void setDirty(boolean dirty) {
		boolean editable = isEditable();
		if (dirty) {
			if (editable)
				this.dirty = true;
		} else {
			this.dirty = false;
			if (!isUndoInProgress()) {
				undoManager.resetClearDirty();
			}
		}
	}
	
	public boolean isUndoInProgress() {
		return undoInProgress;
	}
	
	public void setFoldHandler(@NonNull FoldHandler foldHandler) {
		FoldHandler oldFoldHandler = this.foldHandler;
		
		if (foldHandler.equals(oldFoldHandler))
			return;
		
		this.foldHandler = foldHandler;
		
		lineManager.setFirstInvalidFoldLevel(0);
		
		fireFoldHandlerChanged();
	}
	
	public FoldHandler getFoldHandler() {
		return foldHandler;
	}
	
	public boolean isFoldStart(int line) {
		return line != getLineCount() - 1
				&& getFoldLevel(line) < getFoldLevel(line + 1);
	}
	
	public boolean isFoldEnd(int line) {
		int foldLevel = getFoldLevel(line);
		int nextLineFoldLevel = line == getLineCount() - 1 ? 0 : getFoldLevel(line + 1);
		return foldLevel > nextLineFoldLevel;
	}
	
	public int getFoldLevel(int line) {
		if (line < 0 || line >= lineManager.getLineCount())
			throw new ArrayIndexOutOfBoundsException(line);
		
		if (foldHandler instanceof DummyFoldHandler)
			return 0;
		
		int firstInvalidFoldLevel = lineManager.getFirstInvalidFoldLevel();
		if (firstInvalidFoldLevel == -1 || line < firstInvalidFoldLevel) {
			return lineManager.getFoldLevel(line);
		} else {
		
			int newFoldLevel = 0;
			boolean changed = false;
			int firstUpdatedFoldLevel = firstInvalidFoldLevel;
			
			for (int i = firstInvalidFoldLevel; i <= line; i++) {
				Segment seg = new Segment();
				newFoldLevel = foldHandler.getFoldLevel(this, i, seg);
				if (newFoldLevel != lineManager.getFoldLevel(i)) {
					changed = true;
					
					if (i == firstInvalidFoldLevel) {
						IntArrayList precedingFoldLevels =
								foldHandler.getPrecedingFoldLevels(this, i, seg, newFoldLevel);
						if (precedingFoldLevels != null) {
							int j = i;
							for (int index = 0; index < precedingFoldLevels.size(); index++) {
								j--;
								lineManager.setFoldLevel(j, precedingFoldLevels.get(index));
							}
							if (j < firstUpdatedFoldLevel)
								firstUpdatedFoldLevel = j;
						}
					}
				}
				lineManager.setFoldLevel(i, newFoldLevel);
			}
			
			if (line == lineManager.getLineCount() - 1)
				lineManager.setFirstInvalidFoldLevel(-1);
			else
				lineManager.setFirstInvalidFoldLevel(line + 1);
			
			if (changed)
				fireFoldLevelChanged(firstUpdatedFoldLevel, line);
			
			return newFoldLevel;
		}
	}
	public int[] getFoldAtLine(int line) {
		int start, end;
		
		if (isFoldStart(line)) {
			start = line;
			int foldLevel = getFoldLevel(line);
			
			line++;
			
			while (getFoldLevel(line) > foldLevel) {
				line++;
				
				if (line == getLineCount())
					break;
			}
			
			end = line - 1;
		} else {
			start = line;
			int foldLevel = getFoldLevel(line);
			while (getFoldLevel(start) >= foldLevel) {
				if (start == 0)
					break;
				else
					start--;
			}
			
			end = line;
			while (getFoldLevel(end) >= foldLevel) {
				end++;
				
				if (end == getLineCount())
					break;
			}
			
			end--;
		}
		
		while (getLineLength(end) == 0 && end > start)
			end--;
		
		return new int[]{start, end};
	}
	private void fireFoldLevelChanged(int startLine, int endLine) {
		for (ModelListener listener : modelListeners) {
			listener.foldLevelChanged(this,startLine,endLine);
		}
	}
	
	private void fireFoldHandlerChanged() {
		for (ModelListener listener : modelListeners) {
			listener.foldHandlerChanged(this);
		}
	}
}
