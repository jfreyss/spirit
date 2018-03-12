/*
 * Spirit, a study/biosample management tool for research.
 * Copyright (C) 2018 Idorsia Pharmaceuticals Ltd., Hegenheimermattweg 91,
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

package com.actelion.research.spiritapp.ui.pivot.designer;

import java.util.HashMap;
import java.util.Map;

import com.actelion.research.spiritcore.business.pivot.PivotItem;
import com.actelion.research.spiritcore.business.pivot.PivotTemplate.Where;


public class ItemPanelControler {
	
	private Map<Where, DropZonePanel> where2panel = new HashMap<>();
	
	public ItemPanelControler() {}
	
	
	public void clear() {
		for (DropZonePanel p : where2panel.values()) {
			p.clear();
		}
		where2panel.clear();
	}
	
	public void addDropZone(Where panelId, DropZonePanel panel) {
		where2panel.put(panelId, panel);
	}

	public void onDragEnd() {}
	
	public boolean placeItem(PivotItem item, DropZonePanel panel) {		
		for (DropZonePanel p : where2panel.values()) {
			p.removeItem(item);
		}
		if(panel!=null) {
			panel.addItem(item);
		}
		return true;
	}
	
	public void placeItem(PivotItem item, Where where) {
		placeItem(item, where2panel.get(where));
	}
	
	public void updateView() {
		for (DropZonePanel p : where2panel.values()) {
			p.updateView();
		}		
	}
		
	public Where getPanelId(PivotItem pivotItem) {
		for (Where key : where2panel.keySet()) {
			if(where2panel.get(key).hasItem(pivotItem)) {
				return key;
			}
		}
		return Where.MERGE;
	}

}
