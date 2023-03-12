package com.faslyling.xdev.util.text;

import com.faslyling.xdev.util.IntArrayList;

class UndoManager {
	
	private final TextModel model;
	private Object undoId;
	private int limit;
	private int undoCount;
	private int compoundEditCount;
	private CompoundEdit compoundEdit;
	private Edit undoFirst;
	private Edit undoLast;
	private Edit undoClearDirty;
	private Edit redoFirst;
	private Edit redoClearDirty;
	
	public UndoManager(TextModel model) {
		this.model = model;
	}
	
	public void contentInserted(int offset, String text, boolean clearDirty) {
		Edit toMerge = getMergeEdit();
		
		if (!clearDirty && toMerge instanceof InsertEdit && redoFirst == null) {
			InsertEdit ins = (InsertEdit) toMerge;
			if (ins.offset == offset) {
				ins.str = text.concat(ins.str);
				return;
			} else if (ins.offset + ins.str.length() == offset) {
				ins.str = ins.str.concat(text);
				return;
			}
		}
		
		InsertEdit ins = new InsertEdit(offset, text);
		
		if (clearDirty) {
			redoClearDirty = getLastEdit();
			undoClearDirty = ins;
		}
		
		if (compoundEdit != null)
			compoundEdit.add(this, ins);
		else {
			reviseUndoId();
			addEdit(ins);
		}
	}
	
	public void contentRemoved(int offset, int length, String text, boolean clearDirty) {
		Edit toMerge = getMergeEdit();
		
		if (!clearDirty && toMerge instanceof DeleteEdit && redoFirst == null) {
			DeleteEdit rem = (DeleteEdit) toMerge;
			if (rem.offset == offset) {
				rem.str = rem.str.concat(text);
				return;
			} else if (offset + length == rem.offset) {
				String newStr = text.concat(rem.str);
				rem.offset = offset;
				rem.str = newStr;
				return;
			}
		}
		
		DeleteEdit rem = new DeleteEdit(offset, text.intern());
		
		if (clearDirty) {
			redoClearDirty = getLastEdit();
			undoClearDirty = rem;
		}
		
		if (compoundEdit != null)
			compoundEdit.add(this, rem);
		else {
			reviseUndoId();
			addEdit(rem);
		}
	}
	
	public void setLimit(int limit) {
		this.limit = limit;
	}
	
	public int getLimit() {
		return limit;
	}
	
	public void clear() {
		undoFirst = undoLast = redoFirst = null;
		undoCount = 0;
	}
	
	public void resetClearDirty() {
		redoClearDirty = getLastEdit();
		if (redoFirst instanceof CompoundEdit)
			undoClearDirty = ((CompoundEdit) redoFirst).first;
		else
			undoClearDirty = redoFirst;
	}
	
	private Edit getLastEdit() {
		if (undoLast instanceof CompoundEdit)
			return ((CompoundEdit) undoLast).last;
		else
			return undoLast;
	}
	
	private Edit getMergeEdit() {
		return (compoundEdit != null ? compoundEdit.last : getLastEdit());
	}
	
	
	public boolean canUndo() {
		return undoLast != null;
	}
	
	public boolean canRedo() {
		return redoFirst != null;
	}
	
	public void undo() {
		if (insideCompoundEdit())
			throw new InternalError("Unbalanced begin/endCompoundEdit()");
		
		if (undoLast != null) {
			reviseUndoId();
			undoCount--;
			
			undoLast.undo(this);
			redoFirst = undoLast;
			undoLast = undoLast.prev;
			if (undoLast == null)
				undoFirst = null;
		}
	}
	
	public void redo() {
		if (insideCompoundEdit())
			throw new InternalError("Unbalanced begin/endCompoundEdit()");
		
		if (redoFirst != null) {
			reviseUndoId();
			undoCount++;
			
			redoFirst.redo(this);
			undoLast = redoFirst;
			if (undoFirst == null)
				undoFirst = undoLast;
			redoFirst = redoFirst.next;
		}
	}
	
	public boolean insideCompoundEdit() {
		return compoundEditCount != 0;
	}
	
	public void beginCompoundEdit() {
		if (compoundEditCount == 0) {
			compoundEdit = new CompoundEdit();
			reviseUndoId();
		}
		
		compoundEditCount++;
	}
	
	public void endCompoundEdit() {
		if (compoundEditCount == 0) {
			return;
		} else if (compoundEditCount == 1) {
			if (compoundEdit.first != null) {
				if (compoundEdit.first == compoundEdit.last)
					addEdit(compoundEdit.first);
				else
					addEdit(compoundEdit);
			}
			compoundEdit = null;
		}
		
		compoundEditCount--;
	}
	
	private void addEdit(Edit edit) {
		if (undoFirst == null)
			undoFirst = undoLast = edit;
		else {
			undoLast.next = edit;
			edit.prev = undoLast;
			undoLast = edit;
		}
		
		redoFirst = null;
		undoCount++;
		
		while (undoCount > limit) {
			undoCount--;
			
			if (undoFirst == undoLast)
				undoFirst = undoLast = null;
			else {
				undoFirst.next.prev = null;
				undoFirst = undoFirst.next;
			}
		}
	}
	
	private void reviseUndoId() {
		undoId = new Object();
	}
	
	public Object getUndoId() {
		return undoId;
	}
	
	private ReplaceEdit getReplaceFromRemoveInsert(Edit lastElement, Edit newElement) {
		if (lastElement instanceof DeleteEdit && newElement instanceof InsertEdit) {
			
			if (lastElement == undoClearDirty || newElement == undoClearDirty)
				return null;
			
			assert newElement != redoClearDirty;
			assert lastElement != redoClearDirty;
			
			DeleteEdit rem = (DeleteEdit) lastElement;
			InsertEdit ins = (InsertEdit) newElement;
			
			if (rem.offset == ins.offset)
				return new ReplaceEdit(rem.offset, rem.str, ins.str);
		}
		return null;
	}
	
	private CompressedReplaceEdit getCompressedReplaceFromReplaceReplace(Edit lastElement, Edit newElement) {
		if (newElement instanceof ReplaceEdit) {
			CompressedReplaceEdit rep;
			if (lastElement instanceof CompressedReplaceEdit) {
				rep = (CompressedReplaceEdit) lastElement;
				return rep.add((ReplaceEdit) newElement);
			}
			
			if (lastElement instanceof ReplaceEdit) {
				rep = new CompressedReplaceEdit((ReplaceEdit) lastElement);
				return rep.add((ReplaceEdit) newElement);
			}
		}
		return null;
	}
	
	
	private static class InsertEdit extends Edit {
		int offset;
		String str;
		
		InsertEdit(int offset, String str) {
			this.offset = offset;
			this.str = str;
		}
		
		@Override
		public void undo(UndoManager manager) {
			manager.model.delete(offset, str.length());
			if (manager.undoClearDirty == this)
				manager.model.setDirty(false);
		}
		
		@Override
		public void redo(UndoManager manager) {
			manager.model.insert(offset, str);
			if (manager.redoClearDirty == this)
				manager.model.setDirty(false);
		}
	}
	
	private static class DeleteEdit extends Edit {
		int offset;
		String str;
		
		DeleteEdit(int offset, String str) {
			this.offset = offset;
			this.str = str;
		}
		
		@Override
		public void undo(UndoManager manager) {
			manager.model.insert(offset, str);
			if (manager.undoClearDirty == this)
				manager.model.setDirty(false);
		}
		
		@Override
		public void redo(UndoManager manager) {
			manager.model.delete(offset, str.length());
			if (manager.redoClearDirty == this)
				manager.model.setDirty(false);
		}
	}
	
	private static class ReplaceEdit extends Edit {
		int offset;
		String strRemove, strInsert;
		
		ReplaceEdit(int offset, String strRemove, String strInsert) {
			this.offset = offset;
			this.strRemove = strRemove;
			this.strInsert = strInsert;
		}
		
		@Override
		public void undo(UndoManager manager) {
			manager.model.delete(offset, strInsert.length());
			manager.model.insert(offset, strRemove);
			assert manager.undoClearDirty != this;
		}
		
		@Override
		public void redo(UndoManager manager) {
			manager.model.delete(offset, strRemove.length());
			manager.model.insert(offset, strInsert);
			if (manager.redoClearDirty == this)
				manager.model.setDirty(false);
		}
	}
	
	private static class CompressedReplaceEdit extends ReplaceEdit {
		IntArrayList offsets;
		
		CompressedReplaceEdit(ReplaceEdit r1) {
			super(r1.offset, r1.strRemove, r1.strInsert);
			offsets = new IntArrayList(4);
			offsets.add(r1.offset);
		}
		
		CompressedReplaceEdit add(ReplaceEdit rep) {
			if (this.strInsert.equals(rep.strInsert) && this.strRemove.equals(rep.strRemove)) {
				offsets.add(rep.offset);
				return this;
			}
			return null;
		}
		
		@Override
		public void undo(UndoManager manager) {
			for (int i = offsets.size() - 1; i >= 0; i--) {
				offset = offsets.get(i);
				super.undo(manager);
			}
		}
		
		@Override
		public void redo(UndoManager manager) {
			for (int i = 0; i < offsets.size(); i++) {
				offset = offsets.get(i);
				super.redo(manager);
			}
		}
	}
	
	private abstract static class Edit {
		Edit prev, next;
		
		public abstract void undo(UndoManager manager);
		
		public abstract void redo(UndoManager manager);
	}
	
	private static class CompoundEdit extends Edit {
		Edit first, last;
		
		@Override
		public void undo(UndoManager manager) {
			Edit edit = last;
			while (edit != null) {
				edit.undo(manager);
				edit = edit.prev;
			}
		}
		
		@Override
		public void redo(UndoManager manager) {
			Edit edit = first;
			while (edit != null) {
				edit.redo(manager);
				edit = edit.next;
			}
		}
		
		public void add(UndoManager mgr, Edit edit) {
			add(edit);
			if (last.prev != null) {
				Edit rep = mgr.getReplaceFromRemoveInsert(last.prev, last);
				if (rep != null)
					exchangeLastElement(rep);
			}
			
			if (last.prev != null) {
				Edit rep = mgr.getCompressedReplaceFromReplaceReplace(last.prev, last);
				if (rep != null)
					exchangeLastElement(rep);
			}
			
			if (last.prev != null) {
				Edit rep = mgr.getCompressedReplaceFromReplaceReplace(last.prev, last);
				if (rep != null)
					exchangeLastElement(rep);
			}
		}
		
		private void add(Edit edit) {
			if (first == null)
				first = last = edit;
			else {
				edit.prev = last;
				last.next = edit;
				last = edit;
			}
		}
		
		private void exchangeLastElement(Edit edit) {
			if (first == last)
				first = last = null;
			else {
				last.prev.next = null;
				last = last.prev;
			}
			
			if (first == null || first == last)
				first = last = edit;
			else {
				edit.prev = last.prev;
				last.prev.next = edit;
				last = edit;
			}
		}
	}
}
