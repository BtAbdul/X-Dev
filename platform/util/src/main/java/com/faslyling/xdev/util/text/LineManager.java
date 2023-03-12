package com.faslyling.xdev.util.text;

import static java.lang.System.arraycopy;

import androidx.annotation.NonNull;

import com.faslyling.xdev.util.IntArrayList;

class LineManager {
	private int[] endOffsets;
	private short[] foldLevels;
	private int lineCount;
	private int gapLine;
	private int gapWidth;
	private int firstInvalidLineContext;
	
	private int firstInvalidFoldLevel;
	
	private int getLineOfOffsetLine = -1;
	
	public LineManager() {
		endOffsets = new int[1];
		endOffsets[0] = 1;
		foldLevels = new short[1];
		lineCount = 1;
	}
	
	public int getLineCount() {
		return lineCount;
	}
	
	public int getLineOfOffset(int offset) {
		if (getLineOfOffsetLine > 0 && getLineOfOffsetLine < lineCount) {
			int s = getLineEndOffset(getLineOfOffsetLine - 1);
			int e = getLineEndOffset(getLineOfOffsetLine);
			if (offset >= s && offset < e)
				return getLineOfOffsetLine;
		}
		
		int start = 0;
		int end = lineCount - 1;
		
		for (; ; ) {
			switch (end - start) {
				case 0:
					if (getLineEndOffset(start) <= offset)
						getLineOfOffsetLine = start + 1;
					else
						getLineOfOffsetLine = start;
					return getLineOfOffsetLine;
				case 1:
					if (getLineEndOffset(start) <= offset) {
						if (getLineEndOffset(end) <= offset)
							getLineOfOffsetLine = end + 1;
						else
							getLineOfOffsetLine = end;
					} else
						getLineOfOffsetLine = start;
					return getLineOfOffsetLine;
				default:
					int pivot = (end + start) / 2;
					int value = getLineEndOffset(pivot);
					if (value == offset) {
						getLineOfOffsetLine = pivot + 1;
						return getLineOfOffsetLine;
					} else if (value < offset)
						start = pivot + 1;
					else
						end = pivot - 1;
					break;
			}
		}
	}
	
	public int getLineEndOffset(int line) {
		if (gapLine != -1 && line >= gapLine)
			return endOffsets[line] + gapWidth;
		else
			return endOffsets[line];
	}
	
	public int getFoldLevel(int line) {
		return foldLevels[line];
	}
	
	public void setFoldLevel(int line, int level) {
		if (level > 0xffff)
			level = 0xffff;
		
		foldLevels[line] = (short) level;
	}
	
	public int getFirstInvalidFoldLevel() {
		return firstInvalidFoldLevel;
	}
	
	
	public void setFirstInvalidFoldLevel(int firstInvalidFoldLevel) {
		this.firstInvalidFoldLevel = firstInvalidFoldLevel;
	}
	
	
	public int getFirstInvalidLineContext() {
		return firstInvalidLineContext;
	}
	
	public void setFirstInvalidLineContext(int firstInvalidLineContext) {
		this.firstInvalidLineContext = firstInvalidLineContext;
	}
	
	public void _contentInserted(@NonNull IntArrayList endOffsets) {
		gapLine = -1;
		gapWidth = 0;
		firstInvalidLineContext = firstInvalidFoldLevel = 0;
		lineCount = endOffsets.size();
		this.endOffsets = endOffsets.toArray();
		foldLevels = new short[lineCount];
		
	}
	
	public void contentInserted(int startLine, int offset, int length, int numLines, IntArrayList endOffsets) {
		int endLine = startLine + numLines;
		
		if (numLines > 0) {
			lineCount += numLines;
			
			if (this.endOffsets.length <= lineCount) {
				int[] endOffsetsN = new int[(lineCount + 1) * 2];
				arraycopy(this.endOffsets, 0, endOffsetsN, 0,
						this.endOffsets.length);
				this.endOffsets = endOffsetsN;
			}
			
			if (foldLevels.length <= lineCount) {
				short[] foldLevelsN = new short[(lineCount + 1) * 2];
				arraycopy(foldLevels, 0, foldLevelsN, 0,
						foldLevels.length);
				foldLevels = foldLevelsN;
			}
			
			
			arraycopy(this.endOffsets, startLine,
					this.endOffsets, endLine, lineCount - endLine);
			arraycopy(foldLevels, startLine, foldLevels,
					endLine, lineCount - endLine);
			
			if (startLine <= gapLine)
				gapLine += numLines;
			else if (gapLine != -1)
				offset -= gapWidth;
			
			for (int i = 0; i < numLines; i++) {
				this.endOffsets[startLine + i] = (offset + endOffsets.get(i));
				foldLevels[startLine + i] = 0;
			}
		}
		
		if (firstInvalidLineContext == -1 || firstInvalidLineContext > startLine)
			firstInvalidLineContext = startLine;
		
		if (firstInvalidFoldLevel == -1 || firstInvalidFoldLevel > startLine)
			firstInvalidFoldLevel = startLine;
		moveGap(endLine, length);
	}
	
	
	public void contentRemoved(int startLine, int offset, int length, int numLines) {
		int endLine = startLine + numLines;
		
		if (numLines > 0) {
			if (startLine + numLines < gapLine)
				gapLine -= numLines;
			else if (startLine < gapLine)
				gapLine = startLine;
			
			lineCount -= numLines;
			
			arraycopy(endOffsets, endLine, endOffsets,
					startLine, lineCount - startLine);
			arraycopy(foldLevels, endLine, foldLevels,
					startLine, lineCount - startLine);
		}
		
		if (firstInvalidLineContext == -1 || firstInvalidLineContext > startLine)
			firstInvalidLineContext = startLine;
		
		if (firstInvalidFoldLevel == -1 || firstInvalidFoldLevel > startLine)
			firstInvalidFoldLevel = startLine;
		moveGap(startLine, -length);
	}
	
	private void setLineEndOffset(int line, int end) {
		endOffsets[line] = end;
	} //}}}
	
	private void moveGap(int newGapLine, int newGapWidth) {
		if (gapLine == -1)
			gapWidth = newGapWidth;
		else if (newGapLine == -1) {
			if (gapWidth != 0) {
				for (int i = gapLine; i < lineCount; i++)
					setLineEndOffset(i, getLineEndOffset(i));
			}
			
			gapWidth = newGapWidth;
		} else if (newGapLine < gapLine) {
			if (gapWidth != 0) {
				for (int i = newGapLine; i < gapLine; i++)
					setLineEndOffset(i, getLineEndOffset(i) - gapWidth);
			}
			gapWidth += newGapWidth;
		} else{
			if (gapWidth != 0) {
				for (int i = gapLine; i < newGapLine; i++)
					setLineEndOffset(i, getLineEndOffset(i));
			}
			gapWidth += newGapWidth;
		}
		
		if (newGapLine == lineCount)
			gapLine = -1;
		else
			gapLine = newGapLine;
	}
}
