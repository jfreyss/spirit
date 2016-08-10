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

package com.actelion.research.spiritapp.spirit.ui.location.edit;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritapp.spirit.ui.biosample.BiosampleTable;
import com.actelion.research.spiritapp.spirit.ui.biosample.BiosampleTableModel.Mode;
import com.actelion.research.spiritapp.spirit.ui.location.LocationActions;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeListener;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeType;
import com.actelion.research.spiritapp.spirit.ui.util.component.JSpiritEscapeDialog;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Status;
import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.spiritcore.services.dao.DAOBiosample;
import com.actelion.research.spiritcore.services.dao.DAOLocation;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.iconbutton.JIconButton;
import com.actelion.research.util.ui.iconbutton.JIconButton.IconType;


public class LocationBatchEditDlg extends JSpiritEscapeDialog {
	
	private LocationEditTable table = new LocationEditTable();
	private List<Location> savedLocations = null; 
	
	public static LocationBatchEditDlg duplicate(List<Location> locations) {
		Set<Location> tree = new HashSet<Location>();
		for (Location l : locations) {
			tree.addAll(l.getChildrenRec(10));
		}
		
		if(tree.size()!=locations.size()) {
			int res = JOptionPane.showConfirmDialog(UIUtils.getMainFrame(), "Do you want to duplicate the children also?", "Duplicate Locations", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
			if(res==JOptionPane.YES_OPTION) {
				locations = new ArrayList<Location>(tree);
			} else if(res==JOptionPane.NO_OPTION) {
				//ok
			} else {
				return null;
			}
		}
		
		return new LocationBatchEditDlg(locations, true, true);
		
		
	}
	public static LocationBatchEditDlg edit(List<Location> locations) {
		return new LocationBatchEditDlg(locations, false, true);
	}
	
	public static LocationBatchEditDlg editInSameTransaction(List<Location> locations) {
		return new LocationBatchEditDlg(locations, false, false);
	}
	
	private LocationBatchEditDlg(List<Location> locations, boolean duplicate, final boolean newTransaction) {
		super(UIUtils.getMainFrame(), "Location - Edit", newTransaction? LocationBatchEditDlg.class.getName(): null);
		locations = JPAUtil.reattach(locations);
		
		
		if(duplicate) {
			locations = DAOLocation.duplicate(locations);
		}
		
		//Create Print Button
		JButton printButton = new JButton(new LocationActions.Action_Print(locations) {
			@Override
			public List<Location> getLocations() {
				return table.getRows();
			}
		});

		//Create Save Button
		JButton saveButton = new JIconButton(IconType.SAVE, newTransaction? "Save": "Ok");
		saveButton.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					List<Location> locations = table.getRows();
					for (Iterator<Location> iterator = locations.iterator(); iterator.hasNext();) {
						Location location = iterator.next();
						if((location.getName()==null || location.getName().length()==0) && location.getLocationType()==null) {
							iterator.remove();
						}						
					}
					if(newTransaction) {
						save(locations);								
					} else {
						savedLocations = locations;
						dispose();
					}
				} catch(Exception ex) {
					JExceptionDialog.showError(LocationBatchEditDlg.this, ex);
				}
			}
		});
		
		//Panel Layout
		JPanel contentPanel = new JPanel(new BorderLayout());
		contentPanel.add(BorderLayout.CENTER, new JScrollPane(table));
		contentPanel.add(BorderLayout.SOUTH, UIUtils.createHorizontalBox(Box.createHorizontalGlue(), printButton, saveButton));
		
		setContentPane(contentPanel);
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		setSize(Math.min(900, dim.width-50), Math.min(700, dim.height-100));		
		setLocationRelativeTo(UIUtils.getMainFrame());
		
		//init
		table.setRows(locations);
		

	
		setVisible(true);
	}
	
	public static boolean deleteInNewContext(List<Location> locations) {
		try {

			JPAUtil.pushEditableContext(Spirit.getUser());
			locations = JPAUtil.reattach(locations);
			SpiritUser user = Spirit.askForAuthentication();
			for (Location loc : locations) {
				if (!SpiritRights.canEdit(loc, user))
					throw new Exception("You are not allowed to delete the location " + loc);
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
		int res = JOptionPane.showConfirmDialog(UIUtils.getMainFrame(), "Are you sure you want to delete " + (locations.size() == 1 ? locations.get(0).getHierarchyFull() : locations.size() + " locations") + "?", "Delete Location", JOptionPane.YES_NO_OPTION);
		if (res != JOptionPane.YES_OPTION) return;
		
		List<Biosample> toCheckout = new ArrayList<Biosample>();			
		for (Location l : locations) {
			toCheckout.addAll(l.getBiosamples());
		}

		Status status = null;
		if(toCheckout.size()>0) {
			
			BiosampleTable table = new BiosampleTable();
			table.getModel().setCanExpand(false);
			table.getModel().setMode(Mode.COMPACT);
			table.setRows(toCheckout);
			JScrollPane sp = new JScrollPane(table);
			sp.setPreferredSize(new Dimension(750, 400));
			
			JPanel msgPanel = new JPanel(new BorderLayout());
			msgPanel.add(BorderLayout.NORTH, new JLabel("<html><div style='color:orange'>What do you want to do with the " + toCheckout.size() + " biosamples of those locations?</div>"));
			msgPanel.add(BorderLayout.CENTER, sp);
			
			res = JOptionPane.showOptionDialog(UIUtils.getMainFrame(), msgPanel, "Checkout", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, new String[] {"Mark as "+Status.TRASHED, "Mark as "+Status.USEDUP, "Cancel"}, null);
			if(res==0) {
				status = Status.TRASHED;
			} else if(res==1) {
				status = Status.USEDUP;
			} else {
				return;
			}
			
		}

		//Update in a transaction
		EntityManager session = null;
		EntityTransaction txn = null;
		try {
			session = JPAUtil.getManager();
			txn = session.getTransaction();
			txn.begin();
			
			for (Biosample b : toCheckout) {
				b.setLocPos(null, -1);
				b.setStatus(status);
			}				
			if(toCheckout.size()>0) DAOBiosample.persistBiosamples(session, toCheckout, Spirit.getUser());
			DAOLocation.removeLocations(session, locations, Spirit.getUser());
			

			txn.commit();
			txn = null;
			
			SpiritChangeListener.fireModelChanged(SpiritChangeType.MODEL_DELETED, Location.class, locations);

		} finally {
			if(txn!=null && txn.isActive()) try{txn.rollback();}catch (Exception e) {}
		} 
	}
	


	private void save(List<Location> locations) throws Exception {
		DAOLocation.persistLocations(locations, Spirit.askForAuthentication());
		SpiritChangeListener.fireModelChanged(SpiritChangeType.MODEL_UPDATED, Location.class, locations);
		dispose();
		this.savedLocations = locations;
	}
	

	public List<Location> getSavedLocations() {
		return savedLocations;
	}

}
