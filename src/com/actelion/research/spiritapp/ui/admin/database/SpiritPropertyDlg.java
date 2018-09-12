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

package com.actelion.research.spiritapp.ui.admin.database;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.text.JTextComponent;

import com.actelion.research.spiritapp.Spirit;
import com.actelion.research.spiritapp.ui.util.SpiritChangeListener;
import com.actelion.research.spiritapp.ui.util.SpiritChangeType;
import com.actelion.research.spiritapp.ui.util.component.JSpiritEscapeDialog;
import com.actelion.research.spiritcore.adapter.DBAdapter;
import com.actelion.research.spiritcore.business.property.PropertyKey;
import com.actelion.research.spiritcore.business.property.PropertyKey.Tab;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.spiritcore.services.SpiritRights.ActionType;
import com.actelion.research.spiritcore.services.SpiritRights.UserType;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.spiritcore.services.dao.SpiritProperties;
import com.actelion.research.spiritcore.util.MiscUtils;
import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.JComboCheckBox;
import com.actelion.research.util.ui.JCustomLabel;
import com.actelion.research.util.ui.JCustomTabbedPane;
import com.actelion.research.util.ui.JCustomTextField;
import com.actelion.research.util.ui.JCustomTextField.CustomFieldType;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.JInfoLabel;
import com.actelion.research.util.ui.SwingWorkerExtended;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.iconbutton.IconType;
import com.actelion.research.util.ui.iconbutton.JIconButton;

/**
 * Class responsible for setting/testing the connection (ie. dbadapter.testConnection should be successful)
 *
 * @author freyssj
 *
 */
public class SpiritPropertyDlg extends JSpiritEscapeDialog {


	private DBAdapter adapter;
	private Map<String, String> propertyMap;

	private JTabbedPane tabbedPane = new JCustomTabbedPane();
	private JPanel systemPanel = new JPanel(new GridLayout());
	private JPanel userPanel = new JPanel(new GridLayout());
	private JPanel studyPanel = new JPanel(new GridLayout());
	private JPanel biosamplePanel = new JPanel(new GridLayout());
	private JPanel userRightsPanel = new JPanel(new GridLayout());


	public SpiritPropertyDlg() {
		super(UIUtils.getMainFrame(), "Database Settings", SpiritPropertyDlg.class.getName());

		//Initialize with current adapter
		adapter = DBAdapter.getInstance();
		try {
			propertyMap = new HashMap<>(SpiritProperties.getInstance().getValues());
		} catch (Exception e) {
			propertyMap = new HashMap<>();
		}

		//Buttons
		JButton okButton = new JIconButton(IconType.SAVE, "Save");
		okButton.addActionListener(e-> {
			new SwingWorkerExtended("Save", tabbedPane) {
				@Override
				protected void doInBackground() throws Exception {
					try {
						save();
					} catch(Exception ex) {
						JExceptionDialog.showError(SpiritPropertyDlg.this, ex);
					}
				}
			};
		});

		refreshConfigPanels();

		//TabbedPane
		tabbedPane.setFont(FastFont.BOLD);
		tabbedPane.add("General", new JScrollPane(systemPanel));
		//		tabbedPane.add("User", new JScrollPane(userPanel));
		tabbedPane.add("Study", new JScrollPane(studyPanel));
		tabbedPane.add("Biosample", new JScrollPane(biosamplePanel));
		tabbedPane.add("User Rights", UIUtils.createBox(new JScrollPane(userRightsPanel), userPanel));

		JButton button = new JButton("View Properties");
		button.addActionListener(e -> {
			StringBuilder sb = new StringBuilder();
			for (String key : new TreeSet<String>(propertyMap.keySet())) {
				sb.append(key + "=" + propertyMap.get(key) + "\n");
			}
			JTextArea editorPane = new JTextArea();
			editorPane.setText(sb.toString());
			editorPane.setLineWrap(false);
			editorPane.setEditable(false);
			JScrollPane sp = new JScrollPane(editorPane);
			sp.setPreferredSize(new Dimension(400, 300));

			JOptionPane.showOptionDialog(SpiritPropertyDlg.this, sp, "Spirit Properties", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, null, null);
		});


		//contentpane
		setContentPane(UIUtils.createBox(tabbedPane,
				UIUtils.createTitleBox(new JInfoLabel("<html>The configuration settings have no impact on the user's data (samples. results).<br>IE. If you remove one study's metadata, it will actually only be removed from the display and not from the DB. You can always go back.")),
				UIUtils.createHorizontalBox(button, Box.createHorizontalGlue(), okButton)));
		UIUtils.adaptSize(this, 840, 750);
		setVisible(true);
	}

	private void refreshConfigPanels() {
		createPropertyPanel(systemPanel, "", "", PropertyKey.getPropertyKeys(Tab.SYSTEM), new String[0]);
		createPropertyPanel(userPanel, "", "", PropertyKey.getPropertyKeys(Tab.USER), new String[0]);
		createPropertyPanel(studyPanel, "", "", PropertyKey.getPropertyKeys(Tab.STUDY), new String[0]);
		createPropertyPanel(biosamplePanel, "", "", PropertyKey.getPropertyKeys(Tab.BIOSAMPLE), new String[0]);
		createUserRightsPanel(userRightsPanel);
	}

	private Map<PropertyKey, JComponent> prop2editor = new HashMap<>();
	private void createPropertyPanel(final JPanel panel, final String propertyPrefix, final String labelPrefix, final List<PropertyKey> properties, final String[] nestedValues) {
		List<JComponent> tableComps = new ArrayList<>();
		List<Component> panels = new ArrayList<>();
		JTabbedPane nestedPanes = new JCustomTabbedPane(JTabbedPane.LEFT);

		//Loop through properties
		for (final PropertyKey p : properties) {

			//Create editor for the property
			String val = propertyMap.get(propertyPrefix + p.getKey())==null? p.getDefaultValue(nestedValues): propertyMap.get(propertyPrefix + p.getKey());
			final JComponent editorComp;
			if("true,false".equals(p.getOptions()) || "true, false".equals(p.getOptions())) {
				final JCheckBox c = new JCheckBox();
				editorComp = c;
				c.setSelected("true".equals(val));
				c.addActionListener(e -> propertyMap.put(propertyPrefix + p.getKey(), c.isSelected()?"true":"false"));
			} else if(p.getOptions()!=null) {
				String[] choices = p.getChoices();
				final JComboBox<String> c = new JComboBox<>(choices);
				editorComp = c;

				System.out.println("SpiritPropertyDlg.createPropertyPanel() "+p+" > "+val);
				if(val!=null && val.length()>0) {
					if(Arrays.asList(p.getChoices()).contains(val) ) {
						c.setSelectedItem(val);
					} else {
						for (int i = 0; i < choices.length; i++) {
							if(choices[i]!=null && choices[i].toLowerCase().startsWith(val.toLowerCase())) {
								c.setSelectedIndex(i);
								break;
							}
						}
					}
				}
				c.addActionListener(e-> propertyMap.put(propertyPrefix + p.getKey(), (String) c.getSelectedItem()));
			} else if(p.getLinkedOptions()!=null) {
				String parentVal = propertyMap.get(p.getLinkedOptions().getKey());
				if(parentVal==null) parentVal = p.getLinkedOptions().getDefaultValue(nestedValues);

				final JComboCheckBox c = new JComboCheckBox(p.getChoices(parentVal));
				c.setEditable(false);
				c.setSeparator(", ");
				editorComp = c;
				c.setText(val);
				c.addTextChangeListener(comp -> propertyMap.put(propertyPrefix + p.getKey(), c.getText()));
			} else {
				final JCustomTextField c = new JCustomTextField(CustomFieldType.ALPHANUMERIC, p.getNestedProperties().size()>0? 38: 16);
				editorComp = c;
				c.setText(val);
				c.addTextChangeListener(comp -> propertyMap.put(propertyPrefix + p.getKey(), c.getText()));
			}
			prop2editor.put(p, editorComp);

			if(p.getLinkedOptions()!=null && prop2editor.get(p.getLinkedOptions())!=null) {
				prop2editor.get(p.getLinkedOptions()).addFocusListener(new FocusAdapter() {
					private String t;
					@Override
					public void focusLost(FocusEvent e) {
						if(!t.equals(getValue(prop2editor.get(p.getLinkedOptions())))) {
							refreshConfigPanels();
						}
					}
					@Override
					public void focusGained(FocusEvent e) {
						t = getValue(prop2editor.get(p.getLinkedOptions()));
					}
				});
			}

			//Disable the component if the value is already fixed by the adapter
			if(DBAdapter.getInstance().getProperties().containsKey(propertyPrefix + p.getKey())) {
				editorComp.setEnabled(false);
			} else {
				editorComp.setEnabled(true);
				System.out.println(propertyPrefix + p.getKey()+"="+val);

			}

			//Add a label and a tooltip
			JLabel labelComp = new JLabel(" " + p.getLabel() + ": ");
			JLabel tooltipComp = p.getTooltip()==null? new JLabel(): new JLabel(IconType.HELP.getIcon());
			labelComp.setToolTipText(p.getTooltip()==null? null: "<html>" + p.getTooltip());
			editorComp.setToolTipText(p.getTooltip()==null? null: "<html>" + p.getTooltip());
			tooltipComp.setToolTipText(p.getTooltip()==null? null: "<html>" + p.getTooltip());

			tableComps.add(tooltipComp);
			tableComps.add(labelComp);
			tableComps.add(editorComp);

			if(p.getNestedProperties().size()>0) {
				//If there ares nested properties, add them to a nested panel
				editorComp.addFocusListener(new FocusAdapter() {
					private String t;
					@Override
					public void focusLost(FocusEvent e) {
						if(!t.equals(getValue(editorComp))) {
							createPropertyPanel(panel, propertyPrefix, labelPrefix, properties, nestedValues);
						}
					}
					@Override
					public void focusGained(FocusEvent e) {
						t = getValue(editorComp);
					}
				});
				List<Component> nestedPanels = new ArrayList<>();
				for(String token: MiscUtils.split(getValue(editorComp), ",")) {
					//Add the nested value to our stack
					final String[] nestedValues2 = new String[nestedValues.length+1];
					System.arraycopy(nestedValues, 0, nestedValues2, 0, nestedValues.length);
					nestedValues2[nestedValues.length] = token;

					//create a nested panel
					JPanel nestedPanel = new JPanel();
					nestedPanel.setOpaque(false);
					createPropertyPanel(nestedPanel, propertyPrefix + p.getKey() + "." + token +".", token, p.getNestedProperties(), nestedValues2);
					nestedPanels.add(nestedPanel);
				}
				nestedPanels.add(Box.createVerticalGlue());
				nestedPanes.add(p.getLabel(), UIUtils.createTitleBox(p.getLabel(), UIUtils.createVerticalBox(nestedPanels)));
			}
		}

		if(tableComps.size()>0) {
			if(labelPrefix.length()>0) {
				//Nested panel
				JPanel nestedPanel;
				if(tableComps.size()>=3*6) {
					int n = (tableComps.size()/3+1)/2;

					nestedPanel =  UIUtils.createGrid(
							UIUtils.createTable(3, 5, 0, tableComps.subList(0, n*3)),
							UIUtils.createTable(3, 5, 0, tableComps.subList(n*3, tableComps.size())));
				} else {
					nestedPanel =
							UIUtils.createTable(3, 5, 0, tableComps);
				}
				panels.add(UIUtils.createBox(UIUtils.createBox(nestedPanel, null, null, Box.createHorizontalStrut(10), Box.createHorizontalGlue()),
						new JCustomLabel(labelPrefix, FastFont.BOLD)));
			} else {
				//Main Panel
				panels.add(UIUtils.createTitleBox(UIUtils.createBox(null, null, UIUtils.createTable(3, 5, 3, tableComps), null)));
			}
		}
		if(nestedPanes.getTabCount()>0) {
			panels.add(nestedPanes);
		}
		panels.add(Box.createVerticalGlue());

		panel.removeAll();
		panel.add(UIUtils.createVerticalBox(panels));
		panel.validate();
	}

	private void createUserRightsPanel(JPanel panel) {

		//Create panel for roles
		String[] roles = SpiritProperties.getInstance().getUserRoles();

		List<Component> comps = new ArrayList<>();

		//User roles Headers
		comps.add(null);
		for (String role : roles) {
			comps.add(new JCustomLabel(role.toString(), Font.ITALIC));
		}
		comps.add(Box.createHorizontalStrut(10));
		if(SpiritProperties.getInstance().isChecked(PropertyKey.USER_USEGROUPS)) {
			for (UserType userType : SpiritRights.UserType.values()) {
				comps.add(new JLabel(userType.toString()));
			}
		}
		int nCols = comps.size();

		//User roles data
		ActionType last = null;
		for (ActionType actionType : SpiritRights.ActionType.values()) {

			if(!SpiritProperties.getInstance().isChecked(PropertyKey.SYSTEM_RESULT) && actionType.name().contains("RESULT")) continue;

			//Draws a separator if we have a new type
			if(last!=null && actionType.name().startsWith("READ")) {
				for (int i = 0; i < nCols; i++) {
					comps.add(Box.createVerticalStrut(6));
				}
			}
			last = actionType;


			//Write the label for the actionType
			JLabel actionLabel = new JLabel(actionType.getDisplay());
			actionLabel.setToolTipText(actionType.getTooltip());
			comps.add(actionLabel);

			//Shows the checkboxes
			for (String role : roles) {
				final JCheckBox cb = new JCheckBox();
				cb.setSelected(SpiritProperties.getInstance().isChecked(actionType, role));
				cb.setEnabled(DBAdapter.getInstance().getProperties().get(SpiritProperties.getKey(actionType, null, role))==null);
				cb.addActionListener(e-> {
					propertyMap.put(SpiritProperties.getKey(actionType, null, role), cb.isSelected()?"true":"false");
				});
				comps.add(cb);
			}
			comps.add(Box.createHorizontalStrut(10));
			if(SpiritProperties.getInstance().isChecked(PropertyKey.USER_USEGROUPS)) {
				for (UserType userType : SpiritRights.UserType.values()) {
					final JCheckBox cb = new JCheckBox();
					cb.setSelected(SpiritProperties.getInstance().isChecked(actionType, userType));
					cb.setEnabled(DBAdapter.getInstance().getProperties().get(SpiritProperties.getKey(actionType, userType, null))==null);
					cb.addActionListener(e-> {
						propertyMap.put(SpiritProperties.getKey(actionType, userType, null), cb.isSelected()?"true":"false");
					});
					comps.add(cb);
				}
			}
		}
		panel.removeAll();
		panel.add(UIUtils.createVerticalBox(UIUtils.createTable(nCols, 5, 2, comps), Box.createVerticalGlue()));
		panel.validate();
	}

	private String getValue(JComponent c) {
		if(c instanceof JComboBox) {
			return (String) ((JComboBox<?>)c).getSelectedItem();
		} else if(c instanceof JTextComponent) {
			return ((JTextComponent)c).getText();
		} else {
			throw new RuntimeException("Invalid component: "+c);
		}
	}

	private void save() throws Exception {
		if(adapter==null) throw new Exception("You must select an adapter");

		//Simply test the connection, or forbids saving
		adapter.testConnection();

		//Success->Save
		//DBProperties in config file (if configurable)
		if(DBAdapter.isConfigurable()) {
			DBAdapter.writeDBProperties();
		}

		//ConfigProperties in DB
		SpiritProperties.getInstance().setValues(propertyMap);

		if(!Spirit.askReasonForChangeIfUpdated(SpiritProperties.getInstance().getProperties())) return;

		SpiritProperties.getInstance().saveValues();
		dispose();

		//Reset Spirit
		DBAdapter.setAdapter(null);
		JPAUtil.closeFactory();
		SpiritChangeListener.fireModelChanged(SpiritChangeType.LOGIN);
	}


}
