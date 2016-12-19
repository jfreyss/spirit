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

package com.actelion.research.util.ui.exceltable;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingUtilities;

import com.actelion.research.util.ui.JCustomTextField;
import com.actelion.research.util.ui.JEscapeDialog;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.JGenericComboBox;
import com.actelion.research.util.ui.UIUtils;

public class FillCellAction extends AbstractAction{

	private ExcelTable<?> table;
	private Column<?, ?> column;
	private List<String> choices;
	
	public FillCellAction(ExcelTable<?> table, Column<?, ?> column) {
		this(table, column, null);
	}
	public FillCellAction(ExcelTable<?> table, Column<?, ?> column, List<String> choices) {
		super("Fill Empty cells");
		this.table = table;
		this.choices = choices;
		this.column = column;
		
		
		setEnabled(false);
		
		try {
			if(column.isEditable(null)) {
				setEnabled(true); 
			}
		} catch (Exception e) {
		}
	}
	
	
	private class FillDlg extends JEscapeDialog {
		private JCustomTextField textField = null;
		private JGenericComboBox<String> comboBox = null;

		public FillDlg() {			
			super(UIUtils.getMainFrame(), "Fill Empty Cells");
			
			//Analyze the selection
			final int col = table.convertColumnIndexToView(table.getModel().getColumns().indexOf(column));
			
			String defaultValue = "";
			if(col>=0 && table.getSelectedRow()>=0) {
				Object obj = table.getValueAt(table.getSelectedRow(), col);
				if(obj!=null) defaultValue = obj.toString();
			}
			int count = 0;
			for(int row=0; row<table.getRowCount(); row++) {
				if(table.getValueAt(row, col)==null || table.getValueAt(row, col).toString().length()==0) {
					count++;
				}			
			}
			if(count==0) {
				JExceptionDialog.showError(table, "All cells are already filled");
				return;
			}
			Column<?, ?> column = table.getModel().getColumn(table.convertColumnIndexToModel(col));

			ButtonGroup group = new ButtonGroup(); 
			//FixedValuePanel
			final Box fixedValuePanel = Box.createVerticalBox();
			final JRadioButton fixedValueButton = new JRadioButton("Fill empty cells with a fixed value", true);
			group.add(fixedValueButton);
			{
				Box line1 = Box.createHorizontalBox(); 
				line1.add(fixedValueButton);
				line1.add(Box.createHorizontalGlue());

				Box line2 = Box.createHorizontalBox(); 
				line2.add(Box.createHorizontalStrut(10));
				line2.add(new JLabel(column.getName()+":"));
				if(choices!=null && choices.size()>0) {
					comboBox = new JGenericComboBox<String>(choices, true);
					line2.add(comboBox);
					SwingUtilities.invokeLater(new Runnable() {@Override public void run() {comboBox.requestFocusInWindow();}});
				} else {
					textField = new JCustomTextField(JCustomTextField.ALPHANUMERIC, defaultValue);
					line2.add(textField);
					SwingUtilities.invokeLater(new Runnable() {@Override public void run() {textField.requestFocusInWindow();}});
				}
				line2.add(Box.createHorizontalGlue());
				fixedValuePanel.add(line1);
				fixedValuePanel.add(line2);
			}
			
			//SequencePanel
			Box sequencePanel = Box.createVerticalBox();
			
			//CenterPanel
			JPanel centerPanel = new JPanel(new GridBagLayout());
			GridBagConstraints c = new GridBagConstraints();
			c.weightx = 1; c.weighty = 1; c.fill = GridBagConstraints.BOTH;
			c.gridx = 0; c.gridy = 0; centerPanel.add(fixedValuePanel, c);
			if(choices==null) {
				c.gridx = 0; c.gridy = 1; centerPanel.add(sequencePanel, c);
			}
//			String value = (String) JOptionPane.showInputDialog(table, "Fill '" + column.getName() + "' with: (" + count +" cells empty)", "Fill Column", JOptionPane.QUESTION_MESSAGE, null, choices==null? null: choices.toArray(new String[0]), defaultValue);
			
			
			//Buttons
			JButton okButton = new JButton("Fill Cells");
			okButton.addActionListener(new ActionListener() {				
				@Override
				public void actionPerformed(ActionEvent ev) {
					try {
						if(fixedValueButton.isSelected()) {
							String value = textField==null? (String) comboBox.getSelectedItem(): textField.getText();
							fillColumn(col, value);
							dispose();
						} else {
							
						}
					} catch (Exception e) {
						JExceptionDialog.showError(e);
					}
				}
			});
			
			
			//ContentPane
			JPanel contentPanel = new JPanel(new BorderLayout());
			contentPanel.add(BorderLayout.CENTER, centerPanel);
			contentPanel.add(BorderLayout.SOUTH, UIUtils.createHorizontalBox(Box.createHorizontalGlue(), new JLabel(count+" empty cells"), okButton));
			setContentPane(contentPanel);
			getRootPane().setDefaultButton(okButton);
			pack();
			setLocationRelativeTo(UIUtils.getMainFrame());
			setVisible(true);
		}
		
	}
	
	@Override
	public void actionPerformed(ActionEvent ev) {
		new FillDlg();
	}
	
	
	public void fillColumn(int col, String value) throws Exception {
		if(value!=null) {
			table.getUndoManager().setTransaction(true);
			try {
				for(int row=0; row<table.getRowCount(); row++) {
					if(table.getValueAt(row, col)==null || table.getValueAt(row, col).toString().length()==0) {
						table.getModel().paste(value, row, table.convertColumnIndexToModel(col));
					}
				}
			} finally {
				table.getUndoManager().setTransaction(false);				
			}
		}
		table.repaint();
	}
}
