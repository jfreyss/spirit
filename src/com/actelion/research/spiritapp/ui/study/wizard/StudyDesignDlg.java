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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import com.actelion.research.spiritapp.Spirit;
import com.actelion.research.spiritapp.ui.SpiritFrame;
import com.actelion.research.spiritapp.ui.study.StudyEditorPane;
import com.actelion.research.spiritapp.ui.study.edit.StudySaveImageDlg;
import com.actelion.research.spiritapp.ui.study.sampling.NamedSamplingDlg;
import com.actelion.research.spiritapp.ui.study.wizard.group.StudyGroupDlg;
import com.actelion.research.spiritapp.ui.study.wizard.phase.PhaseDlg;
import com.actelion.research.spiritapp.ui.study.wizard.treatment.StudyTreatmentDlg;
import com.actelion.research.spiritapp.ui.util.HelpBinder;
import com.actelion.research.spiritapp.ui.util.SpiritChangeListener;
import com.actelion.research.spiritapp.ui.util.SpiritChangeType;
import com.actelion.research.spiritapp.ui.util.component.JSpiritEscapeDialog;
import com.actelion.research.spiritcore.business.IAuditable;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.study.Group;
import com.actelion.research.spiritcore.business.study.NamedSampling;
import com.actelion.research.spiritcore.business.study.NamedTreatment;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.business.study.StudyAction;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.spiritcore.services.dao.DAOBiosample;
import com.actelion.research.spiritcore.services.dao.DAOStudy;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.util.FormatterUtils;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.iconbutton.IconType;
import com.actelion.research.util.ui.iconbutton.JIconButton;

/**
 *
 * @author freyssj
 */
public class StudyDesignDlg extends JSpiritEscapeDialog {

	private Study study;
	private StudyDesignerPanel studyDesigner = new StudyDesignerPanel(this);

	private DefaultListModel<NamedSampling> samplingModel = new DefaultListModel<>();
	private JList<NamedSampling> samplingList = new JList<>(samplingModel);

	private DefaultListModel<NamedTreatment> treatmentModel = new DefaultListModel<>();
	private JList<NamedTreatment> treatmentList = new JList<>(treatmentModel);

	private DefaultListModel<Group> groupModel = new DefaultListModel<>();
	private JList<Group> groupList = new JList<>(groupModel);

	private StudyDocumentEditorPane documentEditorPane = new StudyDocumentEditorPane();

	private JLabel phaseLabel = new JLabel();
	private JButton editNameButton =  new JButton(new Action_EditNameDescription());

	private StudyEditorPane studyEditorPane = new StudyEditorPane();

	private Set<Biosample> toDelete = new HashSet<>();
	private Set<Biosample> toUpdate = new HashSet<>();

	public static StudyDesignDlg duplicateStudy(Study s) {
		return new StudyDesignDlg(s, true);
	}

	/**
	 * Edit a study design
	 * @param myStudy
	 */
	public StudyDesignDlg(Study myStudy) {
		this(myStudy, false);
	}

	public StudyDesignDlg(Study myStudy, boolean duplicateMode) {
		super(UIUtils.getMainFrame(), "Study - Study Design", StudyDesignDlg.class.getName());
		study = JPAUtil.reattach(myStudy);

		//Duplicate modem open the studyInfoDlg
		try {
			if(duplicateMode && myStudy!=null) {
				study = study.duplicate();
				assert study!=null && study.getId()<=0;
			}
			if(study.getId()<=0 ) {
				StudyInfoDlg dlg = new StudyInfoDlg(study, true);
				if(dlg.isCancel()) return;
				study = JPAUtil.reattach(dlg.getStudy());
			}

			//make sure to work on a recent copy
			assert study!=null && study.getId()>0;
		} catch(Throwable e) {
			JExceptionDialog.showError(e);
			return;
		}

		studyEditorPane.setSimplified(true);

		if(!SpiritRights.canEdit(study, SpiritFrame.getUser())) {
			JOptionPane.showMessageDialog(this, "You cannot edit this study", "Error", JOptionPane.ERROR_MESSAGE);
		}


		//Infos panels
		JScrollPane sp1 = new JScrollPane(studyEditorPane);
		sp1.setMinimumSize(new Dimension(200, 130));
		JPanel studyInfoPanel = UIUtils.createTitleBox("1. Study Infos",
				UIUtils.createBox(sp1, UIUtils.createVerticalBox(UIUtils.createHorizontalBox(editNameButton, Box.createHorizontalGlue()))));


		//Documents Panel
		JPanel documentPanel = UIUtils.createTitleBox("2. Documents", new JScrollPane(documentEditorPane));
		documentPanel.setMinimumSize(new Dimension(200, 130));
		documentEditorPane.addPropertyChangeListener(StudyDocumentEditorPane.PROPERTY_DOCUMENTCHANGED, e-> {
			refresh();
			setMustAskForExit(true);
		});

		//PhasePanel
		JPanel phasePanel = UIUtils.createTitleBox("3. Phases", UIUtils.createBox(
				UIUtils.createVerticalBox(phaseLabel,Box.createVerticalGlue()),
				UIUtils.createVerticalBox(UIUtils.createHorizontalBox(new JButton(new Action_EditPhase()), Box.createHorizontalGlue()))));

		//GroupPanel
		JPanel groupPanel = UIUtils.createTitleBox("4. Groups", UIUtils.createBox(
				Box.createVerticalGlue(),
				UIUtils.createHorizontalBox(new JButton(new Action_EditGroup()), Box.createHorizontalGlue())));

		//TreatmentPanel
		final JButton editTreatmentButton = new JButton(new Action_EditNamedTreatment(false));
		editTreatmentButton.setEnabled(false);
		JPanel treatmentPanel = UIUtils.createTitleBox("5. Treatments", UIUtils.createBox(
				new JScrollPane(treatmentList),
				UIUtils.createHorizontalBox(new JButton(new Action_EditNamedTreatment(true)), editTreatmentButton, Box.createHorizontalGlue())));
		treatmentList.setCellRenderer(new DefaultListCellRenderer() {
			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				NamedTreatment ns = (NamedTreatment) value;
				if(!isSelected) {
					setForeground(ns.getColor());
				}
				String compoundUnit = ns.getCompoundAndUnits(true, false);

				setText("<html><b><u>" + ns.getName() + "</u></b>" + (compoundUnit.length()>0? compoundUnit:"")+"</html>");
				return this;
			}
		});
		treatmentList.addListSelectionListener(e-> {
			if(treatmentList.getSelectedValue()!=null) {
				studyDesigner.setSelectedNamedTreatment(treatmentList.getSelectedValue());
			}
			editTreatmentButton.setEnabled(treatmentList.getSelectedValue()!=null);
		});
		treatmentList.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if(e.getClickCount()==2) {
					new StudyTreatmentDlg(study, treatmentList.getSelectedValue());
					refresh();
					setMustAskForExit(true);

				}
				samplingList.clearSelection();
			}
		});


		//SamplingPanel
		final JButton editNamedSamplingButton = new JButton(new Action_EditNamedSampling(false));
		editNamedSamplingButton.setEnabled(false);

		final JButton duplicateSamplingButton = new JButton(new Action_DuplicateSampling());
		duplicateSamplingButton.setEnabled(false);

		JPanel samplingPanel = UIUtils.createTitleBox("6. Sampling Templates",
				UIUtils.createBox(
						new JScrollPane(samplingList),
						UIUtils.createHorizontalBox(new JButton(new Action_EditNamedSampling(true)), editNamedSamplingButton, duplicateSamplingButton, Box.createHorizontalGlue())));
		samplingList.addListSelectionListener(e-> {
			if(samplingList.getSelectedValue()!=null) studyDesigner.setSelectedNamedSampling(samplingList.getSelectedValue());
			editNamedSamplingButton.setEnabled(samplingList.getSelectedValue()!=null);
			duplicateSamplingButton.setEnabled(samplingList.getSelectedValue()!=null);
		});
		samplingList.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if(e.getClickCount()==2) {
					new NamedSamplingDlg(study, samplingList.getSelectedValue(), StudyDesignDlg.this);
					refresh();
				}
				treatmentList.clearSelection();
			}
		});
		samplingList.setCellRenderer(new DefaultListCellRenderer() {
			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				NamedSampling ns = (NamedSampling) value;
				setText("<html><b><u>" + ns.getName() + "</u></b> " + (ns.getStudy()==null?"": "("+ns.getStudy().getStudyId()+")") + "<br>" +
						ns.getDescription());
				return this;
			}
		});

		JPanel topPanel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new  GridBagConstraints();
		c.weighty = 1; c.gridy=0; c.fill = GridBagConstraints.BOTH;
		c.weightx=0.5; c.gridx=0; c.gridy=0; topPanel.add(studyInfoPanel, c);
		c.weightx=0;   c.gridx=1; c.gridy=0; topPanel.add(documentPanel, c);
		c.weightx=0;   c.gridx=2; c.gridy=0; topPanel.add(phasePanel, c);
		c.weightx=0;   c.gridx=3; c.gridy=0; topPanel.add(groupPanel, c);
		c.weightx=0.5; c.gridx=4; c.gridy=0; topPanel.add(treatmentPanel, c);
		c.weightx=0.5; c.gridx=5; c.gridy=0; topPanel.add(samplingPanel, c);

		SwingUtilities.invokeLater(()-> refresh());


		JButton imageButton = new JIconButton(IconType.STUDY, "Export as Image");
		imageButton.addActionListener(ev-> {
			StudySaveImageDlg.showSaveImageDlg(studyDesigner.getDepictor());
		});

		JButton okButton = new JIconButton(IconType.SAVE, "Save");
		okButton.addActionListener(ev->{
			try {
				boolean add = study.getId()<=0;

				if(getToDelete().size()>0) {
					if(!Spirit.askReasonForChange()) return;
				} else {
					List<IAuditable> list = new ArrayList<>();
					list.add(study);
					list.addAll(getToUpdate());
					if(!Spirit.askReasonForChangeIfUpdated(list)) return;
				}

				EntityManager session = JPAUtil.getManager();
				//Start the transaction
				EntityTransaction txn = null;
				try {
					txn = session.getTransaction();
					txn.begin();

					DAOStudy.persistStudies(session, Collections.singletonList(study), Spirit.askForAuthentication());

					if(getToUpdate().size()>0) {
						DAOBiosample.persistBiosamples(session, getToUpdate(), SpiritFrame.getUser());
					}

					if(getToDelete().size()>0) {
						DAOBiosample.deleteBiosamples(session, getToDelete(), SpiritFrame.getUser());
					}

					txn.commit();
					txn = null;

				} catch (Exception e) {
					if(txn!=null && txn.isActive()) try{ txn.rollback();} catch(Exception e2) {e2.printStackTrace();}
					throw e;
				}

				dispose();
				SpiritChangeListener.fireModelChanged(add? SpiritChangeType.MODEL_ADDED: SpiritChangeType.MODEL_UPDATED, Study.class, study);

			} catch (Exception e) {
				JExceptionDialog.showError(StudyDesignDlg.this, e);
			}
		});
		getRootPane().setDefaultButton(okButton);


		//ContentPane
		topPanel.setMaximumSize(new Dimension(500, 250));
		topPanel.setPreferredSize(new Dimension(500, 250));

		setContentPane(UIUtils.createBox(
				UIUtils.createTitleBox("7. Set Actions", studyDesigner),
				topPanel,
				UIUtils.createHorizontalBox(HelpBinder.createHelpButton(), Box.createHorizontalGlue(), imageButton, okButton)));

		SwingUtilities.invokeLater(()-> refresh());

		UIUtils.adaptSize(this, 1550, 1150);
		setLocationRelativeTo(UIUtils.getMainFrame());
		setVisible(true);

	}

	public void refresh() {
		assert study!=null;
		//Make sure the namedSamplings are correctly linked
		TreeSet<NamedSampling> samplings = new TreeSet<>(study.getNamedSamplings());
		for (StudyAction a : study.getStudyActions()) {
			if(a.getNamedSampling1()!=null) samplings.add(a.getNamedSampling1());
			if(a.getNamedSampling2()!=null) samplings.add(a.getNamedSampling2());

		}

		//Refresh NamedSamplings (if it has been modified in the sampling dialog)
		study.setNamedSamplings(samplings);
		for (StudyAction action: study.getStudyActions()) {
			action.setNamedSampling1(action.getNamedSampling1());
			action.setNamedSampling2(action.getNamedSampling2());
		}

		//Refresh Phase
		phaseLabel.setText("Start: " + (study.getFirstDate()==null?"N/A": FormatterUtils.formatDate(study.getFirstDate())));

		//Refresh Groups
		Group selG = groupList.getSelectedValue();
		groupModel.clear();
		for (Group g : study.getGroups()) {
			groupModel.addElement(g);
		}
		groupList.setSelectedValue(selG, true);

		//Refresh Samplings
		NamedSampling selS = samplingList.getSelectedValue();
		samplingModel.clear();
		for (NamedSampling ns : study.getNamedSamplings()) {
			samplingModel.addElement(ns);
		}
		samplingList.setSelectedValue(selS, true);

		//Refresh Treatments
		NamedTreatment selN = treatmentList.getSelectedValue();
		treatmentModel.clear();
		List<NamedTreatment> namedTreatments = new ArrayList<>(study.getNamedTreatments());
		Collections.sort(namedTreatments);
		for (NamedTreatment nt : namedTreatments) {
			treatmentModel.addElement(nt);
		}
		treatmentList.setSelectedValue(selN, true);

		//Refresh Document
		documentEditorPane.setStudy(study);

		//Repaint the study
		studyDesigner.setStudy(study);
		studyEditorPane.setStudy(study);
		repaint();
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public class Action_EditNameDescription extends AbstractAction {
		public Action_EditNameDescription() {
			putValue(AbstractAction.SMALL_ICON, IconType.STUDY.getIcon());
			putValue(AbstractAction.NAME, "Edit Infos");
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			new StudyInfoDlg(study, false);
			refresh();
			setMustAskForExit(true);
		}
	}

	public class Action_EditNamedSampling extends AbstractAction {
		private boolean add;
		public Action_EditNamedSampling(boolean add) {
			putValue(AbstractAction.NAME, add? "Add Sampling": "Edit");
			putValue(AbstractAction.SMALL_ICON, add? IconType.NEW.getIcon(): IconType.EDIT.getIcon());
			this.add = add;
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			if(add) {
				new NamedSamplingDlg(study, null, StudyDesignDlg.this);
			} else {
				NamedSampling ns = samplingList.getSelectedValue();
				if(ns==null) return;
				new NamedSamplingDlg(study, ns, StudyDesignDlg.this);

			}
			refresh();
		}
	}

	public class Action_DuplicateSampling extends AbstractAction {
		public Action_DuplicateSampling() {
			putValue(AbstractAction.NAME, "Duplicate");
			putValue(AbstractAction.SMALL_ICON, IconType.DUPLICATE.getIcon());
		}
		@Override
		public void actionPerformed(ActionEvent e) {

			NamedSampling ns = samplingList.getSelectedValue();
			if(ns==null) return;
			ns = ns.clone();
			ns.setId(0);

			new NamedSamplingDlg(study, ns, StudyDesignDlg.this);

			refresh();
		}
	}


	public class Action_EditNamedTreatment extends AbstractAction {
		private boolean add;
		public Action_EditNamedTreatment(boolean add) {
			putValue(AbstractAction.NAME, add? "Add Treatment": "Edit");
			this.add = add;
			putValue(AbstractAction.SMALL_ICON, add? IconType.NEW.getIcon(): IconType.EDIT.getIcon());
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			if(add) {
				new StudyTreatmentDlg(study, null);
			} else {
				NamedTreatment ns = treatmentList.getSelectedValue();
				if(ns==null) return;
				new StudyTreatmentDlg(study, ns);
			}
			refresh();
			setMustAskForExit(true);
		}
	}

	public class Action_EditGroup extends AbstractAction {
		public Action_EditGroup() {
			putValue(AbstractAction.NAME, "Edit Groups");
			putValue(AbstractAction.SMALL_ICON, IconType.EDIT.getIcon());
		}
		@Override
		public void actionPerformed(ActionEvent e) {

			new StudyGroupDlg(StudyDesignDlg.this, study);

			refresh();
			setMustAskForExit(true);
		}
	}

	public class Action_EditPhase extends AbstractAction {
		public Action_EditPhase() {
			putValue(AbstractAction.NAME, "Edit Phases");
			putValue(AbstractAction.SMALL_ICON, IconType.EDIT.getIcon());
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			new PhaseDlg(StudyDesignDlg.this, study);
			setMustAskForExit(true);
		}
	}

	public Set<Biosample> getToUpdate() {
		return toUpdate;
	}

	public Set<Biosample> getToDelete() {
		return toDelete;
	}

}

