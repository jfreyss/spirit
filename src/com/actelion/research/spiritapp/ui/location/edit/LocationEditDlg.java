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

package com.actelion.research.spiritapp.ui.location.edit;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;

import com.actelion.research.spiritapp.Spirit;
import com.actelion.research.spiritapp.ui.SpiritFrame;
import com.actelion.research.spiritapp.ui.location.LocationActions;
import com.actelion.research.spiritapp.ui.util.SpiritChangeListener;
import com.actelion.research.spiritapp.ui.util.SpiritChangeType;
import com.actelion.research.spiritapp.ui.util.SpiritContextListener;
import com.actelion.research.spiritapp.ui.util.component.JSpiritEscapeDialog;
import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.spiritcore.services.dao.DAOLocation;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.SwingWorkerExtended;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.iconbutton.IconType;
import com.actelion.research.util.ui.iconbutton.JIconButton;


/**
 * Dialog used to edit locations
 *
 * @author Joel Freyss
 */
public class LocationEditDlg extends JSpiritEscapeDialog {

	private LocationEditTable table = new LocationEditTable();
	private List<Location> savedLocations = null;

	/**
	 * Creates a dialog to duplicate studies
	 * @param locations
	 * @return
	 */
	public static LocationEditDlg duplicate(List<Location> locations) {
		Set<Location> tree = new HashSet<>();
		for (Location l : locations) {
			tree.addAll(l.getChildrenRec(10));
		}

		if(tree.size()!=locations.size()) {
			int res = JOptionPane.showConfirmDialog(UIUtils.getMainFrame(), "Do you want to duplicate the children also?", "Duplicate Locations", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
			if(res==JOptionPane.YES_OPTION) {
				locations = new ArrayList<>(tree);
			} else if(res==JOptionPane.NO_OPTION) {
				//ok
			} else {
				return null;
			}
		}

		return new LocationEditDlg(locations, true, true);
	}

	/**
	 * Creates a dialog to edit locations (new or existing).
	 * This function creates a new transaction
	 * @param locations
	 * @return
	 */
	public static LocationEditDlg edit(List<Location> locations) {
		return new LocationEditDlg(locations, false, true);
	}

	/**
	 * Creates a dialog to edit locations (new or existing).
	 * This function does not create a new transaction. The edition is done within the same edit context
	 * @param locations
	 * @return
	 */
	public static LocationEditDlg editInSameTransaction(List<Location> locations) {
		return new LocationEditDlg(locations, false, false);
	}

	/**
	 * private constructor to start the edit Location dialog
	 * @param myLocations
	 * @param duplicate
	 * @param newTransaction
	 */
	private LocationEditDlg(List<Location> myLocations, boolean duplicate, final boolean newTransaction) {
		super(UIUtils.getMainFrame(), "Location - Edit", newTransaction? LocationEditDlg.class.getName(): null);
		List<Location> locations = JPAUtil.reattach(myLocations);

		if(duplicate) {
			locations = Location.duplicate(locations);
		}

		//Create Print Button
		JButton printButton = new JButton(new LocationActions.Action_Print(locations) {
			@Override
			public List<Location> getLocations() {
				List<Location> locs = new ArrayList<>();
				for(Location l: table.getRows()) {
					if(l.getId()>0) locs.add(l);
				}
				return locs;
			}
		});

		//Create Save Button
		JButton saveButton = new JIconButton(IconType.SAVE, newTransaction? "Save": "Ok");
		saveButton.addActionListener(e -> {
			try {
				List<Location> rows = table.getRows();
				for (Iterator<Location> iterator = rows.iterator(); iterator.hasNext();) {
					Location location = iterator.next();
					if((location.getName()==null || location.getName().length()==0) && location.getLocationType()==null) {
						iterator.remove();
					}
				}
				if(newTransaction) {
					save(rows);
				} else {
					savedLocations = rows;
					dispose();
				}
			} catch(Exception ex) {
				JExceptionDialog.showError(LocationEditDlg.this, ex);
			}
		});

		//Panel Layout
		setContentPane( UIUtils.createBox(
				UIUtils.createTitleBox("Locations", new JScrollPane(table)),
				null,
				UIUtils.createHorizontalBox(Box.createHorizontalGlue(), printButton, saveButton)));
		UIUtils.adaptSize(this, 1140, 600);

		table.setRows(locations);
		setVisible(true);
	}

	public static boolean deleteInNewContext(List<Location> locations) {
		try {

			JPAUtil.pushEditableContext(SpiritFrame.getUser());
			locations = JPAUtil.reattach(locations);
			SpiritUser user = Spirit.askForAuthentication();
			for (Location loc : locations) {
				if (!SpiritRights.canDelete(loc, user)) {
					throw new Exception("You are not allowed to delete the location " + loc);
				}
			}

			delete(locations);
			SpiritChangeListener.fireModelChanged(SpiritChangeType.MODEL_DELETED, Location.class, locations);
			return true;
		} catch (Exception e) {
			JExceptionDialog.showError(e);
			return false;
		} finally {
			JPAUtil.popEditableContext();
		}
	}

	private static void delete(List<Location> locations) throws Exception {
		//The user cannot delete a
		//		List<Biosample> toCheckout = new ArrayList<>();
		for (Location l : locations) {
			if(l.getBiosamples().size()>0) throw new Exception("You cannot delete " + l.getHierarchyFull() + " because it is not empty");
			//			toCheckout.addAll(l.getBiosamples());
		}

		int res = JOptionPane.showConfirmDialog(UIUtils.getMainFrame(), "Are you sure you want to delete " + (locations.size() == 1 ? locations.get(0).getHierarchyFull() : locations.size() + " locations") + "?", "Delete Location", JOptionPane.YES_NO_OPTION);
		if (res != JOptionPane.YES_OPTION) return;

		//
		//		Status status = null;
		//		if(toCheckout.size()>0) {
		//			throw new Exception("There are "+toCheckout+" samples in these locations"):
		//			BiosampleTable table = new BiosampleTable();
		//			table.getModel().setCanExpand(false);
		//			table.getModel().setMode(Mode.COMPACT);
		//			table.setRows(toCheckout);
		//			JScrollPane sp = new JScrollPane(table);
		//			sp.setPreferredSize(new Dimension(750, 400));
		//
		//			JPanel msgPanel = new JPanel(new BorderLayout());
		//			msgPanel.add(BorderLayout.NORTH, new JLabel("<html><div style='color:orange'>What do you want to do with the " + toCheckout.size() + " biosamples of those locations?</div>"));
		//			msgPanel.add(BorderLayout.CENTER, sp);
		//
		//			res = JOptionPane.showOptionDialog(UIUtils.getMainFrame(), msgPanel, "Checkout", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, new String[] {"Mark as "+Status.TRASHED, "Mark as "+Status.USEDUP, "Cancel"}, null);
		//			if(res==0) {
		//				status = Status.TRASHED;
		//			} else if(res==1) {
		//				status = Status.USEDUP;
		//			} else {
		//				return;
		//			}
		//		}

		//Ask for a reason
		if(!Spirit.askReasonForChange()) return;

		//Update in a transaction
		EntityManager session = null;
		EntityTransaction txn = null;
		try {
			session = JPAUtil.getManager();
			txn = session.getTransaction();
			txn.begin();

			//			for (Biosample b : toCheckout) {
			//				b.setLocPos(null, -1);
			//				b.setStatus(status);
			//			}
			//			if(toCheckout.size()>0) {
			//				DAOBiosample.persistBiosamples(session, toCheckout, SpiritFrame.getUser());
			//			}
			DAOLocation.deleteLocations(session, locations, SpiritFrame.getUser());

			txn.commit();
			txn = null;

			SpiritChangeListener.fireModelChanged(SpiritChangeType.MODEL_DELETED, Location.class, locations);

		} finally {
			if(txn!=null && txn.isActive()) try{txn.rollback();}catch (Exception e) {e.printStackTrace();}
		}
	}


	/**
	 * Save and dispose the dialog
	 * @param locations
	 * @throws Exception
	 */
	private void save(List<Location> locations) throws Exception {

		if(!Spirit.askReasonForChangeIfUpdated(locations)) return;

		new SwingWorkerExtended(this, SwingWorkerExtended.FLAG_ASYNCHRONOUS20MS) {
			@Override
			protected void doInBackground() throws Exception {
				DAOLocation.persistLocations(locations, Spirit.askForAuthentication());
			}
			@Override
			protected void done() {
				SpiritChangeListener.fireModelChanged(SpiritChangeType.MODEL_UPDATED, Location.class, locations);
				if(locations.size()==1) {
					SpiritContextListener.setLocation(locations.get(0), -1);
				}
				dispose();
				savedLocations = locations;
			}
		};
	}


	public List<Location> getSavedLocations() {
		return savedLocations;
	}

}
