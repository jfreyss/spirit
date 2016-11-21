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

package com.actelion.research.spiritapp.spirit.ui.biosample.dialog;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritapp.spirit.ui.biosample.BiosampleTable;
import com.actelion.research.spiritapp.spirit.ui.biosample.BiosampleTableModel.Mode;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeListener;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeType;
import com.actelion.research.spiritapp.spirit.ui.util.component.JSpiritEscapeDialog;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Status;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.spiritcore.services.dao.DAOBiosample;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.util.ui.JCustomLabel;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.UIUtils;

public class SetBiosampleStatusDlg extends JSpiritEscapeDialog {
	
	private List<Biosample> biosamples;
	private boolean updated = false;
	
	public SetBiosampleStatusDlg(List<Biosample> mySamples, Status status) {
		super(UIUtils.getMainFrame(), "Set Status", SetBiosampleStatusDlg.class.getName());
		this.biosamples = JPAUtil.reattach(mySamples);
		
		//Test if those samples have a location
		boolean hasLoc = false;
		if(status==Status.TRASHED || status==Status.USEDUP) {
			for (Biosample biosample : biosamples) {
				if(biosample.getLocation()!=null) {
					hasLoc = true; break;
				}
			}
		}
		
		
		JPanel centerPanel = new JPanel(new BorderLayout());
		JLabel label = new JCustomLabel("Are you sure you want to modify the status of those biosamples / containers to " + status, Font.BOLD);
		label.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		centerPanel.add(BorderLayout.NORTH, label);

		JScrollPane sp;
		BiosampleTable table = new BiosampleTable();
		table.getModel().setCanExpand(false);
		table.getModel().setMode(Mode.COMPACT);
		
		sp = new JScrollPane(table);			
		centerPanel.add(BorderLayout.CENTER, sp);
		table.setRows(biosamples);
		
		JPanel buttons;		
		if(status==Status.TRASHED) {
			JButton trashCheckoutButton = new JButton(new TrashCheckoutAction(hasLoc));
			buttons = UIUtils.createHorizontalBox(Box.createHorizontalGlue(), trashCheckoutButton, new JButton(new CancelAction()));
			getRootPane().setDefaultButton(trashCheckoutButton);
		} else {
			JButton setStatusButton = new JButton(new MarkAction(status, false));
			buttons = UIUtils.createHorizontalBox(Box.createHorizontalGlue(), setStatusButton, new JButton(new CancelAction()));		
			getRootPane().setDefaultButton(setStatusButton);
		}
		
		JPanel contentPanel = new JPanel(new BorderLayout());
		contentPanel.add(BorderLayout.CENTER, centerPanel);
		contentPanel.add(BorderLayout.SOUTH, buttons);
				
		setContentPane(contentPanel);
		
		
		setSize(900, 400);
		setLocationRelativeTo(UIUtils.getMainFrame());
		setVisible(true);
		
	}
	
	public class CancelAction extends AbstractAction {
		
		public CancelAction() {
			super("Cancel");
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			dispose();
		}
	}
	
	public class TrashCheckoutAction extends AbstractAction {
		public TrashCheckoutAction(boolean hasLoc) {
			super("Checkout and Trash");
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				
				SpiritUser user = Spirit.askForAuthentication();
				biosamples = JPAUtil.reattach(biosamples);
				for (Biosample b : biosamples) {
					b.setLocPos(null, -1);
					b.setStatus(Status.TRASHED);
				}				
				DAOBiosample.persistBiosamples(biosamples, user);
				
				dispose();
				updated = true;
				SpiritChangeListener.fireModelChanged(SpiritChangeType.MODEL_UPDATED, Biosample.class, biosamples);

			} catch (Exception ex) {
				JExceptionDialog.showError(ex);
			}
		}
	}
	
	public class MarkAction extends AbstractAction {
		private Status status;
		public MarkAction(Status status, boolean hasLoc) {
			super("Set As " + status + (hasLoc?" (No Checkout)":""));
			this.status = status;
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				biosamples = JPAUtil.reattach(biosamples);						
				SpiritUser user = Spirit.askForAuthentication();
				for (Biosample b : biosamples) {
					b.setStatus(status);
				}
				DAOBiosample.persistBiosamples(biosamples, user);
				
				dispose();
				updated = true;
				SpiritChangeListener.fireModelChanged(SpiritChangeType.MODEL_UPDATED, Biosample.class, biosamples);

			} catch (Exception ex) {
				JExceptionDialog.showError(ex);
			}
		}
	}
	
	public boolean isUpdated() {
		return updated;
	}
}
