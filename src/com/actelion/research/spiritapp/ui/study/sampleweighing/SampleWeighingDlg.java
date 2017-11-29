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

package com.actelion.research.spiritapp.ui.study.sampleweighing;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.actelion.research.spiritapp.ui.SpiritFrame;
import com.actelion.research.spiritapp.ui.biosample.BiosampleList;
import com.actelion.research.spiritapp.ui.biosample.ContainerComboBox;
import com.actelion.research.spiritapp.ui.biosample.SampleIdLabel;
import com.actelion.research.spiritapp.ui.result.edit.EditResultDlg;
import com.actelion.research.spiritapp.ui.study.CreateSamplesHelper;
import com.actelion.research.spiritapp.ui.study.GroupComboBox;
import com.actelion.research.spiritapp.ui.study.PhaseComboBox;
import com.actelion.research.spiritapp.ui.study.PhaseLabel;
import com.actelion.research.spiritapp.ui.study.monitor.MonitorTextField;
import com.actelion.research.spiritapp.ui.study.monitor.MonitoringAnimalPanel;
import com.actelion.research.spiritapp.ui.util.HelpBinder;
import com.actelion.research.spiritapp.ui.util.component.BalanceDecorator;
import com.actelion.research.spiritcore.business.DataType;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.BiosampleLinker;
import com.actelion.research.spiritcore.business.biosample.BiosampleLinker.LinkerType;
import com.actelion.research.spiritcore.business.biosample.Container;
import com.actelion.research.spiritcore.business.biosample.Status;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.Test;
import com.actelion.research.spiritcore.business.result.TestAttribute;
import com.actelion.research.spiritcore.business.study.Group;
import com.actelion.research.spiritcore.business.study.Measurement;
import com.actelion.research.spiritcore.business.study.Phase;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.dao.DAOResult;
import com.actelion.research.spiritcore.services.dao.DAOTest;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.util.ui.JCustomTextField;
import com.actelion.research.util.ui.JEscapeDialog;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.SwingWorkerExtended;
import com.actelion.research.util.ui.TextChangeListener;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.exceltable.JLabelNoRepaint;
import com.actelion.research.util.ui.iconbutton.IconType;
import com.actelion.research.util.ui.iconbutton.JIconButton;

public class SampleWeighingDlg extends JEscapeDialog {

	private Study study;
	private GroupComboBox groupComboBox = new GroupComboBox();
	private PhaseComboBox phaseComboBox = new PhaseComboBox();
	private BiosampleList animalList = new BiosampleList();
	private ContainerComboBox cageComboBox = new ContainerComboBox();
	private JCheckBox onlyRequired = new JCheckBox("Show only required measurements", true);
	private JCheckBox onlyAlive = new JCheckBox("Show only samples with status alive or necropsied", true);

	private JPanel templatePanel = new JPanel();
	private BalanceDecorator balanceDecorator = new BalanceDecorator(this);

	private Test weighTest = DAOTest.getTest(DAOTest.WEIGHING_TESTNAME);
	private Test lengthTest = DAOTest.getTest(DAOTest.LENGTH_TESTNAME);
	private Test obsTest = DAOTest.getTest(DAOTest.OBSERVATION_TESTNAME);

	private List<JComponent> requiredComponents = new ArrayList<>();
	private Map<Biosample, List<Biosample>> animal2Samples = new LinkedHashMap<>();

	//	private int push = 0;
	private final String elb;

	public SampleWeighingDlg(Study s) {
		super(UIUtils.getMainFrame(), "Sample Weighing - " + s.getStudyId());

		this.study = JPAUtil.reattach(s);
		this.elb = DAOResult.suggestElb(SpiritFrame.getUsername());

		//Check creation of samples
		if(study.isSynchronizeSamples()) {
			//Always synchronize samples to match the study designy
			try {
				boolean res = CreateSamplesHelper.synchronizeSamples(study);
				if(!res) return;

				//Be sure to reload
				study = JPAUtil.reattach(s);
			} catch(Exception e) {
				JExceptionDialog.showError(e);
				return;
			}
		}



		//init components
		phaseComboBox.setValues(study.getPhases());
		groupComboBox.setValues(study.getGroups());

		List<Biosample> animals = study.getParticipantsSorted();
		animalList.setBiosamples(animals);


		List<Container> cages = Biosample.getContainers(animals);
		Collections.sort(cages);
		cages.add(new Container("NoCage"));
		cageComboBox.setValues(cages);

		ActionListener al = e-> {
			animalList.clearSelection();
			initViewInBackground();
		};
		TextChangeListener tl = e-> {
			animalList.clearSelection();
			initViewInBackground();
		};
		phaseComboBox.addTextChangeListener(tl);
		groupComboBox.addTextChangeListener(tl);
		cageComboBox.addTextChangeListener(tl);
		onlyRequired.addActionListener(al);
		onlyAlive.addActionListener(al);

		animalList.addListSelectionListener(e-> {
			if(!e.getValueIsAdjusting()) {
				initViewInBackground();
			}
		});

		//init layout
		//selectors
		JScrollPane sp = new JScrollPane(animalList);
		sp.setPreferredSize(new Dimension(220, 110));
		JPanel filterPanel = UIUtils.createTitleBox("Filters", UIUtils.createBox(
				UIUtils.createVerticalBox(
						UIUtils.createTable(3,
								new JLabel("Phase: "), phaseComboBox, null,
								new JLabel("Group: "), groupComboBox, UIUtils.createHorizontalBox(Box.createHorizontalStrut(15), new JLabel("Container: "), cageComboBox)),
						Box.createVerticalStrut(10),
						UIUtils.createHorizontalBox(onlyRequired, Box.createHorizontalGlue()),
						UIUtils.createHorizontalBox(onlyAlive, Box.createHorizontalGlue()),
						Box.createVerticalGlue()),
				null, null,
				null, sp));

		JScrollPane scrollPane = new JScrollPane(templatePanel);
		scrollPane.setPreferredSize(new Dimension(500, 400));
		scrollPane.getVerticalScrollBar().setUnitIncrement(10);

		JPanel centerPanel = new JPanel(new BorderLayout());
		centerPanel.add(BorderLayout.NORTH, filterPanel);
		centerPanel.add(BorderLayout.CENTER, scrollPane);

		//buttons
		JButton batchButton = new JIconButton(IconType.EDIT.getIcon(), "Edit In Batch Mode");
		batchButton.addActionListener(e-> {
			try {
				List<Result> results = new ArrayList<>();
				Phase phase = phaseComboBox.getSelection();
				for (Biosample topSample : animal2Samples.keySet()) {
					List<Biosample> samples = animal2Samples.get(topSample);
					//Find required results
					boolean hasRequiredWeight = false;
					boolean hasRequiredLength = false;
					for (Biosample sample : samples) {
						if(sample.getAttachedSampling()==null) {
							if(sample.getStudyAction(phase)!=null && sample.getStudyAction(phase).isMeasureWeight()) {
								hasRequiredWeight = true;
							}
						} else {
							if(sample.getAttachedSampling().isWeighingRequired()) {
								hasRequiredWeight = true;
							}
							if(sample.getAttachedSampling().isLengthRequired()) {
								hasRequiredLength = true;
							}
						}
					}
					for (Biosample sample : samples) {

						Result r1 = sample.getAuxResult(weighTest, phase);
						Result r2 = sample.getAuxResult(lengthTest, phase);
						Result r3 = sample.getAuxResult(obsTest, phase);

						if(r1!=null && hasRequiredWeight) results.add(r1);
						if(r2!=null && hasRequiredLength) results.add(r2);
						if(r3!=null) results.add(r3);

						//Find extra measurements
						if(sample.getAttachedSampling()!=null) {
							for(Measurement m: study.getAllMeasurementsFromSamplings()) {
								assert m.getTest()!=null;
								final Result result = sample.getAuxResult(m.getTest(), phase, m.getParameters());
								if(result!=null) results.add(result);
							}
						}
					}
				}
				new EditResultDlg(results);
				initViewInBackground();
			} catch(Exception ex) {
				JExceptionDialog.showError(ex);
			}
		});

		JButton okButton = new JButton("Close");
		okButton.addActionListener(e-> {
			dispose();
		});


		//contentPane
		JPanel contentPane = new JPanel(new BorderLayout());
		contentPane.add(BorderLayout.CENTER, centerPanel);
		contentPane.add(BorderLayout.SOUTH, UIUtils.createHorizontalBox(HelpBinder.createHelpButton(), balanceDecorator.getBalanceCheckBox(), Box.createHorizontalGlue(), batchButton, okButton));

		//init
		initViewInBackground();

		setContentPane(contentPane);
		UIUtils.adaptSize(this, 950, 1000);
		setLocationRelativeTo(UIUtils.getMainFrame());
		setVisible(true);
	}

	private void initViewInBackground() {
		final Group group = groupComboBox.getSelection();
		final Phase phase = phaseComboBox.getSelection();
		final Container cage = cageComboBox.getSelection();
		final List<Biosample> animals = animalList.getSelection();

		templatePanel.removeAll();
		getContentPane().validate();
		getContentPane().repaint();


		final List<Biosample> list = new ArrayList<Biosample>();
		new SwingWorkerExtended("Loading", templatePanel, SwingWorkerExtended.FLAG_ASYNCHRONOUS20MS) {
			@Override
			protected void doInBackground() throws Exception {
				//				try {
				//					push++;

				//Apply filters
				study = JPAUtil.reattach(study);
				List<Biosample> filtered = new ArrayList<>();
				for(Biosample a: study.getParticipantsSorted()) {

					//Filter by animals
					if(onlyAlive.isSelected() && a.getStatus()!=Status.INLAB && a.getStatus()!=Status.NECROPSY) continue;

					if(animals.size()>0 && !animals.contains(a)) continue;

					//Filter by group
					if(group!=null && !group.equals(a.getInheritedGroup())) continue;

					//Filter by cage
					if(cage!=null) {
						if(cage.getContainerType()==null && a.getContainerType()==null) {
							//ok
						} else if(cage.getContainerId()!=null && cage.getContainerId().equals(a.getContainer().getContainerId())) {
							//ok
						} else {
							continue;
						}
					}


					//Retrieve the samples
					List<Biosample> samples = a.getSamplesFromStudyDesign(phase, onlyRequired.isSelected());
					if(samples.size()>0) {
						filtered.add(a);
						list.add(a);
						list.addAll(samples);
					}
				}
				animal2Samples.clear();
				for (Biosample sample : list) {
					Biosample top = sample.getTopParentInSameStudy();
					animal2Samples.putIfAbsent(top, new ArrayList<>());
					animal2Samples.get(top).add(sample);
				}

				//Retrieve results to populate the fields
				DAOResult.attachOrCreateStudyResultsToSamples(study, list, phase, elb);
			}

			@Override
			protected void done() {

				if(animals.size()==0) animalList.setBiosamples(Biosample.getTopParentsInSameStudy(list));

				List<Component> templateComponents = new ArrayList<>();
				templatePanel.removeAll();
				templatePanel.setLayout(new GridLayout(1,1));
				requiredComponents.clear();


				int n = 0;

				//Create panels for each animal
				boolean showOnlyRequired = onlyRequired.isSelected();

				for (Biosample topSample : new ArrayList<>(animal2Samples.keySet())) {
					List<Biosample> samples = animal2Samples.get(topSample);

					//animalHeader
					JPanel animalHeader = MonitoringAnimalPanel.createAnimalPanel(++n, topSample, topSample.getExpectedEndPhase());

					//Required comps?
					boolean hasRequiredWeight = false;
					boolean hasRequiredLength = false;
					for (Biosample sample : samples) {
						//Find required samples
						if(sample.getAttachedSampling()==null) {
							if(sample.getStudyAction(phase)!=null && sample.getStudyAction(phase).isMeasureWeight()) {
								hasRequiredWeight = true;
							}
						} else {
							if(sample.getAttachedSampling().isWeighingRequired()) {
								hasRequiredWeight = true;
							}
							if(sample.getAttachedSampling().isLengthRequired()) {
								hasRequiredLength = true;
							}
						}
					}

					List<Component> comps = new ArrayList<>();
					//TableHeader
					comps.add(null);
					comps.add(null);
					comps.add(null);
					JLabel weightLabel = new JLabel("Weight [g] ");
					weightLabel.setVisible(!showOnlyRequired || hasRequiredWeight);
					comps.add(weightLabel);
					JLabel lenLabel = new JLabel("Len. [mm] ");
					lenLabel.setVisible(!showOnlyRequired || hasRequiredLength);
					comps.add(lenLabel);
					for(Measurement m: study.getAllMeasurementsFromSamplings()) {
						assert m.getTest()!=null;
						List<TestAttribute> tas = m.getTest().getOutputAttributes();
						for (int i = 0; i < tas.size(); i++) {
							JLabelNoRepaint label = new JLabelNoRepaint(m.getDescription()+" \n"+tas.get(i).getName()+" ");
							label.setOpaque(false);
							comps.add(label);
						}
					}
					comps.add(new JLabel("Observation"));
					int cols = comps.size();

					for (Biosample sample : samples) {
						//Create the fields
						MonitorTextField weightTF = new MonitorTextField(sample.getAuxResult(weighTest, phase), 0, false);
						MonitorTextField lengthTF = new MonitorTextField(sample.getAuxResult(lengthTest, phase), 0, false);
						MonitorTextField commentsTF = new MonitorTextField(sample.getAuxResult(obsTest, phase), 0, false);


						//Find required samples
						if(sample.getAttachedSampling()==null) {
							lengthTF.setVisible(false);
							if(sample.getStudyAction(phase)!=null && sample.getStudyAction(phase).isMeasureWeight()) {
								weightTF.setRequired(true);
								requiredComponents.add(weightTF);
								hasRequiredWeight = true;
							}
						} else {
							weightTF.setVisible(!showOnlyRequired || sample.getAttachedSampling().isWeighingRequired());
							lengthTF.setVisible(!showOnlyRequired || sample.getAttachedSampling().isLengthRequired());
							if(sample.getAttachedSampling().isWeighingRequired()) {
								weightTF.setRequired(true);
								requiredComponents.add(weightTF);
								hasRequiredWeight = true;
							}
							if(sample.getAttachedSampling().isLengthRequired()) {
								lengthTF.setRequired(true);
								requiredComponents.add(lengthTF);
								hasRequiredLength = true;
							}
							if(sample.getAttachedSampling().isCommentsRequired()) {
								commentsTF.setRequired(true);
								requiredComponents.add(commentsTF);
							}
						}

						SampleIdLabel sampleIdLabel2 = new SampleIdLabel(sample, true, false);
						sampleIdLabel2.setExtraDisplay(new BiosampleLinker(LinkerType.COMMENTS, null), true);

						comps.add(Box.createHorizontalStrut(15*sample.getParentHierarchy().size()));
						comps.add(sampleIdLabel2);
						comps.add(new PhaseLabel(sample.getInheritedPhase(), sample.getInheritedGroup()));
						comps.add(weightTF);
						comps.add(lengthTF);

						//Find extra measurements
						final List<MonitorTextField> formulaTextFields = new ArrayList<>();
						if(sample.getAttachedSampling()!=null) {
							for(Measurement m: study.getAllMeasurementsFromSamplings()) {
								assert m.getTest()!=null;
								List<TestAttribute> tas = m.getTest().getOutputAttributes();
								final Result result = sample.getAuxResult(m.getTest(), phase, m.getParameters());
								assert result!=null;
								for (int i = 0; i < tas.size(); i++) {
									TestAttribute ta = tas.get(i);
									MonitorTextField tf = new MonitorTextField(result, i, sample.getAttachedSampling().getMeasurements().contains(m));
									tf.setVisible(!showOnlyRequired || tf.isRequired());
									comps.add(tf);


									if(ta.getDataType()==DataType.FORMULA) {
										tf.setEnabled(false);
										formulaTextFields.add(tf);
									} else {
										if(tf.isRequired()) requiredComponents.add(tf);
										tf.addFocusListener(new FocusAdapter() {
											@Override
											public void focusLost(FocusEvent e) {
												if(formulaTextFields.size()==0) return;
												DAOResult.computeFormula(Collections.singletonList(result));
												for(MonitorTextField ftf: formulaTextFields) {
													ftf.refreshText();
												}
											}
										});
									}
								}
							}
						}
						comps.add(commentsTF);
					}


					templateComponents.add(UIUtils.createBox(BorderFactory.createEtchedBorder(), UIUtils.createTable(cols, comps), animalHeader));

				}

				templateComponents.add(Box.createVerticalGlue());
				templatePanel.add(UIUtils.createVerticalBox(templateComponents));


				//Init required components
				for (int i = 0; i < requiredComponents.size()-1; i++) {
					final JComponent next = (requiredComponents.get(i+1));
					if(requiredComponents.get(i) instanceof JCustomTextField) {
						((JCustomTextField)requiredComponents.get(i)).addActionListener(new ActionListener() {
							@Override
							public void actionPerformed(ActionEvent e) {
								next.requestFocusInWindow();
							}
						});
					} else {
						System.err.println("Not supported: "+ requiredComponents.get(i));
					}
				}


				SampleWeighingDlg.this.getContentPane().validate();
				SampleWeighingDlg.this.getContentPane().repaint();
			}
		};

	}


}
