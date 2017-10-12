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

package com.actelion.research.spiritapp.spirit.ui.biosample;

import java.awt.Component;
import java.awt.GridLayout;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.actelion.research.spiritapp.spirit.ui.location.ContainerActions;
import com.actelion.research.spiritapp.spirit.ui.location.LocationActions;
import com.actelion.research.spiritapp.spirit.ui.location.depictor.RackDepictor;
import com.actelion.research.spiritapp.spirit.ui.location.depictor.RackDepictorListener;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritContextListener;
import com.actelion.research.spiritapp.spirit.ui.util.lf.JBGScrollPane;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Container;
import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.util.ui.JCustomTabbedPane;
import com.actelion.research.util.ui.exceltable.IExportable;

public class BiosampleOrRackTab extends JPanel implements IExportable {

	public static final String PROPERTY_SELECTION = "selection";

	private final JTabbedPane tabbedPane = new JCustomTabbedPane();
	private final BiosampleTable biosampleTable = new BiosampleTable();
	private final RackDepictor rackDepictor = new RackDepictor();
	private int push = 0;
	private int currentTab = -1;

	private final JScrollPane biosampleScrollPane = new JBGScrollPane(biosampleTable, 3);

	public BiosampleOrRackTab() {
		super(new GridLayout(1, 1));


		setTabPaneVisible(false);


		tabbedPane.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				int oldTab = currentTab;
				currentTab = tabbedPane.getSelectedIndex();

				if(oldTab<0 || oldTab==currentTab) return;

				if(tabbedPane.getSelectedIndex()==0) {
					biosampleTable.setSelection(Container.getBiosamples(rackDepictor.getSelectedContainers()));
				} else if(tabbedPane.getSelectedIndex()==1) {
					rackDepictor.setSelectedPoses(Biosample.getScannedPoses(biosampleTable.getSelection(), rackDepictor.getBioLocation()));
				}
			}
		});

		//hide or show the selected biosamples if a selection is made
		rackDepictor.addRackDepictorListener(new RackDepictorListener() {
			@Override
			public void onSelect(Collection<Integer> pos, Container lastSelect, boolean dblClick) {
				biosampleTable.setSelection(Container.getBiosamples(rackDepictor.getSelectedContainers()));
			}
			@Override
			public void locationSelected(final Location location) {

			}
			@Override
			public void onPopup(Collection<Integer> pos, Container lastSelect, Component comp, Point point) {
				Set<Container> containers = rackDepictor.getSelectedContainers();
				if(pos.size()>0) {
					ContainerActions.createPopup(containers).show(comp, point.x, point.y);
				}
			}
			@Override
			public void locationPopup(Location location, Component comp, Point point) {
				LocationActions.createPopup(location).show(comp, point.x, point.y);
			}
		});

		BiosampleActions.attachPopup(biosampleTable);
		BiosampleActions.attachPopup(biosampleScrollPane);


		ListSelectionListener listener = new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if(push>0) return;
				try {
					push++;
					firePropertyChange(PROPERTY_SELECTION, null, "");
				} finally {
					push--;
				}
			}
		};

		biosampleTable.getSelectionModel().addListSelectionListener(listener);
		biosampleTable.getColumnModel().getSelectionModel().addListSelectionListener(listener);

		tabbedPane.setTabPlacement(JTabbedPane.BOTTOM);

	}

	public void setTabPaneVisible(boolean rackViewVisible) {
		removeAll();
		try {
			push++;
			if(rackViewVisible) {

				tabbedPane.removeAll();
				tabbedPane.add("Biosamples", biosampleScrollPane);
				tabbedPane.add("Rack", rackDepictor);
				add(tabbedPane);

			} else {
				add(biosampleScrollPane);
			}
		} finally {
			push--;
		}
		validate();


	}

	public boolean isTabbedPaneVisible() {
		return tabbedPane.getParent()==this;
	}

	public boolean isBiosampleTabSelected() {
		return !isTabbedPaneVisible() || tabbedPane.getSelectedIndex()==0;
	}
	public boolean isRackTabVisible() {
		return tabbedPane.getTabCount()>=2;
	}

	@SuppressWarnings("unchecked")
	public<T> List<T> getSelection(Class<T> claz) {
		List<Biosample> res = biosampleTable.getSelection();
		if(claz==Biosample.class) return (List<T>) res;
		else if(claz==Container.class) return (List<T>) Biosample.getContainers(res, true);
		else throw new IllegalArgumentException("Invalid class");
	}

	public void setRack(Location rack) {

		if(rack==null) {
			clear();
			rackDepictor.setBiolocation(null);
			return;
		}
		List<Biosample> biosamples = new ArrayList<Biosample>(rack.getBiosamples());
		Collections.sort(biosamples, Biosample.COMPARATOR_POS);
		biosampleTable.setRows(biosamples);
		rackDepictor.setBiolocation(rack);

		//Make location visible
		setTabPaneVisible(true);
		tabbedPane.setEnabledAt(1, true);
		tabbedPane.setSelectedIndex(1);
	}

	public Location getRack() {
		return rackDepictor.getBioLocation();
	}

	public void setBiosamples(List<Biosample> biosamples) {
		setTabPaneVisible(false);
		biosampleTable.setRows(biosamples);
		rackDepictor.setBiolocation(null);
	}

	public List<Biosample> getBiosamples() {
		return biosampleTable.getRows();
	}

	public BiosampleTable getBiosampleTable() {
		return biosampleTable;
	}

	public RackDepictor getRackDepictor() {
		return rackDepictor;
	}

	public void linkBiosamplePane(final IBiosampleDetail biosampleDetailPanel) {
		ListSelectionListener listener = e-> {
			if(e.getValueIsAdjusting()) return;
			if(push>0) return;
			try {
				push++;
				List<Biosample> sel = getSelection(Biosample.class);
				SpiritContextListener.setStatus(sel.size()+"/"+getBiosampleTable().getRowCount()+" Biosamples");
				biosampleDetailPanel.setBiosamples(sel);
			} finally {
				push--;
			}
		};
		addListSelectionListener(listener);
	}

	public void addListSelectionListener(final ListSelectionListener listener) {
		biosampleTable.getSelectionModel().addListSelectionListener(listener);
		biosampleTable.getColumnModel().getSelectionModel().addListSelectionListener(listener);

	}

	public void clear() {
		setBiosamples(new ArrayList<Biosample>());
		biosampleTable.getModel().showAllHideable(false);
	}

	public void setSelectedBiosamples(Collection<Biosample> rows) {
		biosampleTable.setSelection(rows);
	}

	//	public void setSelectedRackPos(Collection<RackPos> rows) {
	//		rackDepictor.setSelection(rows);
	//	}


	@Override
	public String[][] getTabDelimitedTable() {
		return biosampleTable.getTabDelimitedTable();
	}


}
