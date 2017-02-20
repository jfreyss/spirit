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

package com.actelion.research.spiritapp.spirit.ui.study.sampling;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritapp.spirit.ui.biosample.BiosampleTable;
import com.actelion.research.spiritapp.spirit.ui.container.ContainerTypeComboBox;
import com.actelion.research.spiritapp.spirit.ui.help.HelpBinder;
import com.actelion.research.spiritapp.spirit.ui.study.wizard.StudyWizardDlg;
import com.actelion.research.spiritapp.spirit.ui.util.component.JSpiritEscapeDialog;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.BiosampleQuery;
import com.actelion.research.spiritcore.business.biosample.BiotypeCategory;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.ResultQuery;
import com.actelion.research.spiritcore.business.study.Measurement;
import com.actelion.research.spiritcore.business.study.NamedSampling;
import com.actelion.research.spiritcore.business.study.Sampling;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.business.study.StudyAction;
import com.actelion.research.spiritcore.services.dao.DAOBiosample;
import com.actelion.research.spiritcore.services.dao.DAONamedSampling;
import com.actelion.research.spiritcore.services.dao.DAOResult;
import com.actelion.research.spiritcore.services.dao.DAOStudy;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.spiritcore.services.helper.BiosampleCreationHelper;
import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.JCustomLabel;
import com.actelion.research.util.ui.JCustomTextField;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.TextChangeListener;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.exceltable.ExtendTableCellRenderer;
import com.actelion.research.util.ui.exceltable.JComponentNoRepaint;
import com.actelion.research.util.ui.iconbutton.IconType;
import com.actelion.research.util.ui.iconbutton.JIconButton;

public class NamedSamplingDlg extends JSpiritEscapeDialog {
	
	private final StudyWizardDlg dlg;
	private final boolean addDlg;
	private final Study study;
	private final boolean transactionMode;
	private boolean success;
	
	private NamedSampling namedSamplingToEdit;
	private NamedSampling namedSamplingProxy;
	private JCheckBox necropsyCheckbox = new JCheckBox("Perform Necropsy after this sampling");
	private final JCustomTextField nameTextField = new JCustomTextField(JCustomTextField.ALPHANUMERIC, 14);		
	private final JPanel samplingsPanel = new JPanel(new GridBagLayout());
	private final NamedSamplingEditorPane namedSamplingEditorPane = new NamedSamplingEditorPane();

	public NamedSamplingDlg(final Study myStudy, NamedSampling namedSamplingToEdit, final StudyWizardDlg dlg) {
		super(UIUtils.getMainFrame(), "Edit Sampling Template", dlg==null? NamedSamplingDlg.class.getName(): null);
		this.study = JPAUtil.reattach(myStudy);
		this.transactionMode = dlg==null;
		this.dlg = dlg;
		
		if(transactionMode && study!=null) throw new IllegalArgumentException("You can only edit a Sampling Template outside a study from here");
		if(!transactionMode && study==null) throw new IllegalArgumentException("You can only edit a Sampling Template in a study from here");
		
		
		if(namedSamplingToEdit == null) {
			//New sampling -> propose retrieving an existing template
			addDlg = true;
			
			NamedSamplingSelectorDlg dlg2 = new NamedSamplingSelectorDlg();
			
			if(!dlg2.isSuccess()) return;
			this.namedSamplingToEdit = dlg2.getNamedSampling();
			
		} else {
			//Old sampling
			addDlg = false;
			this.namedSamplingToEdit = JPAUtil.reattach(namedSamplingToEdit);
		}

		//Create a proxy, to avoid changing the object when the user closes the window
		this.namedSamplingProxy = new NamedSampling();
		namedSamplingProxy.copyFrom(this.namedSamplingToEdit);
		necropsyCheckbox.setEnabled(study!=null);

		
		//TopPanel
		JPanel topPanel = UIUtils.createTitleBox("", 
				UIUtils.createHorizontalBox(new JLabel("Template Name: "), nameTextField, Box.createHorizontalStrut(30), necropsyCheckbox, Box.createHorizontalGlue()));
		
		//CenterPanel
		JScrollPane sp1 = new JScrollPane(samplingsPanel);
		sp1.setBorder(BorderFactory.createLoweredBevelBorder());
		samplingsPanel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
				
		nameTextField.setText(this.namedSamplingToEdit.getName());
		necropsyCheckbox.setSelected(this.namedSamplingToEdit.isNecropsy());
		refresh();
		
	
		//Events
		JButton deleteButton = new JIconButton(IconType.DELETE, "Delete");
		JButton saveButton = new JIconButton(IconType.SAVE, transactionMode? "Save": "Accept");

		
		
		if(addDlg) {
			deleteButton.setEnabled(false);
		}
			
		deleteButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ae) {
				try {
					removeEvent();
				} catch (Exception e) {
					JExceptionDialog.showError(NamedSamplingDlg.this, e);
				}
				
			}
		});
		
		getRootPane().setDefaultButton(saveButton);
		saveButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ae) {
				try {
					saveEvent();
				} catch (Exception e) {
					JExceptionDialog.showError(NamedSamplingDlg.this, e);
				}				
			}
		});
		
		setLayout(new BorderLayout());
		add(BorderLayout.NORTH, topPanel);
		add(BorderLayout.CENTER, UIUtils.createTitleBox("Samplings", sp1));
		add(BorderLayout.SOUTH,  UIUtils.createHorizontalBox(
				HelpBinder.createHelpButton(),
				new JCustomLabel("Note: It is advised to enter only the samples that will be labeled.", Font.ITALIC),
				Box.createHorizontalGlue(),
				new JButton(new CloseAction("Cancel")),
				deleteButton,
				Box.createHorizontalGlue(),
				saveButton));
		UIUtils.adaptSize(this, 1000, 850);
//		centerPanel.setDividerLocation(getSize().width-350);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setLocationRelativeTo(UIUtils.getMainFrame());		
		setVisible(true);
		
		
	}	
	
	/**
	 * Refresh the panel
	 * 
	 * Layout:
	 * ---------------------------------------------------------------------------------
	 *     0  1                 2                          5             6          7
	 * 0   Sampling                                
	 * 1   Animal                                          
	 * 2                                                   
	 * 3    - Sample 1                                     
	 * 4                                                   
	 * 5                                                   
	 * 6    - Sample 2          Weight[  ]                                            
	 */
	public void refresh() {
		samplingsPanel.removeAll();
		
		//Header
		rowNo = 0;
		int y = 0;
		

		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.WEST;
		c.weightx = 0;
		c.weighty = 0;
		c.gridy = y;
		//SampleName
		c.gridx = 13; samplingsPanel.add(Box.createHorizontalStrut(5), c);		
		//Container
		c.gridx = 22; samplingsPanel.add(new JLabel("Container"), c);
		//Amount
		c.gridx = 25; samplingsPanel.add(new JLabel("Amount"), c);
		c.gridx = 30; samplingsPanel.add(Box.createHorizontalStrut(5), c);
		
		//Actions
		c.gridx = 34; samplingsPanel.add(new JLabel("Wgh. "), c);
		c.gridx = 35; samplingsPanel.add(new JLabel("Len. "), c);
		c.gridx = 36; samplingsPanel.add(new JLabel("Obs. "), c);
		c.gridx = 37; samplingsPanel.add(new JLabel("Others."), c);
		y++;
		for (Sampling s : namedSamplingProxy.getTopSamplings()) {
			y = drawRec(y, 0, s);
		}
		
		c.gridy = ++y;
		//
		// Add Sample Button
		JButton addSampleButton = new JIconButton(IconType.ADD_ROW, "Add Sampling");
		addSampleButton.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				Sampling sd = new Sampling();
				SamplingDlg dlg = new SamplingDlg(NamedSamplingDlg.this, study, sd, true);
				if(dlg.isSuccess() && sd.getBiotype()!=null) {
					namedSamplingProxy.getAllSamplings().add(sd);
					refresh();
				}
			}
		});
		
		c.gridy++; c.gridwidth = 35; c.gridx = 0; samplingsPanel.add(new JLabel(" "), c);
		c.gridy++; c.gridwidth = 35; c.gridx = 0; samplingsPanel.add(addSampleButton, c);
		
		
		c.gridy++; c.weighty = 1; c.weightx=1;
		c.gridx = 41; samplingsPanel.add(Box.createGlue(), c);
		
		samplingsPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLoweredBevelBorder(), BorderFactory.createEmptyBorder(2, 2, 2, 2)));
		samplingsPanel.setBackground(Color.WHITE);
		samplingsPanel.revalidate();
		repaint();
		
		namedSamplingEditorPane.setNamedSampling(namedSamplingProxy);
	}
	
	

	private int rowNo;
	private int drawRec(int y, int depth, final Sampling sampling) {
		
		final JButton actionButton = new JButton(IconType.NEXT.getIcon());
		actionButton.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
		actionButton.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				createActionMenu(sampling).show(actionButton, 0, actionButton.getBounds().height);
			}
		});

		
		SamplingLabel nameLbl = new SamplingLabel(sampling);
		nameLbl.setWrappingWidth(260);

		final JPanel hierarchyLabel = UIUtils.createHorizontalBox(
				Box.createHorizontalStrut(15*depth),
				new JComponentNoRepaint() {
					@Override
					protected void paintComponent(Graphics g) {
						g.setColor(Color.GRAY);
						g.fillOval(3, 8, 7, 7);
						g.setColor(Color.BLACK);
						g.fillOval(2, 7, 7, 7);
					}
					@Override
					public Dimension getPreferredSize() {
						return new Dimension(15, 24);
					}
				},
				nameLbl);
		
		final ContainerTypeComboBox containerTypeComboBox = new ContainerTypeComboBox();
		containerTypeComboBox.setSelection(sampling.getContainerType());
		
		final BlocNoComboBox blocNoComboBox = new BlocNoComboBox(false);
		blocNoComboBox.setSelection(sampling.getBlocNo()==null? 1: sampling.getBlocNo());
		blocNoComboBox.setVisible(sampling.getContainerType()!=null && sampling.getContainerType().isMultiple());
				
		containerTypeComboBox.setSelection(sampling.getContainerType());
		
		final JCustomTextField amountTextField = new JCustomTextField(JCustomTextField.DOUBLE);
		amountTextField.setText(sampling.getAmount()==null?"": ""+sampling.getAmount());
		amountTextField.addTextChangeListener(new TextChangeListener() {
			@Override
			public void textChanged(JComponent src) {
				try {
					Sampling copyFrom = sampling.clone();
					sampling.setAmount(amountTextField.getText().length()==0? null: Double.parseDouble(amountTextField.getText()));					
					synchronizeSamples(study, copyFrom, sampling);
					
				} catch(Exception e) {
					amountTextField.setText("");
				}
			}
		});
		
		
		final JCheckBox weighingCheckBox = new JCheckBox("", sampling.isWeighingRequired());
		weighingCheckBox.setToolTipText("Measure Weighing");
		final JCheckBox lengthCheckBox = new JCheckBox("", sampling.isLengthRequired());
		lengthCheckBox.setToolTipText("Measure Length");
		final JCheckBox obsCheckBox = new JCheckBox("", sampling.isCommentsRequired());
		obsCheckBox.setToolTipText("Measure Observation");
		weighingCheckBox.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				sampling.setWeighingRequired(weighingCheckBox.isSelected());
			}
		});
		lengthCheckBox.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				sampling.setLengthRequired(lengthCheckBox.isSelected());
			}
		});
		obsCheckBox.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				sampling.setCommentsRequired(obsCheckBox.isSelected());
			}
		});		

		hierarchyLabel.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				new Action_EditSample(sampling);
//				createActionMenu(sampling).show(hierarchyLabel, e.getX(), e.getY());
			}
		});
		
		containerTypeComboBox.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					Sampling copyFrom = sampling.clone();
					sampling.setContainerType(containerTypeComboBox.getSelection());
					blocNoComboBox.setVisible(sampling.getContainerType()!=null && sampling.getContainerType().isMultiple());
					synchronizeSamples(study, copyFrom, sampling);
				} catch(Exception ex) {
					JExceptionDialog.showError(ex);
				}

			}
		});
		blocNoComboBox.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					Sampling copyFrom = sampling.clone();
					sampling.setBlocNo(blocNoComboBox.getSelection());
					synchronizeSamples(study, copyFrom, sampling);
				} catch(Exception ex) {
					JExceptionDialog.showError(ex);
				}
			}
		});		

		GridBagConstraints c = new GridBagConstraints();
		c.gridy = y+1;
		c.weightx = 0;
		c.insets = new Insets(0, 1, 0, 1);
		

		c.anchor = GridBagConstraints.EAST;
		c.gridx = 0; samplingsPanel.add(new JLabel((++rowNo)+". "), c);			

		c.anchor = GridBagConstraints.WEST;
		
		amountTextField.setVisible(sampling.getBiotype()!=null && sampling.getBiotype().getAmountUnit()!=null);
		//Action
		c.gridx = 1; samplingsPanel.add(actionButton, c);
		//SampleName
		c.gridx = 11; samplingsPanel.add(hierarchyLabel, c);
		c.gridx = 13; samplingsPanel.add(Box.createHorizontalStrut(5), c);		
		//Container
		c.gridx = 22; samplingsPanel.add(containerTypeComboBox, c);
		c.gridx = 23; samplingsPanel.add(blocNoComboBox, c);
		//Amount
		c.gridx = 25; samplingsPanel.add(UIUtils.createHorizontalBox(amountTextField, new JCustomLabel(sampling.getBiotype()!=null && sampling.getBiotype().getAmountUnit()!=null? sampling.getBiotype().getAmountUnit().getUnit():"", FastFont.SMALL)), c);
		c.gridx = 30; samplingsPanel.add(Box.createHorizontalStrut(5), c);
		
		//Actions
		c.gridx = 34; samplingsPanel.add(UIUtils.createHorizontalBox(weighingCheckBox, Box.createHorizontalStrut(10)), c);
		c.gridx = 35; samplingsPanel.add(UIUtils.createHorizontalBox(lengthCheckBox, Box.createHorizontalStrut(10)), c);
		c.gridx = 36; samplingsPanel.add(UIUtils.createHorizontalBox(obsCheckBox, Box.createHorizontalStrut(10)), c);
		
		StringBuilder sb = new StringBuilder();
		if(sampling.getMeasurements().size()>0) {
			for (Measurement m : sampling.getMeasurements()) {
				sb.append((sb.length()==0?"":", ")+ m.getDescription());
			}
		} else {
			sb.append("-");
		}
		JLabel measurementLabel = new JCustomLabel(sampling.getMeasurements().size()<=1? sb.toString(): sampling.getMeasurements().size() + " meas.", FastFont.REGULAR);
		measurementLabel.setToolTipText(sb.toString());
		c.gridx = 37; samplingsPanel.add(UIUtils.createHorizontalBox(measurementLabel, Box.createHorizontalStrut(10)), c);

		//nCreated		
		if(sampling.getSamples().size()>0) {
			c.gridx = 40; samplingsPanel.add(UIUtils.createHorizontalBox(Box.createHorizontalStrut(5), new JLabel("> "+sampling.getSamples().size()+" created")), c);
		}
		
		//Spacer		
		c.weightx = 1; c.gridx = 41; samplingsPanel.add(new JLabel(" "), c);					
		
		//Draw separation
		c.gridy++;
		JPanel borderPanel = new JPanel(new BorderLayout());
		borderPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY));
		c.gridx = 0; c.gridwidth=99; c.fill=GridBagConstraints.HORIZONTAL; samplingsPanel.add(borderPanel, c); c.fill=GridBagConstraints.NONE; 

		
		//Draw children
		List<Sampling> children = new ArrayList<>(sampling.getChildren());
		Collections.sort(children);;
		for (int i = 0; i < children.size(); i++) {
			Sampling child = children.get(i);
			assert child!=null;
			c.gridy = drawRec(c.gridy, depth + 1, child);
		}
		
		return c.gridy;
	}
	
	
	private class Action_EditSample extends AbstractAction {
		private Sampling sampling;
		public Action_EditSample(Sampling sampling) {
			super("Edit sampling");
			this.sampling = sampling;
			putValue(AbstractAction.SMALL_ICON, IconType.EDIT.getIcon());
			putValue(AbstractAction.SHORT_DESCRIPTION, "Edit Sample");
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			new SamplingDlg(NamedSamplingDlg.this, study, sampling, true);
			refresh();
		}
	}
	private class Action_DuplicateSample extends AbstractAction {
		private Sampling sampling;
		public Action_DuplicateSample(Sampling sampling) {
			super("Duplicate sampling");
			this.sampling = sampling;
			putValue(AbstractAction.SMALL_ICON, IconType.DUPLICATE.getIcon());
			putValue(AbstractAction.SHORT_DESCRIPTION, "Duplicate Sample");
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			Sampling s = sampling.clone();
			SamplingDlg dlg = new SamplingDlg(NamedSamplingDlg.this, study, s, true);
			if(dlg.isSuccess() && s.getBiotype()!=null) {
				s.setParent(sampling.getParent());
				namedSamplingProxy.getAllSamplings().add(s);
				if(sampling.getParent()!=null) sampling.getParent().getChildren().add(s);
				refresh();
			}
			
			refresh();
		}
	}
	private class Action_AddSample extends AbstractAction {
		private Sampling sampling;
		public Action_AddSample(Sampling sampling) {
			super("Add a child");
			this.sampling = sampling;
			putValue(AbstractAction.SMALL_ICON, IconType.ADD_ROW.getIcon());
			putValue(AbstractAction.SHORT_DESCRIPTION, "Add a child, derived from this sample");
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			Sampling s = new Sampling();
			s.setBiotype(sampling.getBiotype());			
			s.setMetadataMap(sampling.getMetadataMap());
			s.setComments(sampling.getComments());
			
			SamplingDlg dlg = new SamplingDlg(NamedSamplingDlg.this, study, s, true);
			if(dlg.isSuccess() && s.getBiotype()!=null) {
				namedSamplingProxy.getAllSamplings().add(s);
				s.setNamedSampling(namedSamplingProxy);
				s.setParent(sampling);
				sampling.getChildren().add(s);
				refresh();
			}
			
			refresh();
		}
	}
	private class Action_DelSample extends AbstractAction {
		private Sampling sampling;
		public Action_DelSample(Sampling sampling) {
			super("Delete this sampling");
			this.sampling = sampling;
			putValue(AbstractAction.SMALL_ICON, IconType.DEL_ROW.getIcon());
			putValue(AbstractAction.SHORT_DESCRIPTION, "Delete this sample or this aliquot");
		}
		@Override
		public void actionPerformed(ActionEvent ev) {
			try {
				if(sampling.getChildren().size()>0) {
					JExceptionDialog.showError(NamedSamplingDlg.this, "You cannot delete this sampling because it has some children");
					return;
				}
				if(sampling.getSamples().size()>0) {
					
					List<Result> results = DAOResult.queryResults(ResultQuery.createQueryForBiosampleIds(JPAUtil.getIds(sampling.getSamples())), null);
					if(results.size()>0) throw new Exception("There are "+sampling.getSamples().size()+" samples already saved and those have " + results.size()+" associated results.\nYou must first delete the results to delete this sampling.");
					
					if(dlg!=null) {
						List<Biosample> list = new ArrayList<>(sampling.getSamples());					
						int res = createOptionDialog(NamedSamplingDlg.this, "There are "+sampling.getSamples().size()+" samples already saved. Would you like to DELETE those?", list, null, null, new String[] {"Delete", "Cancel"});
						if(res!=0) return;
						
						for (Biosample b : list) {
							b.setAttachedSampling(null);							
						}
						sampling.getSamples().clear();
						
						if(dlg!=null) dlg.getToDelete().addAll(list);
					}
				}
				
				sampling.remove();
				namedSamplingProxy.getAllSamplings().remove(sampling);
				refresh();
			} catch(Exception ex) {
				JExceptionDialog.showError(ex);
			}
		}
	}
	
	private JPopupMenu createActionMenu(Sampling sampling) {
		JPopupMenu menu = new JPopupMenu();
		menu.add(new Action_EditSample(sampling));
		menu.add(new Action_DuplicateSample(sampling));
		menu.add(new Action_AddSample(sampling));
		menu.add(new Action_DelSample(sampling));
		return menu;
	}
	
	
	
	public static int createOptionDialog(Component parent, String message, List<Biosample> biosamplesToDelete, List<Biosample> biosamplesToOverwrite, List<Biosample> biosamplesToKeep, String[] options) {
		
		if(biosamplesToDelete!=null) Collections.sort(biosamplesToDelete);
		if(biosamplesToOverwrite!=null) Collections.sort(biosamplesToOverwrite);
		if(biosamplesToKeep!=null) Collections.sort(biosamplesToKeep);

		
		
		JPanel mainPanel = new JPanel(new GridLayout(0, 1));
		if(biosamplesToDelete!=null && biosamplesToDelete.size()>0) {
			BiosampleTable table = new BiosampleTable();
			table.setRows(biosamplesToDelete);
			JScrollPane sp = new JScrollPane(table);
			sp.setPreferredSize(new Dimension(900, 300));
			table.setDefaultRenderer(Object.class, new ExtendTableCellRenderer<Biosample>(table) {
				@Override
				public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
					Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
					comp.setForeground(Color.RED);
					return comp;
				}
			});
			
			mainPanel.add(UIUtils.createTitleBox("To be deleted", sp));
		}
		
		if(biosamplesToOverwrite!=null && biosamplesToOverwrite.size()>0) {
			BiosampleTable table = new BiosampleTable();
			table.setRows(biosamplesToOverwrite);
			JScrollPane sp = new JScrollPane(table);
			sp.setPreferredSize(new Dimension(900, 250));
			
			mainPanel.add(UIUtils.createTitleBox("To be overwritten", sp));
		}
		if(biosamplesToKeep!=null && biosamplesToKeep.size()>0) {
			BiosampleTable table = new BiosampleTable();
			table.setRows(biosamplesToKeep);
			JScrollPane sp = new JScrollPane(table);
			sp.setPreferredSize(new Dimension(900, 250));
			table.setDefaultRenderer(Object.class, new ExtendTableCellRenderer<Biosample>(table) {
				@Override
				public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
					Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
					comp.setForeground(Color.BLUE);
					return comp;
				}
			});
			
			mainPanel.add(UIUtils.createTitleBox("Attached samples, to be kept unchanged", sp));
		}
		
		JPanel panel = new JPanel(new BorderLayout());
		panel.add(BorderLayout.NORTH, new JLabel("<html><b>" + message + "</b></html>"));
		panel.add(BorderLayout.CENTER, mainPanel); 
				
		
		int res = JOptionPane.showOptionDialog(parent, panel, "Existing Samples", JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
		return res;
		
	}
	
	public Study getStudy() {
		return study;
	}
	
	
	public NamedSampling getSavedNamedSampling() {
		return success? namedSamplingToEdit: null;
	}
	
	private void removeEvent() throws Exception {
		
		
		//Update the Study
		if(namedSamplingToEdit==null) throw new Exception("Cannot remove this");
		if(study!=null) {
			
			//make sure there are no links to it
			for(Sampling s: namedSamplingToEdit.getAllSamplings()) {
				if(!s.getSamples().isEmpty()) throw new Exception("You cannot delete a sampling template, if there are samples associated to it");
			}
			
			
			List<StudyAction> actionsToUpdate = new ArrayList<StudyAction>();
			for ( StudyAction action : study.getStudyActions()) {
				if(NamedSamplingDlg.this.namedSamplingToEdit.equals(action.getNamedSampling1()) || NamedSamplingDlg.this.namedSamplingToEdit.equals(action.getNamedSampling2())) {
					actionsToUpdate.add(action);
				}
			}
			study.getNamedSamplings().remove(namedSamplingToEdit);
			if(actionsToUpdate.size()>0) {
				int res = JOptionPane.showConfirmDialog(NamedSamplingDlg.this, "The study contains actions referencing " + NamedSamplingDlg.this.namedSamplingToEdit + ".\nWould you like to delete them?", "Question", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
				if(res!=JOptionPane.YES_OPTION) return;
				
				for (StudyAction action : actionsToUpdate) {
					if(NamedSamplingDlg.this.namedSamplingToEdit.equals(action.getNamedSampling1())) action.setNamedSampling1(null);								
					if(NamedSamplingDlg.this.namedSamplingToEdit.equals(action.getNamedSampling2())) action.setNamedSampling2(null);								
				}
			}
			study.setUpdUser(Spirit.askForAuthentication().getUsername());
			
			if(transactionMode) DAOStudy.persistStudies(Collections.singleton(study), Spirit.askForAuthentication());								
		}
		
		//Delete the NamedSampling
		if(transactionMode) {
			DAONamedSampling.deleteNamedSampling(namedSamplingToEdit, Spirit.askForAuthentication());
		} else {
			namedSamplingToEdit.setStudy(null);
			namedSamplingToEdit.remove();
		}
		dispose();
	}
	
	private void saveEvent() throws Exception {
		if(nameTextField.getText().trim().length()==0) throw new Exception("The name cannot be empty");
		
		String name = nameTextField.getText();
		if(study!=null) {
			//In a study, the name must be unique for that study
			for(NamedSampling ns: study.getNamedSamplings()) {
				if(ns.equals(namedSamplingToEdit)) continue;
				if(ns.getName().equals(name)) throw new Exception("The name must be unique");
			}
			
			//Check that the user didn't forget to click necropsy
			if(necropsyCheckbox.isEnabled() && !necropsyCheckbox.isSelected()) {
				boolean hasSolid = false;
				for(Sampling s: namedSamplingProxy.getAllSamplings()) {
					if(s.getBiotype().getCategory()==BiotypeCategory.SOLID) {
						hasSolid = true;
						break;
					}
				}
				//If there is a solid sample, and the user didn't click necropsy, gives a warning
				if(hasSolid) {
					int res = JOptionPane.showConfirmDialog(this, "This template contains solid samples. Do you want to set it as Necropsy?", "Necropsy", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
					if(res==JOptionPane.YES_OPTION) {
						//set to necropsy
						necropsyCheckbox.setSelected(true);
					} else if(res==JOptionPane.NO_OPTION) {
						//no necropsy
					} else {
						//cancel
						return;
					}
				}
			}
		} else {
			//No study, the name must be globaly unique
			for(NamedSampling ns: DAONamedSampling.getNamedSamplings(Spirit.getUser(), null)) {
				if(ns.equals(namedSamplingProxy)) continue;
				if(ns.getName().equals(name)) throw new Exception("The name must be unique");				
			}
		}
		
		namedSamplingProxy.setName(name);
		namedSamplingProxy.setNecropsy(necropsyCheckbox.isSelected());
		this.namedSamplingToEdit.copyFrom(namedSamplingProxy);
		
		
		if(transactionMode) DAONamedSampling.persistNamedSampling(namedSamplingToEdit, Spirit.askForAuthentication());
		this.success = true;
		
		if(study!=null) {
			//Update the study
			namedSamplingToEdit.setStudy(study);
			study.getNamedSamplings().add(namedSamplingToEdit);
		}		
		dispose();

	}

	public boolean synchronizeSamples(Study study, Sampling fromSamplingClone, Sampling toSampling) throws Exception {
	
		//Check that there are no samples coming from this sampling
		Set<Biosample> samples = toSampling.getSamples();
		List<Biosample> samplesToUpdate = new ArrayList<>();
		List<Biosample> samplesToKeep = new ArrayList<>();
		if(samples.size()>0) {
			
			//Check that samples have not been modified
			for (Biosample b : samples) {
				double score = fromSamplingClone.getMatchingScore(b);
				if(score<1) samplesToKeep.add(b);
				else samplesToUpdate.add(b);
			}
			
			//Open dialog
			String[] options;
			if(samplesToKeep.size()>0 && samplesToUpdate.size()>0) {
				options = new String[] {"Update (except modified samples)", "Update all samples", "Do not update/synchronize"};
			} else if(samplesToKeep.size()>0) {
				options = new String[] {"Do not update/synchronize", "Update all samples"};
			} else {
				options = new String[] {"Update all samples", "Do not update/synchronize"};
			}
			int res = createOptionDialog(null, "There are "+samples.size()+" samples already saved. Would you like to update those existing samples to the new settings?", null, samplesToUpdate, samplesToKeep, options);
			if(samplesToKeep.size()>0 && samplesToUpdate.size()>0) {				
				if(res==1) {
					samplesToUpdate.addAll(samplesToKeep);
				} else if(res!=0) {
					return false;
				}
			} else if(samplesToKeep.size()>0 ) {				
				if(res==1) {
					samplesToUpdate.addAll(samplesToKeep);
				} else {
					return false;
				}
			} else {
				if(res!=0) {
					return false;				
				}
			}
	
			//Update the samples
			for (Biosample b : samplesToUpdate) {
				if(!b.getBiotype().equals( toSampling.getBiotype())) throw new Exception("The biotype cannot be changed");
				toSampling.populate(b);
				b.setAttachedSampling(toSampling);
			}	
			
			//Update the containers
			BiosampleQuery q = new BiosampleQuery();
			q.setStudyIds(study.getStudyId());
			q.setFilterNotInContainer(true);
	
			List<Biosample> pool = DAOBiosample.queryBiosamples(q, null);
			pool.removeAll(samplesToUpdate);
			BiosampleCreationHelper.assignContainers(pool, samplesToUpdate);
			
			//Inform the parent dlg, that those samples have to be updated before saved (to set upddate)
			if(dlg!=null) {
				dlg.getToUpdate().addAll(samplesToUpdate);
			}
		}
		return true;
	}

}
