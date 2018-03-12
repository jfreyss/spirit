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

package com.actelion.research.spiritapp.ui.location;

import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JPopupMenu;

import com.actelion.research.spiritapp.print.PrintLabel;
import com.actelion.research.spiritapp.ui.SpiritFrame;
import com.actelion.research.spiritapp.ui.audit.LocationHistoryDlg;
import com.actelion.research.spiritapp.ui.location.edit.LocationEditDlg;
import com.actelion.research.spiritapp.ui.location.edit.SetLocationFlagDlg;
import com.actelion.research.spiritapp.ui.print.BrotherLabelsDlg;
import com.actelion.research.spiritcore.business.audit.Revision;
import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.spiritcore.business.location.LocationFlag;
import com.actelion.research.spiritcore.business.location.Privacy;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.spiritcore.services.dao.DAORevision;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.PopupAdapter;
import com.actelion.research.util.ui.iconbutton.IconType;

public class LocationActions {

	public static class Action_New extends AbstractAction {
		private Location parent;

		public Action_New() {
			this((Location)null);
		}
		public Action_New(Location parent) {
			this(Collections.singleton(parent));
		}
		public Action_New(Collection<Location> selection) {
			super("New Locations");
			this.parent = selection==null || selection.size()==0? null: selection.iterator().next();
			if(parent!=null) putValue(NAME, getValue(NAME) + " (under "+parent.getName()+")");
			putValue(AbstractAction.MNEMONIC_KEY, (int)('l'));
			putValue(AbstractAction.SMALL_ICON, IconType.LOCATION.getIcon());
			setEnabled(parent==null || (parent.getLocationType().getPreferredChild()!=null && SpiritRights.canRead(parent, SpiritFrame.getUser())));
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			if(SpiritFrame.getUser()==null) return;
			Location location = new Location();
			if(parent!=null) {
				location.setLocationType(parent.getLocationType().getPreferredChild());
				location.setCols(parent.getCols());
				location.setRows(parent.getRows());
				location.setParent(parent);
			}
			location.setName("");


			if(SpiritFrame.getUser()!=null  && !SpiritFrame.getUser().isSuperAdmin() && (parent==null || parent.getInheritedPrivacy()==Privacy.PUBLIC)) {
				location.setPrivacy(Privacy.PROTECTED);
				location.setEmployeeGroup(SpiritFrame.getUser().getMainGroup());
			}

			LocationEditDlg.edit(Collections.singletonList(location));
		}
	}

	public static class Action_Delete extends AbstractAction {
		private List<Location> locations;

		public Action_Delete(List<Location> locations) {
			super("Delete");
			this.locations = locations;
			putValue(AbstractAction.MNEMONIC_KEY, (int)('l'));
			putValue(AbstractAction.SMALL_ICON, IconType.DELETE.getIcon());
			boolean enabled = true;
			for (Location l : locations) {
				if(!SpiritRights.canDelete(l, SpiritFrame.getUser())) {
					enabled = false;
				}
			}
			setEnabled(enabled);
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			if(SpiritFrame.getUser()==null) return;
			LocationEditDlg.deleteInNewContext(locations);
		}
	}

	public static class Action_History extends AbstractAction {
		private final Collection<Location> locations;
		public Action_History(Collection<Location> locations) {
			super("Audit Trail");
			this.locations = locations;
			putValue(AbstractAction.MNEMONIC_KEY, (int)('a'));
			putValue(Action.SMALL_ICON, IconType.HISTORY.getIcon());
			setEnabled(locations.size()==1);
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				List<Revision> revisions = DAORevision.getLastRevisions(locations.iterator().next());
				new LocationHistoryDlg(revisions);
			} catch(Exception ex) {
				JExceptionDialog.showError(ex);
			}
		}
	}

	public static class Action_Print extends AbstractAction {
		private List<Location> locations;

		public Action_Print(Location location) {
			super("Print Label");
			this.locations = Collections.singletonList(location);
			putValue(AbstractAction.MNEMONIC_KEY, (int)('p'));
			putValue(AbstractAction.SMALL_ICON, IconType.PRINT.getIcon());
			setEnabled(SpiritRights.canRead(location, SpiritFrame.getUser()));
		}

		public Action_Print(List<Location> locations) {
			super("Print Labels");
			this.locations = locations;
			putValue(AbstractAction.MNEMONIC_KEY, (int)('p'));
			putValue(AbstractAction.SMALL_ICON, IconType.PRINT.getIcon());
			boolean enabled = true;
			for (Location l : locations) {
				if(!SpiritRights.canRead(l, SpiritFrame.getUser())) {
					enabled = false;
				}
			}
			setEnabled(enabled);
		}

		public List<Location> getLocations() {
			return locations;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			List<PrintLabel> labels = new ArrayList<PrintLabel>();
			for (Location loc : getLocations()) {
				labels.add(new PrintLabel("" + loc.getLocationId(), loc.getName()+ "\n" + (loc.getDescription()==null?"": loc.getDescription())));
			}
			new BrotherLabelsDlg(labels);
		}
	}
	public static class Action_Duplicate extends AbstractAction {
		private List<Location> locations;
		public Action_Duplicate(List<Location> locations) {
			super("Duplicate");
			this.locations = locations;
			putValue(AbstractAction.MNEMONIC_KEY, (int)('d'));
			putValue(AbstractAction.SMALL_ICON, IconType.DUPLICATE.getIcon());
			boolean enabled = true;
			for (Location location : locations) {
				if(!SpiritRights.canEdit(location.getParent(), SpiritFrame.getUser())) {
					enabled = false;
					break;
				}
			}
			setEnabled(enabled);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if(SpiritFrame.getUser()==null) return;


			LocationEditDlg.duplicate(locations);
		}
	}


	public static class Action_EditBatch extends AbstractAction {
		private List<Location> locations;

		public Action_EditBatch(Location location) {
			super("Edit / Move Location ("+location.getName()+")");
			this.locations = Collections.singletonList(location);
			putValue(AbstractAction.MNEMONIC_KEY, (int)('e'));
			putValue(AbstractAction.SMALL_ICON, IconType.EDIT.getIcon());
			setEnabled(SpiritRights.canEdit(location, SpiritFrame.getUser()));

		}

		public Action_EditBatch(List<Location> locations) {
			super("Edit");
			this.locations = locations;
			putValue(AbstractAction.MNEMONIC_KEY, (int)('e'));
			putValue(AbstractAction.SMALL_ICON, IconType.EDIT.getIcon());
			boolean enabled = true;
			for(Location l: locations) {
				if(!SpiritRights.canEdit(l, SpiritFrame.getUser())) enabled = false;
			}

			setEnabled(enabled);

		}
		@Override
		public void actionPerformed(ActionEvent e) {
			LocationEditDlg.edit(locations);
		}
	}

	public static class Action_SetStatus extends AbstractAction {
		private final List<Location> locations;
		private LocationFlag flag;

		public Action_SetStatus(List<Location> locations, LocationFlag flag) {
			super(flag==null?"none":flag.getName());
			this.locations = locations;
			this.flag = flag;
			putValue(AbstractAction.SMALL_ICON, flag==null? null: flag.getIcon());
			for (Location l : locations) {
				if(!SpiritRights.canEdit(l, SpiritFrame.getUser())) setEnabled(false);
			}

		}
		@Override
		public void actionPerformed(ActionEvent e) {
			new SetLocationFlagDlg(locations, flag);
		}
	}


	public static JPopupMenu createPopup(List<Location> locations) {
		return SpiritFrame.getInstance().getPopupHelper().createLocationPopup(locations);
	}

	public static void attachPopup(final LocationTable table) {
		table.addMouseListener(new PopupAdapter(table) {
			@Override
			protected void showPopup(MouseEvent e) {

				JPopupMenu popupMenu = createPopup(table.getSelection());
				popupMenu.show(table, e.getX(), e.getY());
			}
		});
	}


}
