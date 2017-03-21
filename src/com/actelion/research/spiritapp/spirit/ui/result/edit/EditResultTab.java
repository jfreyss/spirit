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
import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.actelion.research.spiritapp.spirit.ui.result.TestChoice;
import com.actelion.research.spiritapp.spirit.ui.util.closabletab.JClosableTabbedPane.IClosableTab;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.Test;
import com.actelion.research.spiritcore.business.result.TestAttribute.OutputType;
import com.actelion.research.util.CompareUtils;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.iconbutton.IconType;
import com.actelion.research.util.ui.iconbutton.JIconButton;

/**
 *
 * @author freyssj
 *
 */
public class EditResultTab extends JPanel implements IClosableTab {
	/**
	 *
	 */
	private final EditResultDlg dlg;
	private final TestChoice testChoice;

	private final EditResultTable table;
	private JButton pivotPhases = new JIconButton(IconType.PIVOT, "Pivot by Phase");
	private JButton pivotInput = new JIconButton(IconType.PIVOT, "Pivot by Input");

	public EditResultTab(EditResultDlg editResultDlg) {
		super(new BorderLayout());
		dlg = editResultDlg;

		table = new EditResultTable(dlg);
		table.setCanAddRow(editResultDlg.isEditExperimentMode());

		testChoice = new TestChoice();


		pivotPhases.setToolTipText("Enter the results using a pivoted table, with the phase as header");
		pivotInput.setToolTipText("Enter the results using a pivoted table, with one input parameter as header");

		//top results panel
		add(BorderLayout.NORTH, UIUtils.createHorizontalBox(
				new JLabel("Test:"), testChoice,
				Box.createHorizontalGlue(),
				pivotPhases, pivotInput));


		//center panel
		add(BorderLayout.CENTER, new JScrollPane(table));
		setPreferredSize(new Dimension(500, 200));


		testChoice.addActionListener(e-> {
			eventTestChanged();
		});

		pivotPhases.addActionListener(e-> {
			pivot(PivotDlg.PivotMode.ANIMAL_PHASE);
		});

		pivotInput.addActionListener(e-> {
			pivot(PivotDlg.PivotMode.ANIMAL_INPUT);
		});

		eventTestChanged();

	}

	public void pivot(PivotDlg.PivotMode mode) {
		try {
			PivotDlg dlg2 = new PivotDlg(dlg, EditResultTab.this, mode);
			List<Result> results = dlg2.getResults();
			if(results!=null) {
				dlg.addResults(results, true);
			}

		} catch (Exception ex) {
			JExceptionDialog.showError(ex);
		}
	}

	public void resetTabName() {
		Test test = testChoice.getSelection();

		int sel = -1;
		for(int i=0; i<dlg.getTabbedPane().getTabCount(); i++) {
			if(dlg.getTabbedPane().getComponentAt(i)==this) {sel = i; break;}
		}

		if(sel>=0) {
			String title;

			if(test==null) {
				title = "<br>Select Test...<br>";
			} else {
				title =  test.getFullName().replace(" - ", "<br><b>") + "</b><br>";
			}

			title = "<html>" + title + "</html>";
			dlg.getTabbedPane().setTitleAt(sel, title);
		}

	}
	public void eventTestChanged() {
		//Gets the previous test
		Test previousTest = null;
		for(Result r: table.getRows()) {
			if(!r.isEmpty() && r.getTest()!=null) {
				previousTest = r.getTest();
			}
		}

		Test test = testChoice.getSelection();
		resetTabName();
		try {

			if(test==null && previousTest!=null) {
				JExceptionDialog.showError(this, "You must select a test");
				testChoice.setSelection(previousTest);
				return;
			}

			table.updateModel(test);

			table.getModel().fireTableDataChanged();
		} catch (Exception e) {
			JExceptionDialog.showError(this, e);
			setResults(table.getRows());
		}


		if(test==null) {
			pivotPhases.setVisible(false);
			pivotInput.setVisible(false);
		} else {
			pivotPhases.setVisible(true);
			pivotInput.setVisible(test.getAttributes(OutputType.INPUT).size()>=1);
		}
	}

	public void setTest(Test test) {
		testChoice.setSelection(test);
		table.updateModel(test);
		table.getModel().fireTableDataChanged();
	}

	public void setResults(List<Result> results) {
		Collections.sort(results);
		testChoice.setEnabled(true);
		Test test = null;
		//		Study study = null;
		//		Biosample firstBiosample = null;

		if(results.size()>0) {
			test = results.get(0).getTest();
			//			firstBiosample = results.get(0).getBiosample();
			//			study = results.get(0).getStudy();


			//Validate the results given as parameter (same test, same ivv)
			for (Result result : results) {
				if(CompareUtils.compare(result.getTest(), test)!=0) {
					throw new IllegalArgumentException("All results must have the same test");
				}
				//			if(CompareUtils.compare(result.getStudy(), study)!=0) {
				//				throw new IllegalArgumentException("All results must have the same study:\n " +
				//						result.getBiosample() + " is in " + (result.getStudy()==null?"NoStudy":result.getStudy()) +
				//						" but " + (firstBiosample==null?"NoSample":firstBiosample) + " is in "+(study==null?"NoStudy":study));
				//			}

			}
		}

		//Set the test
		if(test==null) {
			test = testChoice.getSelection(); //get default selection
		} else {
			testChoice.setSelection(test);
		}

		//Update the model
		table.getModel().setRows(results);
		table.updateModel(test);
		table.getModel().fireTableDataChanged();

	}

	@Override
	public boolean isClosable() {
		return dlg.isEditExperimentMode();
	}

	@Override
	public boolean onClose() {
		if(dlg.getTabbedPane().getTabCount()<=2) {
			return false;
		}
		dlg.getResultsTabs().remove(this);
		return true;
	}


	public boolean setSelection(Result result) {
		if(result==null) return false;
		for (int i = 0; i < table.getRows().size(); i++) {
			Result r = table.getRows().get(i);
			if(result.equals(r)) {
				table.setSelection(r, null);
				Rectangle rect = table.getCellRect(i, 0, true);

				rect.y-=200;
				table.scrollRectToVisible(rect);
				return true;
			}
		}
		return false;
	}

	public boolean setSelection(Collection<Result> results) {
		if(results==null || results.isEmpty()) return false;
		table.setSelection(results);
		if(table.getSelection().size()>0) {
			table.scrollToSelection();
			return true;
		}
		return false;
	}

	public EditResultTable getTable() {
		return table;
	}

	public TestChoice getTestChoice() {
		return testChoice;
	}


	@Override
	public boolean equals(Object obj) {
		return obj==this;
	}


}