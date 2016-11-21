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

package com.actelion.research.spiritapp.spirit.ui.biosample.edit;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.table.TableColumn;

import com.actelion.research.spiritapp.spirit.ui.biosample.BiosampleTable;
import com.actelion.research.spiritapp.spirit.ui.biosample.column.ContainerFullColumn;
import com.actelion.research.spiritapp.spirit.ui.biosample.column.ContainerIdColumn;
import com.actelion.research.spiritapp.spirit.ui.biosample.column.ContainerLocationPosColumn;
import com.actelion.research.spiritapp.spirit.ui.biosample.column.ContainerTypeColumn;
import com.actelion.research.spiritapp.spirit.ui.biosample.editor.SampleIdCellEditor;
import com.actelion.research.spiritapp.spirit.ui.biosample.linker.AbstractLinkerColumn;
import com.actelion.research.spiritapp.spirit.ui.biosample.linker.LinkedBiosampleColumn;
import com.actelion.research.spiritapp.spirit.ui.biosample.linker.MetadataColumn;
import com.actelion.research.spiritapp.spirit.ui.biosample.linker.SampleIdColumn;
import com.actelion.research.spiritapp.spirit.ui.biosample.linker.SampleNameColumn;
import com.actelion.research.spiritapp.spirit.ui.lf.SpiritExcelTable;
import com.actelion.research.spiritcore.business.DataType;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.biosample.ContainerType;
import com.actelion.research.spiritcore.services.dao.DAOBarcode;
import com.actelion.research.spiritcore.services.dao.DAOBiotype;
import com.actelion.research.util.ui.JCustomLabel;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.exceltable.Column;
import com.actelion.research.util.ui.exceltable.FillCellAction;


public class EditBiosampleTable extends SpiritExcelTable<Biosample> {
	
	
	private SampleIdCellEditor sampleIdCellEditor;

	
	
	/**
	 * Creates a generic table
	 */
	public EditBiosampleTable() {
		this(new EditBiosampleTableModel());
		try {
			setRows(null, new ArrayList<Biosample>());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Creates a table with a custom view
	 * @param model
	 */
	public EditBiosampleTable(EditBiosampleTableModel model) {
		super(model);	
		
		setBorderStrategy(BorderStrategy.WHEN_DIFFERENT_VALUE);
		
		getTableHeader().addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				final int colNo = columnAtPoint(e.getPoint());
				if(e.getClickCount()>=2 && colNo>=0) {
					Column<Biosample, ?> col = getModel().getColumn(convertColumnIndexToModel(colNo));
					if(col instanceof ContainerFullColumn) {
						BiosampleTable.expandContainerLocation(EditBiosampleTable.this, true);
					} else if(col instanceof ContainerTypeColumn || col instanceof ContainerIdColumn || col instanceof ContainerLocationPosColumn) {
						BiosampleTable.expandContainerLocation(EditBiosampleTable.this, false);
					} else if(col instanceof AbstractLinkerColumn) {
						BiosampleTable.expandBiotype(EditBiosampleTable.this, ((AbstractLinkerColumn<?>) col).getBiotype().getName(), null);
					}
					
				}
			}
		});

	}
	
	
	@Override
	public void initCellEditors() {

		//Set the editors
		for (int i = 0; i < getColumnCount(); i++) {
			Column<Biosample, ?> cb = getModel().getColumn(i);
			TableColumn col = getColumnModel().getColumn(i);
			if(cb.getCellEditor(null)!=null) continue; //already one
			
			
			if(cb instanceof SampleIdColumn) {
				if(sampleIdCellEditor==null) sampleIdCellEditor = new SampleIdCellEditor();
				col.setCellEditor(sampleIdCellEditor);											
			}
		}		
	

	}
	
	@Override
	public EditBiosampleTableModel getModel() {
		return (EditBiosampleTableModel) super.getModel();
	}
	
	public void setRows(Biotype type, List<Biosample> biosamples) throws Exception {
		
		//Check validity		
		if(biosamples!=null) {
			for (Biosample biosample : biosamples) {				
				if(type!=null && biosample.getBiotype()==null) {
					biosample.setBiotype(type);
				} else if(type!=null && !biosample.getBiotype().equals(type)) {
					throw new Exception("The biosamples should have the same bioType in the table view");
				}
			}
			
			
		}
		//Reset the model
		getModel().setRows(biosamples);
		getModel().setBiotype(type);
		resetPreferredColumnWidth();
	}
	
	
	public List<Biosample> getBiosamples(){
		return getModel().getRows();
	}
	
	public Biotype getType() {
		return getModel().getBiotype();
	}
	
	public void generateSampleId(Biosample b) {
		String id = sampleIdCellEditor.generateSampleIdFor(b);
		if(id!=null) b.setSampleId(id);		
	}
	
	public class GenerateForLinkedAction extends AbstractAction {
		public GenerateForLinkedAction() {
			super("Generate new AnimalIds");
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				for (Biosample b : getRows()) {
					Biotype type = DAOBiotype.getBiotype(Biotype.ANIMAL);
					if(b.getId()>0 || (b.getSampleId()!=null && b.getSampleId().length()>0)) continue;
						String id = DAOBarcode.getNextId(type);
						b.setSampleId(id);
						b.setBiotype(type);
				}		
				repaint();	
			} catch (Exception ex) {
				ex.printStackTrace();
				JExceptionDialog.showError(ex);
			}
		}
	}
	
//	public class PrintAction extends AbstractAction {
//		public PrintAction() {
//			super("Print Labels for selected biosamples");
//		}
//		@Override
//		public void actionPerformed(ActionEvent e) {
//			List<Biosample> biosamples = new ArrayList<Biosample>();
//			for (int r: getSelectedRows()) {
//				biosamples.add(getModel().getRows().get(r));
//			}
//			
//			PrinterDlg.createForBiosamples(biosamples);
//		}
//	}
	
	@Override
	protected void populateHeaderPopup(JPopupMenu popupMenu, Column<Biosample, ?> column) {
		List<String> fillChoices = null;
		if(column instanceof MetadataColumn && ((MetadataColumn)column).getType().getDataType()==DataType.LIST) {
			fillChoices = ((MetadataColumn)column).getType().extractChoices();
		} else if(column instanceof MetadataColumn && ((MetadataColumn)column).getType().getDataType()==DataType.AUTO) {
			fillChoices = new ArrayList<String>(DAOBiotype.getAutoCompletionFields(((MetadataColumn)column).getType(), null));
		} else if(column instanceof SampleNameColumn && getModel().getBiotype()!=null && getModel().getBiotype().isNameAutocomplete()) {
			fillChoices = new ArrayList<String>(DAOBiotype.getAutoCompletionFieldsForName( getModel().getBiotype(), null));
		} else if(column instanceof ContainerTypeColumn) {
			fillChoices = new ArrayList<String>();
			for (ContainerType ct : ContainerType.values()) fillChoices.add(ct.getName());
		}

		popupMenu.add(new JSeparator());
		popupMenu.add(new JCustomLabel("Misc", Font.BOLD));
		popupMenu.add(new FillCellAction(this, column, fillChoices));
		
		
		if(column instanceof LinkedBiosampleColumn) {
			popupMenu.add(new GenerateForLinkedAction());			
//		} else if(column instanceof SampleIdColumn) {
//			popupMenu.add(new GenerateForScanEditorAction());
//		} else if(column instanceof ParentBiosampleColumn) {
//			popupMenu.add(new ConvertNoToAnimalIdAction(this));			
		}
		
		BiosampleTable.populateExpandPopup(this, popupMenu);
		
	}


	
//	@Override
//	public void setValueAt(Object aValue, int row, int column) {
////		Column<Biosample, ?> col = getModel().getColumn(convertColumnIndexToModel(column));
////		if(col instanceof AuxiliaryColumn && !getUndoManager().isPushed()) {
////			//Auxiliary column should ask confirmation when editing
////			Double val = ((AuxiliaryColumn)col).getValue(getRows().get(row));
////			if(val!=null && aValue!=null && !val.equals(aValue)) {
////				int res = JOptionPane.showConfirmDialog(this, "Are you sure to replace the value "+val+" by "+ aValue + "?", "Replace Value", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
////				setRowSelectionInterval(row, row);
////				setColumnSelectionInterval(column, column);
////				requestFocusInWindow();					
////				if(res!=JOptionPane.YES_OPTION) return;
////			}		
////		}
//		super.setValueAt(aValue, row, column);
//		
//		repaint();
//	}

	
}
