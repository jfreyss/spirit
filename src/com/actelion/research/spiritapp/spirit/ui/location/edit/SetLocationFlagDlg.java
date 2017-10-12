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
import com.actelion.research.spiritapp.spirit.ui.location.LocationTable;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeListener;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeType;
import com.actelion.research.spiritapp.spirit.ui.util.component.JSpiritEscapeDialog;
import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.spiritcore.business.location.LocationFlag;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.spiritcore.services.dao.DAOLocation;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.util.ui.JCustomLabel;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.UIUtils;

public class SetLocationFlagDlg extends JSpiritEscapeDialog {

	private List<Location> locations;
	private boolean updated = false;

	public SetLocationFlagDlg(List<Location> myLocations, LocationFlag flag) {
		super(UIUtils.getMainFrame(), "Set Status", SetLocationFlagDlg.class.getName());
		this.locations = JPAUtil.reattach(myLocations);

		JPanel centerPanel = new JPanel(new BorderLayout());
		JLabel label = new JCustomLabel("Are you sure you want to modify the flags of those locations to " + (flag==null?"none":flag.getName()), Font.BOLD);
		label.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		centerPanel.add(BorderLayout.NORTH, label);

		JScrollPane sp;
		LocationTable table = new LocationTable();
		table.getModel().setCanExpand(false);
		table.setShowHierarchy(false);

		sp = new JScrollPane(table);
		centerPanel.add(BorderLayout.CENTER, sp);
		table.setRows(locations);

		JButton setStatusButton = new JButton(new MarkAction(flag));
		JPanel buttons = UIUtils.createHorizontalBox(Box.createHorizontalGlue(), setStatusButton, new JButton(new CancelAction()));
		getRootPane().setDefaultButton(setStatusButton);

		JPanel contentPanel = new JPanel(new BorderLayout());
		contentPanel.add(BorderLayout.CENTER, centerPanel);
		contentPanel.add(BorderLayout.SOUTH, buttons);

		setContentPane(contentPanel);

		UIUtils.adaptSize(this, 900, 400);
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

	public class MarkAction extends AbstractAction {
		private LocationFlag flag;
		public MarkAction(LocationFlag flag) {
			super("Set to " + (flag==null?"none":flag.getName()));
			this.flag = flag;
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				locations = JPAUtil.reattach(locations);
				SpiritUser user = Spirit.askForAuthentication();
				for (Location l : locations) {
					l.setLocationFlag(flag);
				}
				DAOLocation.persistLocations(locations, user);

				dispose();
				updated = true;
				SpiritChangeListener.fireModelChanged(SpiritChangeType.MODEL_UPDATED, Location.class, locations);

			} catch (Exception ex) {
				JExceptionDialog.showError(ex);
			}
		}
	}

	public boolean isUpdated() {
		return updated;
	}
}
