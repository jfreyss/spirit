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

package com.actelion.research.spiritapp.spirit.ui.home;

import java.awt.BorderLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritapp.spirit.ui.util.ISpiritTab;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeType;

public class HomeTab extends JPanel implements ISpiritTab {
	
	private final LastActivityEditorPane editorPane;
	private boolean initialized = false;
	
	public HomeTab(Spirit spirit) {
		editorPane = new LastActivityEditorPane(spirit);
		
		setLayout(new BorderLayout());
		add(BorderLayout.CENTER, new JScrollPane(editorPane));
		
				
		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentShown(ComponentEvent e) {
				if(!initialized && getRootPane()!=null) {
					initialized = true;
				}
			}
		});
	}
	
	private String studyIds;
	@Override
	public String getStudyIds() {
		return studyIds;
	}
	@Override
	public void setStudyIds(String studyIds) {
		this.studyIds = studyIds;
	}
	
	@Override
	public <T> void fireModelChanged(SpiritChangeType action, Class<T> what, List<T> details) {
		if(isShowing()) {
			editorPane.updateRecentChanges();
		}
	}

	@Override
	public void refreshFilters() {
	}
	
	
	
	


}
