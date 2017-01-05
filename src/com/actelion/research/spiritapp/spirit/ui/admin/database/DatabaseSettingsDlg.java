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

package com.actelion.research.spiritapp.spirit.ui.admin.database;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.text.JTextComponent;

import com.actelion.research.spiritapp.spirit.SpiritDB;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeListener;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeType;
import com.actelion.research.spiritapp.spirit.ui.util.component.JSpiritEscapeDialog;
import com.actelion.research.spiritcore.adapter.DBAdapter;
import com.actelion.research.spiritcore.adapter.HSQLFileAdapter;
import com.actelion.research.spiritcore.business.property.PropertyDescriptor;
import com.actelion.research.spiritcore.business.property.PropertyKey;
import com.actelion.research.spiritcore.business.property.PropertyKey.Tab;
import com.actelion.research.spiritcore.services.StringEncrypter;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.spiritcore.services.dao.SpiritProperties;
import com.actelion.research.spiritcore.services.migration.MigrationScript;
import com.actelion.research.spiritcore.util.MiscUtils;
import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.JComboCheckBox;
import com.actelion.research.util.ui.JCustomLabel;
import com.actelion.research.util.ui.JCustomTabbedPane;
import com.actelion.research.util.ui.JCustomTextField;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.SwingWorkerExtended;
import com.actelion.research.util.ui.TextChangeListener;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.iconbutton.JIconButton;
import com.actelion.research.util.ui.iconbutton.JIconButton.IconType;

/**
 * Class responsible for setting/testing the connection (ie. dbadapter.testConnection should be successful) 
 * 
 * @author freyssj
 *
 */
public class DatabaseSettingsDlg extends JSpiritEscapeDialog {
	
	private JComboBox<String> adapterComboBox = null;	
	private final JPanel specificConfigPane = new JPanel(new GridLayout());
	
	private DBAdapter adapter;
	private final DBAdapter initialAdapter;
	private Map<String, String> propertyMap;

	private final Map<PropertyDescriptor, JComponent> dbproperty2comp = new HashMap<>();
	private final JLabel label = new JLabel();
	private JPanel userPanel = new JPanel(new GridLayout());
	private JPanel studyPanel = new JPanel(new GridLayout());

	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public DatabaseSettingsDlg(boolean testImmediately) {
		super(UIUtils.getMainFrame(), "Database Settings", DatabaseSettingsDlg.class.getName());
		
		//Initialize with current adapter
		adapter = DBAdapter.getAdapter();
		initialAdapter = adapter;
		propertyMap = new HashMap<>(SpiritProperties.getInstance().getValues());

		//Buttons
		JButton okButton = new JIconButton(IconType.SAVE, "Save");
		okButton.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				new SwingWorkerExtended("Test Connection", specificConfigPane, SwingWorkerExtended.FLAG_CANCELABLE | SwingWorkerExtended.FLAG_ASYNCHRONOUS20MS) {
					@Override
					protected void doInBackground() throws Exception {
						try {
							save();
						} catch(Exception ex) {
							JExceptionDialog.showError(DatabaseSettingsDlg.this, ex); 
						}
					}
				};
			}
		});
		
		JButton testConnectionButton = new JButton("Test Connection");
		testConnectionButton.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				new SwingWorkerExtended("Test Connection", getContentPane(), SwingWorkerExtended.FLAG_CANCELABLE | SwingWorkerExtended.FLAG_ASYNCHRONOUS20MS) {
					@Override
					protected void doInBackground() throws Exception {
						try {
							label.setText("<html><div style='color:blue'>Testing Connection...</div></html>");
							updateDBProperties();
							testConnection();
							label.setText("<html><div style='color:green'>Successful</div></html>");
						} catch(Exception ex) {
							label.setText("<html><div style='color:red'>Error</div></html>");
							throw ex;
						}
					}
				};
			}
		});
		
		JButton testSchemaButton = new JButton("Test Schema");
		testSchemaButton.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				new SwingWorkerExtended("Test/Create Schema", getContentPane(), SwingWorkerExtended.FLAG_CANCELABLE | SwingWorkerExtended.FLAG_ASYNCHRONOUS20MS) {
					@Override
					protected void doInBackground() throws Exception {
						try {
							label.setText("<html><div style='color:blue'>Testing Schema...</div></html>");
							updateDBProperties();
							DatabaseMigrationDlg.testSchema(adapter);
							label.setText("<html><div style='color:green'>Successful</div></html>");
						} catch(Exception ex) {
							label.setText("<html><div style='color:red'>Error</div></html>");
							throw ex;
						}
					}
				};
			}
		});
		JButton examplesButton = new JButton("Recreate Examples");
		examplesButton.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				int res = JOptionPane.showConfirmDialog(DatabaseSettingsDlg.this, "Are you sure you want to delete the existing examples and reimport the original ones?", "Recreate examples", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
				if(res==JOptionPane.YES_OPTION) {
					new SwingWorkerExtended("Recreate Examples", getContentPane(), SwingWorkerExtended.FLAG_CANCELABLE | SwingWorkerExtended.FLAG_ASYNCHRONOUS20MS) {
						@Override
						protected void doInBackground() throws Exception {
							try {
								label.setText("<html><div style='color:blue'>Recreate Examples...</div></html>");
								SpiritDB.checkImportExamples(true);
								label.setText("<html><div style='color:green'>Successful</div></html>");
								SpiritChangeListener.fireModelChanged(SpiritChangeType.LOGIN);
							} catch(Exception ex) {
								label.setText("<html><div style='color:red'>Error</div></html>");
								throw ex;
							}
						}
					};
				}
			}
		});
		
		
		//generalConfigPane
		List<JComponent> comps = new ArrayList<>();
		if(DBAdapter.isConfigurable()) {
			adapterComboBox = (JComboBox) addComp(DBAdapter.ADAPTER_PROPERTY, comps);			
			adapterComboBox.setSelectedItem(DBAdapter.ADAPTER_PROPERTY.getDisplayFromKey(adapter.getDBProperty(DBAdapter.ADAPTER_PROPERTY)));
			adapterComboBox.addActionListener(new ActionListener() {			
				@Override
				public void actionPerformed(ActionEvent e) {
					try {
						String adapterClassName = DBAdapter.ADAPTER_PROPERTY.getKeyFromDisplay((String) adapterComboBox.getSelectedItem());
						if(adapterClassName==null || adapterClassName.length()==0) {
							adapter = null;							
						} else {
							adapter = DBAdapter.getAdapter(adapterClassName);
						}
					} catch(Exception ex) {
						JExceptionDialog.showError(DatabaseSettingsDlg.this, ex); 
						adapter = new HSQLFileAdapter();							
					}
					initUISpecificProperties();					
				}
			});
		} else {
			comps.add(new JLabel(DBAdapter.ADAPTER_PROPERTY.getDisplayName()+": "));
			comps.add(new JCustomLabel(initialAdapter==null?"":initialAdapter.getClass().getSimpleName(), Font.BOLD));			
		}
		
		//specificConfigPane
		specificConfigPane.setPreferredSize(new Dimension(450, 340));
		
		//initialization
		initUISpecificProperties();
		
		
		refreshConfigPanels();
		
		//TabbedPane
		JTabbedPane tabbedPane = new JCustomTabbedPane();
		tabbedPane.setFont(FastFont.BOLD);
		tabbedPane.add("Database", UIUtils.createBox(
				UIUtils.createTitleBox("DB Connection",specificConfigPane), 
				UIUtils.createTitleBox("DB Type", UIUtils.createTable(comps)),
				UIUtils.createHorizontalBox(Box.createHorizontalGlue(), testConnectionButton, testSchemaButton, examplesButton)));
		tabbedPane.add("User Settings", new JScrollPane(userPanel));
		tabbedPane.add("Study Settings", new JScrollPane(studyPanel));
		
		//contentpane
		setContentPane(UIUtils.createBox(tabbedPane, null, UIUtils.createHorizontalBox(label, Box.createHorizontalGlue(), okButton)));
		UIUtils.adaptSize(this, 840, 750);
		setVisible(true);
		
		if(testImmediately) {
			try {
				testSchema();
			} catch(Exception ex) {
				JExceptionDialog.showError(DatabaseSettingsDlg.this, ex); 
			}			
		}
	}
	
	private void refreshConfigPanels() {
		createPropertyPanel(userPanel, "", "", PropertyKey.getPropertyKeys(Tab.SYSTEM), new String[0]);
		createPropertyPanel(studyPanel, "", "", PropertyKey.getPropertyKeys(Tab.STUDY), new String[0]);
	}
	
	private Map<PropertyKey, JComponent> prop2comp = new HashMap<>();
	private void createPropertyPanel(final JPanel panel, final String propertyPrefix, final String labelPrefix, final List<PropertyKey> properties, final String[] nestedValues) {
		List<JComponent> tableComps = new ArrayList<>();
		List<Component> panels = new ArrayList<>();
		for (final PropertyKey p : properties) {
			
			//PropertyEditor
			List<PropertyKey> toPropertyKeys = p.getNestedProperties();
			String val = propertyMap.get(propertyPrefix + p.getKey())==null? p.getDefaultValue(nestedValues): propertyMap.get(propertyPrefix + p.getKey());
			final JComponent parentComp;
			if("true,false".equals(p.getOptions())) {
				final JCheckBox c = new JCheckBox();
				parentComp = c;
				c.setSelected("true".equals(val));
				c.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {					
						propertyMap.put(propertyPrefix + p.getKey(), c.isSelected()?"true":"false");
					}
				});
			} else if(p.getOptions()!=null) {
				final JComboBox<String> c = new JComboBox<>(p.getChoices());
				parentComp = c;
				c.setSelectedItem(val);
				c.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						propertyMap.put(propertyPrefix + p.getKey(), (String) c.getSelectedItem());
					}
				});
			} else if(p.getLinkedOptions()!=null) {
				String parentVal = propertyMap.get(p.getLinkedOptions().getKey());
				if(parentVal==null) parentVal = p.getLinkedOptions().getDefaultValue(nestedValues);

				final JComboCheckBox c = new JComboCheckBox(p.getChoices(parentVal));
				c.setEditable(false);
				c.setSeparator(", ");
				parentComp = c;
				c.setText(val);
				c.addTextChangeListener(new TextChangeListener() {
					@Override
					public void textChanged(JComponent src) {
						propertyMap.put(propertyPrefix + p.getKey(), c.getText());
					}
				});
			} else {
				final JCustomTextField c = new JCustomTextField(JCustomTextField.ALPHANUMERIC, toPropertyKeys.size()>0? 38: 16);
				parentComp = c;
				c.setText(val);
				c.addTextChangeListener(new TextChangeListener() {
					@Override
					public void textChanged(JComponent src) {
						propertyMap.put(propertyPrefix + p.getKey(), c.getText());
					}
				});
			}
			prop2comp.put(p, parentComp);
			
			if(p.getLinkedOptions()!=null && prop2comp.get(p.getLinkedOptions())!=null) {
				prop2comp.get(p.getLinkedOptions()).addFocusListener(new FocusAdapter() {
					private String t;
					@Override
					public void focusLost(FocusEvent e) {
						if(!t.equals(getValue(prop2comp.get(p.getLinkedOptions())))) {
							refreshConfigPanels();
						}
					}
					@Override
					public void focusGained(FocusEvent e) {
						t = getValue(prop2comp.get(p.getLinkedOptions()));						
					}
				});
			}
			//components
			JLabel labelComp = new JLabel(" " + p.getLabel() + ": ");
			JLabel tooltipComp = p.getTooltip()==null? new JLabel(): new JLabel(IconType.HELP.getIcon());
			labelComp.setToolTipText(p.getTooltip()==null? null: "<html>" + p.getTooltip());
			parentComp.setToolTipText(p.getTooltip()==null? null: "<html>" + p.getTooltip());
			tooltipComp.setToolTipText(p.getTooltip()==null? null: "<html>" + p.getTooltip());
			
			//Are there derived properties?
			if(toPropertyKeys.size()>0) {
				parentComp.addFocusListener(new FocusAdapter() {
					private String t;
					@Override
					public void focusLost(FocusEvent e) {
						if(!t.equals(getValue(parentComp))) {
							createPropertyPanel(panel, propertyPrefix, labelPrefix, properties, nestedValues);
						}						
					}					
					@Override
					public void focusGained(FocusEvent e) {
						t = getValue(parentComp);						
					}
				});
				List<JPanel> nestedPanels = new ArrayList<>();
				for(String token: MiscUtils.split(getValue(parentComp), ",")) {
					//Add the nested value to our stack
					final String[] nestedValues2 = new String[nestedValues.length+1];
					System.arraycopy(nestedValues, 0, nestedValues2, 0, nestedValues.length);
					nestedValues2[nestedValues.length] = token;

					//create a nested panel
					JPanel nestedPanel = new JPanel();
					nestedPanel.setOpaque(false);
					createPropertyPanel(nestedPanel, propertyPrefix + p.getKey() + "." + token +".", token, toPropertyKeys, nestedValues2);
					nestedPanels.add(nestedPanel);
				}
				
				panels.add(UIUtils.createTitleBox(p.getLabel(), 
						UIUtils.createBox(UIUtils.createHorizontalBox(labelComp, parentComp, tooltipComp, Box.createHorizontalGlue()), 
								null, UIUtils.createVerticalBox(nestedPanels))));
			} else {
				
				//Standard components: add them to the table
				tableComps.add(tooltipComp);
				tableComps.add(labelComp);
				tableComps.add(parentComp);
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
					nestedPanel = UIUtils.createTable(3, 5, 0, tableComps);
				}
				panels.add(UIUtils.createBox(UIUtils.createBox(nestedPanel, null, null, Box.createHorizontalStrut(10), Box.createHorizontalGlue()),
						new JCustomLabel(labelPrefix, FastFont.BOLD)));
			} else {
				//Main Panel
				panels.add(UIUtils.createTitleBox(UIUtils.createTable(3, 5, 3, tableComps)));
			}
		}
		panels.add(Box.createVerticalGlue());
		
		panel.removeAll();
		panel.add(UIUtils.createVerticalBox(panels));
		panel.validate();
	}
	
	private void initUISpecificProperties() {
		List<JComponent> comps = new ArrayList<>();
		if(adapter!=null) {
			for(PropertyDescriptor prop: adapter.getSpecificProperties()) {
				addComp(prop, comps);
			}
		}
		String help = adapter==null?"": adapter.getHelp();
		JEditorPane editorPane = new JEditorPane("text/html", help==null?"": help);
		editorPane.setEditable(false);
		JScrollPane sp = new JScrollPane(editorPane);
		sp.setVisible(help!=null && help.length()>0);
		sp.setPreferredSize(new Dimension(450, 200));
		specificConfigPane.removeAll();
		specificConfigPane.add(UIUtils.createBox(
				UIUtils.createTable(comps), 
				sp));
	}
	
	private JComponent addComp(PropertyDescriptor property, List<JComponent> comps) {
		comps.add(new JLabel(property.getDisplayName()+": "));		
		
		Map<String, String> optionMap = property.getOptionMap();
		if(optionMap.size()>0) {
			//Add a combobox
			Vector<String> options = new Vector<>();
			options.add("");
			options.addAll(optionMap.values());
					
			JComboBox<String> comp = new JComboBox<>(options);
			comp.setSelectedItem(property.getDisplayFromKey(adapter.getDBProperty(property)));
			dbproperty2comp.put(property, comp);
			comps.add(comp);
			comp.setEnabled(DBAdapter.isConfigurable());

			return comp;
		} else if(property.getDisplayName().contains("Password")) {
			//Add a password field
			final JCustomTextField comp = new JCustomTextField(JCustomTextField.ALPHANUMERIC, 22);
			comp.setTextWhenEmpty("Encrypted password");
			JButton encryptButton = new JButton("...");
			encryptButton.addActionListener(new ActionListener() {				
				@Override
				public void actionPerformed(ActionEvent e) {
					String res = JOptionPane.showInputDialog(DatabaseSettingsDlg.this, "Please enter the DB password (clear text) to encrypt it", "Encrypt password", JOptionPane.QUESTION_MESSAGE);
					if(res==null) return;
					StringEncrypter encrypter = new StringEncrypter("program from joel");
					comp.setText(encrypter.encrypt(res.trim().toCharArray()));
					
				}
			});
			dbproperty2comp.put(property, comp);
			comps.add(UIUtils.createHorizontalBox(comp, encryptButton));
			comp.setEnabled(DBAdapter.isConfigurable());
			encryptButton.setEnabled(DBAdapter.isConfigurable());
			comp.setText(adapter.getDBProperty(property));
			return comp;
		} else {
			JCustomTextField comp = new JCustomTextField(JCustomTextField.ALPHANUMERIC, 25);
			comp.setText(adapter.getDBProperty(property));
			comp.setEnabled(DBAdapter.isConfigurable());
			dbproperty2comp.put(property, comp);
			comps.add(comp);
			return comp;
		}
	}
	
	
	/**
	 * Updates settings linked to the DB connection (stored in config file or in adapter)
	 */
	@SuppressWarnings("rawtypes")
	private void updateDBProperties() {
		Map<String, String> map = new HashMap<>();
		
		if(adapter!=null) {
			//Adapter
			if(adapterComboBox!=null) {
				map.put(DBAdapter.ADAPTER_PROPERTY.getPropertyName(), DBAdapter.ADAPTER_PROPERTY.getKeyFromDisplay((String) adapterComboBox.getSelectedItem()));
			}
			
			//Specific properties
			for (PropertyDescriptor prop : adapter.getSpecificProperties()) {
				assert dbproperty2comp.get(prop)!=null;
				if(!dbproperty2comp.get(prop).isEnabled()) continue;
				if(dbproperty2comp.get(prop) instanceof JTextField) {
					map.put(prop.getPropertyName(),  ((JTextField)dbproperty2comp.get(prop)).getText());
				} else if(dbproperty2comp.get(prop) instanceof JComboBox) {
					map.put(prop.getPropertyName(), prop.getKeyFromDisplay((String) ((JComboBox)dbproperty2comp.get(prop)).getSelectedItem()));				
				}
			}
		}
		DBAdapter.setDBProperty(map);
		DBAdapter.setAdapter(null);
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
	
	private void testConnection() throws Exception {		
		if(adapter==null) throw new Exception("You must select an adapter");
		updateDBProperties();	

		//Init adapter without testing (start server if needed)
		adapter.preInit();
		
		//Simply test the connection
		adapter.testConnection();
	}
	

	private void testSchema() throws Exception {		
		if(adapter==null) throw new Exception("You must select an adapter");
		updateDBProperties();
		
		try {
			MigrationScript.assertLatestVersion();
		} catch(MigrationScript.MismatchDBException e) {
			new DatabaseMigrationDlg();			
		}
		
		//Init adapter with testing (start server if needed)
		adapter.preInit();
		adapter.validate();
	}
	
	
	private void save() throws Exception {
		if(adapter==null) throw new Exception("You must select an adapter");

		//Update properties
		updateDBProperties();
		
		//Simply test the connection, or forbids saving
		adapter.testConnection();
		
		//Success->Save
		//DBProperties in config file (if configurable)
		if(DBAdapter.isConfigurable()) {
			DBAdapter.saveDBProperties();
		}
		
		//ConfigProperties in DB
		SpiritProperties.getInstance().setValues(propertyMap);
		SpiritProperties.getInstance().saveValues();
		dispose();
		SpiritChangeListener.fireModelChanged(SpiritChangeType.LOGIN);
		
		//Reset Spirit
		DBAdapter.setAdapter(null);
		JPAUtil.closeFactory();
		SpiritChangeListener.fireModelChanged(SpiritChangeType.LOGIN);
	}
	
	
}
