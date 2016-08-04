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

package com.actelion.research.spiritapp.spirit.ui.result.dialog;

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
import com.actelion.research.spiritapp.spirit.ui.result.ResultTable;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeListener;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeType;
import com.actelion.research.spiritapp.spirit.ui.util.component.JSpiritEscapeDialog;
import com.actelion.research.spiritcore.business.Quality;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.spiritcore.services.dao.DAOResult;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.util.ui.JCustomLabel;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.UIUtils;

public class SetResultQualityDlg extends JSpiritEscapeDialog {
	
	private List<Result> results;
	
	public SetResultQualityDlg(List<Result> res, Quality quality) {
		super(UIUtils.getMainFrame(), "Set Quality", SetResultQualityDlg.class.getName());
		this.results = JPAUtil.reattach(res);
		
		JPanel contentPanel = new JPanel(new BorderLayout());
		JLabel label = new JCustomLabel("Are you sure you want to modify the quality of those results to " + quality, Font.BOLD);
		label.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		contentPanel.add(BorderLayout.NORTH, label);

		ResultTable table = new ResultTable();
		JScrollPane sp = new JScrollPane(table);
		table.setRows(results);
		contentPanel.add(BorderLayout.CENTER, sp);		
		contentPanel.add(BorderLayout.SOUTH, UIUtils.createHorizontalBox(Box.createHorizontalGlue(), new JButton(new MarkAction(quality))));
		
		setContentPane(contentPanel);
		setSize(900, 400);
		setLocationRelativeTo(UIUtils.getMainFrame());
		setVisible(true);
		
	}
	
	
	public class MarkAction extends AbstractAction {
		private Quality quality;
		public MarkAction(Quality quality) {
			super("Set As " + quality);
			this.quality = quality;
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				
				SpiritUser user = Spirit.askForAuthentication();
				DAOResult.updateQuality(results, quality, user);
				SpiritChangeListener.fireModelChanged(SpiritChangeType.MODEL_UPDATED, Result.class, results);
				dispose();
			} catch (Exception ex) {
				JExceptionDialog.showError(ex);
			}
		}
	}
	
}
