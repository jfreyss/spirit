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

package com.actelion.research.spiritapp.spirit.ui.result.edit;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritapp.spirit.ui.SpiritFrame;
import com.actelion.research.spiritapp.spirit.ui.result.ResultTable;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeListener;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeType;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.spiritcore.services.dao.DAOResult;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.JCustomLabel;
import com.actelion.research.util.ui.UIUtils;

public class ResultDiscardDlg {

	public static void createDialogForDelete(List<Result> results) throws Exception {
		//this.biosamples = biosamples;
		
		SpiritUser user = Spirit.askForAuthentication();
		
		try {
			JPAUtil.pushEditableContext(SpiritFrame.getUser());			
			results = JPAUtil.reattach(results);
			
			for (Result result : results) {			
				if(!SpiritRights.canDelete(result, user)) throw new Exception("You cannot delete "+result);
			}
			
			ResultTable table = new ResultTable();
			table.setRows(results);
			JScrollPane sp = new JScrollPane(table);
			sp.setPreferredSize(new Dimension(700, 400));
			
			JPanel msgPanel = new JPanel(new BorderLayout());
			msgPanel.add(BorderLayout.NORTH, new JCustomLabel("Are you sure you want to 'DEFINITELY' delete " + (results.size()>1? "those " + results.size() + " results": " this result"), FastFont.BOLD));
			msgPanel.add(BorderLayout.CENTER, sp);
			
			int res = JOptionPane.showOptionDialog(UIUtils.getMainFrame(), msgPanel, "DELETE Results", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, new String[] {"Delete", "Cancel"}, "Cancel");
			if(res!=0) return;

			DAOResult.deleteResults(results, user);
		} finally {
			JPAUtil.popEditableContext();			
		}
		SpiritChangeListener.fireModelChanged(SpiritChangeType.MODEL_DELETED, Result.class, results);
	}
}
