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

package com.actelion.research.spiritapp.ui.study.monitor;

import java.awt.Color;
import java.util.Collections;
import java.util.Date;

import javax.swing.JComponent;

import com.actelion.research.spiritapp.Spirit;
import com.actelion.research.spiritapp.ui.SpiritFrame;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.services.dao.DAOResult;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.util.ui.JCustomTextField;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.TextChangeListener;

/**
 * Listener responsible for the saving a value to the DB immediately after being entered
 * @author freyssj
 *
 */
public class MonitorTextChangeListener implements TextChangeListener {
	private Result result;
	private int valueNo;
	
	public MonitorTextChangeListener(Result result, int valueNo) {
		this.result = result;
		this.valueNo = valueNo;
	}
	
	@Override
	public void textChanged(JComponent s) {
		assert s instanceof JCustomTextField;
		JCustomTextField src = (JCustomTextField) s;
		try {
			JPAUtil.pushEditableContext(SpiritFrame.getUser());
			
			Date now = JPAUtil.getCurrentDateFromDatabase();
			result.getOutputResultValues().get(valueNo).setValue(src.getText());
			result.setUpdUser(Spirit.askForAuthentication().getUsername());
			result.setUpdDate(now);
			
			DAOResult.persistResults(Collections.singletonList(result), SpiritFrame.getUser());
			src.setBorderColor(Color.BLUE);					
			src.setToolTipText((src.getToolTipText()==null?"<html>":src.getToolTipText()+"<br>") + "Updated value: "+MonitoringCagePanel.formatTooltipText(src.getText(), result.getUpdUser(), result.getUpdDate()));
		} catch(Exception ex) {
			src.setBorderColor(Color.RED);					
			JExceptionDialog.showError(src, ex);
		} finally {
			JPAUtil.popEditableContext();				
		}
	}
	
}