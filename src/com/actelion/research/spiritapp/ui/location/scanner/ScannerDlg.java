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

package com.actelion.research.spiritapp.ui.location.scanner;

import java.awt.Color;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.Collections;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import com.actelion.research.spiritapp.Spirit;
import com.actelion.research.spiritapp.ui.location.LocationBrowser.LocationBrowserFilter;
import com.actelion.research.spiritapp.ui.location.LocationTextField;
import com.actelion.research.spiritapp.ui.location.depictor.RackDepictor;
import com.actelion.research.spiritapp.ui.util.SpiritChangeListener;
import com.actelion.research.spiritapp.ui.util.SpiritChangeType;
import com.actelion.research.spiritapp.ui.util.SpiritContextListener;
import com.actelion.research.spiritapp.ui.util.component.JHeaderLabel;
import com.actelion.research.spiritapp.ui.util.component.JSpiritEscapeDialog;
import com.actelion.research.spiritapp.ui.util.component.LF;
import com.actelion.research.spiritapp.ui.util.scanner.SpiritScannerHelper;
import com.actelion.research.spiritcore.adapter.DBAdapter;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Status;
import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.spiritcore.business.location.LocationType.LocationCategory;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.spiritcore.services.dao.DAOBiosample;
import com.actelion.research.spiritcore.services.dao.DAOLocation;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.JCustomLabel;
import com.actelion.research.util.ui.JCustomTextField;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.JGenericComboBox;
import com.actelion.research.util.ui.SwingWorkerExtended;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.iconbutton.IconType;
import com.actelion.research.util.ui.iconbutton.JIconButton;

/**
 * Dialog used to scan and set status
 *
 * @author Joel Freyss
 */
public class ScannerDlg extends JSpiritEscapeDialog {

	private RackDepictor rackView = new RackDepictor();

	private String title;
	private Location rack;

	private boolean askRackLocation;
	private Location parentLocation;
	private boolean askStatus;
	private Status status = null;

	protected JCustomTextField rackIdTextfield = new JCustomTextField();
	protected LocationTextField locationTextField = new LocationTextField(LocationBrowserFilter.CONTAINER);
	protected JCustomLabel locationLabel = new JCustomLabel("", FastFont.BOLD, Color.GREEN);
	protected JGenericComboBox<Status> statusComboBox = new JGenericComboBox<Status>(DBAdapter.getInstance().getAllowedStatus(), true);

	private Location rackToReuse = null;
	//private List<Biosample> biosamplesToCheck;

	public ScannerDlg(String title) {
		this(title, null);
	}

	public ScannerDlg(String title, Location rack) {
		super(UIUtils.getMainFrame(),  title, ScannerDlg.class.getName());
		this.title = title;
		this.rack  = rack;
	}

	protected void afterInit() {}
	protected void afterSave() {}

	public void scan() throws Exception {
		//Ask to scan rack, if none was prodided
		if(this.rack==null) {
			//Scan
			SpiritScannerHelper scanner = new SpiritScannerHelper();
			this.rack = scanner.scan();
			if(rack==null) return;
		} else {
			this.rack = JPAUtil.reattach(rack);
		}

		//Diplay the rack.
		rackView.setBiolocation(this.rack);

		//Button
		JIconButton cancelButton = new JIconButton("Cancel" , e -> dispose());
		JIconButton confirmReceptionButton = new JIconButton(IconType.SAVE, "Save" , e -> ok());

		//ContentPane
		JPanel saveRackPanel = UIUtils.createTitleBox("Save Rack", UIUtils.createTable(
				Box.createVerticalStrut(24), locationLabel,
				new JLabel("Rack Id: "), rackIdTextfield,
				new JLabel("Location: "), locationTextField));

		if(askRackLocation) {
			saveRackPanel.setVisible(true);
			locationTextField.setBioLocation(parentLocation);
			locationTextField.setFrameWidth(400);
			locationTextField.setFrameHeight(100);
		} else if(parentLocation!=null) {
			saveRackPanel.setVisible(true);
			locationTextField.setEnabled(false);
		} else {
			saveRackPanel.setVisible(false);
		}

		JPanel saveStatusPanel = UIUtils.createTitleBox("Save Status", UIUtils.createTable(new JLabel("Status: "), statusComboBox));
		statusComboBox.setSelection(status);
		if(askStatus) {
			saveStatusPanel.setVisible(true);
		} else if(status!=null) {
			saveStatusPanel.setVisible(true);
			statusComboBox.setEnabled(false);
		} else {
			saveStatusPanel.setVisible(false);
		}

		setContentPane(UIUtils.createBox(UIUtils.createBox(BorderFactory.createEmptyBorder(2, 2, 2, 2), rackView),new JHeaderLabel(title),
				/*UIUtils.createVerticalBox(
						new JHeaderLabel(title),
						checkLabel),*/
				UIUtils.createVerticalBox(
						saveRackPanel,
						saveStatusPanel,
						UIUtils.createHorizontalBox(Box.createHorizontalGlue(), cancelButton, confirmReceptionButton))));

		//suggest existing rack?
		FocusAdapter locationFocusAdapter = new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				try {
					if(rackToReuse!=null) {
						locationLabel.setText("Moving Rack: "+rackToReuse.getHierarchyFull());
						locationLabel.setForeground(Color.BLUE);
					} else if(rackIdTextfield.getText().length()==0) {
						locationLabel.setText("");
					} else {
						Location loc = DAOLocation.getCompatibleLocation(rackIdTextfield.getText(), JPAUtil.getSpiritUser());
						if(loc!=null) {
							if(loc.getLocationType()!=rack.getLocationType() || loc.getCols()!=rack.getCols() || loc.getRows()!=rack.getRows()) {
								locationLabel.setText("Wrong Rack's Type");
								locationLabel.setForeground(Color.RED);
							} else {
								locationLabel.setText("Already Existing Rack");
								locationLabel.setForeground(Color.RED);
							}
						} else {
							locationLabel.setText("New Rack");
							locationLabel.setForeground(LF.DARK_GREEN);
						}
					}

				} catch (Exception ex) {
					JExceptionDialog.showError(ScannerDlg.this, ex);
				}
			}
		};

		rackIdTextfield.addFocusListener(locationFocusAdapter);
		locationTextField.addFocusListener(locationFocusAdapter);

		//Preassign the RackId if rack is scan is identical from the DB
		if(askRackLocation) {
			Set<Location> locations = Biosample.getLocations(rack.getBiosamples());
			if(locations.size()==1) {
				boolean identical = true;
				Location loc = locations.iterator().next();
				if(loc.getBiosamples().size()==rack.getBiosamples().size() && loc.getLocationType()==rack.getLocationType() && loc.getCols()==rack.getCols() && loc.getRows()==rack.getRows()) {
					for (Biosample b : loc.getBiosamples()) {
						Biosample b2 = rack.getBiosample(b.getPos());
						if(!b.equals(b2)) {
							identical = false;
							break;
						}
					}
					if(identical) {
						rackToReuse = loc;
						rackIdTextfield.setText(rackToReuse.getName());
						locationTextField.setBioLocation(rackToReuse.getParent());
					}
				}
			}
			locationFocusAdapter.focusLost(null);
		}


		afterInit();
		UIUtils.adaptSize(this, 1000, 700);
		super.setVisible(true);
	}

	public void setAskRackLocation(boolean askRackLocation) {
		this.askRackLocation = askRackLocation;
	}

	public void setParentLocation(Location parentLocation) {
		this.parentLocation = parentLocation;
	}

	public void setAskStatus(boolean askStatus) {
		this.askStatus = askStatus;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	private void ok() {

		//Check RackId/Location - if the rack is new or must be reused
		if(askRackLocation && rackIdTextfield.getText().length()==0) {
			int res = JOptionPane.showConfirmDialog(this, "You didn't specify a Rack. Do you want to continue without saving the new position?", "No RackId", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
			if(res!=JOptionPane.OK_OPTION) return;
		} else {
			try {
				Location existing = DAOLocation.getLocation(locationTextField.getBioLocation(), rackIdTextfield.getText());
				if(existing!=null && !existing.equals(rackToReuse)) {
					throw new Exception("The rack "+existing.getHierarchyFull()+" already exists");
				}
				if(rackToReuse!=null) {
					rack = rackToReuse;
				}

				rack.setName(rackIdTextfield.getText());
				Location parent = locationTextField.getBioLocation();
				if(parent!=null) {
					if(parent.getLocationType().getCategory()!=null && parent.getLocationType().getCategory()==LocationCategory.MOVEABLE) throw new Exception("The location "+parent.getHierarchyFull()+" is not a container");
					if(DAOLocation.getLocation(parent, rackIdTextfield.getName())!=null) throw new Exception("The rackId '" + rackIdTextfield.getName() + "' exists already");
				}
				rack.setParent(parent);
			} catch (Exception e) {
				JExceptionDialog.showError(this, e);
				return;
			}
		}



		try {
			for (Biosample b : rack.getBiosamples()) {
				if(b.getId()<=0) throw new Exception("The sample " + b.getContainerId() + " at " + b.getScannedPosition() + " is not registered");

				if(statusComboBox.getSelection()!=null) {
					b.setStatus(statusComboBox.getSelection());
				}

				if(rackIdTextfield.getText().length()>0) {
					b.setLocation(rack);
					b.setPos(rack.parsePosition(b.getScannedPosition()));
				}
			}			
		} catch (Exception e) {
			JExceptionDialog.showError(this, e);
			return;
		}

		//Save the samples
		//Don't ask for reason of change, because this is normal process
		new SwingWorkerExtended("Saving", rackView) {

			@Override
			protected void doInBackground() throws Exception {
				SpiritUser user = Spirit.askForAuthentication();


				EntityManager session = JPAUtil.getManager();
				EntityTransaction txn = null;
				try {
					txn = session.getTransaction();
					txn.begin();
					if(rackToReuse!=null) {
						DAOLocation.persistLocations(session, Collections.singleton(rackToReuse), user);
					}
					DAOBiosample.persistBiosamples(session, rack.getBiosamples(), user);
					txn.commit();
					txn = null;
				} finally {
					if(txn!=null && txn.isActive()) try{txn.rollback();}catch (Exception e) {e.printStackTrace();}
				}
			}

			@Override
			protected void done() {
				afterSave();
				JOptionPane.showMessageDialog(null, "Samples saved");
				SpiritChangeListener.fireModelChanged(SpiritChangeType.MODEL_UPDATED, Biosample.class, rack.getBiosamples());
				SpiritChangeListener.fireModelChanged(SpiritChangeType.MODEL_UPDATED, Location.class, rack);
				SpiritContextListener.setLocation(rack, -1);
				dispose();
			}
		};


	}
}
