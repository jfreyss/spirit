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

package com.actelion.research.spiritapp.spirit.ui.admin;

import java.awt.BorderLayout;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritapp.spirit.ui.biosample.BiosampleTable;
import com.actelion.research.spiritapp.spirit.ui.biosample.BiosampleTableModel.Mode;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.BiosampleQuery;
import com.actelion.research.spiritcore.services.dao.DAOBiosample;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.util.ui.JEscapeDialog;
import com.actelion.research.util.ui.SwingWorkerExtended;
import com.actelion.research.util.ui.UIUtils;

public class ExpiredSamplesDlg extends JEscapeDialog {
	
	private BiosampleTable expiredTable = new BiosampleTable();
	private BiosampleTable goingToExpireTable = new BiosampleTable();
	
	public ExpiredSamplesDlg() {
		super(UIUtils.getMainFrame(), "Expired & Going to Expire Samples");
		
		
		expiredTable.getModel().setCanExpand(false);
		expiredTable.getModel().setMode(Mode.COMPACT);

		
		goingToExpireTable.getModel().setCanExpand(false);
		goingToExpireTable.getModel().setMode(Mode.COMPACT);

		final JSplitPane centerPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, 
				UIUtils.createBox(new JScrollPane(expiredTable), UIUtils.createHorizontalTitlePanel("Expired Biosamples"), null, null, null),
				UIUtils.createBox(new JScrollPane(goingToExpireTable), UIUtils.createHorizontalTitlePanel("Biosamples going to expire next month"), null, null, null)
			);		
		
		JPanel contentPane = new JPanel(new BorderLayout());
		contentPane.add(BorderLayout.CENTER, centerPane);
		setContentPane(centerPane);
		setSize(1000, 800);
		setLocationRelativeTo(UIUtils.getMainFrame());		
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		new SwingWorkerExtended() {			
			List<Biosample> alreadyExpired;
			List<Biosample> goingtoExpire;
			
			@Override
			protected void doInBackground() throws Exception {
				Date now = JPAUtil.getCurrentDateFromDatabase();
				
				Calendar cal = GregorianCalendar.getInstance();
				cal.set(Calendar.DAY_OF_MONTH, cal.get(Calendar.DAY_OF_MONTH) + 31);
				Date inOneMonth = cal.getTime();
				
				BiosampleQuery alreadyExpiredQuery = new BiosampleQuery();
				alreadyExpiredQuery.setExpiryDateMax(now);
				
				BiosampleQuery goingtoExpireQuery = new BiosampleQuery();
				goingtoExpireQuery.setExpiryDateMin(now);
				goingtoExpireQuery.setExpiryDateMax(inOneMonth);

				
				alreadyExpired = DAOBiosample.queryBiosamples(alreadyExpiredQuery, Spirit.getUser());
				goingtoExpire = DAOBiosample.queryBiosamples(goingtoExpireQuery, Spirit.getUser());
			}
			
			@Override
			protected void done() {
				centerPane.setDividerLocation(.5);
				expiredTable.setRows(alreadyExpired);
				goingToExpireTable.setRows(goingtoExpire);
			}
		};
		
		centerPane.setDividerLocation(.5);
		setVisible(true);

	}
	
	public static void main(String[] args) throws Exception {
		
	}

}
