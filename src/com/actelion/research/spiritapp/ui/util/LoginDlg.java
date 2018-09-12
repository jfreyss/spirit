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

package com.actelion.research.spiritapp.ui.util;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import com.actelion.research.spiritapp.Spirit;
import com.actelion.research.spiritapp.ui.SpiritFrame;
import com.actelion.research.spiritcore.business.audit.LogEntry;
import com.actelion.research.spiritcore.business.employee.Employee;
import com.actelion.research.spiritcore.business.employee.EmployeeGroup;
import com.actelion.research.spiritcore.business.property.PropertyKey;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.spiritcore.services.dao.Cache;
import com.actelion.research.spiritcore.services.dao.DAOEmployee;
import com.actelion.research.spiritcore.services.dao.DAOLog;
import com.actelion.research.spiritcore.services.dao.DAOSpiritUser;
import com.actelion.research.spiritcore.services.dao.SpiritProperties;
import com.actelion.research.spiritcore.util.Config;
import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.JCustomLabel;
import com.actelion.research.util.ui.JEscapeDialog;
import com.actelion.research.util.ui.JGenericComboBox;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.iconbutton.IconType;

/**
 * Login Dialog
 * @author freyssj
 */
public class LoginDlg extends JEscapeDialog {


	private Config config = Spirit.getConfig();
	private boolean loginWithJustOneRole;
	private boolean loginWithDepartment;
	private JTextField userTextField = new JTextField(28);
	private JPasswordField passwordTextField = new JPasswordField(28);
	private JLabel roleLabel = new JLabel("Role: ");
	private JGenericComboBox<String> roleComboBox = new JGenericComboBox<>();
	private JLabel errorLabel = new JLabel();

	private LoginDlg(Frame frame, final String title, String msg) {
		super(frame, "Login");
		loginWithJustOneRole = "true".equals(SpiritProperties.getInstance().getValue(PropertyKey.USER_ONEROLE));
		loginWithDepartment  = "true".equals(SpiritProperties.getInstance().getValue(PropertyKey.USER_USEGROUPS));

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
		loginButton.addActionListener(e-> {
			try {
				login(userTextField.getText(), passwordTextField.getPassword());
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
		});

		//Banner
		JPanel banner = new JPanel() {
			@Override
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
										new JLabel("User Name: "), Box.createHorizontalStrut(20), userTextField,
										Box.createVerticalStrut(10), null, null,
										new JLabel("Password: "), null, passwordTextField,
										Box.createVerticalStrut(10), null, null,
										roleLabel, null, roleComboBox),
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
		roleLabel.setVisible(loginWithJustOneRole);
		roleComboBox.setVisible(loginWithJustOneRole);
		if(loginWithJustOneRole) {
			Set<String> roles = new HashSet<>();
			if(userTextField.getText().length()>0) {
				try {
					Employee user = DAOEmployee.getEmployee(userTextField.getText());
					if(user!=null) roles = user.getRoles();
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
			if(roles.size()==0) {
				roles.add("No Role");
			}
			roleComboBox.setValues(roles, "Select a role");
			if(roles.size()==1) {
				roleComboBox.setSelection(roles.iterator().next());
				roleComboBox.setEnabled(false);
			} else {
				roleComboBox.setEnabled(true);
			}
		}
	}

	private void login(String username, char[] password) throws Exception {

		SpiritUser user;
		try {
			//Validate username/password
			DAOSpiritUser.authenticateUser(username, password);

			//Load the user
			user = DAOSpiritUser.loadUser(username);
			if(user==null) {
				throw new Exception(username+" is invalid");
			}
			SpiritFrame.setUser(user);
		} catch (Exception e) {
			DAOLog.log(username, LogEntry.Action.LOGON_FAILED);
			throw e;
		} finally {
			Cache.getInstance().clear();
		}

		//If the property loginWithJustOneRole is true. The user could select its role upon login
		if(loginWithJustOneRole) {
			String role = roleComboBox.getSelection();
			if(role==null || role.length()==0) throw new Exception("Please select a role");
			for (String r : new ArrayList<>(user.getRoles())) {
				user.setRole(r, false);
			}
			user.setRole(role,  true);
			config.setProperty("role", role);
		}

		//If the property loginWithDepartment is true.
		if(loginWithDepartment) {
			List<EmployeeGroup> groups = new ArrayList<>(user.getGroups());
			for (Iterator<EmployeeGroup> iterator = groups.iterator(); iterator.hasNext();) {
				EmployeeGroup g = iterator.next();
				if(g.isFunctional()) iterator.remove();
			}

			if(groups.size()>1) {
				int res = JOptionPane.showOptionDialog(null, "Please select your department", "User login", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, groups.toArray(new EmployeeGroup[0]), null);
				if(res<0) {
					throw new Exception("You must select a group");
				}
				user.setMainGroup(groups.get(res));
			} else if(groups.size()==1) {
				user.setMainGroup(groups.get(0));
			} else {
				user.setMainGroup(null);
			}
		} else {
			user.setMainGroup(null);
		}

		if(DAOLog.isLocked(username)) {
			throw new Exception("Your account has been disabled after "+SpiritProperties.getInstance().getValue(PropertyKey.USER_LOCKAFTER)+" attempts (ask an admin to reenable your account)");
		} else {
			DAOLog.log(username, LogEntry.Action.LOGON_SUCCESS, ((user.getMainGroup()==null?"": user.getMainGroup().getName()) + " " + user.getRolesString()).trim());
			SpiritFrame.getInstance().startInactivityListener();
		}
	}

	public static void openLoginDialog(Frame frame, String title) {
		openLoginDialog(frame, title, "");
	}

	public static void openLoginDialog(Frame frame, String title, String msg) {
		new LoginDlg(frame, title, msg);
	}

}
