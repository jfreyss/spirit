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

package com.actelion.research.spiritapp.spirit.ui.study.wizard;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritapp.spirit.ui.help.HelpBinder;
import com.actelion.research.spiritapp.spirit.ui.lf.EmployeeGroupComboBox;
import com.actelion.research.spiritapp.spirit.ui.lf.LF;
import com.actelion.research.spiritapp.spirit.ui.lf.UserIdTextArea;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeListener;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeType;
import com.actelion.research.spiritcore.adapter.DBAdapter;
import com.actelion.research.spiritcore.adapter.PropertyKey;
import com.actelion.research.spiritcore.adapter.DBAdapter.UserAdministrationMode;
import com.actelion.research.spiritcore.business.DataType;
import com.actelion.research.spiritcore.business.employee.EmployeeGroup;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.spiritcore.services.dao.ConfigProperties;
import com.actelion.research.spiritcore.services.dao.DAOStudy;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.spiritcore.services.helper.WorkflowHelper;
import com.actelion.research.spiritcore.util.MiscUtils;
import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.JCustomLabel;
import com.actelion.research.util.ui.JCustomTextField;
import com.actelion.research.util.ui.JEscapeDialog;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.JGenericComboBox;
import com.actelion.research.util.ui.JInfoLabel;
import com.actelion.research.util.ui.JTextComboBox;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.iconbutton.JIconButton;

public class StudyInfoDlg extends JEscapeDialog {

	private Study study;
	private final boolean inTransaction;
	
	private JCustomTextField ivvField = new JCustomTextField(JCustomTextField.ALPHANUMERIC, 12);
	private JCustomTextField studyIdField = new JCustomTextField(JCustomTextField.ALPHANUMERIC, 12);
	private JCustomTextField titleField = new JCustomTextField(JCustomTextField.ALPHANUMERIC, 60);
	
	private JGenericComboBox<String> stateComboBox;
	private Map<String, JComponent> metadataKey2comp = new HashMap<>();
	private EmployeeGroupComboBox eg1ComboBox = new EmployeeGroupComboBox(false);
	private EmployeeGroupComboBox eg2ComboBox = new EmployeeGroupComboBox(false);
//	private EmployeeGroupComboBox eg3ComboBox = new EmployeeGroupComboBox(false);


	private UserIdTextArea adminUsersTextArea = new UserIdTextArea(5, 22);
	private UserIdTextArea expertUsersTextArea = new UserIdTextArea(5, 22);
	private JTextArea blindAllUsersField = new JTextArea(2, 20);
	private JTextArea blindNamesUsersField = new JTextArea(2, 20);
	private JTextArea commentsTextArea = new JTextArea(4, 28);
	
	private JCheckBox synchroCheckbox = new JCheckBox("Generate samples automatically based on the design");
	
	private JTextArea notesTextArea = new JTextArea(8, 25);
	
	private boolean cancel = true;
	
	public StudyInfoDlg(Study study, boolean inTransaction) {
		super(UIUtils.getMainFrame(), "Study - Definition");
				
		this.study = study;		
		this.inTransaction = inTransaction;
		studyIdField.setEnabled(false);
		
//		typeComboBox = new JTextComboBox(DAOStudy.getAllStudyTypes());
//		siteComboBox = new JTextComboBox(DAOStudy.getAllStudyCenters());
//		siteComboBox.setEditable(true);
//		
//		
//		projectComboBox = new JTextComboBox(DAOStudy.getAllStudyProjects());
//		diseaseComboBox = new JComboCheckBox(DAOStudy.getAllStudyDiseaseArea());
	
		JPanel studyDescPanel = UIUtils.createTitleBox("Definition", UIUtils.createTable(
					new JLabel("StudyId*: "), UIUtils.createHorizontalBox(studyIdField, new JInfoLabel("Unique, cannot be changed")),
					new JLabel("InternalId:"), UIUtils.createHorizontalBox(ivvField, new JInfoLabel("Your own identifier (unique)")),
					new JLabel("Title*: "), titleField,
					null, synchroCheckbox, 
					null, new JInfoLabel("(recommended when the samples are perfectly defined in the study design, through the sampling templates)")));
		//Metadata
		List<JComponent> comps = new ArrayList<>();
		for (String metadataKey : ConfigProperties.getInstance().getValues(PropertyKey.STUDY_METADATA)) {
			String name = ConfigProperties.getInstance().getValue(PropertyKey.STUDY_METADATA_NAME, metadataKey);
			String datatype = ConfigProperties.getInstance().getValue(PropertyKey.STUDY_METADATA_DATATYPE, metadataKey);
			String params = ConfigProperties.getInstance().getValue(PropertyKey.STUDY_METADATA_PARAMETERS, metadataKey);
			String[] roles = ConfigProperties.getInstance().getValues(PropertyKey.STUDY_METADATA_ROLES, metadataKey);
			String[] states = ConfigProperties.getInstance().getValues(PropertyKey.STUDY_METADATA_STATES, metadataKey);
			boolean req = ConfigProperties.getInstance().isChecked(PropertyKey.STUDY_METADATA_REQUIRED, metadataKey);
			JComponent comp;
			if(DataType.LIST.name().equals(datatype)) {
				JGenericComboBox<String> combo = new JGenericComboBox<String>(MiscUtils.split(params), true);
				combo.setSelection(study.getMetadata().get(metadataKey));
				comp = combo;
			} else if(DataType.DATE.name().equals(datatype)) {
				JCustomTextField dateField = new JCustomTextField(JCustomTextField.DATE);
				dateField.setText(study.getMetadata().get(metadataKey));
				comp = dateField;
			} else if(DataType.AUTO.name().equals(datatype)) {
				JTextComboBox field = new JTextComboBox();
				field.setChoices(DAOStudy.getAllMetadata(metadataKey));
				field.setText(study.getMetadata().get(metadataKey));
				comp = field;
			} else {
				JCustomTextField textField = new JCustomTextField(20);
				textField.setText(study.getMetadata().get(metadataKey));
				comp = textField;
			}
			if(states.length>0 && !MiscUtils.contains(states, study.getState())) comp.setEnabled(false);
			if(roles.length>0 && !MiscUtils.contains(roles, Spirit.getUser().getRoles())) comp.setEnabled(false);
			if(req) comp.setBackground(LF.BGCOLOR_REQUIRED);
			
			comps.add(new JLabel(name+": " + (req?"(req.)":"")));				
			comps.add(comp);		
			metadataKey2comp.put(metadataKey, comp);
		}
		JPanel metadataPanel = null;
		if(comps.size()>0) {
			if(comps.size()>=4) {
				int med = (((comps.size()/2)+1)/2)*2;
				metadataPanel = UIUtils.createTitleBox("Metadata", UIUtils.createHorizontalBox(UIUtils.createTable(comps.subList(0, med)), Box.createHorizontalStrut(30), UIUtils.createTable(comps.subList(med, comps.size())), Box.createHorizontalGlue()));
			} else {
				metadataPanel = UIUtils.createTitleBox("Metadata", UIUtils.createTable(comps));
			}
		}
		
		
		//States
		List<String> possibleStates = WorkflowHelper.getNextStates(study, Spirit.getUser());		
		stateComboBox = new JGenericComboBox<String>(possibleStates, false);

		JPanel statusPanel = UIUtils.createTitleBox("State", 
				UIUtils.createHorizontalBox(
						UIUtils.createVerticalBox(UIUtils.createHorizontalBox(new JLabel("State: "), stateComboBox), Box.createVerticalGlue()), 
						new JScrollPane(new JLabel(WorkflowHelper.getStateDescriptions())), 
						Box.createHorizontalGlue()));
		
		
		//Rights
		JPanel studyRightsPanel = new JPanel(new BorderLayout());
		if(DBAdapter.getAdapter().getUserManagedMode()!=UserAdministrationMode.UNIQUE_USER && !ConfigProperties.getInstance().isChecked(PropertyKey.RIGHT_ROLEONLY)) {
			
			blindNamesUsersField.setToolTipText("The group's name and the treatment's compounds are hidden. Those users can still see the group no (1A) and the treatment's name (blue) in AnimalCare");
			blindAllUsersField.setToolTipText("The groups and the treatments compounds are completely hidden. Those users can only see the samples in AnimalCare");
						
			JPanel editUsersPanel =  UIUtils.createVerticalBox(BorderFactory.createEtchedBorder(), 
					UIUtils.createHorizontalBox(new JCustomLabel("Admin Users: ", FastFont.BOLD), Box.createHorizontalGlue()),
					UIUtils.createHorizontalBox(new JInfoLabel("(can edit the infos/design)"), Box.createHorizontalGlue()),
					UIUtils.createHorizontalBox(Box.createHorizontalStrut(10), adminUsersTextArea, Box.createHorizontalGlue()),
					Box.createVerticalGlue());
			 
			JPanel readUsersPanel = UIUtils.createVerticalBox(BorderFactory.createEtchedBorder(),
					UIUtils.createHorizontalBox(new JCustomLabel("Experimenters:", FastFont.BOLD), Box.createHorizontalGlue()),
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
					new JInfoLabel("(cannot view the groups/treaments)"),
					UIUtils.createHorizontalBox(Box.createHorizontalStrut(10), blindAllUsersField, Box.createHorizontalGlue()),
					Box.createVerticalGlue());
					

			studyRightsPanel = UIUtils.createTitleBox("Specific User rights", 
					UIUtils.createHorizontalBox(
							editUsersPanel, //UIUtils.createBox(editUsersPanel, UIUtils.createHorizontalBox(BorderFactory.createEtchedBorder(), new JCustomLabel("Owner: ", Font.BOLD), ownerComboBox, Box.createHorizontalGlue())),  
							readUsersPanel, 
							blindPanel, 
							Box.createHorizontalGlue()));
			studyRightsPanel.setVisible(!"false".equals(ConfigProperties.getInstance().getValue(PropertyKey.RIGHT_ROLEONLY)));
		}
		
		//NotesPanel
		JPanel studyNotesPanel = UIUtils.createTitleBox("Notes", new JScrollPane(notesTextArea));
		
		//Buttons
		JButton okButton;
		if(inTransaction) {
			okButton = new JIconButton(JIconButton.IconType.SAVE, "Save");
		} else {
			okButton = new JButton("OK");
		}

		okButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ev) {
				try {
					ok();
					cancel = false;
					dispose();
				} catch (Exception e) {
					JExceptionDialog.showError(StudyInfoDlg.this, e);
				}
				
			}
		});
		
		//Update View
		if(study.getId()<=0) {
			study.setStudyId(DAOStudy.getNextStudyId());
		}

		studyIdField.setText(study.getStudyId());
		ivvField.setText(study.getIvv());
//		projectComboBox.setText(study.getProject());
		
		titleField.setText(study.getTitle());
//		diseaseComboBox.setText(study.getDiseaseArea());
//		typeComboBox.setText(study.getType());
//		ownerComboBox.setText(study.getOwner());
//		clinicalComboBox.setSelection(study.getClinicalStatus());
		List<EmployeeGroup> egs = study.getEmployeeGroups();
		eg1ComboBox.setSelection(egs.size()>0? egs.get(0): null);
		eg2ComboBox.setSelection(egs.size()>1? egs.get(1): null);
//		eg3ComboBox.setSelection(egs.size()>2? egs.get(2): null);
//		siteComboBox.setText(study.getSite());
		stateComboBox.setSelection(study.getState());

		synchroCheckbox.setSelected(study.isSynchronizeSamples());
		
		adminUsersTextArea.setText(study.getWriteUsers());
		expertUsersTextArea.setText(study.getExpertUsers());
		
		notesTextArea.setText(study.getNotes());
		blindAllUsersField.setText(study.getBlindAllUsers());
		blindNamesUsersField.setText(study.getBlindDetailsUsers());
		
		commentsTextArea.setText(study.getNotes());			
		
		setLayout(new BorderLayout());
		add(BorderLayout.CENTER, UIUtils.createVerticalBox(studyDescPanel, metadataPanel, statusPanel, studyRightsPanel, studyNotesPanel));
		add(BorderLayout.SOUTH, UIUtils.createHorizontalBox(HelpBinder.createHelpButton(), Box.createHorizontalGlue(), okButton));
		
		stateComboBox.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				eventStudyStatusChanged();
			}
		});
		
//		titleField.addTextChangeListener(new TextChangeListener() {
//			@Override
//			public void textChanged(JComponent src) {
//				if(projectComboBox.getText().length()==0) {
//					for (String v : projectComboBox.getChoices()) {
//						if(v.length()>2 && titleField.getText().toUpperCase().contains(v.toUpperCase())) {
//							projectComboBox.setText(v);
//							break;
//						}
//					}
//				}
//				
//			}
//		});
		eventStudyStatusChanged();
						
		//Set visible
		pack();
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setLocationRelativeTo(UIUtils.getMainFrame());		
		setVisible(true);
		
	}
	

	
	private void eventStudyStatusChanged() {
		String state =  stateComboBox.getSelection();
		String[] expertRoles = ConfigProperties.getInstance().getValues(PropertyKey.STUDY_STATES_EXPERT, state);
		String[] adminRoles = ConfigProperties.getInstance().getValues(PropertyKey.STUDY_STATES_ADMIN, state);
		
		adminUsersTextArea.setEnabled(!MiscUtils.contains(adminRoles, "ALL"));
		expertUsersTextArea.setEnabled(!MiscUtils.contains(expertRoles, "ALL"));
		eg1ComboBox.setEnabled(!MiscUtils.contains(expertRoles, "ALL"));
		eg2ComboBox.setEnabled(!MiscUtils.contains(expertRoles, "ALL"));
	}
	
	
	@SuppressWarnings("unchecked")
	public void ok() throws Exception {
		
		if(stateComboBox.getSelection()==null)  new Exception("You must select a status");
		SpiritUser user = Spirit.askForAuthentication();
		
		//Users
		if(DBAdapter.getAdapter().getUserManagedMode()!=UserAdministrationMode.UNIQUE_USER && !"false".equals(ConfigProperties.getInstance().getValue(PropertyKey.RIGHT_ROLEONLY))) {
			//Check that the write-users are valid
			String writeUsers = adminUsersTextArea.getText().trim();
			if(writeUsers.length()==0) throw new Exception("One Write-User is required");
			
			//Check that the read-users are valid
			String expertUsers = expertUsersTextArea.getText().trim();
	
			//Check that the blind-users are valid
			if(!blindAllUsersField.isEnabled()) {
				blindAllUsersField.setText("");
				blindNamesUsersField.setText("");
			}
			String blindAllUsers = blindAllUsersField.getText().trim();
			String blindNamesUsers = blindNamesUsersField.getText().trim();
			DBAdapter.getAdapter().checkValid(Arrays.asList(MiscUtils.split(blindAllUsers)));

//			String owner = ownerComboBox.getText();
			Set<String> setExpertUsers = new TreeSet<>(Arrays.asList(MiscUtils.split(expertUsers)));
			Set<String> setAdminUsers = new TreeSet<>(Arrays.asList(MiscUtils.split(writeUsers)));
			Set<String> setBlindNames = new TreeSet<>(Arrays.asList(MiscUtils.split(blindNamesUsers)));
			Set<String> setBlindAll = new TreeSet<>(Arrays.asList(MiscUtils.split(blindAllUsers)));
			
			if(!Collections.disjoint(setExpertUsers, setAdminUsers)) throw new Exception("Some users have expert and admin access");
			if(!Collections.disjoint(setExpertUsers, setBlindNames)) throw new Exception("Some users have expert and blind access");
			if(!Collections.disjoint(setExpertUsers, setBlindAll)) throw new Exception("Some users have expert and blind access");
			if(!Collections.disjoint(setAdminUsers, setBlindNames)) throw new Exception("Some users have admin and blind access");
			if(!Collections.disjoint(setAdminUsers, setBlindAll)) throw new Exception("Some users have admin and blind access");
			if(!Collections.disjoint(setBlindNames, setBlindAll)) throw new Exception("Some users have different blind access");
			

			Set<String> allUsers = new HashSet<>();
//			if(owner!=null && owner.length()>0) allUsers.add(owner);
			allUsers.addAll(setExpertUsers);
			allUsers.addAll(setAdminUsers);
			allUsers.addAll(setBlindNames);
			allUsers.addAll(setBlindAll);
			DBAdapter.getAdapter().checkValid(allUsers);

//			study.setOwner(owner);
			study.setWriteUsers(writeUsers);
			study.setExpertUsers(expertUsers);
			study.setBlindAllUsers(setBlindAll);
			study.setBlindDetailsUsers(setBlindNames);
		} else {
//			study.setOwner("");
			study.setWriteUsers("");
			study.setExpertUsers("");
			study.setBlindAllUsers(new HashSet<String>());
			study.setBlindDetailsUsers(new HashSet<String>());	
		}
		
		//Metadata
		Map<String, String> metaMap = study.getMetadata();
		for (String metadataKey : ConfigProperties.getInstance().getValues(PropertyKey.STUDY_METADATA)) {
			String name = ConfigProperties.getInstance().getValue(PropertyKey.STUDY_METADATA_NAME, metadataKey);
			boolean req = ConfigProperties.getInstance().isChecked(PropertyKey.STUDY_METADATA_REQUIRED, metadataKey);
			JComponent comp = metadataKey2comp.get(metadataKey);
			if(!comp.isEnabled()) continue;
			
			String val = null;
			if(comp instanceof JGenericComboBox) {
				val = ((JGenericComboBox<String>)comp).getSelection();
			} else if(comp instanceof JTextField) {
				val = ((JTextField)comp).getText();
			} else if(comp instanceof JCheckBox) {
				val = ((JCheckBox)comp).isSelected()?"true":"false";				
			}
			if(val==null) val = "";
			
			if(req && val.length()==0) throw new Exception(name + " is required");  
			metaMap.put(metadataKey, val);
		}
		study.setMetadata(metaMap);
		
		
		
		study.setStudyId(studyIdField.getText());
		study.setState(stateComboBox.getSelection());
		study.setIvv(ivvField.getText());
//		study.setProject(projectComboBox.getText());
//		study.setDiseaseArea(diseaseComboBox.getText());
//		study.setType(typeComboBox.getText());
		study.setTitle(titleField.getText());
		study.setNotes(notesTextArea.getText());
		study.setState(stateComboBox.getSelection());
//		study.setClinicalStatus(clinicalComboBox.getSelection());
		study.setSynchronizeSamples(synchroCheckbox.isSelected());
		
		List<EmployeeGroup> egs = new ArrayList<>();
		if(eg1ComboBox.getSelection()!=null) egs.add(eg1ComboBox.getSelection());
		if(eg2ComboBox.getSelection()!=null) egs.add(eg2ComboBox.getSelection());
//		if(eg3ComboBox.getSelection()!=null) egs.add(eg3ComboBox.getSelection());
		study.setEmployeeGroups(egs);
		
//		study.setSite(siteComboBox.getText());
		study.setUpdUser(user.getUsername());
		
		
		if(inTransaction) {
			boolean inEditContext = JPAUtil.isEditableContext();
			try {
				if(!inEditContext) JPAUtil.pushEditableContext(user);
				int id = study.getId();
				study = DAOStudy.persistStudy(study, user);
				SpiritChangeListener.fireModelChanged(id<=0? SpiritChangeType.MODEL_ADDED: SpiritChangeType.MODEL_UPDATED, Study.class, study);
			} finally {
				if(!inEditContext) JPAUtil.popEditableContext();
			}
		}
	}

	public Study getStudy() {
		return study;
	}
	
	public boolean isCancel() {
		return cancel;
	}
	
}
