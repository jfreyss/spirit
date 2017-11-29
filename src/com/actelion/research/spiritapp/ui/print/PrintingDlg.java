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

package com.actelion.research.spiritapp.ui.print;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;

import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Container;
import com.actelion.research.spiritcore.business.biosample.ContainerType;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.spiritcore.util.ListHashMap;
import com.actelion.research.util.ui.JCustomTabbedPane;
import com.actelion.research.util.ui.JEscapeDialog;
import com.actelion.research.util.ui.UIUtils;

public class PrintingDlg extends JEscapeDialog {

	private final JTabbedPane tabbedPane = new JCustomTabbedPane(JTabbedPane.NORTH);

	private List<Biosample> biosamples;
	private ContainerType missingType;

	public PrintingDlg(Collection<Biosample> colBiosamples) {
		super(UIUtils.getMainFrame(), "Label Printer");

		this.biosamples = JPAUtil.reattach(colBiosamples);

		List<Container> containers = Biosample.getContainers(biosamples, true);

		//Find biosamples without a ContainerType
		if(missingType==null) {
			boolean allWithoutContainer = true;
			for (Container container: containers) {
				if(container.getContainerType()!=null) {
					allWithoutContainer = false;
				}
			}

			if(allWithoutContainer) {
				missingType = (ContainerType) JOptionPane.showInputDialog(this, "What is the container's type of those biosamples?", "Missing ContainerType", JOptionPane.OK_CANCEL_OPTION, null,  ContainerType.values(), null);
				if(missingType==null) return;
			}
		}

		//TabbedPane
		//Filters and Splits locations by ContainerType
		ListHashMap<ContainerType, Container> type2containers = new ListHashMap<>();
		for (Biosample b : biosamples) {
			if(b.getContainer()!=null && b.getContainerType()!=null) {
				Container container = b.getContainer();
				if(type2containers.get(b.getContainerType())==null || !type2containers.get(b.getContainerType()).contains(container)) {
					type2containers.add(b.getContainerType(), container);
				}
			} else if(missingType!=null) {
				Container container = new Container(missingType);
				b.setContainer(container);
				assert container.getBiosamples().contains(b);
				type2containers.add(missingType, container);
			}
		}

		//Creates a new tab for each ContainerType
		Set<ContainerType> containerTypes = new TreeSet<>(type2containers.keySet());
		for (ContainerType containerType : containerTypes) {
			int n;
			List<Container> list = type2containers.get(containerType);
			n = list.size();

			String tabName = "<html><div style='text-align:left;width:100px'>" +
					"<b style='font-size:11px'>" + containerType.getName() + "</b><br>" +
					"<span style='font-size:10px;font-weight:plain'>   - " + n + " label" + (n>1?"s":"") + " -  </span>" +
					"</div></html>";

			PrintingTab tab = new PrintingTab(this, containerType);
			tabbedPane.add(tabName, tab);
			tabbedPane.setIconAt(tabbedPane.getTabCount()-1, new ImageIcon(containerType.getImage(22)));
			tab.setRows(list);
		}

		//ContentPane
		setContentPane(tabbedPane);
		UIUtils.adaptSize(this, 1150, 800);
		setVisible(true);
	}






}

