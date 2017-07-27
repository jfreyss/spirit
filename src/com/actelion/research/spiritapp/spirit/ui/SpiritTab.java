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

package com.actelion.research.spiritapp.spirit.ui;

import java.awt.BorderLayout;
import java.util.Collection;

import javax.swing.Icon;
import javax.swing.JPanel;

import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeType;

/**
 * SpiritTabs reprensents a Perspective in the Spirit's system.
 * Each tab is responsible to listen to the Spirit events.
 *
 * @author Joel Freyss
 *
 */
public abstract class SpiritTab extends JPanel {

	private SpiritFrame frame;
	private String name;
	private Icon icon;
	private String selectedStudy = "";;


	public SpiritTab(SpiritFrame frame, String name, Icon icon) {
		super(new BorderLayout());
		this.frame = frame;
		this.name = name;
		this.icon = icon;
	}

	public SpiritFrame getFrame() {
		return frame;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

	public void setIcon(Icon icon) {
		this.icon = icon;
	}

	public Icon getIcon() {
		return icon;
	}

	protected final String getSelectedStudyId() {
		return selectedStudy;
	}
	protected final void setSelectedStudyId(String selectedStudy) {
		this.selectedStudy = selectedStudy;
	}

	/**
	 * Called when the tab is selected
	 */
	public abstract void onTabSelect();
	/**
	 * Called when a new study is selected
	 * This function can be called just after onTabSelect, if the selected study is different from the one currently in the model
	 */
	public abstract void onStudySelect();
	public abstract <T> void fireModelChanged(SpiritChangeType action, Class<T> what, Collection<T> details);


}
