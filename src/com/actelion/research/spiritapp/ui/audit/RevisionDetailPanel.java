/*
 * Spirit, a study/biosample management tool for research.
 * Copyright (C) 2018 Idorsia Pharmaceuticals Ltd., Hegenheimermattweg 91,
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

package com.actelion.research.spiritapp.ui.audit;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.Box;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;

import com.actelion.research.spiritapp.ui.admin.BiotypeDocumentPane;
import com.actelion.research.spiritapp.ui.admin.TestDocumentPane;
import com.actelion.research.spiritapp.ui.admin.database.SpiritPropertyTable;
import com.actelion.research.spiritapp.ui.admin.user.EmployeeGroupTable;
import com.actelion.research.spiritapp.ui.admin.user.EmployeeTable;
import com.actelion.research.spiritapp.ui.biosample.BiosampleTable;
import com.actelion.research.spiritapp.ui.location.LocationTable;
import com.actelion.research.spiritapp.ui.location.depictor.LocationDepictor;
import com.actelion.research.spiritapp.ui.result.ResultActions;
import com.actelion.research.spiritapp.ui.result.ResultTable;
import com.actelion.research.spiritapp.ui.study.StudyDetailPanel;
import com.actelion.research.spiritapp.ui.study.StudyTable;
import com.actelion.research.spiritcore.business.audit.Revision;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.employee.Employee;
import com.actelion.research.spiritcore.business.employee.EmployeeGroup;
import com.actelion.research.spiritcore.business.property.SpiritProperty;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.dao.DAORevision;
import com.actelion.research.util.ui.JCustomTabbedPane;
import com.actelion.research.util.ui.SwingWorkerExtended;

public class RevisionDetailPanel extends JPanel {

	public RevisionDetailPanel() {
		super(new GridLayout());

	}

	public void setRevision(Revision revTmp) {
		if(revTmp==null) {
			removeAll();
			validate();
			repaint();
			return;
		}

		final JTabbedPane detailPanel = new JCustomTabbedPane();
		new SwingWorkerExtended("Loading Revision", this, SwingWorkerExtended.FLAG_ASYNCHRONOUS100MS) {
			@Override
			protected void doInBackground() throws Exception {
				Revision rev = DAORevision.getRevision(revTmp.getRevId());
				if(rev.getTests().size()>0) {
					Box panel = Box.createVerticalBox();
					TestDocumentPane doc = new TestDocumentPane(rev.getTests());
					panel.add(doc);
					detailPanel.addTab(rev.getTests().size()+ " Tests", new JScrollPane(panel));
				}

				if(rev.getBiotypes().size()>0) {
					Box panel = Box.createVerticalBox();
					for (Biotype t : rev.getBiotypes()) {
						BiotypeDocumentPane doc = new BiotypeDocumentPane();
						panel.add(doc);

						doc.setSelection(t);
					}
					detailPanel.addTab(rev.getBiotypes().size()+ " Biotypes", new JScrollPane(panel));
				}

				if(rev.getSpiritProperties().size()>0) {
					List<SpiritProperty> rows = new ArrayList<>(rev.getSpiritProperties());
					final SpiritPropertyTable table = new SpiritPropertyTable();
					detailPanel.addTab(rows.size() + " Properties", new JScrollPane(table));
					Collections.sort(rows);
					table.setRows(rows);
				}

				if(rev.getEmployees().size()>0) {
					List<Employee> rows = new ArrayList<>(rev.getEmployees());
					final EmployeeTable table = new EmployeeTable();
					table.getModel().setCanExpand(false);
					detailPanel.addTab(rows.size() + " Employees", new JScrollPane(table));
					Collections.sort(rows);
					table.setRows(rows);
				}

				if(rev.getEmployeeGroups().size()>0) {
					List<EmployeeGroup> rows = new ArrayList<>(rev.getEmployeeGroups());
					final EmployeeGroupTable table = new EmployeeGroupTable();
					table.getModel().setCanExpand(false);
					detailPanel.addTab(rows.size() + " Employees", new JScrollPane(table));
					Collections.sort(rows);
					table.setRows(rows);
				}


				if(rev.getBiosamples().size()>0) {
					List<Biosample> rows = new ArrayList<>(rev.getBiosamples());
					final BiosampleTable table = new BiosampleTable();
					table.getModel().setRevId(rev.getRevId());
					table.getModel().setCanExpand(false);
					//					table.getModel().showHideable(new LastChangeColumn(rev.getRevId()), true);
					detailPanel.addTab(rows.size() + " Biosample", new JScrollPane(table));
					Collections.sort(rows);
					table.setRows(rows);
				}

				if(rev.getLocations().size()>0) {


					final LocationTable locationTable = new LocationTable();
					final LocationDepictor locationDepictor = new LocationDepictor();

					locationDepictor.setForRevisions(true);
					locationTable.getSelectionModel().addListSelectionListener(e-> {
						if(e.getValueIsAdjusting()) return;
						locationDepictor.setBioLocation(locationTable.getSelection().size()==1? locationTable.getSelection().get(0): null);
					});

					JScrollPane sp = new JScrollPane(locationTable);
					sp.setPreferredSize(new Dimension(400, 180));

					JSplitPane sp2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, sp, locationDepictor);
					sp2.setDividerLocation(200);
					Box panel = Box.createVerticalBox();
					LocationTable table = new LocationTable();
					panel.add(new JScrollPane(table));
					table.setRows(rev.getLocations());
					locationDepictor.setBioLocation(rev.getLocations().size()==1?rev.getLocations().get(0):null);
					detailPanel.addTab(rev.getLocations().size()+ " Locations", sp2);
				}

				if(rev.getResults().size()>0) {
					List<Result> rows = new ArrayList<>(rev.getResults());
					ResultTable table = new ResultTable();
					ResultActions.attachRevisionPopup(table);
					detailPanel.addTab(rows.size() + " Results", new JScrollPane(table));

					Collections.sort(rows);
					table.setRows(rows);
				}

				if(rev.getStudies().size()>0) {
					List<Study> rows = new ArrayList<>(rev.getStudies());
					if(rows.size()==1) {
						rows.get(0).getParticipants();
						final StudyDetailPanel detail = new StudyDetailPanel(JSplitPane.VERTICAL_SPLIT);
						detail.setForRevision(true);
						detailPanel.addTab(rows.get(0).getStudyId(), detail);
						detail.setStudy(rows.get(0));
					} else {
						final StudyTable table = new StudyTable();
						detailPanel.addTab(rows.size()+ " Studies", new JScrollPane(table));
						Collections.sort(rows);
						table.setRows(rows);
					}
				}

			}

			@Override
			protected void done() {
				removeAll();
				add(detailPanel);
				validate();
				repaint();
			}
		};
	}
}
