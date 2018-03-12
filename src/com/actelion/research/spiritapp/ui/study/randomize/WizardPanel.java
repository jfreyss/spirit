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

package com.actelion.research.spiritapp.ui.study.randomize;

import java.awt.BorderLayout;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.actelion.research.util.ui.JExceptionDialog;

/**
 * Basic Helper class to make wizards.
 * This provides an interface but the logic has to performed by the MainClass 
 * @author freyssj
 *
 */
public abstract class WizardPanel extends JPanel {
	
	public static final String PROPERTY_BACK = "back"; 
	public static final String PROPERTY_NEXT_OR_FINISH = "next"; 
	
	public WizardPanel() {
		super(new BorderLayout());
	}
	public WizardPanel(LayoutManager layoutManager) {
		super(layoutManager);
	}
	
	public JButton getBackButton() {
		JButton prevButton = new JButton("<< Back");
		prevButton.addActionListener(new ActionListener() {				
			@Override
			public void actionPerformed(ActionEvent e) {
				WizardPanel.this.firePropertyChange(PROPERTY_BACK, "0", "1");
			}
		});
		return prevButton;
	}
	public JButton getNextButton() {
		JButton nextButton = new JButton("Continue >>>");
		nextButton.addActionListener(new ActionListener() {				
			@Override
			public void actionPerformed(ActionEvent e) {
				WizardPanel.this.firePropertyChange(PROPERTY_NEXT_OR_FINISH, "0", "1");
			}
		});
		return nextButton;
	}
	
	public abstract void updateModel(boolean allowDialogs) throws Exception;
	
	public abstract void updateView();
	
	public static void updateModel(final JTabbedPane pane, boolean allowDialogs) throws Exception {
		if(pane.getSelectedComponent() instanceof WizardPanel) {
			((WizardPanel)pane.getSelectedComponent()).updateModel(allowDialogs);
		}
	}
	
	public static void configureEvents(final JTabbedPane pane, final WizardPanel... tabs) {
		pane.addChangeListener(new ChangeListener() {		
			WizardPanel previous = tabs.length>0? tabs[0]: null;
			@Override
			public void stateChanged(ChangeEvent e) {
				
				if(pane.getSelectedComponent() instanceof WizardPanel) {
					if(previous!=null) {
						try {
							previous.updateModel(false);
						} catch (Exception ex) {
							WizardPanel memo = previous;
							previous=null;
							pane.setSelectedComponent(memo);
							JExceptionDialog.showError(memo, ex);
							previous = memo;
							return;
						}
					}
					previous = ((WizardPanel) pane.getSelectedComponent());
					previous.updateView();
				} else {
					previous = null;
				}
			}
		});
		for (int i = 0; i < tabs.length; i++) {
			final int tabNo = i;
			final WizardPanel p  = tabs[i];
			p.addPropertyChangeListener(PROPERTY_BACK, new PropertyChangeListener() {				
				@Override
				public void propertyChange(PropertyChangeEvent evt) {
					try {
						p.updateModel(true);
						if(tabNo-1>=0) {
							pane.setEnabledAt(tabNo-1, true);
							pane.setSelectedIndex(tabNo-1);
							tabs[tabNo-1].updateView();
						}
					} catch (Exception ex) {
						JExceptionDialog.showError(p, ex);
					}
				}
			});
			p.addPropertyChangeListener(PROPERTY_NEXT_OR_FINISH, new PropertyChangeListener() {				
				@Override
				public void propertyChange(PropertyChangeEvent evt) {
					try {
						p.updateModel(true);
						if(tabNo+1<tabs.length) {
							pane.setEnabledAt(tabNo+1, true);
							pane.setSelectedIndex(tabNo+1);
							tabs[tabNo+1].updateView();
						}
					} catch (Exception ex) {
						JExceptionDialog.showError(p, ex);
					}
				}
			});
		}
	}
}
