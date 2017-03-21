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

package com.actelion.research.spiritapp.spirit.ui.exchange;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;

import com.actelion.research.spiritapp.spirit.ui.lf.LF;
import com.actelion.research.spiritapp.spirit.ui.result.ResultTable;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.Test;
import com.actelion.research.spiritcore.services.dao.DAOResult;
import com.actelion.research.spiritcore.services.exchange.ExchangeMapping;
import com.actelion.research.spiritcore.services.exchange.ExchangeMapping.EntityAction;
import com.actelion.research.util.ui.JCustomLabel;
import com.actelion.research.util.ui.UIUtils;
import com.itextpdf.text.Font;

public class ResultMappingPanel extends JPanel implements IMappingPanel {

	private ImporterDlg dlg;
	
	private final Test inputTest;
	private JRadioButton r1 = new JRadioButton("Keep the existing one");
	private JRadioButton r2 = new JRadioButton("Replace the result");
	private JRadioButton r3 = new JRadioButton("Create duplicate");

	private Map<String, Result> existing;
	
	private final ResultTable table = new ResultTable() {
		public final void postProcess(Result row, int rowNo, Object value, JComponent c) {	
			super.postProcess(row, rowNo, value, c);
			if(existing!=null && existing.get(row.getTestBiosamplePhaseInputKey())!=null) c.setBackground(LF.COLOR_ERROR_BACKGROUND);
		}
	};

	public ResultMappingPanel(ImporterDlg dlg, Test inputTest, List<Result> inputResults) { 
		super(new GridLayout());
	
		this.dlg = dlg;
		this.inputTest = inputTest;
		setMinimumSize(new Dimension(200, 200));
		
		ButtonGroup group = new ButtonGroup(); group.add(r1); group.add(r2); group.add(r3);		
		r1.setSelected(true);
		r1.setToolTipText("The Result from the imported file will be ignored, and possible links will be made to the existing ones");
		r2.setToolTipText("The Result from the imported file will be replaced by the one in this file");
		r3.setToolTipText("The Result from the imported file will be added");
		
		
		JPanel existingPanel = UIUtils.createVerticalBox(BorderFactory.createEtchedBorder(),
						new JCustomLabel("What do you want to do for the results with matching test/sampleIds/phase/input? ", Font.BOLD),
						UIUtils.createHorizontalBox(r1, r2, r3, Box.createVerticalGlue()));
		existingPanel.setOpaque(true);
		existingPanel.setBackground(LF.COLOR_ERROR_BACKGROUND);
				
		if(inputResults!=null) {
			Collections.sort(inputResults);
			
			//Map existing results
			existing = DAOResult.findSimilarResults(inputResults);
			
			table.setRows(inputResults);
			
			
			existingPanel.setVisible(existing.size()>0);			
			add(UIUtils.createBox(
					new JScrollPane(table), 
					new JLabel(inputResults.size()+" "+inputTest.getName() + " (" + existing.size()+" overlapping results)"),
					existingPanel,
					null, 
					null));
		}  else {
			existingPanel.setVisible(false);
		}
		
	}
	
	public void updateView() {
		ExchangeMapping mapping = dlg.getMapping();
		EntityAction action = mapping.getTest2existingResultAction().get(inputTest.getName());
		if(action==EntityAction.SKIP) r1.setSelected(true);
		if(action==EntityAction.MAP_REPLACE) r2.setSelected(true);
		if(action==EntityAction.CREATE) r3.setSelected(true);
	}
	
	public void updateMapping() {
		ExchangeMapping mapping = dlg.getMapping();
		EntityAction action = r1.isSelected()? EntityAction.SKIP: r2.isSelected()? EntityAction.MAP_REPLACE: r3.isSelected()? EntityAction.CREATE: EntityAction.SKIP;
		mapping.getTest2existingResultAction().put(inputTest.getName(), action);
	}

}
