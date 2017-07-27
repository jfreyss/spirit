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

package com.actelion.research.spiritapp.spirit.ui.biosample.linker;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.util.Comparator;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.table.TableCellEditor;

import com.actelion.research.spiritapp.spirit.ui.biosample.SampleIdLabel;
import com.actelion.research.spiritapp.spirit.ui.util.lf.LF;
import com.actelion.research.spiritcore.adapter.DBAdapter;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.BiosampleLinker;
import com.actelion.research.spiritcore.business.biosample.BiosampleLinker.LinkerType;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.util.CompareUtils;
import com.actelion.research.util.ui.JCustomLabel;
import com.actelion.research.util.ui.exceltable.AbstractExtendTable;

/**
 * Column used to represent the SampleId and the SampleName in one field
 *
 * @author freyssj
 */
public class SampleIdColumn extends AbstractLinkerColumn<String> {

	private final SampleIdLabel sampleIdLabel = new SampleIdLabel();

	/**
	 * Display sampleId, name, group, phase of the sample
	 */
	public SampleIdColumn() {
		this(new BiosampleLinker(LinkerType.SAMPLEID, null), true, true);
	}

	/**
	 * Display sampleId of the linked sample
	 */
	public SampleIdColumn(BiosampleLinker linker, boolean displayName, boolean displayPhase) {
		super(linker, String.class, 80, 200);
		sampleIdLabel.setDisplayName(displayName);
		sampleIdLabel.setDisplayGroup(displayPhase);
		sampleIdLabel.setHighlight(!this.linker.isLinked());
	}

	public boolean isDisplayName() {
		return sampleIdLabel.isDisplayName();
	}

	public void setDisplayName(boolean v) {
		sampleIdLabel.setDisplayName(v);
	}

	@Override
	public void paste(Biosample b, String value) throws Exception {
		//Only allowed if the sampleId is not hidden
		if(b!=null && b.getBiotype()!=null && !b.getBiotype().isHideSampleId()) {
			super.paste(b, value);
		}
	}

	@Override
	public String getValue(Biosample row) {
		row = linker.getLinked(row);
		if(row==null) return null;


		if(isDisplayName() && (row.getBiotype()==null || row.getBiotype().getSampleNameLabel()!=null)) {
			String s =  row.getSampleId()==null? "": row.getSampleId();
			s+="\t";
			s+= row.getBiotype()!=null && row.getBiotype().getSampleNameLabel()!=null? (row.getSampleName()==null?"":row.getSampleName()): "";
			return s;
		} else {
			return row.getSampleId();
		}

	}

	@Override
	public boolean shouldMerge(Biosample r1, Biosample r2) {
		if(linker.isLinked()) return super.shouldMerge(r1, r2);
		return r1.getTopParentInSameStudy().equals(r2.getTopParentInSameStudy());
	}



	@Override
	public void setValue(Biosample row, String value) {
		if(value==null) value = "";
		String split[] = value.split("\t");


		if(isDisplayName() && (row.getBiotype()==null || (!row.getBiotype().isHideSampleId() && row.getBiotype().getSampleNameLabel()!=null))) {
			row.setSampleId(split[0].trim());
			if(split.length>1) row.setSampleName(split[1].trim());
		} else {
			row.setSampleId(value);
		}

		//Prepopulate the other metadata from external db
		try {
			DBAdapter.getAdapter().populateFromExternalDB(row);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void populateHeaderPopup(final AbstractExtendTable<Biosample> table, JPopupMenu popupMenu) {
		popupMenu.add(new JSeparator());
		popupMenu.add(new JCustomLabel("Sort", Font.BOLD));

		Biotype biotype = linker.getBiotypeForLabel();
		popupMenu.add(new AbstractAction("Sort by SampleId") {
			@Override
			public void actionPerformed(ActionEvent e) {
				table.sortBy(SampleIdColumn.this, 1, new Comparator<Biosample>() {
					@Override
					public int compare(Biosample o1, Biosample o2) {
						Biosample l1 = getLinker().getLinked(o1);
						Biosample l2 = getLinker().getLinked(o2);
						return CompareUtils.compare(l1==null?"": l1.getSampleId(), l2==null?"": l2.getSampleId());
					}
				});
			}
		});

		if(biotype==null || biotype.getSampleNameLabel()!=null ) {
			popupMenu.add(new AbstractAction("Sort by " + (biotype==null?"SampleName": biotype.getSampleNameLabel())) {
				@Override
				public void actionPerformed(ActionEvent e) {
					table.sortBy(SampleIdColumn.this, 2, new Comparator<Biosample>() {
						@Override
						public int compare(Biosample o1, Biosample o2) {
							Biosample l1 = getLinker().getLinked(o1);
							Biosample l2 = getLinker().getLinked(o2);
							return CompareUtils.compare(l1==null?"": l1.getSampleName(), l2==null?"": l2.getSampleName());
						}
					});

				}
			});
		}
	}


	@Override
	public JComponent getCellComponent(AbstractExtendTable<Biosample> table, Biosample row, int rowNo, Object value) {
		row = linker.getLinked(row); //Only one sampleid
		sampleIdLabel.setBiosample(row);

		return sampleIdLabel;
	}

	@Override
	public void postProcess(AbstractExtendTable<Biosample> table, Biosample row, int rowNo, Object value, JComponent comp) {
		super.postProcess(table, row, rowNo, value, comp);

		row = linker.getLinked(row);

		if(table.isEditable() && !linker.isLinked()) {
			comp.setBackground(LF.BGCOLOR_REQUIRED);
		}
	}

	@Override
	public TableCellEditor getCellEditor(AbstractExtendTable<Biosample> table) {
		return null;
	}

}