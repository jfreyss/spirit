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

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.actelion.research.spiritapp.spirit.ui.biosample.BiosampleTable;
import com.actelion.research.spiritapp.spirit.ui.result.ResultTable;
import com.actelion.research.spiritcore.business.Exchange;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.Test;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.util.Pair;
import com.actelion.research.spiritcore.util.Triple;
import com.actelion.research.util.ui.UIUtils;

public class ExchangePanel extends JPanel {
	
//	private Exchange exchange;	

	private TripleTable studyTable = new TripleTable();
	private JLabel studyLabel = new JLabel();	
	
	private PairTable biotypeTable = new PairTable();
	private JLabel biotypeLabel = new JLabel();	
	private BiosampleTable biosampleTable = new BiosampleTable();
	private JLabel biosampleLabel = new JLabel();
	
	private TripleTable locationTable = new TripleTable();
	private JLabel locationLabel = new JLabel();
	
	private PairTable testTable = new PairTable();
	private JLabel testLabel = new JLabel();	
	private ResultTable resultTable = new ResultTable();
	private JLabel resultLabel = new JLabel();
	
	
	public ExchangePanel() {
		super(new BorderLayout());
		
		
		add(BorderLayout.CENTER,
			UIUtils.createVerticalBox(new int[]{1,4},
				UIUtils.createTitleBox("Studies", UIUtils.createBox(new JScrollPane(studyTable), null, studyLabel)),							
				UIUtils.createHorizontalBox(new int[] {1, 3},
					UIUtils.createVerticalBox(new int[]{2,3}, 
							UIUtils.createGrid(
									UIUtils.createTitleBox("Biotypes", UIUtils.createBox(new JScrollPane(biotypeTable), null, biotypeLabel)),
									UIUtils.createTitleBox("Tests", UIUtils.createBox(new JScrollPane(testTable), null, testLabel))),
							UIUtils.createTitleBox("Locations", UIUtils.createBox(new JScrollPane(locationTable), null, locationLabel))),
					UIUtils.createVerticalBox(new int[]{1,1}, 
							UIUtils.createTitleBox("Biosamples", UIUtils.createBox(new JScrollPane(biosampleTable), null, biosampleLabel)),
							UIUtils.createTitleBox("Results", UIUtils.createBox(new JScrollPane(resultTable), null, resultLabel))
							))));
	}
	
	public void setExchange(Exchange exchange) {
		if(exchange==null) {
			exchange = new Exchange();
		}

		//StudyTable
		List<Triple<String, String, Object>> studyRows = new ArrayList<>();
		for (Study study : exchange.getStudies()) {
			studyRows.add(new Triple<String, String, Object>(study.getLocalIdOrStudyId(), study.getTitle(), study));
		}
		studyTable.setRows(studyRows);
		studyLabel.setText(studyRows.size() + " studies");

		//BiosampleTable
		List<Pair<String, Object>> biotypeRows = new ArrayList<>();
		for (Biotype biotype : exchange.getBiotypes()) {
			biotypeRows.add(new Pair<String, Object>(biotype.getName(), biotype));
		}
		biotypeTable.setRows(biotypeRows);
		biotypeLabel.setText(biotypeRows.size() + " biotypes");

		
		biosampleTable.setRows(new ArrayList<>(exchange.getBiosamples()));
		biosampleLabel.setText(exchange.getBiosamples().size() + " biosamples");
		
		
		//LocationTable
		List<Triple<String, String, Object>> locationRows = new ArrayList<>();
		for (Location l : exchange.getLocations()) {
			locationRows.add(new Triple<String, String, Object>(l.getLocationType().getName(), l.getName(), l));
		}
		locationTable.setRows(locationRows);
		locationLabel.setText(locationRows.size() + " locations");

		//ResultTable
		List<Pair<String, Object>> testRows = new ArrayList<>();
		for (Test test : exchange.getTests()) {
			testRows.add(new Pair<String, Object>(test.getName(), test));
		}
		testTable.setRows(testRows);
		testLabel.setText(testRows.size() + " tests");
		

		resultTable.setRows(new ArrayList<Result>(exchange.getResults()));
		resultLabel.setText(exchange.getResults().size() + " results");


	}
	

}
