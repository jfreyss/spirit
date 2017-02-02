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

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritapp.spirit.ui.biosample.BiosampleActions;
import com.actelion.research.spiritapp.spirit.ui.biosample.BiosampleTable;
import com.actelion.research.spiritapp.spirit.ui.biosample.BiosampleTableModel.Mode;
import com.actelion.research.spiritapp.spirit.ui.util.ISpiritChangeObserver;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeListener;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeType;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.BiosampleQuery;
import com.actelion.research.spiritcore.services.dao.DAOBiosample;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.util.ui.JEscapeDialog;
import com.actelion.research.util.ui.SwingWorkerExtended;
import com.actelion.research.util.ui.UIUtils;

public class ExpiredSamplesDlg extends JEscapeDialog implements ISpiritChangeObserver {
	
	private BiosampleTable expiredTable = new BiosampleTable();
	private BiosampleTable goingToExpireTable = new BiosampleTable();
	private final JSplitPane centerPane;
	
	public ExpiredSamplesDlg() {
		super(UIUtils.getMainFrame(), "Expired & Going to Expire Samples");
		
		SpiritChangeListener.register(this);
		expiredTable.getModel().setCanExpand(false);
		expiredTable.getModel().setMode(Mode.COMPACT);
		
		goingToExpireTable.getModel().setCanExpand(false);
		goingToExpireTable.getModel().setMode(Mode.COMPACT);

		centerPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
				UIUtils.createTitleBox("Expired Biosamples", new JScrollPane(expiredTable)),
				UIUtils.createTitleBox("Biosamples going to expire in the next 90 days", new JScrollPane(goingToExpireTable)));		
		
		JPanel contentPane = new JPanel(new BorderLayout());
		contentPane.add(BorderLayout.CENTER, centerPane);
		setContentPane(centerPane);
		UIUtils.adaptSize(this, 1000, 800);
		BiosampleActions.attachPopup(expiredTable);
		BiosampleActions.attachPopup(goingToExpireTable);
		refresh();
		
		centerPane.setDividerLocation(.5);
		setVisible(true);

	}
	
	public void refresh() {
		new SwingWorkerExtended(getContentPane(), SwingWorkerExtended.FLAG_ASYNCHRONOUS20MS) {			
			List<Biosample> alreadyExpired;
			List<Biosample> goingtoExpire;
			
			@Override
			protected void doInBackground() throws Exception {
				Date now = JPAUtil.getCurrentDateFromDatabase();
				
				Calendar cal = GregorianCalendar.getInstance();
				cal.set(Calendar.DAY_OF_MONTH, cal.get(Calendar.DAY_OF_MONTH) + 90);
				Date in3Months = cal.getTime();
				
				BiosampleQuery alreadyExpiredQuery = new BiosampleQuery();
				alreadyExpiredQuery.setExpiryDateMax(now);
				alreadyExpiredQuery.setFilterTrashed(true);
				
				BiosampleQuery goingtoExpireQuery = new BiosampleQuery();
				goingtoExpireQuery.setExpiryDateMin(now);
				goingtoExpireQuery.setExpiryDateMax(in3Months);
				goingtoExpireQuery.setFilterTrashed(true);

				
				alreadyExpired = DAOBiosample.queryBiosamples(alreadyExpiredQuery, Spirit.getUser());
				goingtoExpire = DAOBiosample.queryBiosamples(goingtoExpireQuery, Spirit.getUser());
			}
			
			@Override
			protected void done() {
				centerPane.setDividerLocation(
						alreadyExpired.size()==0 && goingtoExpire.size()==0? .5:
						alreadyExpired.size()>0 && goingtoExpire.size()>0? .5:
						alreadyExpired.size()>0? .75: .25);
				expiredTable.setRows(alreadyExpired);
				goingToExpireTable.setRows(goingtoExpire);
			}
		};
	}

	@Override
	public <T> void actionModelChanged(SpiritChangeType action, Class<T> what, List<T> details) {
		refresh();
	}

}
