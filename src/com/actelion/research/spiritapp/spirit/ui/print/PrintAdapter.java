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

package com.actelion.research.spiritapp.spirit.ui.print;

import java.util.List;

import javax.swing.JComponent;

import com.actelion.research.spiritcore.business.biosample.Container;

public abstract class PrintAdapter {
	
	public static final int PREVIEW_WIDTH = 1000;
	public static final int PREVIEW_HEIGHT = 200;
	private PrintingTab observer;
	
	public abstract JComponent getConfigPanel();

	/**One of those 2 function should be overidden*/
	public JComponent getPreviewPanelForList(List<Container> containers) {return null;}
	public JComponent getPreviewPanel(Container container) {return null;}

	public PrintAdapter(PrintingTab observer) {
		this.observer = observer;
	}
	
	public void fireConfigChanged() {
		observer.fireConfigChanged();
	}
	
	public abstract void print(List<Container> containers) throws Exception;
	
	
	public void eventSetRows(List<Container> containers) {}
}
