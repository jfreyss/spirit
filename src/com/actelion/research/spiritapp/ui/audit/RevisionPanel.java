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

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.io.Serializable;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;

import com.actelion.research.spiritapp.Spirit;
import com.actelion.research.spiritapp.ui.SpiritFrame;
import com.actelion.research.spiritapp.ui.admin.AdminActions;
import com.actelion.research.spiritapp.ui.util.SpiritChangeListener;
import com.actelion.research.spiritapp.ui.util.SpiritChangeType;
import com.actelion.research.spiritcore.business.audit.Revision;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.spiritcore.services.dao.DAORevision;
import com.actelion.research.spiritcore.services.dao.SpiritProperties;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.PopupAdapter;
import com.actelion.research.util.ui.SwingWorkerExtended;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.exceltable.JSplitPaneWithZeroSizeDivider;

public class RevisionPanel extends JPanel {

	private String filterByType;
	private Serializable filterById;
	private Integer filterBySid;

	private CardLayout cardLayout = new CardLayout();
	private JPanel cardPanel = new JPanel(cardLayout);
	private RevisionTable revisionTable = new RevisionTable();
	private RevisionItemTable revisionItemTable = new RevisionItemTable();
	private RevisionDetailPanel detailPanel = new RevisionDetailPanel();

	private List<Revision> revisions = null;
	private boolean singular = true;

	public RevisionPanel() {
		super(new GridLayout());
		JSplitPaneWithZeroSizeDivider sp = new JSplitPaneWithZeroSizeDivider(JSplitPane.VERTICAL_SPLIT, new JScrollPane(revisionTable), UIUtils.createTitleBox("Changed entity", detailPanel));
		SwingUtilities.invokeLater(()-> sp.setDividerLocation(.5));

		cardPanel.add("revision", UIUtils.createTitleBox("Changes per transaction", sp));
		cardPanel.add("singular", UIUtils.createTitleBox("Changes per field", new JScrollPane(revisionItemTable)));
		cardLayout.show(cardPanel, "singular");


		revisionTable.getSelectionModel().addListSelectionListener(e-> {
			if(e.getValueIsAdjusting()) return;
			List<Revision> s = revisionTable.getSelection();
			detailPanel.setRevision(s.size()!=1? null: s.get(0));
		});
		if(SpiritProperties.getInstance().isAdvancedMode()) {
			revisionTable.addMouseListener(new PopupAdapter() {
				@Override
				protected void showPopup(MouseEvent e) {
					List<Revision> s = revisionTable.getSelection();
					if(s.size()==1 && s.get(0).getStudies().size()==1) {
						JPopupMenu menu = new JPopupMenu();
						menu.add(new AdminActions.Action_Restore(s.get(0).getStudies()));
						menu.show(revisionTable, e.getX(), e.getY());
					}
				}
			});
		}
		add(BorderLayout.CENTER, cardPanel);
	}

	/**
	 * Sets filters to show only changes related to the entityType / entityId
	 * (sets to null to show all changes)
	 * @param filterByType
	 * @param filterById
	 */
	public void setFilters(String filterByType, Serializable filterById, Integer filterBySid) {
		this.filterByType = filterByType;
		this.filterById = filterById;
		this.filterBySid = filterBySid;
		revisionTable.getModel().setFilters(filterByType, filterById, filterBySid);
	}


	public void setSingular(boolean singular) {
		this.singular = singular;
		cardLayout.show(cardPanel, singular? "singular": "revision");
		setRows(revisions);
	}

	public void clear() {
		revisionItemTable.clear();
		revisionTable.clear();
	}


	public void setRows(List<Revision> revisions) {
		this.revisions = revisions;
		if(singular) {
			revisionItemTable.setRows(Revision.getRevisionItems(revisions, filterByType, filterById, filterBySid));
		} else {
			revisionTable.setRows(revisions);
		}
	}

	public class Action_Revert extends AbstractAction {
		private Revision revision;

		public Action_Revert(Revision revision) {
			super("Cancel this change");
			putValue(AbstractAction.MNEMONIC_KEY, (int)('v'));
			setEnabled(SpiritRights.isSuperAdmin(SpiritFrame.getUser()) || (revision.getUser()!=null && revision.getUser().equals(SpiritFrame.getUsername())));
			this.revision = revision;

		}
		@Override
		public void actionPerformed(ActionEvent e) {
			if(!Spirit.askReasonForChange()) return;
			new SwingWorkerExtended("Revert", UIUtils.getMainFrame()) {
				@Override
				protected void doInBackground() throws Exception {
					DAORevision.revert(revision, SpiritFrame.getUser());
				}
				@Override
				protected void done() {
					JExceptionDialog.showInfo(UIUtils.getMainFrame(), "The changes have been successfully reverted");

					//full refresh
					SpiritChangeListener.fireModelChanged(SpiritChangeType.LOGIN);
				}
			};
		}
	}

}
