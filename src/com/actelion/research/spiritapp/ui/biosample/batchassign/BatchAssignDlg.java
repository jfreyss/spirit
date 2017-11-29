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

package com.actelion.research.spiritapp.ui.biosample.batchassign;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import com.actelion.research.spiritapp.Spirit;
import com.actelion.research.spiritapp.print.PrintLabel;
import com.actelion.research.spiritapp.ui.SpiritFrame;
import com.actelion.research.spiritapp.ui.biosample.BiosampleActions;
import com.actelion.research.spiritapp.ui.biosample.BiosampleSearchTree;
import com.actelion.research.spiritapp.ui.biosample.BiosampleTable;
import com.actelion.research.spiritapp.ui.biosample.BiosampleTableModel;
import com.actelion.research.spiritapp.ui.biosample.column.CombinedColumn;
import com.actelion.research.spiritapp.ui.biosample.column.ContainerFullColumn;
import com.actelion.research.spiritapp.ui.biosample.column.StudyGroupColumn;
import com.actelion.research.spiritapp.ui.biosample.column.StudyIdColumn;
import com.actelion.research.spiritapp.ui.biosample.column.StudyParticipantIdColumn;
import com.actelion.research.spiritapp.ui.biosample.column.StudyPhaseColumn;
import com.actelion.research.spiritapp.ui.biosample.column.StudySubGroupColumn;
import com.actelion.research.spiritapp.ui.biosample.linker.LinkerColumnFactory;
import com.actelion.research.spiritapp.ui.biosample.linker.SampleIdColumn;
import com.actelion.research.spiritapp.ui.location.DirectionCombobox;
import com.actelion.research.spiritapp.ui.print.BrotherLabelsDlg;
import com.actelion.research.spiritapp.ui.util.ISpiritChangeObserver;
import com.actelion.research.spiritapp.ui.util.SpiritChangeListener;
import com.actelion.research.spiritapp.ui.util.SpiritChangeType;
import com.actelion.research.spiritapp.ui.util.component.JSpiritEscapeDialog;
import com.actelion.research.spiritapp.ui.util.lf.LF;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.BiosampleLinker;
import com.actelion.research.spiritcore.business.biosample.BiosampleLinker.LinkerType;
import com.actelion.research.spiritcore.business.biosample.BiosampleQuery;
import com.actelion.research.spiritcore.business.biosample.Container;
import com.actelion.research.spiritcore.business.biosample.ContainerType;
import com.actelion.research.spiritcore.business.location.Direction;
import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.spiritcore.services.dao.DAOBiosample;
import com.actelion.research.spiritcore.util.Pair;
import com.actelion.research.spiritcore.util.Triple;
import com.actelion.research.util.ui.JCustomLabel;
import com.actelion.research.util.ui.JEscapeDialog;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.SwingWorkerExtended;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.exceltable.AbstractExtendTable;
import com.actelion.research.util.ui.exceltable.Column;
import com.actelion.research.util.ui.exceltable.JSplitPaneWithZeroSizeDivider;
import com.actelion.research.util.ui.iconbutton.IconType;
import com.actelion.research.util.ui.iconbutton.JIconButton;
import com.itextpdf.text.Font;

public class BatchAssignDlg extends JSpiritEscapeDialog implements ISpiritChangeObserver {

	public static class RackPos extends Pair<Integer, String> {
		public RackPos(RackPosTubeId pos) {
			super(pos.getFirst(), pos.getSecond());
		}
		public RackPos(Integer rackNo, String pos) {
			super(rackNo, pos);
		}
	}

	public static class RackPosTubeId extends Triple<Integer, String, String> {
		public RackPosTubeId(Integer rackNo, String pos, String tubeId) {
			super(rackNo, pos, tubeId);
		}
	}

	public static final String AUX_NEEDED_RACKNO = "RackNo";
	public static final String AUX_RACKPOS = "RackPos";


	//Query Panel
	private BiosampleQuery query = new BiosampleQuery();
	private BiosampleTable table = new BiosampleTable();
	private JScrollPane tableScrollPane = new JScrollPane(table);
	private JLabel queryLabel = new JCustomLabel("", Font.BOLD);

	//Assignement
	private JCheckBox autoCheckBox = new JCheckBox("Automatic Assignment", true);
	private JButton rack0Button = new JButton("None");
	private JButton rack1Button = new JButton("1");
	private JButton rack2Button = new JButton("2");
	private JButton rack3Button = new JButton("3");
	private JButton rack4Button = new JButton("4");
	private JPanel manualBox = UIUtils.createHorizontalBox(new JLabel("Move Selected Rows to Rack: "), rack0Button, rack1Button, rack2Button, rack3Button, rack4Button);

	private JButton saveButton = new JIconButton(IconType.SAVE, "Save");
	private DirectionCombobox directionComboBox = new DirectionCombobox(false);

	private BatchAssignRackPanel[] rackPanels = new BatchAssignRackPanel[] {
			new BatchAssignRackPanel(this, 0),
			new BatchAssignRackPanel(this, 1),
			new BatchAssignRackPanel(this, 2),
			new BatchAssignRackPanel(this, 3)};
	private int push = 0;
	private List<RackPosTubeId> positions = new ArrayList<>();
	private Map<RackPos, Biosample> mapBiosamples = new HashMap<>();




	public String getError(Biosample row) {
		if(row==null) return null;
		if(row.getContainerId()!=null) return row.getSampleId() + " is already assigned to "+row.getContainerId();


		RackPosTubeId scanned = (RackPosTubeId) row.getAuxiliaryInfos().get(AUX_RACKPOS);
		if(scanned==null) return null;
		int rackNo = scanned.getFirst();
		BatchAssignRackPanel rp = rackNo>=0 && rackNo<rackPanels.length? rackPanels[rackNo]: null;
		boolean hasWrongType = row.getContainerType()!=null && row.getContainerType()!=ContainerType.UNKNOWN && rp!=null && rp.getContainerType()!=null  && !row.getContainerType().equals(rp.getContainerType());
		if(hasWrongType) return row.getSampleId() + " has a wrong containerType";

		return null;
	}


	private Column<Biosample, String> COLUMN_RACKNO = new Column<Biosample, String>("To\nRack", String.class, 40) {
		@Override
		public String getValue(Biosample row) {
			if(autoCheckBox.isSelected()) {
				RackPosTubeId rp = (RackPosTubeId) row.getAuxiliaryInfos().get(AUX_RACKPOS);
				return rp==null? null: "Rack" + (rp.getFirst()+1);
			} else {
				Integer rackNo = (Integer) row.getAuxiliaryInfos().get(AUX_NEEDED_RACKNO);
				return rackNo==null? null: "Rack" + (rackNo+1);
			}
		}
		@Override
		public void postProcess(AbstractExtendTable<Biosample> table, Biosample row, int rowNo, Object value, JComponent comp) {
			RackPosTubeId rp = (RackPosTubeId) row.getAuxiliaryInfos().get(AUX_RACKPOS);
			comp.setForeground(getForeground(rp==null?-1: rp.getFirst()));
			comp.setFont(comp.getFont().deriveFont(Font.BOLD));
			comp.setBackground(getError(row)!=null?LF.COLOR_ERROR_BACKGROUND: new Color(240, 240, 255));
		}
		@Override
		public String getToolTipText(Biosample row) {return getError(row);}
	};

	private Column<Biosample, String> COLUMN_RACKPOSITION = new Column<Biosample, String>("To\nPos.", String.class, 35) {
		@Override
		public String getValue(Biosample row) {
			RackPosTubeId rp = (RackPosTubeId) row.getAuxiliaryInfos().get(AUX_RACKPOS);
			return rp==null? null: rp.getSecond();
		}
		@Override
		public void postProcess(AbstractExtendTable<Biosample> table, Biosample row, int rowNo, Object value, JComponent comp) {
			RackPosTubeId rp = (RackPosTubeId) row.getAuxiliaryInfos().get(AUX_RACKPOS);
			comp.setForeground(getForeground(rp==null?-1: rp.getFirst()));
			comp.setBackground(getError(row)!=null?LF.COLOR_ERROR_BACKGROUND:new Color(240, 240, 255));
			comp.setFont(comp.getFont().deriveFont(Font.BOLD));
		}
		@Override
		public String getToolTipText(Biosample row) {return getError(row);}
	};
	private Column<Biosample, String> COLUMN_TUBEID = new Column<Biosample, String>("To\nTubeId", String.class, 80) {
		@Override
		public String getValue(Biosample row) {
			RackPosTubeId rp = (RackPosTubeId) row.getAuxiliaryInfos().get(AUX_RACKPOS);
			return rp==null? null: rp.getThird();
		}
		@Override
		public void postProcess(AbstractExtendTable<Biosample> table, Biosample row, int rowNo, Object value, JComponent comp) {
			RackPosTubeId rp = (RackPosTubeId) row.getAuxiliaryInfos().get(AUX_RACKPOS);
			comp.setForeground(getForeground(rp==null?-1: rp.getFirst()));

			comp.setBackground(getError(row)!=null?LF.COLOR_ERROR_BACKGROUND:new Color(240, 240, 255));
			comp.setFont(comp.getFont().deriveFont(Font.BOLD));
		}
		@Override
		public String getToolTipText(Biosample row) {return getError(row);}
	};

	/**
	 * Constructor of the Dialog
	 */
	public BatchAssignDlg() {
		super(UIUtils.getMainFrame(), "Batch Assign Tubes", BatchAssignDlg.class.getName());
		try {
			Spirit.askForAuthentication();
		} catch(Exception e) {
			return;
		}

		autoCheckBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				eventAutoButton();
			}
		});
		eventAutoButton();

		JButton printButton = new JIconButton(IconType.PRINT, "Print Rack Labels");
		printButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					List<PrintLabel> labels = new ArrayList<PrintLabel>();
					for (int i = 0; i < rackPanels.length; i++) {
						String name = rackPanels[i].getRackLabel();
						if(name.length()>0) {
							labels.add(new PrintLabel(name));
						}
					}

					new BrotherLabelsDlg(labels);

				} catch(Exception ex) {
					JExceptionDialog.showError(ex);
				}

			}
		});
		rack0Button.addActionListener(new Action_ToRack(-1));
		rack1Button.addActionListener(new Action_ToRack(0));
		rack2Button.addActionListener(new Action_ToRack(1));
		rack3Button.addActionListener(new Action_ToRack(2));
		rack4Button.addActionListener(new Action_ToRack(3));


		//QueryPanel
		JButton queryButton = new JIconButton(IconType.SEARCH, "Set Query");
		queryButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				editQuery();
			}
		});
		JPanel queryPanel = UIUtils.createVerticalBox(
				UIUtils.createHorizontalBox(queryButton, queryLabel, Box.createHorizontalGlue()),
				UIUtils.createHorizontalBox(autoCheckBox, Box.createHorizontalGlue()),
				UIUtils.createHorizontalBox(manualBox, Box.createHorizontalGlue())
				);
		queryPanel.setBorder(BorderFactory.createEtchedBorder());

		//BiosampleTable
		BiosampleActions.attachPopup(table);
		table.setSmartColumns(false);
		setBiosamples(null);
		table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if(push>0) return;
				try {
					push++;
					List<Biosample> sel = table.getSelection();
					for(int i=0; i<rackPanels.length; i++) {
						List<Integer> list = new ArrayList<Integer>();
						for (Biosample b : sel) {
							RackPosTubeId p = (RackPosTubeId) b.getAuxiliaryInfos().get(AUX_RACKPOS);
							if(p==null || p.getFirst()!=i) continue;
							try{
								list.add(rackPanels[i].getBiolocation().parsePosition(p.getSecond()));
							} catch(Exception ex) {
								JExceptionDialog.showError(ex);
							}
						}
						rackPanels[i].setSelectedRackPos(list);
					}
				} finally {
					push--;
				}

			}
		});

		table.getModel().addTableModelListener(new TableModelListener() {
			private int push = 0;
			@Override
			public void tableChanged(TableModelEvent e) {
				if(push>0) return;

				try {
					push++;
					if(autoCheckBox.isSelected()) {
						assignPositions();
					}
				} finally {
					push--;
				}
			}
		});

		//ScanPanel
		directionComboBox.addActionListener(e-> {
			assignPositions();
		});

		saveButton.addActionListener(e-> {
			try {
				eventSave();
			} catch(Exception ex) {
				JExceptionDialog.showError(BatchAssignDlg.this, ex);
			}
		});

		JPanel centerPanel = new JPanel(new GridLayout(4, 1));
		for (BatchAssignRackPanel rackPanel : rackPanels) {
			centerPanel.add(rackPanel);
		}

		//ContentPanel
		JSplitPane contentPane = new JSplitPaneWithZeroSizeDivider(JSplitPane.HORIZONTAL_SPLIT,
				UIUtils.createBox(tableScrollPane, queryPanel),
				UIUtils.createBox(new JScrollPane(centerPanel),
						UIUtils.createHorizontalBox(new JLabel("Direction: "), directionComboBox, Box.createHorizontalGlue()),
						UIUtils.createHorizontalBox(Box.createHorizontalGlue(), printButton, saveButton)));
		contentPane.setDividerLocation(getWidth()-450);
		setContentPane(contentPane);

		UIUtils.adaptSize(this, 1400, 900);
		SpiritChangeListener.register(this);
	}

	private void setBiosamples(List<Biosample> biosamples) {
		if(biosamples==null) biosamples = new ArrayList<>();


		table.clear();

		BiosampleTableModel model = table.getModel();
		List<Column<Biosample, ?>> columns = new ArrayList<>();
		columns.add(model.COLUMN_ROWNO);
		columns.add(COLUMN_RACKNO);
		columns.add(COLUMN_RACKPOSITION);
		columns.add(COLUMN_TUBEID);
		columns.add(new ContainerFullColumn());
		columns.add(new StudyIdColumn());
		columns.add(new StudyGroupColumn());
		columns.add(new StudySubGroupColumn());
		columns.add(new StudyPhaseColumn());
		columns.add(new StudyParticipantIdColumn());
		columns.add(new SampleIdColumn());
		columns.add(new CombinedColumn());
		columns.add(LinkerColumnFactory.create(new BiosampleLinker(LinkerType.COMMENTS, null)));

		model.setColumns(columns);
		table.setRows(biosamples);

		assignPositions();

		model.fireTableDataChanged();
		table.resetPreferredColumnWidth();
	}

	private void eventAutoButton() {
		manualBox.setVisible(!autoCheckBox.isSelected());
		assignPositions();
	}

	protected void assignPositions() {

		//Prepare the list of positions
		positions.clear();


		//Enumerate the scanned tube using the selected direction
		Direction dir = directionComboBox.getSelection();
		for (int i = 0; i < rackPanels.length; i++) {
			for (RackPosTubeId c : rackPanels[i].getOrderedPositions(dir)) {
				positions.add(c);
			}
		}

		//Assign the biosamples to those positions
		List<Biosample> biosamples = table.getRows();
		mapBiosamples.clear();
		if(autoCheckBox.isSelected()) {
			int used = 0;
			for (int i = 0; i < biosamples.size(); i++) {
				Biosample b = biosamples.get(i);
				if(used>=positions.size()) {
					b.getAuxiliaryInfos().put(AUX_RACKPOS, null);
				} else {
					RackPosTubeId scanned = positions.get(used);
					b.getAuxiliaryInfos().put(AUX_RACKPOS, scanned);
					mapBiosamples.put(new RackPos(scanned.getFirst(), scanned.getSecond()), b);
					used++;
				}
			}
		} else {
			int[] indexes = new int[rackPanels.length];
			for (int i = 0; i < biosamples.size(); i++) {
				Biosample b = biosamples.get(i);
				Integer rackNo = (Integer) b.getAuxiliaryInfos().get(AUX_NEEDED_RACKNO);
				RackPosTubeId rackPos;
				if(rackNo==null || rackNo<0 || rackNo>=indexes.length) {
					rackPos = null;
				} else {
					while(indexes[rackNo]<positions.size() && positions.get(indexes[rackNo]).getFirst()!=rackNo) {
						indexes[rackNo]++;
					}
					rackPos = indexes[rackNo]<positions.size()? positions.get(indexes[rackNo]): null;
					indexes[rackNo]++;
				}
				if(rackPos==null) {
					b.getAuxiliaryInfos().remove(AUX_RACKPOS);
				} else {
					b.getAuxiliaryInfos().put(AUX_RACKPOS, rackPos);
					mapBiosamples.put(new RackPos(rackPos.getFirst(), rackPos.getSecond()), b);
				}
			}
		}

		for (int i = 0; i < rackPanels.length; i++) {
			rackPanels[i].refreshData();
		}
		table.getModel().fireTableDataChanged();
		repaint();

	}


	protected Biosample getBiosample(int rackNo, int row, int col) {
		return mapBiosamples.get(new RackPos(rackNo, rackPanels[rackNo].getBiolocation().formatPosition(row, col)));
	}

	private void editQuery() {
		final BiosampleSearchTree searchTree = new BiosampleSearchTree(null);
		searchTree.setQuery(query);

		final JDialog dlg = new JEscapeDialog(this, "Edit Query");
		JButton okButton = new JIconButton(IconType.SEARCH, "Query");
		okButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				query.copyFrom(searchTree.getQuery());
				queryLabel.setText(query.getSuggestedQueryName());
				dlg.dispose();
				searchQuery();
			}
		});

		JPanel contentPane = new JPanel(new BorderLayout());
		contentPane.add(BorderLayout.CENTER, new JScrollPane(searchTree));
		contentPane.add(BorderLayout.SOUTH, UIUtils.createHorizontalBox(Box.createHorizontalGlue(), okButton));

		dlg.getRootPane().setDefaultButton(okButton);
		dlg.setContentPane(contentPane);
		dlg.setSize(300, 600);
		dlg.setLocationRelativeTo(tableScrollPane);
		dlg.setVisible(true);
	}

	protected List<Biosample> getBiosamples() {
		return table.getRows();
	}

	protected List<Biosample> getBiosamples(int rackNo) {
		List<Biosample> res = new ArrayList<>();
		for(Biosample b: getBiosamples()) {
			RackPosTubeId rp = (RackPosTubeId) b.getAuxiliaryInfos().get(AUX_RACKPOS);
			if(rp==null || rp.getFirst()!=rackNo) continue;
			res.add(b);
		}
		return res;
	}

	protected void setTableSelection(List<Biosample> biosamples) {
		if(push>0) return;
		try {
			push++;
			table.setSelection(biosamples);
		} finally {
			push--;
		}

	}

	public static Color getForeground(int rackNo) {
		return rackNo==0? new Color(0,0,255):
			rackNo==1? new Color(150,0,150):
				rackNo==2? new Color(0,150,150):
					rackNo==3? new Color(150,150,0):
						Color.BLACK;
	}

	private void searchQuery() {
		if(query.isEmpty()) return;
		new SwingWorkerExtended("Querying", tableScrollPane) {
			private List<Biosample> biosamples;
			private boolean canEdit = true;

			@Override
			protected void doInBackground() throws Exception {
				biosamples = DAOBiosample.queryBiosamples(query, SpiritFrame.getUser());
				Collections.sort(biosamples);
				for (Biosample b : biosamples) {
					if(!SpiritRights.canEdit(b, SpiritFrame.getUser())) {canEdit = false; break;}
				}
			}

			@Override
			protected void done() {
				try {
					setBiosamples(biosamples);
					if(!canEdit) JExceptionDialog.showError(BatchAssignDlg.this, "You don't have write access on some of the samples, so assigning is not possible");
					saveButton.setEnabled(canEdit);
				} catch(Exception e) {
					JExceptionDialog.showError(e);
				}
			}
		};


	}

	private void eventSave() throws Exception {

		List<Biosample> biosamples = getBiosamples();
		List<Biosample> toSave = new ArrayList<>();
		boolean auto = autoCheckBox.isSelected();

		//Check tubes not assigned
		int alreadyAssigned = 0;
		int notAssigned = 0;
		int rackNoButNotAssigned = 0;
		List<String> tubeIds = new ArrayList<>();
		for (Biosample b : biosamples) {
			Integer rackNo = (Integer) b.getAuxiliaryInfos().get(AUX_NEEDED_RACKNO);
			RackPosTubeId rp = (RackPosTubeId) b.getAuxiliaryInfos().get(AUX_RACKPOS);

			if(rp==null || rp.getThird()==null) continue;
			tubeIds.add(rp.getThird());


			boolean hasTubeId = rp.getThird()!=null;
			if(b.getContainerId()!=null && b.getContainerId().length()>0) {
				alreadyAssigned++;
			}

			if(!hasTubeId) {
				notAssigned++;
			} else {
				toSave.add(b);
			}
			if(!auto) {
				if(rackNo!=null && !hasTubeId) rackNoButNotAssigned++;
			}
		}

		List<Container> containers = DAOBiosample.getContainers(tubeIds);
		for (Container c : containers) {
			if(c.getBiosamples().size()>0) {
				throw new Exception("The tube " + c.getContainerId() + " at " + c.getPos() + " is not empty "+c.getBiosamples());
			}
		}


		if(alreadyAssigned>0) {
			throw new Exception(alreadyAssigned+" tubes have already been assigned, ");
		}

		if(rackNoButNotAssigned>0) {
			throw new Exception(rackNoButNotAssigned+" tubes have a rackNo but no tubeId");
		} else if(toSave.size()==0) {
			throw new Exception("There an no assignments to be made! Did you scan the racks?");
		} else if(notAssigned>0) {
			int res = JOptionPane.showConfirmDialog(this, notAssigned+" tubes have no tubeId!\n Do you want to assign the other "+toSave.size()+" valid tubes?", "Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
			if(res!=JOptionPane.YES_OPTION) return;
		}

		//warning if a rack does not have a rackid
		for (Biosample b : toSave) {
			RackPosTubeId scanned = (RackPosTubeId) b.getAuxiliaryInfos().get(AUX_RACKPOS);
			BatchAssignRackPanel rackPanel = rackPanels[scanned.getFirst()];
			if(rackPanel.getBiolocation()==null) {
				int r = JOptionPane.showConfirmDialog(this, "Rackids and locations are not specified. Do you want to continue the assignment without saving the rackIds?", "Assign Location", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
				if(r!=JOptionPane.YES_OPTION) return;
				break;
			}
		}

		//Check that tubes are unique
		Set<String> seen = new HashSet<String>();
		for (Biosample b : toSave) {
			RackPosTubeId scanned = (RackPosTubeId) b.getAuxiliaryInfos().get(AUX_RACKPOS);
			if(seen.contains(scanned.getThird())) {
				throw new Exception("The container Rack"+(scanned.getFirst()+1)+"/"+ scanned.getThird()+" at "+scanned.getSecond()+" is redundant");
			}
			seen.add(scanned.getThird());
		}


		//Set the location
		Map<Biosample, Container> oldContainers = new HashMap<>();
		Map<Biosample, Location> oldLocations = new HashMap<>();
		Map<Biosample, Integer> oldPos = new HashMap<>();
		try {

			for (Biosample b : toSave) {
				RackPosTubeId scanned = (RackPosTubeId) b.getAuxiliaryInfos().get(AUX_RACKPOS);
				BatchAssignRackPanel rackPanel = rackPanels[scanned.getFirst()];

				oldContainers.put(b, b.getContainer());
				oldLocations.put(b, b.getLocation());
				oldPos.put(b, b.getPos());

				b.setContainerType(rackPanel.getContainerType());
				b.setContainerId(scanned.getThird());
				if(rackPanel.getBiolocation()!=null && rackPanel.getBiolocation().getName()!=null && rackPanel.getBiolocation().getName().length()>0) {
					if(rackPanel.getBiolocation().getLocationType()==null) throw new Exception("rackPanel.getBiolocation().getLocationType()==null");
					System.out.println("BatchAssignDlg.eventSave() "+b+" to "+rackPanel.getBiolocation().getLocationType()+" "+rackPanel.getBiolocation().getName());
					b.setLocPos(rackPanel.getBiolocation(), rackPanel.getBiolocation().parsePosition(scanned.getSecond()));
				} else {
					b.setLocPos(null,-1);
				}
				//
				//
				//				Container c = new Container(rackPanel.getContainerType(), scanned.getThird());
				//				b.setContainer(c);
				//				if(rackPanel.getBiolocation()!=null) {
				//					b.setLocPos(rackPanel.getBiolocation(), rackPanel.getBiolocation().parsePosition(scanned.getSecond()));
				//				} else {
				//					b.setLocPos(null,-1);
				//				}
			}
			DAOBiosample.persistBiosamples(toSave, Spirit.askForAuthentication());

			JExceptionDialog.showInfo(UIUtils.getMainFrame(), toSave.size() + " tubes assigned");

			for(BatchAssignRackPanel rp: rackPanels) {
				rp.clear();
			}

			SpiritChangeListener.fireModelChanged(SpiritChangeType.MODEL_UPDATED, Biosample.class, toSave);

		} catch(Exception e) {
			//Trick to rollback changes without reload
			for (Biosample b : toSave) {
				if(!oldContainers.containsKey(b)) continue;
				b.setContainer(oldContainers.get(b));
				b.setLocPos(oldLocations.get(b), oldPos.get(b));
			}
			throw e;
		}
	}


	private class Action_ToRack extends AbstractAction {
		private int rackNo;
		public Action_ToRack(int rackNo) {
			this.rackNo = rackNo;
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			List<Biosample> biosamples = table.getSelection();
			for (Biosample biosample : biosamples) {
				if(rackNo<0) {
					biosample.getAuxiliaryInfos().remove(AUX_NEEDED_RACKNO);
				} else{
					biosample.getAuxiliaryInfos().put(AUX_NEEDED_RACKNO, rackNo);
				}
			}

			assignPositions();
		}
	}

	@Override
	public <T> void actionModelChanged(SpiritChangeType action, Class<T> what, Collection<T> details) {
		if(what==Biosample.class) {
			table.reload();
		}
	}

	public static void main(String[] args) {
		new BatchAssignDlg();
	}
}
