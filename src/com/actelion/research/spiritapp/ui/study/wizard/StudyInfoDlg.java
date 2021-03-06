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
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import com.actelion.research.spiritapp.Spirit;
import com.actelion.research.spiritapp.ui.SpiritFrame;
import com.actelion.research.spiritapp.ui.util.DatePicker;
import com.actelion.research.spiritapp.ui.util.HelpBinder;
import com.actelion.research.spiritapp.ui.util.SpiritChangeListener;
import com.actelion.research.spiritapp.ui.util.SpiritChangeType;
import com.actelion.research.spiritapp.ui.util.component.EmployeeGroupComboBox;
import com.actelion.research.spiritapp.ui.util.component.LF;
import com.actelion.research.spiritapp.ui.util.component.UserIdTextArea;
import com.actelion.research.spiritcore.adapter.DBAdapter;
import com.actelion.research.spiritcore.adapter.DBAdapter.UserManagedMode;
import com.actelion.research.spiritcore.business.DataType;
import com.actelion.research.spiritcore.business.employee.EmployeeGroup;
import com.actelion.research.spiritcore.business.property.PropertyKey;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.spiritcore.services.dao.DAOStudy;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.spiritcore.services.dao.SpiritProperties;
import com.actelion.research.spiritcore.util.MiscUtils;
import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.JCustomLabel;
import com.actelion.research.util.ui.JCustomTextField;
import com.actelion.research.util.ui.JCustomTextField.CustomFieldType;
import com.actelion.research.util.ui.JEscapeDialog;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.JGenericComboBox;
import com.actelion.research.util.ui.JInfoLabel;
import com.actelion.research.util.ui.JTextComboBox;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.iconbutton.IconType;
import com.actelion.research.util.ui.iconbutton.JIconButton;


public class StudyInfoDlg extends JEscapeDialog {

	protected Study study;
	protected final boolean inTransaction;

	protected JCustomTextField localIdField = new JCustomTextField(CustomFieldType.ALPHANUMERIC, 12);
	protected JCustomTextField studyIdField = new JCustomTextField(CustomFieldType.ALPHANUMERIC, 12);
	protected JCustomTextField titleField = new JCustomTextField(CustomFieldType.ALPHANUMERIC, 60);

	protected JGenericComboBox<String> stateComboBox;
	protected JTextComboBox typeComboBox;
	protected Map<String, JComponent> metadataKey2comp = new HashMap<>();
	private final JPanel metadataPanel = new JPanel(new GridLayout());
	private EmployeeGroupComboBox eg1ComboBox = new EmployeeGroupComboBox(false);
	private EmployeeGroupComboBox eg2ComboBox = new EmployeeGroupComboBox(false);

	private UserIdTextArea adminUsersTextArea = new UserIdTextArea(5, 22);
	private UserIdTextArea expertUsersTextArea = new UserIdTextArea(5, 22);
	private JTextArea blindAllUsersField = new JTextArea(2, 20);
	private JTextArea blindNamesUsersField = new JTextArea(2, 20);
	private JTextArea commentsTextArea = new JTextArea(4, 28);

	private JCheckBox synchroCheckbox = new JCheckBox("Generate samples automatically from the sampling's templates (recommended)");
	private JTextArea notesTextArea = new JTextArea(8, 25);

	private boolean cancel = true;
	private final boolean studyIdEditable;
	private final String studyIdPattern;
	private final boolean designMode;
	private String[] studyTypes;


	public StudyInfoDlg(Study s) {
		this(s, true);
	}

	public StudyInfoDlg(Study s, boolean inTransaction) {
		super(UIUtils.getMainFrame(), "Edit Study - Infos");
		if(inTransaction) this.study = JPAUtil.reattach(s);
		else this.study = s;
		this.inTransaction = inTransaction;

		studyIdEditable = SpiritProperties.getInstance().isChecked(PropertyKey.STUDY_STUDYID_EDITABLE);
		studyIdPattern = SpiritProperties.getInstance().getValue(PropertyKey.STUDY_STUDYID_PATTERN);
		designMode = SpiritProperties.getInstance().isChecked(PropertyKey.STUDY_FEATURE_STUDYDESIGN);
		studyTypes = SpiritProperties.getInstance().getValues(PropertyKey.STUDY_TYPES);

		//Definition
		studyIdField.setEnabled(studyIdEditable && (SpiritRights.isSuperAdmin(Spirit.getUser()) || (study.getId()<=0)));
		studyIdField.setToolTipText("Unique, " + (studyIdEditable?" automatically generated": "only editable by an admin or at study creation"));

		typeComboBox = new JTextComboBox(studyTypes, false);
		typeComboBox.setColumns(25);

		JPanel studyDescPanel = UIUtils.createTitleBox("Definition", UIUtils.createTable(
				new JLabel("StudyId*: "), UIUtils.createHorizontalBox(studyIdField),
				(!studyIdEditable? new JLabel("InternalId:"): null), (!studyIdEditable? UIUtils.createHorizontalBox(localIdField, new JInfoLabel("Your own identifier (unique)")): null),
				new JLabel("Title*: "), titleField,
				null, designMode? synchroCheckbox: null));


		//Rights Panel
		JPanel studyRightsPanel = new JPanel();
		if(DBAdapter.getInstance().getUserManagedMode()!=UserManagedMode.UNIQUE_USER && SpiritProperties.getInstance().isChecked(PropertyKey.USER_USEGROUPS)) {
			blindNamesUsersField.setToolTipText("The group's name and the treatment's compounds are hidden. Those users can still see the group no (1A) and the treatment's name (blue) in AnimalCare");
			blindAllUsersField.setToolTipText("The groups and the treatments compounds are completely hidden. Those users can only see the samples in AnimalCare");

			JPanel editUsersPanel =  UIUtils.createVerticalBox(BorderFactory.createEtchedBorder(),
					UIUtils.createHorizontalBox(new JCustomLabel("Admin Users: ", FastFont.BOLD), Box.createHorizontalGlue()),
					UIUtils.createHorizontalBox(new JInfoLabel("(can edit the infos/design)"), Box.createHorizontalGlue()),
					UIUtils.createHorizontalBox(Box.createHorizontalStrut(10), adminUsersTextArea, Box.createHorizontalGlue()),
					Box.createVerticalGlue());

			JPanel readUsersPanel = UIUtils.createVerticalBox(BorderFactory.createEtchedBorder(),
					UIUtils.createHorizontalBox(new JCustomLabel("Expert Users:", FastFont.BOLD), Box.createHorizontalGlue()),
					UIUtils.createHorizontalBox(new JInfoLabel("(can add biosamples/results)"), Box.createHorizontalGlue()),
					UIUtils.createHorizontalBox(Box.createHorizontalStrut(10), expertUsersTextArea, Box.createHorizontalGlue()),
					new JSeparator(),
					UIUtils.createHorizontalBox(new JCustomLabel("Departments:"), Box.createHorizontalGlue()),
					UIUtils.createHorizontalBox(Box.createHorizontalStrut(15), eg1ComboBox, Box.createHorizontalGlue()),
					UIUtils.createHorizontalBox(Box.createHorizontalStrut(15), eg2ComboBox, Box.createHorizontalGlue()),
					Box.createVerticalGlue());

			JPanel blindPanel = UIUtils.createVerticalBox(BorderFactory.createEtchedBorder(),
					new JCustomLabel("Blind Users (Names):", FastFont.BOLD),
					new JInfoLabel("(can view the groupNo and treatmentNo but not the names)"),
					UIUtils.createHorizontalBox(Box.createHorizontalStrut(10), blindNamesUsersField, Box.createHorizontalGlue()),
					Box.createVerticalStrut(20),
					new JCustomLabel("Blind Users (All):", FastFont.BOLD),
					new JInfoLabel("(cannot view the groups/treatments)"),
					UIUtils.createHorizontalBox(Box.createHorizontalStrut(10), blindAllUsersField, Box.createHorizontalGlue()),
					Box.createVerticalGlue());
			blindPanel.setVisible(designMode);

			studyRightsPanel = UIUtils.createHorizontalBox(
					editUsersPanel,
					readUsersPanel,
					blindPanel,
					Box.createHorizontalGlue());
		} else {
			studyRightsPanel.setVisible(false);
		}

		//Status Panel
		JPanel statesRightsPanel = new JPanel();
		List<String> possibleStates = Arrays.asList(SpiritProperties.getInstance().getValues(PropertyKey.STUDY_STATES));
		stateComboBox = new JGenericComboBox<>(possibleStates, false);
		if(possibleStates.size()>0) {
			statesRightsPanel = UIUtils.createTitleBox("Status" + (studyRightsPanel.isVisible()?" / User Rights":""),
					UIUtils.createVerticalBox(
							UIUtils.createHorizontalBox(new JLabel("Status: "), stateComboBox, Box.createHorizontalGlue()),
							studyRightsPanel));
			stateComboBox.setEnabled(SpiritRights.canWork(study, SpiritFrame.getUser()));

		} else {
			stateComboBox.setEnabled(false);
		}
		statesRightsPanel.setVisible(SpiritProperties.getInstance().getValues(PropertyKey.STUDY_STATES).length>=0);

		//Documents Panel
		StudyDocumentEditorPane documentEditorPane = new StudyDocumentEditorPane();
		documentEditorPane.setStudy(study);
		JPanel documentPanel = UIUtils.createTitleBox("Documents", new JScrollPane(documentEditorPane));
		documentPanel.setVisible(SpiritProperties.getInstance().isChecked(PropertyKey.STUDY_DOCUMENTS));

		//NotesPanel
		JPanel studyNotesPanel = UIUtils.createTitleBox("Notes", new JScrollPane(notesTextArea));

		//Buttons
		JButton okButton;
		if(inTransaction) {
			okButton = new JIconButton(IconType.SAVE, "Save");
		} else {
			okButton = new JButton("OK");
		}

		okButton.addActionListener(ev-> save() );

		//Update View
		if(studyIdPattern.length()>0 && study.getId()<=0) {
			study.setStudyId(DAOStudy.getNextStudyId());
		}

		studyIdField.setText(study.getStudyId());
		localIdField.setText(study.getLocalId());
		titleField.setText(study.getTitle());
		typeComboBox.setText(study.getType());
		stateComboBox.setSelection(study.getState());

		List<EmployeeGroup> egs = study.getEmployeeGroups();
		eg1ComboBox.setSelection(egs.size()>0? egs.get(0): null);
		eg2ComboBox.setSelection(egs.size()>1 && !egs.get(1).equals(egs.get(0))? egs.get(1): null);

		synchroCheckbox.setSelected(study.isSynchronizeSamples());

		adminUsersTextArea.setText(study.getAdminUsers());
		expertUsersTextArea.setText(study.getExpertUsers());

		notesTextArea.setText(study.getNotes());
		blindAllUsersField.setText(study.getBlindAllUsers());
		blindNamesUsersField.setText(study.getBlindDetailsUsers());

		commentsTextArea.setText(study.getNotes());

		setLayout(new BorderLayout());
		add(BorderLayout.CENTER, UIUtils.createVerticalBox(studyDescPanel, metadataPanel, statesRightsPanel, documentPanel, studyNotesPanel));
		add(BorderLayout.SOUTH, UIUtils.createHorizontalBox(HelpBinder.createHelpButton(), Box.createHorizontalGlue(), okButton));

		typeComboBox.addTextChangeListener(e-> {
			updateMetadataPanel();
			typeComboBox.requestFocusInWindow();
		});

		updateMetadataPanel();
		postConstruct();

		//Set visible
		UIUtils.adaptSize(this, -1, -1);
		setVisible(true);

	}

	/**
	 * To be overriden for specific construct
	 */
	protected void postConstruct() {

	}

	private void updateMetadataPanel() {
		//Study Type
		JPanel typePanel = null;
		if(studyTypes.length>0) {
			typePanel = UIUtils.createHorizontalBox(new JCustomLabel("Type*: ", FastFont.BOLD), typeComboBox, Box.createHorizontalGlue());
			typeComboBox.setBackground(LF.BGCOLOR_REQUIRED);
		}

		//Metadata
		List<JComponent> comps = new ArrayList<>();
		for (String metadataKey : SpiritProperties.getInstance().getValues(PropertyKey.STUDY_METADATA)) {
			String name = SpiritProperties.getInstance().getValue(PropertyKey.STUDY_METADATA_NAME, metadataKey);
			String datatype = SpiritProperties.getInstance().getValue(PropertyKey.STUDY_METADATA_DATATYPE, metadataKey);
			String params = SpiritProperties.getInstance().getValue(PropertyKey.STUDY_METADATA_PARAMETERS, metadataKey);
			String[] roles = SpiritProperties.getInstance().getValues(PropertyKey.STUDY_METADATA_ROLES, metadataKey);
			String[] states = SpiritProperties.getInstance().getValues(PropertyKey.STUDY_METADATA_STATES, metadataKey);
			String[] types = SpiritProperties.getInstance().getValues(PropertyKey.STUDY_METADATA_TYPES, metadataKey);
			boolean req = SpiritProperties.getInstance().isChecked(PropertyKey.STUDY_METADATA_REQUIRED, metadataKey);

			//Create the component
			JComponent comp = metadataKey2comp.get(metadataKey);
			if(comp==null) {
				if(DataType.LIST.name().equals(datatype)) {
					JGenericComboBox<String> combo = new JGenericComboBox<String>(MiscUtils.split(params), true);
					combo.setSelection(study.getMetadataMap().get(metadataKey));
					comp = combo;
				} else if(DataType.DATE.name().equals(datatype)) {
					DatePicker datePicker = new DatePicker();
					datePicker.setText(study.getMetadataMap().get(metadataKey));
					comp = datePicker;

				} else if(DataType.AUTO.name().equals(datatype)) {
					JTextComboBox field = new JTextComboBox();
					field.setChoices(DAOStudy.getMetadataValues(metadataKey));
					field.setText(study.getMetadataMap().get(metadataKey));
					comp = field;
				} else {
					JCustomTextField textField = new JCustomTextField();
					textField.setText(study.getMetadataMap().get(metadataKey));
					comp = textField;
				}
				metadataKey2comp.put(metadataKey, comp);
			}

			comp.setToolTipText(null);
			comp.setEnabled(true);

			if(types.length>0 && !MiscUtils.contains(types, typeComboBox.getText())) {
				comp.setToolTipText("Disabled because study's type: "+typeComboBox.getText()+" not in "+MiscUtils.flatten(types));
				comp.setEnabled(false);
			}
			if(states.length>0 && !MiscUtils.contains(states, study.getState())) {
				comp.setToolTipText("Disabled because study's state: "+study.getState()+" not in "+MiscUtils.flatten(states));
				comp.setEnabled(false);
			}
			if(roles.length>0 && !MiscUtils.contains(roles, SpiritFrame.getUser().getRoles())) {
				comp.setToolTipText("Disabled because user's roles: "+SpiritFrame.getUser().getRoles()+" not in "+MiscUtils.flatten(roles));
				comp.setEnabled(false);
			}
			if(req) comp.setBackground(LF.BGCOLOR_REQUIRED);

			comps.add(new JLabel(name + ": " + (req?"*":"")));
			comps.add(comp);
		}
		metadataPanel.removeAll();
		if(comps.size()>0) {
			if(comps.size()>=4) {
				int med = (((comps.size()/2)+1)/2)*2;
				metadataPanel.add(UIUtils.createTitleBox("Metadata", UIUtils.createBox(
						UIUtils.createHorizontalBox(UIUtils.createTable(comps.subList(0, med)), Box.createHorizontalStrut(30), UIUtils.createTable(comps.subList(med, comps.size())), Box.createHorizontalGlue()),
						typePanel)));
			} else {
				metadataPanel.add(UIUtils.createTitleBox("Metadata", UIUtils.createBox(
						UIUtils.createTable(comps),
						typePanel)));
			}
		}
		metadataPanel.validate();
		pack();
	}

	@SuppressWarnings("unchecked")
	public void save() {
		try {

			if(studyIdField.getText().length()==0) throw new Exception("You must enter a unique studyId");
			SpiritUser user = Spirit.askForAuthentication();

			//Update Model
			//Definition
			if(typeComboBox.isVisible() && studyTypes.length>0 && typeComboBox.getText().length()==0) throw new Exception("The type is required");
			study.setStudyId(studyIdField.getText());
			study.setState(stateComboBox.getSelection());
			study.setLocalId(localIdField.getText());
			study.setTitle(titleField.getText());
			study.setSynchronizeSamples(synchroCheckbox.isSelected());
			study.setType(typeComboBox.getText());

			if(stateComboBox.isEnabled()) {
				if(stateComboBox.getSelection()==null) throw new Exception("You must select a status");
				study.setState(stateComboBox.getSelection());
			}

			//Users
			if(DBAdapter.getInstance().getUserManagedMode()!=UserManagedMode.UNIQUE_USER && SpiritProperties.getInstance().isChecked(PropertyKey.USER_USEGROUPS)) {
				//Check that the write-users are valid
				String adminUsers = adminUsersTextArea.getText().trim();
				if(adminUsers.length()==0) throw new Exception("One admin is required");

				//Check that the read-users are valid
				String expertUsers = expertUsersTextArea.getText().trim();

				//Check that the blind-users are valid
				if(!blindAllUsersField.isEnabled()) {
					blindAllUsersField.setText("");
					blindNamesUsersField.setText("");
				}
				String blindAllUsers = blindAllUsersField.getText().trim();
				String blindNamesUsers = blindNamesUsersField.getText().trim();
				DBAdapter.getInstance().checkValid(Arrays.asList(MiscUtils.split(blindAllUsers)));

				Set<String> setExpertUsers = new TreeSet<>(Arrays.asList(MiscUtils.split(expertUsers)));
				Set<String> setAdminUsers = new TreeSet<>(Arrays.asList(MiscUtils.split(adminUsers)));
				Set<String> setBlindNames = new TreeSet<>(Arrays.asList(MiscUtils.split(blindNamesUsers)));
				Set<String> setBlindAll = new TreeSet<>(Arrays.asList(MiscUtils.split(blindAllUsers)));

				if(!Collections.disjoint(setExpertUsers, setAdminUsers)) throw new Exception("Some users have expert and admin access");
				if(!Collections.disjoint(setExpertUsers, setBlindNames)) throw new Exception("Some users have expert and blind access");
				if(!Collections.disjoint(setExpertUsers, setBlindAll)) throw new Exception("Some users have expert and blind access");
				if(!Collections.disjoint(setAdminUsers, setBlindNames)) throw new Exception("Some users have admin and blind access");
				if(!Collections.disjoint(setAdminUsers, setBlindAll)) throw new Exception("Some users have admin and blind access");
				if(!Collections.disjoint(setBlindNames, setBlindAll)) throw new Exception("Some users have different blind access");


				Set<String> allUsers = new HashSet<>();
				allUsers.addAll(setExpertUsers);
				allUsers.addAll(setAdminUsers);
				allUsers.addAll(setBlindNames);
				allUsers.addAll(setBlindAll);
				DBAdapter.getInstance().checkValid(allUsers);

				study.setAdminUsers(adminUsers);
				study.setExpertUsers(expertUsers);
				study.setBlindUsers(setBlindAll, setBlindNames);
			} else {
				study.setAdminUsers("");
				study.setExpertUsers("");
				study.setBlindAllUsers(new HashSet<String>());
				study.setBlindDetailsUsers(new HashSet<String>());
			}

			//Metadata
			Map<String, String> metaMap = study.getMetadataMap();
			for (String metadataKey : SpiritProperties.getInstance().getValues(PropertyKey.STUDY_METADATA)) {
				String name = SpiritProperties.getInstance().getValue(PropertyKey.STUDY_METADATA_NAME, metadataKey);
				boolean req = SpiritProperties.getInstance().isChecked(PropertyKey.STUDY_METADATA_REQUIRED, metadataKey);
				JComponent comp = metadataKey2comp.get(metadataKey);
				String val = "";
				if(comp!=null && comp.isEnabled() && comp.isVisible()) {

					if(comp instanceof JGenericComboBox) {
						val = ((JGenericComboBox<String>)comp).getSelection();
					} else if(comp instanceof JCustomTextField) {
						if(!((JCustomTextField)comp).isValidFormat()) throw new Exception("The format of '" + name + "' is invalid");
						val = ((JTextField)comp).getText();
					} else if(comp instanceof JTextField) {
						val = ((JTextField)comp).getText();
					} else if(comp instanceof JCheckBox) {
						val = ((JCheckBox)comp).isSelected()?"true":"false";
					} else if (comp instanceof DatePicker) {
						DatePicker c = (DatePicker) comp;
						val = c.getText();
					} else {
						assert false;
					}
					if(val==null) val = "";

					if(req && val.length()==0) throw new Exception(name + " is required");
					metaMap.put(metadataKey, val);
				}
			}
			study.setMetadataMap(metaMap);

			//Notes
			study.setNotes(notesTextArea.getText());

			List<EmployeeGroup> egs = new ArrayList<>();
			if(eg1ComboBox.getSelection()!=null) egs.add(eg1ComboBox.getSelection());
			if(eg2ComboBox.getSelection()!=null) egs.add(eg2ComboBox.getSelection());
			study.setEmployeeGroups(egs);
			study.setUpdUser(user.getUsername());

			if(inTransaction) {
				if(!Spirit.askReasonForChangeIfUpdated(Collections.singleton(study))) return;
				try {
					JPAUtil.pushEditableContext(user);
					int id = study.getId();
					DAOStudy.persistStudies(Collections.singleton(study), user);
					SpiritChangeListener.fireModelChanged(id<=0? SpiritChangeType.MODEL_ADDED: SpiritChangeType.MODEL_UPDATED, Study.class, study);
				} finally {
					JPAUtil.popEditableContext();
				}
			}
			cancel = false;
			dispose();
		} catch (Exception e) {
			JExceptionDialog.showError(StudyInfoDlg.this, e);
		}
	}

	public Study getStudy() {
		return study;
	}

	public boolean isCancel() {
		return cancel;
	}


}
