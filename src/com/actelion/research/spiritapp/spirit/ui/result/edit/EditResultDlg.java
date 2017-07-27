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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import com.actelion.research.spiritapp.spirit.ui.SpiritFrame;
import com.actelion.research.spiritapp.spirit.ui.util.POIUtils;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeListener;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeType;
import com.actelion.research.spiritapp.spirit.ui.util.closabletab.JClosableTabbedPane;
import com.actelion.research.spiritapp.spirit.ui.util.component.JSpiritEscapeDialog;
import com.actelion.research.spiritapp.spirit.ui.util.correction.Correction;
import com.actelion.research.spiritapp.spirit.ui.util.correction.CorrectionDlg;
import com.actelion.research.spiritapp.spirit.ui.util.correction.CorrectionMap;
import com.actelion.research.spiritapp.spirit.ui.util.editor.ImageEditorPane;
import com.actelion.research.spiritcore.business.DataType;
import com.actelion.research.spiritcore.business.Quality;
import com.actelion.research.spiritcore.business.ValidationException;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.ResultQuery;
import com.actelion.research.spiritcore.business.result.ResultValue;
import com.actelion.research.spiritcore.business.result.Test;
import com.actelion.research.spiritcore.business.result.TestAttribute;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.spiritcore.services.dao.DAOBiosample;
import com.actelion.research.spiritcore.services.dao.DAOResult;
import com.actelion.research.spiritcore.services.dao.DAOTest;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.util.ui.JCustomLabel;
import com.actelion.research.util.ui.JCustomTextField;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.SwingWorkerExtended;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.iconbutton.IconType;
import com.actelion.research.util.ui.iconbutton.JIconButton;

public class EditResultDlg extends JSpiritEscapeDialog {

	private final JClosableTabbedPane tabbedPane = new JClosableTabbedPane();
	private final List<EditResultTab> resultsTabs = new ArrayList<>();

	private JTextField elbTextField = new JCustomTextField(JCustomTextField.ALPHANUMERIC, 15);

	private boolean newExperiment;
	private final boolean editWholeExperiment;


	/**
	 * Constructor used to edit results from a elb
	 * @param elb
	 */
	public EditResultDlg(List<Result> myResults) {
		super(UIUtils.getMainFrame(), "Edit Results", EditResultDlg.class.getName());
		editWholeExperiment = false;
		//Reload results in the current session
		List<Result> results = JPAUtil.reattach(myResults);
		if(results.size()>40000) {
			JExceptionDialog.showError(this, "The maximum number of results allowed is 40000.");
			return;
		}

		Collections.sort(results);
		try {
			for(Result result: results) {
				if(!SpiritRights.canEdit(result, SpiritFrame.getUser()))	{
					throw new Exception("You are not allowed to edit "+result);
				}
			}
			newExperiment = false;
			initTabbedPane(results);

			elbTextField.setEnabled(false);
			init();
		} catch (Exception e) {
			JExceptionDialog.showError(e);
		}
	}


	public EditResultDlg(final String elb, final Result selectedResult) {
		super(UIUtils.getMainFrame(), "Edit Results - " + elb, EditResultDlg.class.getName());
		editWholeExperiment = true;

		new SwingWorkerExtended("Loading results", tabbedPane, SwingWorkerExtended.FLAG_ASYNCHRONOUS) {
			List<Result> results;
			@Override
			protected void doInBackground() throws Exception {
				ResultQuery query = ResultQuery.createQueryForElb(elb);
				results = DAOResult.queryResults(query, null);
				Collections.sort(results);

				newExperiment = false;
				if(results.size()==0) {
					throw new Exception("The ELB didn't contain any results");
				} else if(results.size()>40000) {
					throw new Exception("The ELB contains " + results.size() +" results. The maximum allowed is 40000.");
				}
				newExperiment = false;
			}
			@Override
			protected void done() {
				try {
					results = JPAUtil.reattach(results);
					initTabbedPane(results);
					setSelection(selectedResult);
				} catch (Exception e) {
					JExceptionDialog.showError(e);
					dispose();
					return;
				}

				elbTextField.setEnabled(false);

			}
		};
		init();

	}

	/**
	 * Constuctor used when entering a new experiment
	 * @param initialResults
	 * @param phase
	 */
	public EditResultDlg(boolean askForElb, List<Result> initialResults) {
		super(UIUtils.getMainFrame(), "Results - " + (askForElb?"New":"Edit"), EditResultDlg.class.getName());
		assert initialResults!=null;
		editWholeExperiment = true;

		try {
			List<Result> toDisplay = new ArrayList<>();
			if(askForElb) {
				//New experiment, ask for the elb (new or existing)
				EditResultSelectElbDlg dlg = new EditResultSelectElbDlg();
				String elb = dlg.getReturnedValue();
				if(elb==null) return;

				List<Result> existing = DAOResult.queryResults(ResultQuery.createQueryForElb(elb), null);
				if(existing.size()>0) {
					newExperiment = false;
					//This is an existing elb, check the rights
					if(!SpiritRights.canEditResults(existing, SpiritFrame.getUser())) {
						throw new Exception("You are not allowed to edit / append results to this elb");
					}
					toDisplay.addAll(existing);
				} else {
					newExperiment = true;
					elbTextField.setText(elb);
				}


				for (Result result : initialResults) {
					result.setElb(elb);
				}
				toDisplay.addAll(initialResults);
			} else {
				toDisplay.addAll(initialResults);
				for (Result result : initialResults) {
					if(result.getId()>=0) newExperiment = false;
				}
			}

			initTabbedPane(toDisplay);
			setSelection(initialResults);
			init();
		} catch (Exception e) {
			JExceptionDialog.showError(e);
		}


	}

	public void setSelection(Result result) {
		for (int index = 0; index < resultsTabs.size(); index++) {
			EditResultTab tab = resultsTabs.get(index);
			boolean success = tab.setSelection(result);
			if(success) {
				tabbedPane.setSelectedIndex(index);
			}
		}
	}

	public void setSelection(Collection<Result> results) {
		for (int index = 0; index < resultsTabs.size(); index++) {
			EditResultTab tab = resultsTabs.get(index);
			boolean success = tab.setSelection(results);
			if(success) {
				tabbedPane.setSelectedIndex(index);
			}
		}
	}

	private void init() {
		//Build the layout
		JPanel topPanel = null;
		if(editWholeExperiment) {
			topPanel = UIUtils.createTitleBox(UIUtils.createHorizontalBox(new JLabel("ELB: "), elbTextField, Box.createHorizontalGlue()));
		}

		JButton deleteButton = new JIconButton(IconType.DELETE, "Delete experiment");
		deleteButton.setVisible(editWholeExperiment && !newExperiment);
		deleteButton.setDefaultCapable(false);
		deleteButton.addActionListener(e-> {
			eventDelete();
		});

		JButton excelButton = new JIconButton(IconType.EXCEL, "To Excel");
		excelButton.addActionListener(e-> {
			try {
				EditResultTab tab = resultsTabs.get(tabbedPane.getSelectedIndex());
				POIUtils.exportToExcel(tab.getTable().getTabDelimitedTable(), POIUtils.ExportMode.HEADERS_TOP);
			} catch (Exception ex) {
				JExceptionDialog.showError(EditResultDlg.this, ex);
			}
		});

		JButton okButton = new JIconButton(IconType.SAVE, editWholeExperiment? (!newExperiment? "Update Experiment": "Save Experiment"): "Update Results");
		okButton.addActionListener(e-> {
			final SpiritUser user = SpiritFrame.getUser();
			assert user!=null;

			new SwingWorkerExtended("Saving", getContentPane()) {
				private Exception exception = null;
				private List<Result> toSave;
				@Override
				protected void doInBackground() throws Exception {
					try {
						toSave = validateResults();
						if(toSave==null) return;

						if(editWholeExperiment) {
							DAOResult.persistExperiment(newExperiment, elbTextField.getText().trim(), toSave, user);

						} else {
							//Save the visible results
							DAOResult.persistResults(toSave, user);
						}
					} catch (Exception e) {
						e.printStackTrace();
						exception = e;
					}
				}
				@Override
				protected void done() {
					if(exception==null) {

						if(toSave==null) return;
						try {
							JOptionPane.showMessageDialog(EditResultDlg.this, toSave.size() + " Results saved", "Success", JOptionPane.INFORMATION_MESSAGE);
							SpiritChangeListener.fireModelChanged(SpiritChangeType.MODEL_ADDED, Result.class, toSave);
							dispose();
						} catch(Exception e) {
							JExceptionDialog.showError(EditResultDlg.this, e);
						}

					} else if(exception instanceof ValidationException) {
						ValidationException e = (ValidationException) exception;
						String col = e.getCol();
						Result result = (Result) e.getRow();
						int index = -1;
						for (int i = 0; index<0 && i < resultsTabs.size(); i++) {
							EditResultTab tab = resultsTabs.get(i);
							index = tab.getTable().getRows().indexOf(result);
						}
						if(index>=0 && index<tabbedPane.getTabCount()) {
							tabbedPane.setSelectedIndex(index);
							EditResultTable table = resultsTabs.get(index).getTable();
							table.setSelection(result, col);
						} else {
							System.err.println("Could not select "+e.getCol()+" "+e.getRow()+ ">? " + index);
						}
						JExceptionDialog.showError(EditResultDlg.this, e);
					} else {
						JExceptionDialog.showError(EditResultDlg.this, exception);
					}
				}

			};
		});

		setContentPane(UIUtils.createBox(tabbedPane, topPanel, UIUtils.createHorizontalBox(deleteButton, excelButton, Box.createHorizontalGlue(), okButton)));
		UIUtils.adaptSize(this, 1000, 780);
		setVisible(true);
	}

	/**
	 * Set results and update where elb/events could be selected (top or in table)
	 * Must be called before init()
	 * @param results
	 */
	private void initTabbedPane(List<Result> results) throws Exception {
		//Check rights
		List<Result> notAllowed = new ArrayList<>();
		List<Result> allowed = new ArrayList<>();
		for (Result result : results) {
			if(!SpiritRights.canEdit(result, SpiritFrame.getUser())) {
				notAllowed.add(result);
			} else {
				allowed.add(result);
			}
		}
		results = allowed;
		if(results.size()>0 && allowed.size()==0) {
			throw new Exception("You are not allowed to edit those results");
		} else if(notAllowed.size()>0) {
			throw new Exception("Due to limited rights, You are not allowed to edit all those results");
		}

		resultsTabs.clear();


		String elb = "";
		elbTextField.setEnabled(false);
		if(editWholeExperiment) {
			//There should be max 1 elb
			Set<String> elbs = Result.getElbs(results);
			if(elbs.size()==1) {
				elb = elbs.iterator().next();
				elbTextField.setText(elb);
			} else if(elbs.size()>1) {
				throw new Exception("The results should not be linked to 2 elbs ("+elbs+")");
			}
		}

		//Center panel
		tabbedPane.addChangeListener(e-> {
			int sel = tabbedPane.getSelectedIndex();
			if(sel<0) return;
			String title = tabbedPane.getTitleAt(sel);
			if(title.equals("+") && sel>=0 && sel==tabbedPane.getTabCount()-1) {
				tabbedPane.removeTabAt(sel);

				//Create a new resultsPanel
				EditResultTab resultsTab = resultsTabs.size()>0? resultsTabs.get(resultsTabs.size()-1): null;
				EditResultTab newPanel = new EditResultTab(EditResultDlg.this);
				newPanel.getTestComboBox().setSelection(resultsTab==null? null: resultsTab.getTestComboBox().getSelection());
				resultsTabs.add(newPanel);
				newPanel.setResults(new ArrayList<Result>());
				tabbedPane.addTab("Select Test", newPanel);
				tabbedPane.setSelectedIndex(sel);

				//Update the tab
				newPanel.getTestComboBox().reset();
				newPanel.getTestComboBox().setSelection(null);

				if(editWholeExperiment) {
					tabbedPane.addTab("+", new JPanel());
					tabbedPane.setSelectedIndex(sel);
				}
			}
		});

		addResults(results, false);

	}

	protected EditResultTab getCurrentTab() {
		int index = tabbedPane.getSelectedIndex();
		if(index<0 || index>=resultsTabs.size()) return null;
		return resultsTabs.get(index);
	}

	/**
	 *
	 * @param results
	 * @param emptyCurrentTab
	 */
	protected void addResults(List<Result> results, boolean emptyCurrentTab) {
		EditResultTab current = getCurrentTab();

		Map<EditResultTab, List<Result>> tab2results = new HashMap<>();
		Map<Test, List<Result>> mapTest = Result.mapTest(results);
		List<Test> tests = new ArrayList<>(mapTest.keySet());
		for (Test test : tests) {
			EditResultTab tab = new EditResultTab(this);
			tab2results.put(tab, mapTest.get(test));
			resultsTabs.add(tab);
		}

		if(emptyCurrentTab) {
			removeTab(current);
		}

		//Add an empty tab, if there are no results
		if(resultsTabs.size()==0) {
			EditResultTab tab = new EditResultTab(this);
			resultsTabs.add(tab);
			tab2results.put(tab, new ArrayList<Result>());
		}

		//Update the components
		updateCenterPanel();

		//Set the results
		for(EditResultTab tab: tab2results.keySet()) {
			tab.setResults(tab2results.get(tab));
		}

		//Delete tabs if nresults =0
		for (EditResultTab resultTab : new ArrayList<EditResultTab>(resultsTabs)) {
			if(resultsTabs.size()<=1) break;
			boolean empty = true;
			for(Result result: resultTab.getTable().getRows()) {
				if(!result.isEmpty()) {empty = false; break;}
			}
			if(empty) {
				removeTab(resultTab);
			}
		}

		tabbedPane.setSelectedIndex(Math.max(0, tabbedPane.getTabCount()-2));
	}

	protected void removeTab(EditResultTab tab) {
		int index = resultsTabs.indexOf(tab);
		if(index<0) return;
		resultsTabs.remove(index);
		updateCenterPanel();
	}

	private void updateCenterPanel() {
		for (int i = tabbedPane.getTabCount()-1; i >=0 ; i--) {

			tabbedPane.removeTabAt(i);
		}

		for (EditResultTab tab : resultsTabs) {
			tabbedPane.addTab("", tab);
			tab.resetTabName();

		}
		if(editWholeExperiment) {
			tabbedPane.addTab("+", new JPanel());
		}
	}

	public void eventDelete() {
		try {
			String elb = elbTextField.getText();

			//Reload the results
			final List<Result> results = DAOResult.queryResults(ResultQuery.createQueryForElb(elb), SpiritFrame.getUser());

			//Check rights to be really sure
			for (Result result : results) {
				if(!SpiritRights.canDelete(result, SpiritFrame.getUser())) {
					throw new Exception("You are not allowed to delete those results");
				}
			}

			int res = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete " + elb + " (" + results.size() + " results in "+Result.getTests(results).size()+" tests) ?", "Delete Experiment", JOptionPane.YES_NO_OPTION);
			if(res!=JOptionPane.YES_OPTION) return;


			new SwingWorkerExtended("Delete "+elb, getContentPane()) {
				@Override
				protected void doInBackground() throws Exception {
					DAOResult.deleteResults(results, SpiritFrame.getUser());
				}
				@Override
				protected void done() {
					SpiritChangeListener.fireModelChanged(SpiritChangeType.MODEL_DELETED, Result.class, results);
					JOptionPane.showMessageDialog(EditResultDlg.this, results.size() + " Results deleted", "Success", JOptionPane.INFORMATION_MESSAGE);
					dispose();
				}
			};
		} catch (Exception e) {
			JExceptionDialog.showError(this, e);

		}
	}

	/**
	 *
	 * @param validateOnly
	 * @return List of result to be saved, null if canceled
	 * @throws Exception if cannot be validated
	 */
	public List<Result> validateResults() throws Exception {

		//Synchronize the results with the elb, test, phases
		final List<Result> toSave = new ArrayList<>();
		List<Result> warnQualityResults = new ArrayList<>();
		final String elb = elbTextField.getText().trim();
		for (EditResultTab tab : resultsTabs) {
			Test test = tab.getTable().getModel().getTest();
			if(test==null) continue; //last tab
			for(Result result: tab.getTable().getRows()) {
				if(result.isEmpty()) continue;

				if(elb!=null && elb.length()>0) result.setElb(elb);

				if(result.getBiosample()==null || result.getBiosample().getSampleId().length()==0) {
					throw new ValidationException("SampleId is required", result, "SampleId");
				}
				if(result.getBiosample()!=null && result.getBiosample().getQuality()!=null && result.getBiosample().getQuality().getId()<Quality.VALID.getId() && (result.getQuality()==null || result.getQuality().getId()>result.getBiosample().getQuality().getId())) {
					warnQualityResults.add(result);
				}
				if(result.getBiosample()!=null && result.getBiosample().getId()<=0 && result.getBiosample().getSampleId().length()>0) {
					Biosample b = DAOBiosample.getBiosample(result.getBiosample().getSampleId());
					if(b==null) throw new ValidationException(result.getBiosample().getSampleId() + " is not a valid sampleId", result, "SampleId");
					result.setBiosample(b);
				}


				toSave.add(result);
			}
		}

		//Check that all results linked to a biosample have a study
		for (Result result : toSave) {
			Study s = result.getBiosample()!=null && result.getBiosample().getInheritedStudy()!=null? result.getBiosample().getInheritedStudy(): null;
			if(s!=null) {
				if(result.getPhase()!=null && result.getPhase().getStudy().getId()!=s.getId()) {
					throw new ValidationException("The phase for the result "+result+" should be on study "+result.getPhase().getStudy().getLocalId(), result, "Phase");
				}
			}
		}

		//Check the autocompletion fields for approximate spelling
		CorrectionMap<TestAttribute, Result> correctionMap = new CorrectionMap<TestAttribute, Result>();
		//		int obviousProblems = 0;
		for (Result result : toSave) {
			Test test = result.getTest();
			for (TestAttribute att : test.getAttributes()) {
				ResultValue rv = result.getResultValue(att);
				if(rv==null || rv.getValue()==null || rv.getValue().length()==0) continue;
				String value = rv.getValue();

				if(att.getDataType()==DataType.LIST) {
					//Choice

					Set<String> possibleValues = new TreeSet<String>(Arrays.asList(att.getParametersArray()) );
					if(!possibleValues.contains(value)) {
						Correction<TestAttribute, Result> correction = correctionMap.getCorrection(att, value);

						if(correction==null) {
							correction = correctionMap.addCorrection(att, value, new ArrayList<String>(possibleValues), true);
						}
						correction.getAffectedData().add(result);
						//						obviousProblems++;
					}

				} else if(att.getDataType()==DataType.AUTO) {
					//Autocompletion
					Set<String> possibleValues = DAOTest.getAutoCompletionFields(att);
					if(!possibleValues.contains(value)) {
						Correction<TestAttribute, Result> correction = correctionMap.getCorrection(att, value);
						if(correction==null) {
							correction = correctionMap.addCorrection(att, value, new ArrayList<String>(possibleValues), false);
						}
						correction.getAffectedData().add(result);
						//						if(correction.getSuggestedValue()!=null) obviousProblems++;
					}
				}
			}
		}

		//Display the correction dialog
		if(correctionMap.getItemsWithSuggestions()>0) {
			CorrectionDlg<TestAttribute, Result> dlg = new CorrectionDlg<TestAttribute, Result>(this, correctionMap) {
				@Override
				public String getSuperCategory(TestAttribute att) {
					return att.getTest().getFullName();
				}
				@Override
				protected String getName(TestAttribute att) {
					return att.getName();
				}
				@Override
				protected void performCorrection(Correction<TestAttribute, Result> correction, String newValue) {
					for (Result result : correction.getAffectedData()) {
						result.getResultValue(correction.getAttribute()).setValue(newValue);
					}
				}
			};
			if(dlg.getReturnCode()!=CorrectionDlg.OK) return null;
		}

		//Check unicity of results
		Map<String, List<Result>> input2Results = new HashMap<>();
		List<Result> duplicated = new ArrayList<>();
		for(Result r: toSave) {
			String key = r.getTest().getId()+"_"+(r.getPhase()==null?"":r.getPhase().getId())+"_"+(r.getBiosample()==null?"":r.getBiosample().getSampleId())+"_"+r.getInputResultValuesAsString();
			List<Result> list = input2Results.get(key);
			if(list==null) {
				list = new ArrayList<>();
				input2Results.put(key, list);
			} else {
				duplicated.add(r);
			}
			list.add(r);
		}
		StringBuilder sb = new StringBuilder();
		String s = null;
		for (List<Result> list  : input2Results.values()) {
			if(list.size()>1) {
				Result r = list.get(0);
				String ns = "<b><u>" + r.getTest().getFullName() + " " + (r.getPhase()==null?"": r.getPhase().getStudy().getLocalId()+" - "+ r.getPhase().getLabel()) + "</u></b><br>";
				if(!ns.equals(s)) {
					s = ns;
					sb.append(s);
				}
				sb.append("<table border=0>");
				for (Result r2 : list) {
					sb.append("<tr>");
					sb.append("<td><b>" + (r.getBiosample()==null?"": " " + r.getBiosample().getSampleId()) + "</b></td>");
					sb.append("<td>" + r.getInputResultValuesAsString() + "</td>");
					sb.append("<td>" + r2.getOutputResultValuesAsString() + "</td>");
					sb.append("</tr>");
				}
				sb.append("<tr></tr>");
				sb.append("</table>");
			}
		}
		if(sb.length()>0) {

			int res = showConfirmDialog(this,
					"Some results are duplicated. What do you want to do?",
					"<html>"+sb.toString()+"</html>",
					"Duplicated results?",
					new String[] {"Keep duplicates", "Keep one result", "Cancel"});
			if(res==0) {
				//OK
			} else if(res==1) {
				toSave.removeAll(duplicated);
				JExceptionDialog.showInfo(this, duplicated.size()+" duplicated results removed");
				initTabbedPane(toSave);
			} else {
				return null;
			}
		}

		if(warnQualityResults.size()>0) {
			int res = JOptionPane.showConfirmDialog(this, "Some of the results are linked to biosamples with questionable or bogus quality.\nWould you like to set the quality of those results to questionable or bogus?", "Quality of the biosamples?", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
			if(res==JOptionPane.NO_OPTION) {
				//OK
			} else if(res==JOptionPane.YES_OPTION) {
				for (Result result : warnQualityResults) {
					result.setQuality(result.getBiosample().getQuality());
				}
			} else {
				return null;
			}
		}

		return toSave;
	}

	public static int showConfirmDialog(Component parent, String header, String longMessage, String title, String[] options) {
		JEditorPane textArea = new ImageEditorPane(longMessage);
		textArea.setEditable(false);
		textArea.setCaretPosition(0);
		textArea.setPreferredSize(new Dimension(400, 400));

		JPanel panel = UIUtils.createBox(new JScrollPane(textArea), new JCustomLabel(header, Font.BOLD));
		int res = JOptionPane.showOptionDialog(parent, panel, title, 0, JOptionPane.QUESTION_MESSAGE, null, options, null);
		return res;

	}


	@Override
	protected boolean mustAskForExit() {
		if(super.mustAskForExit()) return true;
		for (EditResultTab tab : resultsTabs) {
			if(tab.getTable().getUndoManager().hasChanges()) return true;
		}
		return false;
	}

	public boolean isEditExperimentMode() {
		return editWholeExperiment;
	}

	public JTabbedPane getTabbedPane() {
		return tabbedPane;
	}

	public List<EditResultTab> getResultsTabs() {
		return resultsTabs;
	}

}
