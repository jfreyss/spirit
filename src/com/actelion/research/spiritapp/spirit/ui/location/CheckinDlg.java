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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritapp.spirit.ui.location.ContainerTableModel.ContainerTableModelType;
import com.actelion.research.spiritapp.spirit.ui.location.depictor.LocationDepictor;
import com.actelion.research.spiritapp.spirit.ui.location.depictor.RackDepictorListener;
import com.actelion.research.spiritapp.spirit.ui.location.depictor.RackDropListener;
import com.actelion.research.spiritapp.spirit.ui.location.edit.LocationEditDlg;
import com.actelion.research.spiritapp.spirit.ui.util.HelpBinder;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeListener;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeType;
import com.actelion.research.spiritapp.spirit.ui.util.component.JSpiritEscapeDialog;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Container;
import com.actelion.research.spiritcore.business.location.Direction;
import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.spiritcore.business.location.LocationLabeling;
import com.actelion.research.spiritcore.business.location.LocationType;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.spiritcore.services.dao.DAOBiosample;
import com.actelion.research.spiritcore.services.dao.DAOLocation;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.spiritcore.util.Config;
import com.actelion.research.util.ui.JCustomLabel;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.exceltable.JSplitPaneWithZeroSizeDivider;
import com.actelion.research.util.ui.iconbutton.IconType;
import com.actelion.research.util.ui.iconbutton.JIconButton;

public class CheckinDlg extends JSpiritEscapeDialog {

	private final boolean commitTransaction;
	private boolean success = false;

	/** Biosample to set location */
	private Location currentLocation;
	private final ContainerTable containerTable = new ContainerTable(ContainerTableModelType.CHECKIN);

	private LocationBrowser locationBrowser = new LocationBrowser();
	private LocationDepictor locationDepictor = new LocationDepictor();


	private JRadioButton patternRadioButton = new JRadioButton(Direction.PATTERN.getName());
	private JRadioButton rightRadioButton = new JRadioButton(Direction.LEFT_RIGHT.getName());
	private JRadioButton bottomRadioButton = new JRadioButton(Direction.TOP_BOTTOM.getName());

	private JLabel commentsLabel = new JCustomLabel("", Color.RED);
	private final JButton newLocationButton = new JIconButton(IconType.NEW, "New Location");

	private final List<Container> containers;
	private Set<Container> toSave = new HashSet<>();


	private JLabel containerStatusLabel = new JLabel();
	private int push = 0;

	public CheckinDlg(Collection<Biosample> mySamples, boolean commitTransaction) {
		super(UIUtils.getMainFrame(), commitTransaction? "Checkin / Relocate": "Set Location", commitTransaction? CheckinDlg.class.getName(): null);
		this.commitTransaction = commitTransaction;

		List<Biosample> biosamples;
		if(commitTransaction) {
			biosamples = JPAUtil.reattach(mySamples);
		} else {
			biosamples = new ArrayList<>(mySamples);
		}
		this.containers = Biosample.getContainers(biosamples, true);

		//Test that containers are not empty
		if(containers==null || containers.size()==0) {
			JExceptionDialog.showError("You must have some containers");
			return;
		}


		Set<Location> locations = Container.getLocations(containers);
		currentLocation = locations.size()>0? locations.iterator().next(): null;

		if(currentLocation==null) {
			int currentLocId = Config.getInstance(".spirit").getProperty("checkin.location", -1);
			if(currentLocId>0) {
				currentLocation = DAOLocation.getLocation(currentLocId);
			}
		}

		//Configure LocationPane
		locationBrowser.addPropertyChangeListener(LocationBrowser.PROPERTY_LOCATION_SELECTED, evt-> {
			Location location = locationBrowser.getBioLocation();
			locationDepictor.setBioLocation(location);
			eventChangeLocation();
		});

		locationDepictor.setHighlightContainers(containers);
		//		locationDepictor.setDisplayChildren(false);
		locationDepictor.addRackDepictorListener(new RackDepictorListener() {
			@Override
			public void onSelect(Collection<Integer> pos, Container lastSelect, boolean dblClick) {

				//Select the containers from the table (if some of those are selected)
				makePreview();
				if(dblClick) {
					try {
						eventSetPositions();
						commentsLabel.setText("");
					} catch(Exception e) {
						commentsLabel.setText(e.getMessage());
						e.printStackTrace();
					}
				}
			}

			@Override
			public void containerDropped(List<Container> containers, List<Integer> toPos) {
				CheckinDlg.this.containerDropped(containers, toPos);
			}

			@Override
			public boolean acceptDrag() {
				return true;
			}
		});

		containerTable.enableDragSource(false);
		containerTable.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if(push>0) return;
				try {
					push++;
					if(e.getClickCount()>=2) {
						List<Container> sel = containerTable.getSelection();
						if(sel.size()>0) {
							Location l = sel.get(0).getLocation();
							if(l!=null && !l.equals(locationDepictor.getBioLocation())) {
								locationBrowser.setBioLocation(l);
								locationDepictor.setBioLocation(l);
								eventChangeLocation();
							}
						}
					}
					eventTableSelection();
				} finally {
					push--;
				}
			}
		});
		containerTable.getSelectionModel().addListSelectionListener(e-> {
			if(e.getValueIsAdjusting()) return;
			if(push>0) return;
			try {
				push++;
				eventTableSelection();
			} finally {
				push--;
			}
		});


		//newLocationButton
		newLocationButton.addActionListener(e-> {
			Location location = new Location();
			location.setParent(locationBrowser.getBioLocation());
			location.setLocationType(LocationType.RACK);
			location.setCols(12);
			location.setRows(8);
			location.setName("Rack-"+System.currentTimeMillis());
			LocationEditDlg dlg = LocationEditDlg.editInSameTransaction(Collections.singletonList(location));

			List<Location> locs = dlg.getSavedLocations();
			if(locs!=null && locs.size()>0) {
				//link location
				Location loc = locs.get(0);
				//				loc = JPAUtil.reattach(loc);
				locationBrowser.setBioLocation(loc);
				locationDepictor.setBioLocation(loc);
				eventChangeLocation();
			} else {
				//unset the parent
				location.setParent(null);
			}
		});

		//ContentPane Layout
		JPanel containerPanel = new JPanel(new BorderLayout());
		JScrollPane sp1 = new JScrollPane(containerTable);
		containerPanel.add(BorderLayout.CENTER, sp1);
		containerPanel.add(BorderLayout.NORTH, containerStatusLabel);

		if(Container.getScannedPoses(containers).size()>0) {
			patternRadioButton.setSelected(true);
			RackDropListener.setDirection(Direction.PATTERN);
			locationDepictor.setBioLocation(null);
			locationDepictor.computeDroppedPoses(-1, containers);
		} else if(Container.getPoses(containers).size()==containers.size()) {
			patternRadioButton.setSelected(true);
			RackDropListener.setDirection(Direction.PATTERN);
		} else {
			rightRadioButton.setSelected(true);
			RackDropListener.setDirection(Direction.LEFT_RIGHT);
		}

		ButtonGroup group = new ButtonGroup();
		group.add(rightRadioButton);
		group.add(bottomRadioButton);
		group.add(patternRadioButton);

		ActionListener al = e-> {
			RackDropListener.setDirection(rightRadioButton.isSelected()? Direction.LEFT_RIGHT: bottomRadioButton.isSelected()? Direction.TOP_BOTTOM: patternRadioButton.isSelected()? Direction.PATTERN: Direction.DEFAULT);
			makePreview();
		};
		rightRadioButton.addActionListener(al);
		bottomRadioButton.addActionListener(al);
		patternRadioButton.addActionListener(al);

		JPanel moveToPanel = UIUtils.createHorizontalBox(
				rightRadioButton,
				new JLabel(Direction.LEFT_RIGHT.getImage()),
				Box.createHorizontalStrut(5),
				bottomRadioButton,
				new JLabel(Direction.TOP_BOTTOM.getImage()),
				Box.createHorizontalStrut(5),
				patternRadioButton,
				Box.createHorizontalStrut(15),
				commentsLabel,
				Box.createHorizontalGlue()
				);


		//Configure OkButton
		JButton okButton;
		if(commitTransaction) {
			okButton = new JIconButton(IconType.SAVE, "Save");
		} else {
			okButton = new JButton("Close");
		}

		//TopPanel
		//TODO readd newLocationButton
		JPanel locationPanel = UIUtils.createBox(
				UIUtils.createTitleBox("Location", UIUtils.createBox(locationDepictor, UIUtils.createBox(locationBrowser, null, null, null, locationDepictor.createZoomPanel()))),
				UIUtils.createTitleBox("Direction", moveToPanel),
				UIUtils.createHorizontalBox(HelpBinder.createHelpButton(), /*newLocationButton,*/ Box.createHorizontalGlue(), okButton));
		getRootPane().setDefaultButton(okButton);


		okButton.addActionListener(e-> {
			try {
				eventOk();
			} catch (Exception ex) {
				JExceptionDialog.showError(CheckinDlg.this, ex);
			}
		});

		JSplitPane centerPane = new JSplitPaneWithZeroSizeDivider(JSplitPane.HORIZONTAL_SPLIT, UIUtils.createTitleBox("Containers", containerPanel), locationPanel);
		centerPane.setDividerLocation(340);
		setContentPane(centerPane);

		if(currentLocation!=null) {
			locationBrowser.setBioLocation(currentLocation);
			locationDepictor.setBioLocation(currentLocation);
		}


		//Init the container table
		containerTable.setRows(new ArrayList<>(containers));
		containerTable.selectAll();


		eventChangeLocation();

		UIUtils.adaptSize(this, 1200, 760);
		setVisible(true);
	}

	public void eventTableSelection() {
		List<Container> containers = containerTable.getSelection();
		containerStatusLabel.setText("<html><b>" + containers.size()+ " Containers selected</b><br>Drag or Dbl-Click where you want to move them</html>");
		locationDepictor.setSelectedContainers(containers);
		locationDepictor.setHighlightContainers(containerTable.getRows());
		makePreview();
	}

	private void eventChangeLocation() {
		Location location = locationDepictor.getBioLocation();

		if(location==null) {
			bottomRadioButton.setEnabled(false);
			rightRadioButton.setEnabled(false);
			patternRadioButton.setEnabled(false);
			return;
		}

		//Update MoveTo -> direction
		if(location.getLocationType().getPositionType()==LocationLabeling.NONE || location.getLocationType().getPositionType()==null) {
			bottomRadioButton.setEnabled(false);
			rightRadioButton.setEnabled(false);
			patternRadioButton.setEnabled(false);
		} else {
			bottomRadioButton.setEnabled(true);
			rightRadioButton.setEnabled(true);
			patternRadioButton.setEnabled(true);

			//preview if empty rack
			Set<Biosample> present = new HashSet<Biosample>(location.getBiosamples());
			present.removeAll(containerTable.getRows());

		}

		makePreview();
		locationDepictor.validate();
		locationDepictor.repaint();
	}

	/**
	 * Compute where the container would be moved and preview the result
	 */
	private void makePreview() {
		Location location = locationDepictor.getBioLocation();
		if(location==null) return;
		location = JPAUtil.reattach(location);

		Set<Integer> selPoses = locationDepictor.getSelectedPoses();
		List<Container> containers = containerTable.getSelection();
		if(selPoses.size()>0) {
			int min = location.getSize()-1;
			for (Integer p : selPoses) {
				if(p<min) min = p;
			}
			locationDepictor.computeDroppedPoses(min, containers);
		} else {
			locationDepictor.computeDroppedPoses( location.getSize()>0?0:-1, containers);
		}
	}

	private void eventSetPositions() throws Exception {
		Location location = locationDepictor.getBioLocation();
		if(location==null) throw new Exception("You must select a location");


		List<Integer> poses = locationDepictor.getDroppedPoses();
		List<Container> containers = containerTable.getSelection();
		if(location.getLabeling()==LocationLabeling.NONE) {
			for (int i = 0; i < containers.size(); i++) {
				containers.get(i).setLocation(location);
			}
		} else {
			if(containers.size()!=poses.size()) throw new Exception("There is not enough place");
			for (int i = 0; i < containers.size(); i++) {
				containers.get(i).setLocation(location);
				containers.get(i).setPos(poses.get(i));
			}
		}
		toSave.addAll(containers);

		locationDepictor.setSelectedContainers(containers);
		containerTable.repaint();
		locationDepictor.setBioLocation(location);
	}

	private void containerDropped(List<Container> containers, Collection<Integer> toPos) {
		if(locationDepictor.getBioLocation()==null || containers.size()==0) return;
		if(toPos.size()!=containers.size()) return;

		List<Integer> poses = new ArrayList<>(toPos);
		Collections.sort(poses);

		for (int i = 0; i < containers.size(); i++) {
			Container c = containers.get(i);
			int pos = poses.get(i);
			c.setLocation(locationDepictor.getBioLocation());
			c.setPos(pos);
			toSave.add(c);
		}
		//		locationDepictor.setBioLocation(locationDepictor.getBioLocation());
	}

	public void eventOk() throws Exception {
		SpiritUser user = Spirit.askForAuthentication();
		if(!commitTransaction && toSave.size()==0) {
			List<Container> containers = containerTable.getRows();
			containerDropped(containers, locationDepictor.getSelectedPoses());
		}

		if(toSave.size()==0) {
			throw new Exception("To validate the location, you should double-click on the new position");
		}

		if(locationDepictor.getBioLocation()!=null) {
			Config.getInstance(".spirit").setProperty("checkin.location", locationDepictor.getBioLocation().getId());
		}

		if(commitTransaction) {
			List<Biosample> biosamples = Container.getBiosamples(toSave);
			DAOBiosample.persistBiosamples(biosamples, user);
			SpiritChangeListener.fireModelChanged(SpiritChangeType.MODEL_UPDATED, Biosample.class, biosamples);
		}

		dispose();
		success = true;
	}

	public boolean isSuccess() {
		return success;
	}


}
