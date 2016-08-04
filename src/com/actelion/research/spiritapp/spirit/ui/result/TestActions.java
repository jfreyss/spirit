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

package com.actelion.research.spiritapp.spirit.ui.result;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritapp.spirit.ui.admin.TestEditDlg;
import com.actelion.research.spiritcore.business.result.Test;
import com.actelion.research.spiritcore.services.SpiritRights;

public class TestActions {

	public static class Action_Edit extends AbstractAction {
		private final Test test;
		public Action_Edit(Test test) {
			super("Edit Test");
			this.test = test;
			putValue(AbstractAction.MNEMONIC_KEY, (int)('e'));
			setEnabled(SpiritRights.isSuperAdmin(Spirit.getUser()));
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			new TestEditDlg(test);
		}
	}
	
	public static class Action_Duplicate extends AbstractAction {
		private final Test test;
		public Action_Duplicate(Test test) {
			super("Duplicate");
			this.test = test;
			putValue(AbstractAction.MNEMONIC_KEY, (int)('d'));
			setEnabled(SpiritRights.isSuperAdmin(Spirit.getUser()));
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			Test dupl = test.duplicate();
			new TestEditDlg(dupl);
		}
	}
	
//	public static class Action_NewResultForTest extends AbstractAction {
//		private final Test test;
//		public Action_NewResultForTest(Test test) {
//			super("New Results for "+test);
//			this.test = test;
//			putValue(AbstractAction.MNEMONIC_KEY, (int)('n'));
//			setEnabled(SpiritRights.canEdit(test, Spirit.getUser()));
//		}
//		@Override
//		public void actionPerformed(ActionEvent e) {
//			new EditResultDlg(test);
//		}
//	}
	
	/*
	public static JPopupMenu createPopup(Test test) {
		JPopupMenu popupMenu = new JPopupMenu();
		if(test!=null) {
			if(SpiritRights.canEditTests(Spirit.getUser())) {
				popupMenu.add(new JMenuItem(new Action_Edit(test)));
				popupMenu.add(new JMenuItem(new Action_Duplicate(test)));
				popupMenu.add(new JSeparator());
			}
			popupMenu.add(new JMenuItem(new Action_NewResultForTest(test)));
		} else {
			popupMenu.add(new JMenuItem(new Action_NewTest()));
		}
		
		return popupMenu;
	}
	*/
}
