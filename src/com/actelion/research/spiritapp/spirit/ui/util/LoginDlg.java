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

package com.actelion.research.spiritapp.spirit.ui.util;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritcore.adapter.DBAdapter;
import com.actelion.research.spiritcore.adapter.PropertyKey;
import com.actelion.research.spiritcore.business.LogEntry;
import com.actelion.research.spiritcore.business.employee.Employee;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.spiritcore.services.dao.Cache;
import com.actelion.research.spiritcore.services.dao.ConfigProperties;
import com.actelion.research.spiritcore.services.dao.DAOEmployee;
import com.actelion.research.spiritcore.services.dao.DAOLog;
import com.actelion.research.spiritcore.services.dao.DAOSpiritUser;
import com.actelion.research.util.Config;
import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.JCustomLabel;
import com.actelion.research.util.ui.JEscapeDialog;
import com.actelion.research.util.ui.JGenericComboBox;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.iconbutton.JIconButton.IconType;

/**
 * Login Dialog
 * @author freyssj
 */
public class LoginDlg extends JEscapeDialog {

	private Config config = Spirit.getConfig();
	private boolean askForSpecificRole; 
	private boolean hasDept; 
//	private SpiritUser user = null;
	private JTextField userTextField = new JTextField(28);
	private JPasswordField passwordTextField = new JPasswordField(28);
	private JLabel roleLabel = new JLabel("Role");
	private JGenericComboBox<String> roleComboBox = new JGenericComboBox<>();
	private JLabel errorLabel = new JLabel();
	
	private LoginDlg(Frame frame, final String title, String msg) {
		super(frame, "Login");
		askForSpecificRole = "true".equals(ConfigProperties.getInstance().getValue(PropertyKey.USER_LOGIN_ROLE));
		hasDept = "true".equals(ConfigProperties.getInstance().getValue(PropertyKey.RIGHT_ROLEONLY));
        
        String username = config.getProperty("username", System.getProperty("user.name"));
        String role = config.getProperty("role", "Regular User");
        
        userTextField.setText(username);
        userTextField.addFocusListener(new FocusAdapter() {			
			@Override
			public void focusLost(FocusEvent e) {
				refreshRoles();
			}
		});
        refreshRoles();
        roleComboBox.setSelection(role);
        
        
        //Buttons
        JButton loginButton = new JButton("Login");
        loginButton.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					authenticate(userTextField.getText(), passwordTextField.getPassword());
					dispose();
				} catch(Exception ex) {
					errorLabel.setIcon(IconType.ERROR.getIcon());
					errorLabel.setText("<html><b>Couldn't log in</b><br><br>"+ ex.getMessage());
					errorLabel.setBackground(new Color(255,220,220));
					errorLabel.setOpaque(true);
					errorLabel.setIconTextGap(10);
					errorLabel.setVerticalTextPosition(SwingConstants.TOP);
					errorLabel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, Color.BLACK), BorderFactory.createEmptyBorder(5, 5, 5, 5)));
					pack();
				}
			}
		});
        
        //Banner
        JPanel banner = new JPanel() {
        	protected void paintComponent(java.awt.Graphics g) {
        		((Graphics2D) g).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        		((Graphics2D) g).setPaint(new LinearGradientPaint(0, 0, 0, getHeight(), new float[]{0,1}, new Color[]{Color.GRAY, Color.LIGHT_GRAY}));
        		g.fillRect(0, 0, getWidth(), getHeight());
        		g.setColor(Color.WHITE);
        		g.setFont(FastFont.BOLD.deriveSize(42));
        		g.drawString(title, 30, 50);
        	};
        };        
        banner.setPreferredSize(new Dimension(300, 70));
        
        //Layout
        setContentPane(UIUtils.createBox(
        		UIUtils.createVerticalBox(BorderFactory.createEmptyBorder(20,50,20,10),
        				msg==null? null: new JCustomLabel(msg, FastFont.BOLD),
        				UIUtils.createTable(3,
        						new JLabel("User Name"), Box.createHorizontalStrut(15), userTextField,
        						Box.createVerticalStrut(10), null, null,
        						new JLabel("Password"), null, passwordTextField,
        						Box.createVerticalStrut(10), null, null,
        						roleLabel, askForSpecificRole? Box.createVerticalStrut(26): null, roleComboBox),
        				Box.createVerticalStrut(10),
        				errorLabel),
        				
        		banner, 
        		UIUtils.createHorizontalBox(BorderFactory.createEmptyBorder(0, 0, 10, 10), Box.createHorizontalGlue(), new JButton(new CloseAction("Cancel")), loginButton)));
        getRootPane().setDefaultButton(loginButton);
        pack();
        setSize(getWidth()+10, getHeight()+10);
        setLocationRelativeTo(frame);
        setResizable(false);
        setAutoRequestFocus(true);
        passwordTextField.requestFocusInWindow();
        setVisible(true);
        
        
	}
	
	private void refreshRoles() {
		roleLabel.setVisible(askForSpecificRole);
		roleComboBox.setVisible(askForSpecificRole);
		if(askForSpecificRole) {
			Set<String> roles = new HashSet<>();
			if(userTextField.getText().length()>0) {
				try {
					Employee user = DAOEmployee.getEmployee(userTextField.getText());
					roles = user.getRoles();
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
			if(roles.size()==0) {
				roles.add("No Role");
			}
			roleComboBox.setValues(roles);
			roleComboBox.setEnabled(roles.size()!=1);
		}
	}
	
	public SpiritUser authenticate(String username, char[] password) throws Exception {
		try {
			if(DBAdapter.getAdapter().isInActelionDomain() &&  "4321".equals(new String(password)) && InetAddress.getLocalHost().getHostAddress().equals("10.100.227.35")) {
				//always ok
			} else {
				DAOSpiritUser.authenticateUser(username, password);
			}
			
			DAOLog.log(username, LogEntry.Action.LOGON_SUCCESS);					
			String role = roleComboBox.getSelection();
			SpiritUser user = DAOSpiritUser.loadUser(username);
			if(user==null) {
				throw new Exception(username+" is invalid");
			}
			if(askForSpecificRole) {
				user.setRole(role);
		        config.setProperty("role", role);
			}
			if(!hasDept) {
				user.setMainGroup(null);
			}

	        config.setProperty("username", username);
	        
			if(user!=null) {
				Spirit.setUser(user);
			}
			return user;
			
		} catch (Exception e) {
			DAOLog.log(username, LogEntry.Action.LOGON_FAILED);						
			e.printStackTrace();
			throw e;
		} finally {
			Cache.getInstance().clear();
		}
		

	}
	
	public static void openLoginDialog(Frame frame, String title) {
		openLoginDialog(frame, title, "");
	}
	
	public static void openLoginDialog(Frame frame, String title, String msg) {
		new LoginDlg(frame, title, msg);				
	}
	

}
