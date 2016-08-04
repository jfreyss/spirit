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

package com.actelion.research.spiritapp.spirit.ui.result.column;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritapp.spirit.ui.biosample.SampleIdLabel;
import com.actelion.research.spiritapp.spirit.ui.result.edit.EditResultTable;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.spiritcore.services.dao.DAOBiosample;
import com.actelion.research.util.CompareUtils;
import com.actelion.research.util.ui.JCustomLabel;
import com.actelion.research.util.ui.exceltable.AbstractExtendTable;
import com.actelion.research.util.ui.exceltable.Column;

public final class SampleIdColumn extends Column<Result, String> {
	private SampleIdLabel sampleIdLabel = new SampleIdLabel();
	private Map<String, Biosample> id2sampleCache = new HashMap<String, Biosample>();
	
	public SampleIdColumn() {
		super("Result\nSampleId", String.class, 80);
	}
	
	@Override
	public float getSortingKey() {
		return 3.0f;
	}
	
	@Override
	public String getValue(Result row) {
		return row.getBiosample()==null? "": row.getBiosample().getSampleId();
	}		
	@Override
	public void setValue(Result row, String value) {
		try {
			paste(row, value);
		} catch(Exception e) {
			
		}		
	}
	
	@Override
	public void paste(Result row, String value) throws Exception {
		if(value==null) {
			row.setBiosample(null);
		} else {
			//Load from id2sampleCache
			Biosample b = id2sampleCache.get(value);
			if(b==null) {
				if(id2sampleCache.size()>1000) id2sampleCache.clear();
				//Load the bisoamples from the sampleId, if the value is longer than 3  
				if(value.length()>3) {
					b = DAOBiosample.getBiosample(value);					
				} 
				if(b==null) {
					//Load the bisoamples from the study/sampleNo, if it is not found
					Study study = ((EditResultTable) getTable()).getModel().getStudy();
					if(study!=null) {
						b = DAOBiosample.getBiosample(study, value);
					}
				}
				
				if(b==null) {
					//If not found, create a fake biosample
					b = new Biosample(value);
				} else {
					//If found, check the rights
					if(SpiritRights.canEdit(b, Spirit.getUser())) {
						//OK
					} else if(b.getInheritedStudy()!=null && SpiritRights.canBlind(b.getInheritedStudy(), Spirit.getUser())) {
						//OK
					} else {						
						throw new Exception("You cannot add results on "+b+" because you have no rights on this sample.\n "+(b.getInheritedStudy()==null?"":" Please contact someone from study "+b.getInheritedStudy()));
					}
				}
				id2sampleCache.put(value, b);
			}
			row.setBiosample(b);
		}
	}
	
	
	@Override
	public JComponent getCellComponent(AbstractExtendTable<Result> table, Result row, int rowNo, Object value) {
		sampleIdLabel.setBiosample(row==null?null: row.getBiosample());
		return sampleIdLabel;
	}
	
	
	

	@Override
	public void populateHeaderPopup(final AbstractExtendTable<Result> table, JPopupMenu popupMenu) {
		popupMenu.add(new JSeparator());
		popupMenu.add(new JCustomLabel("Sort", Font.BOLD));
		
		popupMenu.add(new AbstractAction("Sort by SampleId") {
			@Override
			public void actionPerformed(ActionEvent e) {
				table.sortBy(SampleIdColumn.this, 1, new Comparator<Result>() {
					@Override
					public int compare(Result o1, Result o2) {
						return CompareUtils.compare(o1.getBiosample()==null?"": o1.getBiosample().getSampleId(), o2.getBiosample()==null?"": o2.getBiosample().getSampleId());
					}
				});
			}
		});
		popupMenu.add(new AbstractAction("Sort by SampleName") {
			@Override
			public void actionPerformed(ActionEvent e) {
				table.sortBy(SampleIdColumn.this, 1, new Comparator<Result>() {
					@Override
					public int compare(Result o1, Result o2) {
						return CompareUtils.compare(o1.getBiosample()==null?"": o1.getBiosample().getSampleName(), o2.getBiosample()==null?"": o2.getBiosample().getSampleName());
					}
				});
			}
		});
		popupMenu.add(new AbstractAction("Sort by Group") {
			@Override
			public void actionPerformed(ActionEvent e) {
				table.sortBy(SampleIdColumn.this, 1, new Comparator<Result>() {
					@Override
					public int compare(Result o1, Result o2) {
						return CompareUtils.compare(o1.getBiosample(), o2.getBiosample());
					}
				});
			}
		});

	}
}