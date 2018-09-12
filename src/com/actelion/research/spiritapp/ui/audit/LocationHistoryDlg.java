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

import java.util.List;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import com.actelion.research.spiritapp.ui.location.LocationActions.Action_ExportLocationEvents;
import com.actelion.research.spiritcore.business.audit.Revision;
import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.spiritcore.services.dao.DAORevision;
import com.actelion.research.spiritcore.services.dao.SpiritProperties;
import com.actelion.research.util.ui.JEscapeDialog;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.SwingWorkerExtended;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.iconbutton.IconType;
import com.actelion.research.util.ui.iconbutton.JIconButton;

/**
 * Dialog to view the revisions per location
 *
 * @author Joel Freyss
 */
public class LocationHistoryDlg extends JEscapeDialog {

	private RevisionPanel revisionPanel = new RevisionPanel();
	private JCheckBox byFieldCheckbox = new JCheckBox("Changes per field", true);

	public LocationHistoryDlg(final Location location) {
		super(UIUtils.getMainFrame(), "Location - Audit Trail - " + location.getName());

		Action_ExportLocationEvents exportLocationEventsAction = new Action_ExportLocationEvents();
		exportLocationEventsAction.setParentDlg(this);
		JIconButton exportLocationEventsButton = new JIconButton(IconType.PDF, "Export Location Events...", exportLocationEventsAction);
		JPanel actionPanel = UIUtils.createHorizontalBox(Box.createHorizontalGlue(), exportLocationEventsButton);

		byFieldCheckbox.setVisible(SpiritProperties.getInstance().isAdvancedMode());
		JPanel topPanel = UIUtils.createHorizontalBox(Box.createHorizontalGlue(), byFieldCheckbox);
		setContentPane(UIUtils.createBox(revisionPanel, topPanel, actionPanel));

		//Load revisions in background
		new SwingWorkerExtended(revisionPanel, SwingWorkerExtended.FLAG_ASYNCHRONOUS20MS) {
			private List<Revision> revisions;

			@Override
			protected void doInBackground() throws Exception {
				revisions = DAORevision.getLastRevisions(location);
			}

			@Override
			protected void done() {
				if(revisions.size()==0) {
					JExceptionDialog.showError("There are no revisions saved");
				}
				revisionPanel.setSingular(byFieldCheckbox.isSelected());
				revisionPanel.setRows(revisions);
				exportLocationEventsAction.setLocation(location);
				exportLocationEventsAction.setRevisions(revisions);
			}
		};

		UIUtils.adaptSize(this, 1000, 800);
		setVisible(true);
	}

}
