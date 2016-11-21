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

package com.actelion.research.spiritapp.spirit.ui.util;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;

import com.actelion.research.spiritcore.services.dao.DAORevision.Revision;
import com.actelion.research.util.FormatterUtils;
import com.actelion.research.util.ui.exceltable.JLabelNoRepaint;

public class RevisionList extends JList<Revision> {
	
	private Map<Revision, String> changeMap = new HashMap<>();
	
	public RevisionList() {	
		setCellRenderer(new DefaultListCellRenderer() {
			private JLabelNoRepaint lbl = new JLabelNoRepaint();
			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				Revision r = (Revision) value;
				
				String text = (index+1) + ". " + FormatterUtils.formatDateTime(r.getDate()) + " - "+ r.getUser();
				if(changeMap!=null && changeMap.size()>0) {
					text += "\n" + (changeMap.get(r)==null || changeMap.get(r).length()==0? "": "<b> > " + changeMap.get(r).replace("; ", "\n<b> > ") + "\n ");
				}
				lbl.setText(text);
				lbl.setForeground(getForeground());
				lbl.setBackground(getBackground());
				lbl.setBorder(getBorder());
				lbl.setToolTipText("Revision: "+r.getRevId());
				return lbl;
			}
		});

	}
	
	public RevisionList(Collection<Revision> revisions) {
		this();
		setRevisions(revisions);
	}
	
	public void setRevisions(Collection<Revision> revisions) {
		setListData(revisions.toArray(new Revision[revisions.size()]));
	}	
	
	public void setChangeMap(Map<Revision, String> changeMap) {
		this.changeMap = changeMap;
	}
	
	public Map<Revision, String> getChangeMap() {
		return changeMap;
	}
}
