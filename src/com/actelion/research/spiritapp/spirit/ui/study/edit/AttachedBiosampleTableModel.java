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

package com.actelion.research.spiritapp.spirit.ui.study.edit;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.table.TableCellEditor;

import com.actelion.research.spiritapp.spirit.ui.biosample.SampleIdLabel;
import com.actelion.research.spiritapp.spirit.ui.biosample.editor.BiosampleCellEditor;
import com.actelion.research.spiritapp.spirit.ui.biosample.editor.GroupCellEditor;
import com.actelion.research.spiritapp.spirit.ui.study.GroupLabel;
import com.actelion.research.spiritapp.spirit.ui.study.edit.AttachedBiosampleTable.CageCellEditor;
import com.actelion.research.spiritcore.adapter.DBAdapter;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.BiosampleLinker;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.biosample.BiotypeCategory;
import com.actelion.research.spiritcore.business.biosample.BiotypeMetadata;
import com.actelion.research.spiritcore.business.biosample.BiosampleLinker.LinkerType;
import com.actelion.research.spiritcore.business.study.AttachedBiosample;
import com.actelion.research.spiritcore.business.study.Group;
import com.actelion.research.spiritcore.business.study.Phase;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.dao.DAOBiosample;
import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.exceltable.AbstractExtendTable;
import com.actelion.research.util.ui.exceltable.Column;
import com.actelion.research.util.ui.exceltable.ExcelTableModel;

public class AttachedBiosampleTableModel extends ExcelTableModel<AttachedBiosample> {
	
	public static enum Mode {
		RND_WEIGHING,
		RND_SETGROUPS,
		RND_SETCAGES,
		RND_SUMMARY,
		MANUALASSIGN
	}

	
	/** Biotype to link metadata (can be null) */
	private Biotype biotype;	

	private final Mode mode;

	private Study study;
	private Phase phase;
	private Group group;
	private int nData;
	
	
	public static class InitialNoColumn extends Column<AttachedBiosample, Integer> {
		private final boolean editable;
		public InitialNoColumn(boolean editable) {
			super("Tmp\nNo.", Integer.class, 35, 35);			
			this.editable = editable;
		}
		@Override
		public String getToolTipText() {
			return "Temporary No";
		}
		@Override
		public Integer getValue(AttachedBiosample row) {
			return row.getNo();
		}
		@Override
		public void setValue(AttachedBiosample row, Integer value) {
			row.setNo(value);
		}
		@Override
		public void paste(AttachedBiosample row, String value) throws Exception {
			setValue(row, value==null? null: Integer.parseInt(value));
		}
		@Override
		public boolean isEditable(AttachedBiosample row) {return editable;}
		
	}
	
	public class SampleIdColumn extends Column<AttachedBiosample, String> {
		private SampleIdLabel sampleIdLabel = new SampleIdLabel();		
		private final boolean editable;
		
		public SampleIdColumn(boolean editable) {
			super((biotype==null?"":biotype.getName()) + "\nId", String.class, 120, 120);
			this.editable = editable;
		}
		@Override
		public String getValue(AttachedBiosample row) {
			return row.getSampleId();
		}
		@Override
		public void setValue(AttachedBiosample row, String value) {
			if(value==null || value.length()==0) {
				row.setBiosample(null);
				row.setSampleId(null);
				return;
			}
			//Load biosample from db if possible
			Biosample b = DAOBiosample.getBiosample(value);
			
			if(b!=null) {
				
				//check the biotype
				if(biotype!=null && b.getBiotype()!=null && !biotype.equals(b.getBiotype())) {
					biotype = null;
					initColumns();
				}
				
				//If it exists, replace it and populate the name				
				row.setSampleName(b.getSampleName());				
			} else {			
				//If it does not exist, create a new one
				b = new Biosample(biotype, value);
			}
			
			row.setBiosample(b);
			row.setSampleId(b.getSampleId());

			//The populate the rest of the data from external DB 
			try {			
				DBAdapter.getAdapter().populateFromExternalDB(b);
			} catch (Exception e) {
				e.printStackTrace();
			}			
		}
		
		@Override
		public boolean isEditable(AttachedBiosample row) {return editable;}
		
		@Override
		public JComponent getCellComponent(AbstractExtendTable<AttachedBiosample> table, AttachedBiosample row, int rowNo, Object value) {
			if(row!=null) sampleIdLabel.setBiosample(row.getBiosample());				
			return sampleIdLabel;
		}
		
		@Override
		public TableCellEditor getCellEditor(AbstractExtendTable<AttachedBiosample> table) {
			AttachedBiosampleIdCellEditor editor = new AttachedBiosampleIdCellEditor(AttachedBiosampleTableModel.this);
			return editor;
		}
		
		@Override
		public void postProcess(AbstractExtendTable<AttachedBiosample> table, AttachedBiosample row, int rowNo, Object value, JComponent comp) {
			if(row.isSkipRando()) {
				comp.setBackground(Color.PINK);
			}
		}
		
		@Override
		public void populateHeaderPopup(final AbstractExtendTable<AttachedBiosample> table, JPopupMenu popupMenu) {
			super.populateHeaderPopup(table, popupMenu);
			popupMenu.add(new AbstractAction("Sort by Group") {
				@Override
				public void actionPerformed(ActionEvent e) {
					Collections.sort(table.getRows(), new Comparator<AttachedBiosample>() {
						@Override
						public int compare(AttachedBiosample o1, AttachedBiosample o2) {
							return Biosample.COMPARATOR_GROUP_SAMPLENAME.compare(o1.getBiosample(), o2.getBiosample());
						}
					});
				}
			});
		}
	}
	
	public class SampleNameColumn extends Column<AttachedBiosample, String> {
		public SampleNameColumn() {
			super((biotype==null?"":biotype.getName()) + "\n" + (biotype==null || biotype.getSampleNameLabel()==null? "": biotype.getSampleNameLabel()), String.class, 30, 100);
		}
		@Override
		public String getValue(AttachedBiosample row) {
			return row.getSampleName();
		}
		@Override
		public void setValue(AttachedBiosample row, String value) {
			row.setSampleName(value);
		}
		@Override
		public boolean isEditable(AttachedBiosample row) {
			return biotype!=null && biotype.getSampleNameLabel()!=null && biotype.getSampleNameLabel().length()>0;
		}
		
	}
	
	public class WeightColumn extends Column<AttachedBiosample, Double> {
		private final boolean editable;
		public WeightColumn(boolean editable) {
			super((phase==null?"": phase.getShortName())+ "\nWeight[g]", Double.class, 50, 50);		
			this.editable = editable;
		}
		@Override
		public Double getValue(AttachedBiosample row) {
			return row.getWeight();
		}
		@Override
		public void setValue(AttachedBiosample row, Double value) {
			row.setWeight(value);
		}
		@Override
		public boolean isEditable(AttachedBiosample row) {return editable;}
		
		@Override
		public boolean shouldMerge(AttachedBiosample r1, AttachedBiosample r2) {
			return false;
		}
	}
	
	public class DataColumn extends Column<AttachedBiosample, Double> {
		private final boolean editable;
		private final int index;
		public DataColumn(int index, boolean editable) {			
			super("\nData"+(index+1), Double.class, 50, 50);
			this.index = index;
			this.editable = editable;
		}
		@Override
		public Double getValue(AttachedBiosample row) {
			return index>=0 && index<row.getDataList().size()? row.getDataList().get(index): null;
		}
		@Override
		public void setValue(AttachedBiosample row, Double value) {
			while(row.getDataList().size()<index) row.getDataList().add(null);
			row.getDataList().set(index, value);
		}
		@Override
		public boolean isEditable(AttachedBiosample row) {return editable;}
	}
	
	public class NewGroupColumn extends Column<AttachedBiosample, Group> {
		public NewGroupColumn() {
			super("StudyGroup\n", Group.class, 50, 110);
		}
		@Override
		public Group getValue(AttachedBiosample row) {			
			return row.getGroup();
		}
		
		@Override
		public boolean isEditable(AttachedBiosample row) {return true;}
		
		@Override
		public void setValue(AttachedBiosample row, Group value) {
			row.setGroup(value);
		}
		
		@Override
		public void paste(AttachedBiosample row, String value) throws Exception {
			if(value==null || value.length()==0) {
				setValue(row, null);
			} else {
				Set<Group> possibleGroups = study==null? new HashSet<Group>(): study.getGroups();
				for (Group group : possibleGroups) {
					if(value.equals(group.getName()) || value.equals(group.getShortName())) {
						setValue(row, group);
						return;
					} 
				}
			}
		}
		
		private GroupLabel groupLabel = new GroupLabel();
		@Override
		public JComponent getCellComponent(AbstractExtendTable<AttachedBiosample> table, AttachedBiosample row, int rowNo, Object value) {
			groupLabel.setGroup((Group)value);
			return groupLabel;
		}
		@Override
		public TableCellEditor getCellEditor(AbstractExtendTable<AttachedBiosample> table) {
			return new GroupCellEditor() {
				@Override
				public Study getStudy(int row) {
					return study;
				}
			};
		}
	}
	

	public final class SubgroupColumn extends Column<AttachedBiosample, Integer> {
		public SubgroupColumn() {
			super("StudyGroup\nSt.", Integer.class, 30, 30);	
			groupLabel.setFont(FastFont.SMALL);
		}
		@Override
		public Integer getValue(AttachedBiosample row) {			
			return row.getGroup()==null || row.getGroup().getNSubgroups()<=1? null: row.getSubGroup()+1;
		}
		@Override
		public void setValue(AttachedBiosample row, Integer value) {
			row.setSubGroup(value==null || value<=0 || value>row.getGroup().getNSubgroups()? 0: value-1);
			fireTableDataChanged();
		}
		@Override
		public boolean isEditable(AttachedBiosample row) {
			return row!=null && row.getGroup()!=null && row.getGroup().getNSubgroups()>1;
		}
		
		private GroupLabel groupLabel = new GroupLabel();
		@Override
		public JComponent getCellComponent(AbstractExtendTable<AttachedBiosample> table, AttachedBiosample row, int rowNo, Object value) {
			groupLabel.setText(value==null? null: "'"+value, row.getGroup());
			return groupLabel;
		}		
	}
	
	
	public static class FormerContainerColumn extends Column<AttachedBiosample, String> {
		public FormerContainerColumn() {
			super("ContainerId\n(origin)", String.class, 68, 68);
		}
		@Override
		public String getValue(AttachedBiosample row) {
			if(row.getBiosample()==null) return null;
			return row.getBiosample().getContainerId();
		}
		@Override
		public boolean isEditable(AttachedBiosample row) {return false;}
	}
	
	public class ContainerIdColumn extends Column<AttachedBiosample, String> {
		public ContainerIdColumn() {
			super("ContainerId\n", String.class, 68, 68);
		}
		@Override
		public String getValue(AttachedBiosample row) {
			return row.getContainerId();
		}
		@Override
		public void setValue(AttachedBiosample row, String value) {
			row.setContainerId(value);
		}
		@Override
		public boolean isEditable(AttachedBiosample row) {
			Biotype type = row.getBiosample().getBiotype()==null? biotype: row.getBiosample().getBiotype();
			return row.getBiosample()!=null && type!=null && !type.isAbstract() && !type.isHideContainer();
		}
		
		@Override
		public void postProcess(com.actelion.research.util.ui.exceltable.AbstractExtendTable<AttachedBiosample> table, AttachedBiosample row, int rowNo, Object value, JComponent comp) {
			comp.setFont(FastFont.BOLD);
		}
		
		@Override
		public TableCellEditor getCellEditor(AbstractExtendTable<AttachedBiosample> table) {
			Biotype type = getBiotype();
			if(type!=null && Biotype.ANIMAL.equals(type.getName())) {
				return new CageCellEditor(study);
			} else {
				return super.getCellEditor(table);
			}
		}
	}	
	
	public static final class TextFieldLinkerColumn extends Column<AttachedBiosample, String> {
		private BiosampleLinker linker; 
		public TextFieldLinkerColumn(BiosampleLinker linker) {
			super(linker.getLabel(), String.class, 50);
			this.linker = linker;			
		}
		@Override
		public String getValue(AttachedBiosample row) {
			String v = linker.getValue(row.getBiosample());
			return v;
		}
		@Override
		public boolean isEditable(AttachedBiosample row) {
			return row.getBiosample()!=null && row.getBiosample().getBiotype()!=null && row.getBiosample().getBiotype().equals(linker.getBiotypeForLabel());
		}
		
		@Override
		public void setValue(AttachedBiosample row, String value) {
			linker.setValue(row.getBiosample(), value);
		}
		
	}
	
	public static final class ParentColumn extends Column<AttachedBiosample, Biosample> {
		private Biotype biotype;
		private boolean top; 
		public ParentColumn(Biotype biotype, boolean top) {
			super(biotype==null? (top?"TopParent\nId":"Parent\nId"): biotype.getName(), Biosample.class, 50);
			this.biotype = biotype;
			this.top = top;
		}
		@Override
		public Biosample getValue(AttachedBiosample row) {
			Biosample v = row.getBiosample()==null? null: top? row.getBiosample().getTopParent(): row.getBiosample().getParent(); 
			return v;
		}
		@Override
		public boolean isEditable(AttachedBiosample row) {
			return row.getBiosample()!=null && !top;
		}
		
		@Override
		public void setValue(AttachedBiosample row, Biosample value) {
			assert !top;
			assert row.getBiosample()!=null;
			row.getBiosample().setParent(value);
		}
		
		private SampleIdLabel label = new SampleIdLabel(true, true);
		
		@Override
		public JComponent getCellComponent(AbstractExtendTable<AttachedBiosample> table, AttachedBiosample row, int rowNo, Object value) {
			label.setBiosample((Biosample) value);
			return label;
		}
		@Override
		public TableCellEditor getCellEditor(AbstractExtendTable<AttachedBiosample> table) {
			BiosampleCellEditor editor = new BiosampleCellEditor(biotype);
			return editor;
			
		}
		
	}

	public AttachedBiosampleTableModel(Mode mode, Study study) {
		this(mode, study, null, null, null);
	}
	
	public AttachedBiosampleTableModel(Mode mode, Study study, Group group, Phase phase) {
		this(mode, study, group, phase, null);
	}
	
	public AttachedBiosampleTableModel(Mode mode, Study study, Group group, Phase phase, Biotype biotype) {
		this.mode = mode;
		this.study = study;
		this.group = group;
		this.phase = phase;
		this.biotype = biotype;
		initColumns();
	}
	
	private static Set<BiosampleLinker> getLinkers(Biotype biotype) {
		Set<BiosampleLinker> res = new TreeSet<>();
		if(biotype==null) return res;
		
		for(BiotypeMetadata m: biotype.getMetadata()) {
			res.add(new BiosampleLinker(m));
		}
		res.add(new BiosampleLinker(LinkerType.COMMENTS, biotype));
		return res;
	}
	
	public List<Column<AttachedBiosample, String>> createLinkers(Biotype biotype) {
		List<Column<AttachedBiosample, String>> res = new ArrayList<>();
		for (BiosampleLinker linker : getLinkers(biotype)) {
			res.add(new TextFieldLinkerColumn(linker));
		}		
		return res;
	}	
		
	public void setBiotype(Biotype biotype) throws Exception {
		this.biotype = biotype;
		
		//Check that the conversion is possible
		for (AttachedBiosample ab : getRows()) {
			Biosample b = ab.getBiosample();
			if(b==null) continue;
			if(b.getBiotype()!=null && !b.getBiotype().equals(biotype) && b.getMetadataAsString().length()>0) {
				throw new Exception("You cannot convert " + b.getSampleId() + " because its metadata are already filled. Please delete them first.");
			}
		}
	
		//Convert the biosamples to the corresponding biotype if possible
		for (AttachedBiosample ab : getRows()) {
			Biosample b = ab.getBiosample();
			if(b==null) continue;
			if(b.getBiotype()!=null && b.getBiotype().equals(biotype)) continue;
			if(b.getBiotype()==null && biotype==null) continue;
			b.setBiotype(biotype);
		}
		
		initColumns();
	}
	

	public void setGroup(Group group) {
		this.group = group;
	}

	/**
	 * @return the group
	 */
	public Group getGroup() {
		return group;
	}
	
	public Study getStudy() {
		return study;
	}

	
	@Override
	public AttachedBiosample createRecord() {
		return new AttachedBiosample();
	}

	public void setNData(int nData) {
		this.nData = nData;
	}
	
	public int getNData() {
		return nData;
	}
	
	
	/**
	 * Return the specified biotype of the model, or guess from the biosample (if all have the same biotype)
	 * @return
	 */
	public Biotype getBiotype() {
		Biotype myBiotype = biotype;
		if(myBiotype==null) {
			for (AttachedBiosample s : getRows()) {
				if(s.getBiosample()!=null && s.getBiosample().getBiotype()!=null) {
					if(myBiotype==null) {
						myBiotype = s.getBiosample().getBiotype();
					} else if(!myBiotype.equals(s.getBiosample().getBiotype())) {
						return null;
					}
				}
			}
		}
		return myBiotype;
	}
	
	
	@Override
	public void setRows(List<AttachedBiosample> rows) {
		
		//Remove the biotype if samples don't match (or throw an exception?)
		if(biotype!=null) {
			for (AttachedBiosample ab : rows) {
				if(ab.getBiosample()!=null && ab.getBiosample().getBiotype()!=null && !ab.getBiosample().getBiotype().equals(biotype)) {
					biotype = null;
					initColumns();
					break;
				}
			}
		}
		super.setRows(rows);
		
	}
	/**
	 * To be called after setting the rows or one of the fields (ndata,phase)
	 */
	public void initColumns() {
		//Check if we have former group/cage
		boolean hasFormerContainerId = false;
		boolean hasWeights = false;
		int maxIndex = 0;
		Biotype myBiotype = getBiotype();
		for (AttachedBiosample s : getRows()) {
			if(s.getBiosample()!=null) {
				if(s.getBiosample().getContainerId()!=null && s.getBiosample().getContainerId().length()>0) hasFormerContainerId = true;
			}
			if(s.getWeight()!=null) hasWeights = true;
			
			maxIndex = Math.max(maxIndex, s.getDataList().size());
		}
		
		List<Column<AttachedBiosample, ?>> columns = new ArrayList<Column<AttachedBiosample,?>>();
		columns.add(COLUMN_ROWNO);
		
		
		
			
		if(mode==Mode.MANUALASSIGN || mode==Mode.RND_SUMMARY) {
			if(mode==Mode.RND_SUMMARY) {
				columns.add(new InitialNoColumn(false));
			}
			
			//Container
			if(myBiotype!=null && !myBiotype.isAbstract() && !myBiotype.isHideContainer()) {
				if(mode==Mode.RND_SUMMARY && hasFormerContainerId) {
					columns.add(new AttachedBiosampleTableModel.FormerContainerColumn());
				}
				columns.add(new ContainerIdColumn());				
			}
			
			//Study
			columns.add(new NewGroupColumn());
			columns.add(new SubgroupColumn());

			//Parent
			if(myBiotype!=null && (myBiotype.getCategory()==BiotypeCategory.PURIFIED || myBiotype.getCategory()==BiotypeCategory.LIQUID || myBiotype.getCategory()==BiotypeCategory.SOLID)) {
				if(myBiotype.getParent()!=null && !myBiotype.getParent().equals(myBiotype.getTopParent())) { 
					columns.add(new ParentColumn(myBiotype.getTopParent(), true));
				}
				columns.add(new ParentColumn(myBiotype.getParent(), false));
			}
			
			
			//SampleId/Name
			columns.add(new SampleIdColumn(true));	
			
			if(myBiotype!=null && myBiotype.getSampleNameLabel()!=null) {
				columns.add(new SampleNameColumn());
			}

			//Weight
			if(mode==Mode.RND_SUMMARY && hasWeights) {
				columns.add(new WeightColumn(true));
			}
			
			//Metadata
			columns.addAll(createLinkers(myBiotype));

		}  else { //Weighing, groups, cages			
			columns.add(new InitialNoColumn(mode==Mode.RND_WEIGHING));
			columns.add(new SampleIdColumn(mode==Mode.RND_WEIGHING));
			
			if(mode==Mode.RND_WEIGHING || mode==Mode.RND_SETGROUPS ) {
				for (int i = 0; i < nData; i++) {
					columns.add(new DataColumn(i, mode==Mode.RND_WEIGHING));				
				}
			}
			if(mode==Mode.RND_WEIGHING || mode==Mode.RND_SETGROUPS) {
				columns.add(new WeightColumn(mode==Mode.RND_WEIGHING));
			}
			
			if(mode==Mode.RND_SETCAGES) {
				if(hasFormerContainerId) {
					columns.add(new AttachedBiosampleTableModel.FormerContainerColumn());
				}
				columns.add(new AttachedBiosampleTableModel.ContainerIdColumn());
			}
			if(mode==Mode.RND_WEIGHING) {
				//Metadata
				columns.addAll(createLinkers(myBiotype));
			}
		}
		
		setColumns(columns);
	}

}
