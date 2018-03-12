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

package com.actelion.research.spiritapp.ui.study.wizard;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;

import com.actelion.research.spiritapp.ui.study.PhaseComboBox;
import com.actelion.research.spiritapp.ui.study.depictor.Selection;
import com.actelion.research.spiritapp.ui.study.depictor.StudyDepictor;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.study.Group;
import com.actelion.research.spiritcore.business.study.Measurement;
import com.actelion.research.spiritcore.business.study.NamedSampling;
import com.actelion.research.spiritcore.business.study.NamedTreatment;
import com.actelion.research.spiritcore.business.study.Phase;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.business.study.StudyAction;
import com.actelion.research.util.ui.JCustomLabel;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.JGenericComboBox;
import com.actelion.research.util.ui.UIUtils;

public class StudyDesignerPanel extends JPanel {

	public static String PROPERTY_UPDATED = "UPDATED";

	/**
	 * StudyDepictor with D&D feature
	 */
	private StudyDepictor depictor = new StudyDepictor() {
		private int x = -1, y = -1;
		private int toX, toY;

		@Override
		public void paintComponent(Graphics g) {
			super.paintComponent(g);

			if(x>0 && x>=0 && (toX!=x || toY!=y)) {
				g.setColor(UIUtils.getColor(0, 0, 20, 12));
				g.fillRect(Math.min(x, toX), Math.min(y, toY), Math.abs(toX-x), Math.abs(toY-y));
				g.setColor(Color.GRAY);
				g.drawRect(Math.min(x, toX), Math.min(y, toY), Math.abs(toX-x), Math.abs(toY-y));
			}
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			if(e.getButton()==MouseEvent.BUTTON1) {
				Selection sel = getSelectionAt(e.getX(), e.getY());
				if(sel==null) return;
				if(sel.getGroup()!=null && sel.getPhase()!=null) {
					StudyAction a = study.getOrCreateStudyAction(sel.getGroup(), sel.getSubGroup(), sel.getPhase());
					if(a!=null) applyChange(a);
				}
			}

			repaint(true);

		}

		@Override
		public void mousePressed(MouseEvent e) {
			x = e.getX();
			y = e.getY();
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			applyChange(x, y, e.getX(), e.getY());
			x = y = -1;
			repaint(true);
		}
		@Override
		public void mouseExited(MouseEvent e) {
			x = y = -1;
			repaint(true);
		}

		@Override
		public void mouseDragged(MouseEvent e) {
			toX = e.getX();
			toY = e.getY();
			repaint();
		}
	};

	private final StudyDesignDlg dlg;
	private JScrollPane sp = new JScrollPane(depictor);
	private JGenericComboBox<String> measurementCombobox = new JGenericComboBox<>();
	private List<Measurement> extraMeasurements;
	private JGenericComboBox<NamedTreatment> treatmentCombobox = new JGenericComboBox<>();
	private JGenericComboBox<NamedSampling> samplingCombobox = new JGenericComboBox<>();
	private JGenericComboBox<String> labelCombobox = new JGenericComboBox<>();
	private int push = 0;

	public StudyDesignerPanel(StudyDesignDlg dlg) {
		super(null);
		this.dlg = dlg;

		depictor.setDesignerMode(true);

		measurementCombobox.addActionListener(e-> {
			if(push>0) return;
			push++;
			if(treatmentCombobox.getItemCount()>0) treatmentCombobox.setSelectedIndex(0);
			if(samplingCombobox.getItemCount()>0) samplingCombobox.setSelectedIndex(0);
			if(labelCombobox.getItemCount()>0) labelCombobox.setSelectedIndex(0);
			push--;

			if("<<New>>".equals(measurementCombobox.getSelection())) {
				Measurement em  = askForExtraMeasurement();
				if(em==null) {
					measurementCombobox.setSelectedIndex(0);
					return;
				}

				String key = em.getDescription();
				List<String> measurementOptions = measurementCombobox.getValues();
				measurementOptions.add(key);
				measurementCombobox.setValues(measurementOptions);
				measurementCombobox.setSelection(key);

				extraMeasurements.add(em);
			}
		});
		treatmentCombobox.addActionListener(e-> {
			if(push>0) return;
			push++;
			if(measurementCombobox.getItemCount()>0) measurementCombobox.setSelectedIndex(0);
			if(samplingCombobox.getItemCount()>0) samplingCombobox.setSelectedIndex(0);
			if(labelCombobox.getItemCount()>0) labelCombobox.setSelectedIndex(0);
			push--;
		});
		samplingCombobox.addActionListener(e-> {
			if(push>0) return;
			push++;
			if(measurementCombobox.getItemCount()>0) measurementCombobox.setSelectedIndex(0);
			if(treatmentCombobox.getItemCount()>0) treatmentCombobox.setSelectedIndex(0);
			if(labelCombobox.getItemCount()>0) labelCombobox.setSelectedIndex(0);
			push--;
		});

		labelCombobox.addActionListener(e-> {
			if(push>0) return;
			push++;
			if(measurementCombobox.getItemCount()>0) measurementCombobox.setSelectedIndex(0);
			if(treatmentCombobox.getItemCount()>0) treatmentCombobox.setSelectedIndex(0);
			if(samplingCombobox.getItemCount()>0) samplingCombobox.setSelectedIndex(0);
			push--;

			if("<<New>>".equals(labelCombobox.getSelection())) {
				String selLabel = JOptionPane.showInputDialog(StudyDesignerPanel.this, "Enter the name of the label", "New Label", JOptionPane.QUESTION_MESSAGE);
				if(selLabel==null) {
					labelCombobox.setSelectedIndex(0);
					return;
				}
				List<String> values = labelCombobox.getValues();
				values.add(selLabel);
				labelCombobox.setValues(values);
				labelCombobox.setSelection(selLabel);
			}
		});

		setLayout(new BorderLayout());
		add(BorderLayout.CENTER, sp);
		add(BorderLayout.NORTH, UIUtils.createVerticalBox(
				new JCustomLabel("Select an action and click on the study design to add it.", Color.GRAY),
				UIUtils.createHorizontalBox(
						new JLabel("Add Measurement: "),
						measurementCombobox,
						Box.createHorizontalStrut(10),
						new JLabel("Add Treatment: "),
						treatmentCombobox,
						Box.createHorizontalStrut(10),
						new JLabel("Add Sampling: "),
						samplingCombobox,
						Box.createHorizontalStrut(10),
						new JLabel("Add label: "),
						labelCombobox,
						Box.createHorizontalGlue())));
	}

	public void setSelectedNamedSampling(NamedSampling ns) {
		samplingCombobox.setSelection(ns);
	}

	public void setSelectedNamedTreatment(NamedTreatment nt) {
		treatmentCombobox.setSelection(nt);
	}

	public void setStudy(Study study) {
		depictor.setStudy(study);

		if(study!=null) {
			List<String> measurementOptions = new ArrayList<>();
			measurementOptions.add("<<New>>");
			measurementOptions.add("<<Remove>>");
			measurementOptions.add("Weighing");
			measurementOptions.add("Food");
			measurementOptions.add("Water");
			//Load the extraMeasurement and the associated test
			extraMeasurements = new ArrayList<>();
			for (Measurement em : study.getAllMeasurementsFromActions()) {
				assert em.getTest()!=null;
				extraMeasurements.add(em);
				measurementOptions.add(em.getDescription());
			}

			TreeSet<String> labels = new TreeSet<>();
			labels.add("<<New>>");
			labels.add("<<Remove>>");
			for (StudyAction action : study.getStudyActions()) {
				if(action.getLabel()!=null && action.getLabel().length()>0) labels.add(action.getLabel());
			}

			measurementCombobox.setValues(measurementOptions, "Measurement...");
			treatmentCombobox.setValues(study.getNamedTreatments(), "Treatment...");
			samplingCombobox.setValues(study.getNamedSamplings(), "Sampling...");
			labelCombobox.setValues(labels, "Label...");

			treatmentCombobox.setEnabled(study.getNamedTreatments().size()>0);
			samplingCombobox.setEnabled(study.getNamedSamplings().size()>0);
		}
	}


	private void applyChange(StudyAction model) {
		int x = depictor.getX(model.getPhase());
		int y = depictor.getY(model.getGroup(), model.getSubGroup());
		applyChange(x, y, x, y, model);
	}

	private void applyChange(int fromX, int fromY, int toX, int toY) {
		if(fromX>toX) {int tmp = fromX; fromX = toX; toX = tmp;}
		if(fromY>toY) {int tmp = fromY; fromY = toY; toY = tmp;}

		applyChange(fromX, fromY, toX, toY, null);
	}
	private void applyChange(int fromX, int fromY, int toX, int toY, StudyAction model) {

		Study study = depictor.getStudy();

		for (Phase phase : study.getPhases()) {
			int x = depictor.getX(phase);
			if(x<fromX || x>toX) continue;
			for(Group group: study.getGroups()) {
				for(int subgroup=0; subgroup<group.getNSubgroups(); subgroup++) {
					int y = depictor.getY(group, subgroup);
					if(y<fromY || y>toY) continue;

					StudyAction action = study.getOrCreateStudyAction(group, subgroup, phase);
					if(model==null) model = new StudyAction(action);

					//Apply the actions
					if(measurementCombobox.getSelectedIndex()==2) {
						//Remove Measurement
						action.setMeasureFood(false);
						action.setMeasureWater(false);
						action.setMeasureWeight(false);
						action.setMeasurements(null);
					} else if(measurementCombobox.getSelectedIndex()==3) {
						action.setMeasureWeight(!model.isMeasureWeight());
					} else if(measurementCombobox.getSelectedIndex()==4) {
						action.setMeasureFood(!model.isMeasureFood());
					} else if(measurementCombobox.getSelectedIndex()==5) {
						action.setMeasureWater(!model.isMeasureWater());
					} else if(measurementCombobox.getSelectedIndex()>=6) {
						Measurement toAdd = extraMeasurements.get(measurementCombobox.getSelectedIndex()-6);
						List<Measurement> ems = new ArrayList<Measurement>(action.getMeasurements());
						if(!ems.contains(toAdd)) {
							ems.add(toAdd);
							action.setMeasurements(ems);
							setStudy(study);
						} else {
							ems.remove(toAdd);
							action.setMeasurements(ems);
							setStudy(study);
						}

					} else if(samplingCombobox.getSelection()!=null) {
						NamedSampling ns = samplingCombobox.getSelection();
						boolean present = ns.equals(model.getNamedSampling1()) || ns.equals(model.getNamedSampling2());
						try {
							if(present) {
								//To remove a sampling, we should first delete the samples
								boolean res = checkAndRemoveSampling(action, ns);
								if(!res) return;
							} else {
								//To add a sampling, we should first make sure, that the necropsy is only set in one phase
								boolean res = checkAndAddSampling(action, ns);
								if(!res) return;

							}
						} catch(Exception e) {
							JExceptionDialog.showError(e);
							return;
						}

					} else if(treatmentCombobox.getSelection()!=null) {
						NamedTreatment t = treatmentCombobox.getSelection();
						boolean present = t.equals(model.getNamedTreatment());
						study.setNamedTreatment(action.getGroup(), action.getPhase(), action.getSubGroup(), t, !present);
					} else if(labelCombobox.getSelection()!=null) {
						String selLabel = labelCombobox.getSelection();
						if("<<Remove>>".equals(selLabel) || (selLabel!=null && selLabel.equals(model.getLabel()))) {
							action.setLabel(null);
						} else {
							action.setLabel(selLabel);
							setStudy(study);
							labelCombobox.setSelection(selLabel);
						}
					}
				}
			}
		}


		repaint();
		StudyDesignerPanel.this.firePropertyChange(PROPERTY_UPDATED, "", null);

	}


	private Set<Group> getFromAndToGroups(Group gr, Phase phase) {
		Set<Group> res = new HashSet<>();
		populateFromGroupRec(gr, res);
		populateToGroupRec(gr, res, phase);
		return res;
	}
	private void populateFromGroupRec(Group gr, Set<Group> res) {
		res.add(gr);
		if(gr.getFromGroup()!=null) populateFromGroupRec(gr.getFromGroup(), res);
	}

	private void populateToGroupRec(Group gr, Set<Group> res, Phase phase) {
		res.add(gr);
		for (Group g : gr.getToGroups()) {
			if(g.getFromPhase()!=null && g.getFromPhase().compareTo(phase)<0) continue;
			populateToGroupRec(g, res, phase);
		}
	}

	/**
	 * Return true if done
	 * @param action
	 * @param ns
	 * @return
	 */
	public boolean checkAndAddSampling(StudyAction action, NamedSampling ns) throws Exception {
		Study study = depictor.getStudy();
		if(study.isSynchronizeSamples() && ns.isNecropsy()) {
			for (Group gr : getFromAndToGroups(action.getGroup(), action.getPhase())) {
				Set<StudyAction> actions = gr==action.getGroup()? study.getStudyActions(gr, action.getSubGroup()): study.getStudyActions(gr);
				for(StudyAction a: actions) {
					for(NamedSampling n: a.getNamedSamplings()) {
						if(n.isNecropsy() && !a.getPhase().equals(action.getPhase())) {
							JOptionPane.showMessageDialog(this, "You can only have 1 necropsy per branch. You should first move or delete the necropsy on "+a.getGroup()+" / "+a.getPhase());
							return false;
						}
					}
				}
			}
		}
		study.setNamedSampling(action.getGroup(), action.getPhase(), action.getSubGroup(), ns, true);
		return true;

	}
	/**
	 *
	 * @param action
	 * @param ns
	 * @return true if done
	 */
	public boolean checkAndRemoveSampling(StudyAction action, NamedSampling ns) throws Exception {
		Study study = depictor.getStudy();
		//Check that this sampling can be removed, ie there are no biosamples attached to this sampling at this phase, group, subgroup
		Set<Biosample> samples = study.getSamples(action, ns);

		//Remove the samples from dead animals
		Set<Biosample> availables = new HashSet<>();
		for (Biosample sample : samples) {
			if(sample.getTopParentInSameStudy().getStatus().isAvailable()) {
				availables.add(sample);
			}
		}
		samples = availables;

		if(samples.size()>0) {

			JRadioButton moveButton = new JRadioButton("Move to phase");
			JRadioButton deleteButton = new JRadioButton("Delete those samples");
			ButtonGroup buttonGroup = new ButtonGroup();
			buttonGroup.add(moveButton);
			buttonGroup.add(deleteButton);

			PhaseComboBox phaseComboBox = new PhaseComboBox(study.getPhases());
			phaseComboBox.setSelection(action.getPhase());

			JPanel centerPanel = UIUtils.createBox(
					UIUtils.createVerticalBox(
							UIUtils.createHorizontalBox(Box.createHorizontalStrut(15), moveButton, phaseComboBox, Box.createHorizontalGlue()),
							UIUtils.createHorizontalBox(Box.createHorizontalStrut(15), deleteButton, Box.createHorizontalGlue())
							),
					new JLabel("You have "+samples.size()+" samples already attached to this action.\nDo you want to delete or move them?"), null, null, null);

			int accept = JOptionPane.showConfirmDialog(this, centerPanel, "Samples Attached", JOptionPane.WARNING_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
			if(accept!=JOptionPane.OK_OPTION) return false;

			if(moveButton.isSelected()) {
				Phase movePhase = phaseComboBox.getSelection();
				study.setNamedSampling(action.getGroup(), action.getPhase(), action.getSubGroup(), ns, false);
				study.setNamedSampling(action.getGroup(), movePhase, action.getSubGroup(), ns, true);
				for (Biosample b : samples) {
					b.setInheritedPhase(movePhase);
				}
				dlg.getToUpdate().addAll(samples);
			} else if(deleteButton.isSelected()) {
				study.setNamedSampling(action.getGroup(), action.getPhase(), action.getSubGroup(), ns, false);
				dlg.getToDelete().addAll(samples);
			} else {
				JOptionPane.showMessageDialog(this, "You must select one radio button");
				return false;
			}

		} else {
			study.setNamedSampling(action.getGroup(), action.getPhase(), action.getSubGroup(), ns, false);
		}
		return true;

	}

	public StudyDepictor getDepictor() {
		return depictor;
	}

	private Measurement askForExtraMeasurement() {
		ExtraMeasurementDlg dlg = new ExtraMeasurementDlg();
		return dlg.getExtraMeasurement();

	}

}
