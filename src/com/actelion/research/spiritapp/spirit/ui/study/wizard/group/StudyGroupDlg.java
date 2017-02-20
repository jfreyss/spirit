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

package com.actelion.research.spiritapp.spirit.ui.study.wizard.group;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.actelion.research.spiritapp.spirit.ui.help.HelpBinder;
import com.actelion.research.spiritapp.spirit.ui.study.wizard.StudyWizardDlg;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Biosample.HierarchyMode;
import com.actelion.research.spiritcore.business.study.Group;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.business.study.StudyAction;
import com.actelion.research.spiritcore.util.MiscUtils;
import com.actelion.research.util.CompareUtils;
import com.actelion.research.util.ui.JEscapeDialog;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.exceltable.Column;
import com.actelion.research.util.ui.exceltable.ExtendTable;
import com.actelion.research.util.ui.exceltable.ExtendTableModel;
import com.actelion.research.util.ui.exceltable.AbstractExtendTable.BorderStrategy;
import com.actelion.research.util.ui.iconbutton.IconType;
import com.actelion.research.util.ui.iconbutton.JIconButton;

public class StudyGroupDlg extends JEscapeDialog {

	private final StudyWizardDlg dlg;
	private final Study study;
	private int push = 0;
	
	
	private Column<Group, String> nameColumn = new Column<Group, String>("Name", String.class, 100) {
		public String getValue(Group row) {
			return row.getName();
		}			
	};

	@SuppressWarnings("unchecked")
	private final ExtendTableModel<Group> groupModel = new ExtendTableModel<Group>(new Column[] {nameColumn}) {
		@Override
		public Column<Group, ?> getTreeColumn()  {
			return nameColumn;
		}
		public Group getTreeParent(Group row) {
			return row==null? null: row.getFromGroup();
		}
	};
	private final ExtendTable<Group> groupTable = new ExtendTable<>(groupModel);
	
	private JButton duplicateButton = new JIconButton(IconType.DUPLICATE, "Duplicate");
	private JButton mergeButton = new JButton("Merge");
	private JButton splitButton = new JButton("Split group");
	private JButton deleteButton = new JIconButton(IconType.DELETE, "Delete");

	
	private EditGroupPanel editGroupPanel;

	public StudyGroupDlg(final StudyWizardDlg dlg, final Study s) {
		super(dlg, "Study Wizard - Groups");
		this.dlg  = dlg;
		this.study = s;
		
		groupTable.setFillsViewportHeight(true);
		groupTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		
		editGroupPanel = new EditGroupPanel(this);
		//
		JButton newGroupButton = new JIconButton(IconType.NEW, "New Group");
		newGroupButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				
				try {
					editGroupPanel.updateModel();
				} catch(Exception ex) {
					JExceptionDialog.showError(ex);
					return;
				}
				
				Group newGroup = new Group();
				newGroup.setName(suggestAbbreviation(study, null));
				if(study.isBlind()) {
					newGroup.setColorRgb(Color.WHITE.getRGB());
				} else {
					newGroup.setColorRgb(Group.generateColor(newGroup.getShortName()).getRGB());
				}
				
				
				newGroup.setFromGroup(null);
				newGroup.setFromPhase(study.getReferencePhase());
				newGroup.setStudy(study);

				refreshGroups();
				selectGroup(newGroup);
			}
		});

		groupTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		groupTable.getModel().setCanExpand(false);
		groupTable.setBorderStrategy(BorderStrategy.NO_BORDER);
		groupTable.setRowSelectionAllowed(true);
		groupTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if(e.getValueIsAdjusting()) return;
				if(push>0) return;
				List<Group> selection = groupTable.getSelection();
				
				deleteButton.setEnabled(selection.size()>0);
				duplicateButton.setEnabled(selection.size()>0);
				mergeButton.setEnabled(selection.size()>1);
				splitButton.setEnabled(selection.size()==1 && selection.get(0).getNSubgroups()>1);
				
				
				try {
					push++;
					if(editGroupPanel.getGroup()!=null) {
						editGroupPanel.updateModel();
						refreshGroups();
						groupTable.setSelection(selection);
					}
					
					editGroupPanel.setGroup(selection.size()==1? selection.get(0): null);
					
				} catch(Exception ex) {
					groupTable.setSelection(Collections.singleton(editGroupPanel.getGroup()));
					JExceptionDialog.showError(ex);
					return;
				} finally {
					push--;
				}

			}
		});		
		selectGroup(null);
		
		
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				try { 
					editGroupPanel.updateModel();
				} catch(Exception ex) {
					
				}
			}
		});
		
		
		//Duplicate Button
		duplicateButton.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					editGroupPanel.updateModel();
					editGroupPanel.setGroup(null);
					
					List<Group> selection = groupTable.getSelection();
					
					int res = JOptionPane.showConfirmDialog(dlg, "Are you sure you want to duplicate " + (selection.size()==1?"this group": "these " + selection.size() + " groups") + "?", "Duplicate", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
					if(res!=JOptionPane.OK_OPTION) return;

					Map<Group, Group> createdGroups = new HashMap<>();
					for (Group group : selection) {
						Group newGroup = new Group();				
						newGroup.setColorRgb(Group.generateColor(newGroup.getShortName()).getRGB());				
						newGroup.copyFrom(group);
						
						createdGroups.put(group, newGroup);
						
					}
					for (Group group : createdGroups.keySet()) {
						Group newGroup = createdGroups.get(group);
						newGroup.setName(suggestAbbreviation(study, newGroup) + " " + newGroup.getNameWithoutShortName(), "");
						newGroup.setStudy(study);
						newGroup.setFromGroup(group.getFromGroup()==null? null: createdGroups.get(group.getFromGroup())==null? group.getFromGroup(): createdGroups.get(group.getFromGroup()));
						
						//Copy actions
						List<StudyAction> actions = new ArrayList<>();
						for(StudyAction a: study.getStudyActions(group)) {
							StudyAction a2 = a.clone();
							a2.setGroup(newGroup);
							a2.setId(0);
							actions.add(a2);
						}
						study.addStudyActions(actions);
						
					}
					
					
					refreshGroups();
				} catch(Exception ex) {
					JExceptionDialog.showError(dlg, ex);
					return;
				}				
			}
		});
		
		//Merge Button
		mergeButton.setToolTipText("Merge the selected group into 1 group with several subgroups");
		mergeButton.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				
				try {
					//Update Model
					editGroupPanel.updateModel();
					editGroupPanel.setGroup(null);

					List<Group> selection = groupTable.getSelection();
					if(selection.size()<2) throw new Exception("You must select 2 groups or more");
					
					//Ask the user for the merged group
					int res = JOptionPane.showConfirmDialog(dlg,
							"<html>Merge into " + selection.get(0).getName() + "<br><br>" +
							"<i>(All selected groups will be converted to subgroups of " + selection.get(0).getName() + " and all attached samples will be moved there)</i><br>",
							"Merge",
							JOptionPane.OK_CANCEL_OPTION, 
							JOptionPane.QUESTION_MESSAGE);
					if(res!=JOptionPane.OK_OPTION) return;
					
					//Check if the merge can be done
					Group mergeIntoGroup = selection.get(0);
					for (int i = 1; i < selection.size(); i++) {
						Group group = selection.get(i);
						if(!CompareUtils.equals(mergeIntoGroup.getFromGroup(), group.getFromGroup())) throw new Exception("You cannot merge 2 groups coming from different origins");
						if(!CompareUtils.equals(mergeIntoGroup.getFromPhase(), group.getFromPhase())) throw new Exception("You cannot merge 2 groups if the group assignment is made at a different time");
						if(!CompareUtils.equals(mergeIntoGroup.getToGroups(), group.getToGroups())) throw new Exception("You cannot merge 2 groups, which are then split differently");
						if(!CompareUtils.equals(mergeIntoGroup.getDividingSampling(), group.getDividingSampling())) throw new Exception("You cannot merge 2 groups with a dividing sampling");
						if(!CompareUtils.equals(mergeIntoGroup.getDividingGroups(), group.getDividingGroups())) throw new Exception("You cannot merge 2 groups with a dividing sampling");
						if(!CompareUtils.equals(mergeIntoGroup.getDescription(-1), group.getDescription(-1))) {
							res = JOptionPane.showConfirmDialog(dlg, mergeIntoGroup+" and "+group+" don't seem to be completely comparable. Are you sure you want to continue?", "Merge", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
							if(res!=JOptionPane.YES_OPTION) return;
						}
						
					}

					for (int j = 1; j < selection.size(); j++) {
						Group group = selection.get(j);
						//Set the sizes of the merged subgroups
						int[] mergeIntoSizes = mergeIntoGroup.getSubgroupSizes();
						int[] groupSizes = group.getSubgroupSizes();									
						int[] newSubgroupsSizes = new int[mergeIntoSizes.length+groupSizes.length];
						for (int i = 0; i < newSubgroupsSizes.length; i++) {
							newSubgroupsSizes[i] = i<mergeIntoSizes.length? mergeIntoSizes[i]: groupSizes[i-mergeIntoSizes.length];
						}					
						mergeIntoGroup.setSubgroupSizes(newSubgroupsSizes);
						
						//Move the actions 
						for(StudyAction action: study.getStudyActions(group)) {
							action.setGroup(mergeIntoGroup);
							action.setSubGroup(action.getSubGroup()+mergeIntoSizes.length);
						}
						
						//Move the biosamples
						for(Biosample top: study.getTopAttachedBiosamples(group)) {
							for(Biosample b: top.getHierarchy(HierarchyMode.ATTACHED_SAMPLES)) {
								b.setInheritedGroup(mergeIntoGroup);
								b.setInheritedSubGroup(b.getInheritedSubGroup()+mergeIntoSizes.length);
							}
						}
						
						//Remove the group
						group.remove();
						study.getGroups().remove(group);
						group = null;
					}
					refreshGroups();
					groupTable.setSelection(Collections.singleton(mergeIntoGroup));
					
					
				} catch(Exception ex) {
					JExceptionDialog.showError(dlg, ex);
				}
				
			}
		});

		splitButton.setToolTipText("Split the subgroups of the selected group into the own groups");
		splitButton.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {

				try {
					//Update Model
					editGroupPanel.updateModel();
					editGroupPanel.setGroup(null);
					
					//Check if the merge can be done
					List<Group> selection = groupTable.getSelection();
					if(selection.size()!=1) throw new Exception("You must select 1 group");
					Group group = selection.get(0);
					
					if(group.getNSubgroups()<=1) throw new Exception("The selected group does not have subgroup and therefore cannot be splitted");

					//Ask the user for the merged group
					int res = JOptionPane.showOptionDialog(dlg,
							UIUtils.createVerticalBox(
									new JLabel("<html>This function will split this group into new ones.<br>"
									+ "As a consequence, the subgroups will be converted as new groups<br>"
									+ "and all attached samples will be moved there.<br><br>")),
							"Split into an other group",
							JOptionPane.OK_CANCEL_OPTION, 
							JOptionPane.PLAIN_MESSAGE, 
							null, null, null);
					if(res!=JOptionPane.YES_OPTION) return;
					
						
					//Create the new groups
					for(int subgroup=0; subgroup<group.getNSubgroups(); subgroup++) {
						Group g = new Group();
						g.setName(group.getShortName() + (char) ('A'+subgroup), group.getNameWithoutShortName());
						g.copyFrom(group);
						g.setSubgroupSizes(new int[] {group.getSubgroupSize(subgroup)});
						g.setStudy(study);
						//Move the actions 
						for(StudyAction action: study.getStudyActions(group, subgroup)) {
							action.setGroup(g);
							action.setSubGroup(0);
						}
						
						//Move the biosamples
						for(Biosample top: study.getTopAttachedBiosamples(group, subgroup)) {
							for(Biosample b: top.getHierarchy(HierarchyMode.ATTACHED_SAMPLES)) {
								b.setInheritedGroup(g);
								b.setInheritedSubGroup(0);
							}
						}						
					}					
					
					//Remove the group
					study.getGroups().remove(group);
					group.remove();
					group = null;

					study.resetCache();					
					refreshGroups();
					
					
				} catch(Exception ex) {
					JExceptionDialog.showError(dlg, ex);
				}
			}
		});
		
		
		//Delete Button
		deleteButton.setToolTipText("Delete the selected groups");
		deleteButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ev) {
				try {
					editGroupPanel.setGroup(null);

					List<Group> selection = groupTable.getSelection();
					if(selection.size()<1) throw new Exception("You must select 1 group or more");
					
					for (Group group: selection) {
						if(!study.getTopAttachedBiosamples(group).isEmpty()) throw new Exception("You cannot delete a group if there are samples attached to it");
					}
					
					int res = JOptionPane.showConfirmDialog(dlg, "Are you sure you want to delete " + (selection.size()==1?"this group": "these " + selection.size() + " groups") + "?", "Delete", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
					if(res!=JOptionPane.OK_OPTION) return;

					for (Group group: selection) {
						group.remove();
						study.getGroups().remove(group);
						group = null;
					}
					
					refreshGroups();
				} catch (Exception e) {
					e.printStackTrace();
					JOptionPane.showMessageDialog(dlg, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
				}
				
			}
		});

		
		JButton closeButton = new JButton("Close");
		closeButton.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					editGroupPanel.updateModel();
				} catch(Exception ex) {
					JExceptionDialog.showError(ex);
					return;
				}

				dispose();
			}
		});
		
		//layout
		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, 
				UIUtils.createTitleBox("Groups", 
						UIUtils.createBox(
								new JScrollPane(groupTable),
								UIUtils.createHorizontalBox(newGroupButton, Box.createHorizontalGlue()),
								UIUtils.createVerticalBox(
										UIUtils.createHorizontalBox(duplicateButton, Box.createHorizontalGlue()), 
										UIUtils.createHorizontalBox(splitButton, mergeButton, Box.createHorizontalGlue()), 
										UIUtils.createHorizontalBox(deleteButton, Box.createHorizontalGlue())))),
				editGroupPanel);
		splitPane.setDividerLocation(220);
		add(BorderLayout.CENTER, splitPane); 
		add(BorderLayout.SOUTH, UIUtils.createHorizontalBox(HelpBinder.createHelpButton(), Box.createHorizontalGlue(), closeButton) ); 
		UIUtils.adaptSize(this, 840, 800);
		refreshGroups();
		setLocationRelativeTo(dlg);
		setVisible(true);
	}
	
	public Study getStudy() {
		return study;
	}	
	
	public List<Group> getGroups() {
		return study.getGroupsHierarchical();
	}	

	public void refreshGroups() {
		push++;
		try {
			groupTable.setRows(study.getGroupsHierarchical());
			dlg.refresh();
		} finally {
			push--;
		}
	}
	
	public void refreshStudy() {		
		dlg.refresh();
	}
	
	public void selectGroup(Group selection) {		
		groupTable.setSelection(selection==null? null: Collections.singleton(selection));
		editGroupPanel.setGroup(selection);
		dlg.refresh();
	}
	
	public static String suggestAbbreviation(Study study, Group group) {
		Set<String> seen = new HashSet<>();
		for (Group g : study.getGroups()) {
			seen.add(g.getShortName());
		}
		if(group==null) {
			if(study.getGroups().size()==0) {
				return "1";				
			} else {
				group = new ArrayList<Group>(study.getGroups()).get(study.getGroups().size()-1);
			}
		}
		
		assert group!=null;
		String newAbbr;
		for (int i = 0; i < 200; i++) {
			newAbbr = MiscUtils.incrementName(group.getShortName());
			if(!seen.contains(newAbbr)) return newAbbr;
		}
		return "??";
	}
}
