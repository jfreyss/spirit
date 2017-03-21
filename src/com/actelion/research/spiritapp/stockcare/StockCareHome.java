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

package com.actelion.research.spiritapp.stockcare;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;

import com.actelion.research.spiritapp.spirit.ui.SpiritTab;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeType;
import com.actelion.research.spiritapp.stockcare.ui.item.StockCareItem;
import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.JCustomLabel;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.WrapLayout;
import com.actelion.research.util.ui.iconbutton.IconType;

public class StockCareHome extends SpiritTab {

	public StockCareHome(StockCare frame) {
		super(frame, "", IconType.HOME.getIcon());
		
		List<StockCareItem> stockCareItems = StockCareItem.getStockCareItems();

		////////////////////////////////////////////////////////
		//HomePanel
		ButtonGroup group = new ButtonGroup();

		//Quick Buttons	
		JPanel buttonsPanel = new JPanel(new WrapLayout(FlowLayout.LEFT, 10, 10));
		for(final StockCareItem item: stockCareItems) {
			if(item==null) {
				buttonsPanel.add(Box.createHorizontalStrut(5000));
			} else {
				JToggleButton button = new JToggleButton(item.getName());
				button.setAction(new Action_View(item, true));
				button.setHorizontalTextPosition(SwingConstants.CENTER);
				button.setVerticalTextPosition(SwingConstants.BOTTOM);
				button.setMinimumSize(new Dimension(120, 30));
				group.add(button);
				buttonsPanel.add(button);
			}
		}

		buttonsPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, Color.BLACK));
		add(UIUtils.createVerticalBox(
				Box.createVerticalStrut(20),
				new JCustomLabel("Please select a biotype:", FastFont.BIGGER),
				buttonsPanel,
				Box.createVerticalGlue()));
		setBackground(Color.WHITE);
		setOpaque(true);
		
	}
	
	public class Action_View extends AbstractAction {
		private StockCareItem item;
		public Action_View(StockCareItem item, boolean fullSize) {
			super(fullSize? item.getName(): "View " + item.getName());
			this.item = item;
			putValue(AbstractAction.SMALL_ICON, item.getIcon(fullSize));
		}
		
		@Override
		public void actionPerformed(ActionEvent arg0) {
			((StockCare) getFrame()).setItem(item);
		}
	}

	@Override
	public void onTabSelect() {
	}
	
	@Override
	public void onStudySelect() {
	}

	@Override
	public <T> void fireModelChanged(SpiritChangeType action, Class<T> what, List<T> details) {
		
	}

}
