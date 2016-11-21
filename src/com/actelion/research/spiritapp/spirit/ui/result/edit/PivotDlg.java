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

package com.actelion.research.spiritapp.spirit.ui.result.edit;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Biosample.HierarchyMode;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.Test;
import com.actelion.research.spiritcore.business.result.TestAttribute;
import com.actelion.research.spiritcore.business.study.Phase;
import com.actelion.research.spiritcore.services.dao.DAOBiosample;
import com.actelion.research.util.ui.JEscapeDialog;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.exceltable.GridInputTable;

public class PivotDlg extends JEscapeDialog {
	
//	private JTextArea textArea = new JTextArea(120, 50);
	private GridInputTable gridTable = new GridInputTable();
	private EditResultTab tab;
	private List<Result> results;
	private final TestAttribute inputAtt;
	private PivotMode pivotMode;
	
	public static enum PivotMode {
		ANIMAL_INPUT,
		ANIMAL_PHASE
	}

	public PivotDlg(JDialog owner, final EditResultTab tab, final PivotMode pivotMode) throws Exception {
		super(owner, "Import from pivot table", true);
		this.tab = tab;
		final Test test = tab.getTestChoice().getSelection();
		
		if( test.getInputAttributes().size()>3) {
			throw new Exception("Pivot is only possible when there is max 3 input value");
		}
		if(pivotMode==PivotMode.ANIMAL_PHASE) {
			for (Result r : tab.getTable().getRows()) {
				if(r.getBiosample()!=null && r.getBiosample().getInheritedPhase()!=null) {
					throw new Exception("Pivot by phase is only possible when the samples don't already have a phase");
				}
			}
		}
		
		
		inputAtt = test.getInputAttributes().size()==0? null: test.getInputAttributes().get(0);
		this.pivotMode = pivotMode;
		
		populateTextArea();
		
		
		JButton importButton = new JButton("Import");
		importButton.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					String[][] table = gridTable.getTable();
					results = parse(test, table, pivotMode);
					dispose();
				} catch (Exception ex) {
					JExceptionDialog.showError(ex);
				}
			}
		});

		
		
		gridTable.selectAll();
		
		JEditorPane helpPane = new JEditorPane("text/html", "<html><body>" + 
				"<div style='margin-left-10px'> Copy and Paste your data in the table below here. The data should be formatted like this:<br>" +
				" <li> First column: the list of <b>SampleId</b>s.<br>" +
				(pivotMode!=PivotMode.ANIMAL_PHASE? " <li> If the result is linked to a specific phase, you can add a <b>Phase</b> column.<br>": "") +
				(pivotMode!=PivotMode.ANIMAL_INPUT? " <li> If the result is linked to a specific analyte, you can add a <b>Analyte</b> column.<br>": "") +
				(pivotMode==PivotMode.ANIMAL_PHASE? " <li> Then: the list of <b>Phases</b>.<br>":
					pivotMode==PivotMode.ANIMAL_INPUT && inputAtt!=null? " <li> Then: the list of <b>" + inputAtt.getName()+"s</b> with the units in brackets (ex: lung [g]).<br>" :
						"")				
				);
		helpPane.setEditable(false);
		helpPane.setBorder(BorderFactory.createEtchedBorder());
		
		JPanel contentPane = new JPanel(new BorderLayout());
		contentPane.add(BorderLayout.NORTH, helpPane);
		contentPane.add(BorderLayout.CENTER, new JScrollPane(gridTable));
		contentPane.add(BorderLayout.SOUTH, UIUtils.createHorizontalBox(
				Box.createHorizontalGlue(),
				importButton));
		setContentPane(contentPane);
		setSize(1150, 650);
		setLocationRelativeTo(owner);
		setVisible(true);

	}
	
	/**
	 * Parse a pivoted table, and try to get the pivot mode
	 * @param test
	 * @param table
	 * @param study
	 * @return
	 * @throws Exception
	 */
	public static List<Result> parse(Test test, String[][] table) throws Exception {
		PivotMode pivotMode;
		if(test.getInputAttributes().size()==0) {
			pivotMode = PivotMode.ANIMAL_PHASE;
		} else {
			pivotMode =  PivotMode.ANIMAL_INPUT;
			if(table.length<2) throw new Exception("You need to paste at least 2 lines");

//			//Assess pivot mode
//			for (int i = 2; i < table[0].length; i++) {
//				String phaseName = table[0][i].split("\\s")[0];				
//				if(study!=null && study.getPhase(phaseName)!=null) {
//					pivotMode =  PivotMode.ANIMAL_PHASE;
//					break;
//				}
//			}
		}
		
		return parse(test, table, pivotMode);
	}
	
	/**
	 * Parse a pivoted table, with the specified pivot mode
	 * 
	 * @param test
	 * @param table
	 * @param study
	 * @param mode
	 * @return
	 * @throws Exception
	 */
	public static List<Result> parse(Test test, String[][] table, PivotMode mode) throws Exception {
		if(table.length<2) throw new Exception("You need to paste at least 2 lines");

		
		TestAttribute inputAtt = test.getInputAttributes().size()<=0? null: test.getInputAttributes().get(0);
		TestAttribute inputAtt2 = test.getInputAttributes().size()<=1? null: test.getInputAttributes().get(1);
		TestAttribute inputAtt3 = test.getInputAttributes().size()<=2? null: test.getInputAttributes().get(2);
		
		List<Result> results = new ArrayList<Result>();
		
		//Parse the header and locate the input, phase, metadata, group, eln columns
		int sampleIdIndex = -1;
		int inputIndex = -1;
		int inputIndex2 = -1;
		int inputIndex3 = -1;
		int phaseIndex = -1;
		int metadataIndex = -1;
		int groupIndex = -1;
//		int elnIndex = -1;
		List<Integer> missingHeaders = new ArrayList<Integer>();
		List<String> pivotHeaders = new ArrayList<String>();
		for (int j = 0; j < table[0].length; j++) {
			if(table[0][j].replaceAll(" ", "").equalsIgnoreCase("sampleid") || table[0][j].replaceAll(" ", "").equalsIgnoreCase("animalid")) {
				if(sampleIdIndex>=0) throw new Exception("'SampleId'/'AnimalId' is present 2 times in the header");
				else sampleIdIndex = j;			
			} else if(table[0][j].equalsIgnoreCase("metadata")) {
				if(metadataIndex>=0) throw new Exception("'Metadata' is present 2 times in the header");
				else metadataIndex = j;			
//			} else if(table[0][j].equalsIgnoreCase("eln")) {
//				if(elnIndex>=0) throw new Exception("'ELN' is present 2 times in the header");
//				else elnIndex = j;									
			} else if(table[0][j].equalsIgnoreCase("group")) {
				if(groupIndex>=0) throw new Exception("'Group' is present 2 times in the header");
				else groupIndex = j;									
			} else if(table[0][j].equalsIgnoreCase("phase")) {
				if(phaseIndex>=0) throw new Exception("'Phase' is present 2 times in the header");
				else phaseIndex = j;								
			} else if(inputAtt!=null && table[0][j].equalsIgnoreCase(inputAtt.getName())) {
				if(inputIndex>=0) throw new Exception("'" + inputAtt.getName() + "' is present 2 times in the header");
				else inputIndex = j;
			} else if(inputAtt2!=null && table[0][j].equalsIgnoreCase(inputAtt2.getName())) {
				if(inputIndex2>=0) throw new Exception("'" + inputAtt2.getName() + "' is present 2 times in the header");
				else inputIndex2 = j;
			} else if(inputAtt3!=null && table[0][j].equalsIgnoreCase(inputAtt3.getName())) {
				if(inputIndex3>=0) throw new Exception("'" + inputAtt3.getName() + "' is present 2 times in the header");
				else inputIndex3 = j;
			
			} else if(j==0 || table[0][j].length()==0 && table[1][j].length()>0) {
				missingHeaders.add(j);
			} else {
				pivotHeaders.add(table[0][j]);
			}
		}
		
		
		//Guess the empty headers
		StringBuilder warnings = new StringBuilder();
		
		if(sampleIdIndex<0) {
			if(missingHeaders.size()==0) {
				throw new Exception("The 'SampleId' column is missing");
			} else {
				sampleIdIndex = missingHeaders.remove(0);
				warnings.append("The <b>SampleId</b> column is missing. We assume it is the column '"+(sampleIdIndex+1)+"'<br>");
			}
		}		
		if(mode!=PivotMode.ANIMAL_INPUT && test.getInputAttributes().size()>0 && inputIndex<0) {
			if(missingHeaders.size()==0) {
				throw new Exception("The '"+test.getInputAttributes().get(0).getName()+ "' column is missing");
			} else {
				inputIndex = missingHeaders.remove(0);
				warnings.append("The <b>"+test.getInputAttributes().get(0).getName()+ "</b> column is missing. We assume it it the column '"+(inputIndex+1)+"'<br>");
			}
		}

		if(warnings.length()>0) {
			String msg = "<html>" + warnings + "<br>" +
//				"Pivoting will be done on " + pivotHeaders + "</br>" +
				(missingHeaders.size()>0?"<span style='color:#AA0000'>The columns "+missingHeaders+" will be ignored</span>":"") +
				"</html>";
			JOptionPane.showMessageDialog(null, msg, "Pivot Warning", JOptionPane.WARNING_MESSAGE);
		}

		//PreLoad the biosamples
		Map<String, Biosample> map = new HashMap<String, Biosample>();
		if(sampleIdIndex>=0) {
			List<String> ids = new ArrayList<String>();		
			for (int i = 1; i < table.length; i++) {
				if(table[i][sampleIdIndex].length()>0) {
					ids.add(table[i][sampleIdIndex]);
				}
			}
			map = DAOBiosample.getBiosamplesBySampleIds(ids);
		}
		
		//Extract the data
		for (int i = 1; i < table.length; i++) {
			Biosample b = null;
			
			//Parse the biosample
			String[] data = table[i];
			if(data.length<=1 || data[0].trim().length()==0) continue; //Empty line
			
			if(sampleIdIndex>=0 && data[sampleIdIndex].length()>0) {
				String sampleId = data[sampleIdIndex];
				b = map.get(sampleId);
				if(b==null) b = new Biosample(sampleId);
				
				if((metadataIndex>=0 && data[metadataIndex].length()>0) || (groupIndex>=0 && data[groupIndex].length()>0) ) {
					String criteria = metadataIndex>=0? data[metadataIndex]: null;
					List<Biosample> biosamples = b.getCompatibleInFamily(HierarchyMode.CHILDREN, criteria, groupIndex>=0? data[groupIndex]: null, null);
					
					if(biosamples.size()==0) throw new Exception("The biosample "+sampleId+" "+(metadataIndex>=0? data[metadataIndex]: "")+" could not be found");
					if(biosamples.size()>1) throw new Exception("The biosample "+sampleId+" "+(metadataIndex>=0? data[metadataIndex]: "")+" has several matches: "+biosamples);
					b = biosamples.get(0);
				}
			}
					
			//Parse the phase
			Phase phase = null;
			if(mode!=PivotMode.ANIMAL_PHASE && phaseIndex>=0) {
				String phaseName = data[phaseIndex].split("\\s")[0];
				if(phaseName.trim().length()>0) {
					if(b.getInheritedStudy()==null) throw new Exception("The biosample does not belong to a study");
					phase = b.getInheritedStudy().getPhase(phaseName);
					if(phase==null && phaseName.length()>0) throw new Exception("The phase '"+phaseName+"' does not exist in "+b.getInheritedStudy().getStudyId());
				}
			}
			
			//Parse the input
			String input = "";
			if(mode!=PivotMode.ANIMAL_INPUT) {
				input = inputIndex>=0? data[inputIndex]: "";
			}
			
			String input2 = inputIndex2>=0? data[inputIndex2]: "";
			String input3 = inputIndex3>=0? data[inputIndex3]: "";
			
			//Create the results, one per column
			for (int j = 1; j < data.length; j++) {
				if(j==groupIndex || j==metadataIndex || j==phaseIndex || j==inputIndex || j==inputIndex2 || j==inputIndex3) continue;
				if(data[j]==null || data[j].length()==0) continue;
				
				String header = table[0][j].trim();
				
				if(mode==PivotMode.ANIMAL_INPUT) {
					input = header;					
				} else if(mode==PivotMode.ANIMAL_PHASE) {
					if(b.getInheritedStudy()==null) throw new Exception("You must select a study");
					String phaseName = header.split("\\s")[0];
					phase = b.getInheritedStudy().getPhase(phaseName);
					if(phase==null && phaseName.length()>0) throw new Exception("The phase '"+phaseName+"' does not exist in "+b.getInheritedStudy().getStudyId());				
				} else {
					throw new Exception("Invalid mode: "+mode);
				}
				
				
				Result result = new Result(test);
				if(inputAtt!=null) result.setValue(inputAtt, input);
				if(inputAtt2!=null) result.setValue(inputAtt2, input2);
				if(inputAtt3!=null) result.setValue(inputAtt3, input3);
				result.setPhase(phase);					
				result.setBiosample(b);
				result.setValue(test.getOutputAttributes().get(0), data[j]);
				
				results.add(result);
			}
		}
		return results;
	}

	
	private void populateTextArea() throws Exception {
		Test test = tab.getTestChoice().getSelection();
		List<Result> export = new ArrayList<>(tab.getTable().getRows());

		//Remove empty results
		for (Iterator<Result> iterator = export.iterator(); iterator.hasNext();) {
			Result result = iterator.next();
			if(result.isEmpty()) {
				iterator.remove();
			}
		}
		
		
		//Create the pivoted table
		if(export.size()<=0) {
			//
			//Create Example
			//
			export.clear();
			List<Phase> phases = new ArrayList<>();
			phases.add(new Phase("d0"));
			phases.add(new Phase("d1"));
			phases.add(new Phase("d2"));
			phases.add(new Phase("d3"));
					
			for (int i = 0; i < 40; i++) {
				Result r = new Result(test);
				r.setBiosample(new Biosample("Sample " + new DecimalFormat("00").format(i/12+1)));
				if(phases.size()>0) {
					r.setPhase(phases.get(Math.min(phases.size()-1,  (i%12) / Math.min(phases.size(), 4)) ) );
				}
				int inputIndex = 0;
				for (TestAttribute att : test.getInputAttributes()) {
					r.setValue(att, inputIndex==0? att.getName()+((i%12)%4+1): "");
					inputIndex++;
				}
				for (TestAttribute att : test.getOutputAttributes()) {
					r.setValue(att, "" + (10*i+1000));
				}
				export.add(r);
			}			
		}
		System.out.println("PivotDlg.populateTextArea() "+export);
		
		
		PivotHelper helper = new PivotHelper(export);
		
		String[][] table = new String[0][0];
		if(pivotMode==PivotMode.ANIMAL_INPUT && test.getInputAttributes().size()>0) {							
			table = helper.pivot(test.getInputAttributes().get(0).getName());
		} else if(pivotMode==PivotMode.ANIMAL_PHASE) {
			table = helper.pivot("Phase");
		}
			
		gridTable.setTable(table);		
	}

	
	public List<Result> getResults() {
		return results;
	}
	
}
