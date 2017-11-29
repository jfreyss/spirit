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

package com.actelion.research.spiritapp.ui.study.edit;

import java.awt.Component;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.AbstractCellEditor;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;

import com.actelion.research.spiritcore.business.biosample.BarcodeSequence.Category;
import com.actelion.research.spiritapp.ui.study.edit.AttachedBiosampleTableModel.SampleIdColumn;
import com.actelion.research.spiritapp.ui.util.lf.SpiritExcelTable;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Container;
import com.actelion.research.spiritcore.business.study.AttachedBiosample;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.dao.DAOBarcode;
import com.actelion.research.util.CompareUtils;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.JTextComboBox;
import com.actelion.research.util.ui.exceltable.Column;

public class AttachedBiosampleTable extends SpiritExcelTable<AttachedBiosample> {

	private AttachedBiosampleIdCellEditor sampleIdCellEditor;

	public AttachedBiosampleTable(AttachedBiosampleTableModel model, boolean allowDnd) {
		super(model);

		setCanAddRow(false);
		setGoNextOnEnter(false);
		setBorderStrategy(BorderStrategy.WHEN_DIFFERENT_VALUE);
		setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

		if(allowDnd) {
			setDropTarget(new DropTarget(this, dropListener));
			DragSource dragSource = DragSource.getDefaultDragSource();
			dragSource.createDefaultDragGestureRecognizer(this, DnDConstants.ACTION_COPY_OR_MOVE, dropListener);
		}
		getModel().fireTableStructureChanged();
		resetPreferredColumnWidth();
	}


	@Override
	public void initCellEditors() {
		if(sampleIdCellEditor==null) {
			sampleIdCellEditor = new AttachedBiosampleIdCellEditor(getModel());
		}

		//Set the editors
		for (int i = 0; i < getColumnCount(); i++) {
			Column<AttachedBiosample, ?> cb = getModel().getColumn(i);
			TableColumn	col = getColumnModel().getColumn(i);


			if((cb instanceof SampleIdColumn) && col.getCellEditor()!=sampleIdCellEditor) {
				col.setCellEditor(sampleIdCellEditor);
			}
		}
	}

	public void regenerateSampleIds(Collection<AttachedBiosample> rows) {
		if(rows.size()==0) return;
		boolean empty = true;

		//Try to generate all sampleIds
		for (AttachedBiosample b : rows) {
			if(b.getSampleId()!=null && b.getSampleId().length()>0) {
				empty = false;
				break;
			}
		}

		//If nothing was changed, suggest to force change
		if(!empty) {
			int res = JOptionPane.showConfirmDialog(this, "Do you want to recreate NEW sampleIds for those " +rows.size() + " samples", "Regenerate SampleIds", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
			if(res!=JOptionPane.YES_OPTION) return;

		}
		for (AttachedBiosample b : rows) {
			String id = DAOBarcode.getNextId(Category.BIOSAMPLE, getModel().getBiotype()==null?"INT":getModel().getBiotype().getPrefix());
			if(b.getBiosample()==null) {
				b.setBiosample(new Biosample(getModel().getBiotype(), id));
				b.setSampleId(id);
			} else {
				b.getBiosample().setSampleId(id);
				b.setSampleId(id);
			}
		}
		getModel().fireTableDataChanged();
	}




	public static class CageCellEditor extends AbstractCellEditor implements TableCellEditor {
		private JTextComboBox cageComboBox;
		public CageCellEditor(Study study) {
			List<String> cageNames = new ArrayList<String>();
			for (int i = 0; i < 30; i++) {
				cageNames.add(Container.suggestNameForCage(study, i+1));
			}
			cageComboBox = new JTextComboBox(cageNames);
			cageComboBox.setBorder(UIManager.getBorder("Table.focusCellHighlightBorder"));
		}

		@Override
		public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
			cageComboBox.setText((String) value);
			return cageComboBox;
		}

		@Override
		public Object getCellEditorValue() {
			return cageComboBox.getText();
		}
	}

	@Override
	public AttachedBiosampleTableModel getModel() {
		return (AttachedBiosampleTableModel) super.getModel();
	}


	public List<Double> getDoubles(int index){
		return AttachedBiosample.getData(getRows(), index);
	}

	private DropListener dropListener = new DropListener();
	private static final DataFlavor df = new DataFlavor(List.class, "ListRndSample");

	public static class RndTransferable implements Transferable {

		private List<AttachedBiosample> list;

		public RndTransferable(List<AttachedBiosample> list) {
			this.list = list;
		}
		@Override
		public DataFlavor[] getTransferDataFlavors() {
			return new DataFlavor[] {df};
		}

		@Override
		public boolean isDataFlavorSupported(DataFlavor flavor) {
			return flavor.equals(df);
		}

		@Override
		public List<AttachedBiosample> getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
			//			if(!df.equals(flavor)) throw new UnsupportedFlavorException(flavor);
			return list;
		}

	}

	public class DropListener implements DropTargetListener, DragSourceListener, DragGestureListener {
		@Override
		public void dragGestureRecognized(DragGestureEvent dge) {
			List<AttachedBiosample> list = getSelection();
			if(list.size()==0) return;

			Transferable transferable = new RndTransferable(list);
			dge.startDrag(null, transferable, this);

		}

		@Override
		public void dragEnter(DropTargetDragEvent dtde) {

		}

		@Override
		public void dragOver(DropTargetDragEvent dtde) {
		}

		@Override
		public void dropActionChanged(DropTargetDragEvent dtde) {
		}

		@Override
		public void dragExit(DropTargetEvent dte) {
		}

		@SuppressWarnings("unchecked")
		@Override
		public void drop(DropTargetDropEvent dtde) {

			Transferable t = dtde.getTransferable();
			if(!t.isDataFlavorSupported(df)) {
				System.err.println(df+" not supported");
				dtde.rejectDrop();
				return;
			}
			dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
			try {
				List<AttachedBiosample> list = (List<AttachedBiosample>) t.getTransferData(df);
				for (AttachedBiosample s : list) {
					s.setGroup(getModel().getGroup());
				}
				List<AttachedBiosample> all = getRows();
				all.removeAll(list);
				all.addAll(list);
				Collections.sort(getRows());
				setRows(all);

				dtde.dropComplete(true);
				setSelection(list);
			} catch (Throwable e) {
				e.printStackTrace();
				dtde.dropComplete(false);
			}

		}

		@Override
		public void dragEnter(DragSourceDragEvent dsde) {
		}

		@Override
		public void dragOver(DragSourceDragEvent dsde) {
		}

		@Override
		public void dropActionChanged(DragSourceDragEvent dsde) {
		}

		@Override
		public void dragExit(DragSourceEvent dse) {
		}

		@Override
		public void dragDropEnd(DragSourceDropEvent dsde) {
			if(!dsde.getDropSuccess()) return;

			List<AttachedBiosample> toBeRemoved = new ArrayList<AttachedBiosample>();
			for (AttachedBiosample rndSample : getRows()) {
				if(CompareUtils.compare(rndSample.getGroup(), getModel().getGroup())!=0) {
					toBeRemoved.add(rndSample);
				}
			}

			getRows().removeAll(toBeRemoved);
			getModel().fireTableDataChanged();
		}
	}

	public Study getStudy() {
		return getModel().getStudy();
	}

	public void setStudy(Study study) {
		throw new IllegalArgumentException("Not to be called");
	}

	@Override
	public void setRows(List<AttachedBiosample> data) {
		getModel().setRows(data);
		getModel().initColumns();
		resetPreferredColumnWidth();
	}

	public class RegenerateSampleIdAction extends AbstractAction {
		public RegenerateSampleIdAction() {
			super("(Re)Generate selected SampleIds");
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			List<AttachedBiosample> sel = getSelection();
			if(sel.size()==0) {
				JExceptionDialog.showError("You must select some rows");
				return;
			}
			regenerateSampleIds(sel);
			repaint();
		}
	}
}
