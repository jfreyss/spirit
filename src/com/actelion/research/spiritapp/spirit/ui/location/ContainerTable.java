/*
 * Spirit, a study/biosample management tool for research.
 * Copyright (C) 2016 Actelion Pharmaceuticals Ltd., Gewerbestrasse 16,
 * CH-4123 Allschwil, Switzerland.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 * @author Joel Freyss
 */

package com.actelion.research.spiritapp.spirit.ui.location;

import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.SwingConstants;

import com.actelion.research.spiritapp.spirit.ui.location.ContainerTableModel.ContainerTableModelType;
import com.actelion.research.spiritapp.spirit.ui.location.depictor.ContainerTransferable;
import com.actelion.research.spiritapp.spirit.ui.util.lf.SpiritExtendTable;
import com.actelion.research.spiritcore.business.biosample.Container;
import com.actelion.research.util.ui.exceltable.JLabelNoRepaint;

public class ContainerTable extends SpiritExtendTable<Container> {

	public ContainerTable(ContainerTableModelType type) {
		super(new ContainerTableModel(type));
		setUseSmartHeight(false); //always use max height
		setBorderStrategy(BorderStrategy.WHEN_DIFFERENT_VALUE);
	}

	@Override
	public ContainerTableModel getModel() {
		return (ContainerTableModel) super.getModel();
	}

	@Override
	public void setRows(final List<Container> data) {
		if(data==null || data.isEmpty()) {
			getModel().setRows(new ArrayList<>());
		} else {
			getModel().setRows(data);
		}
		getModel().initColumns();
		getModel().removeEmptyColumns();
		resetPreferredColumnWidth();

	}

	public class DropListener implements DragSourceListener, DragGestureListener {
		private List<Container> containers = null;
		private boolean removeOnDrop;

		public DropListener(boolean removeOnDrop) {
			this.removeOnDrop = removeOnDrop;
		}

		@Override
		public void dragGestureRecognized(DragGestureEvent dge) {
			if(getSelectedRow()<0) return;
			containers = getSelection();
			Transferable transferable = new ContainerTransferable(containers);
			dge.startDrag(null, transferable, this);

		}


		@Override
		public void dragEnter(DragSourceDragEvent dsde) {}

		@Override
		public void dragOver(DragSourceDragEvent dsde) {}

		@Override
		public void dropActionChanged(DragSourceDragEvent dsde) {}

		@Override
		public void dragExit(DragSourceEvent dse) {}

		@Override
		public void dragDropEnd(DragSourceDropEvent dsde) {
			if(dsde.getDropSuccess() && removeOnDrop) {
				getRows().removeAll(containers);
				getModel().fireTableDataChanged();
			}
			containers = null;
			repaint();
		}
	}

	public void enableDragSource(boolean removeOnDrop) {
		DropListener dropListener = new DropListener(removeOnDrop);
		DragSource dragSource = DragSource.getDefaultDragSource();
		dragSource.createDefaultDragGestureRecognizer(this, DnDConstants.ACTION_COPY_OR_MOVE, dropListener);
	}



	@Override
	public void postProcess(Container row, int rowNo, Object value, JComponent c) {
		if(c instanceof JLabelNoRepaint) {
			((JLabelNoRepaint) c).setVerticalAlignment(SwingConstants.TOP);

		}
		super.postProcess(row, rowNo, value, c);
	}

}
