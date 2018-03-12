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

package com.actelion.research.spiritapp.ui.util.component;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JPanel;

import com.actelion.research.spiritapp.ui.admin.config.SpiritConfig;

public class BalanceDecorator {
	private JDialog dlg;
	private JCheckBox balanceCheckBox = new JCheckBox("Use Balance");
	private MTBalance balance;
	private BalanceMonitoringThread balanceMonitoringThread;
	
	public BalanceDecorator(JDialog dlg) {
		this.dlg = dlg;
		final String balanceFile = new SpiritConfig().getWeighingFile();
		final int token = new SpiritConfig().getWeighingCol();
				
		if(balanceFile==null || balanceFile.length()==0 || token<0) {
			balanceCheckBox.setEnabled(false);
		} else {
			try {
				balance = new MTBalance(balanceFile, token);
				balanceCheckBox.setToolTipText("File:" + balanceFile+" / Col: "+token);
			} catch (Exception e) {
				e.printStackTrace();
			}
			startBalance();
		}
		
		balanceCheckBox.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				if(balanceCheckBox.isSelected()) {
					startBalance();
				} else {
					stopBalance();
				}
			}
		});
	}
	
	
	public synchronized void startBalance() {
		if(balance!=null) {
			stopBalance();
			balanceCheckBox.setSelected(true);
			balanceCheckBox.setOpaque(true);
			balanceCheckBox.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.GRAY), BorderFactory.createEmptyBorder(2,2,2,2)));
			balanceCheckBox.setBackground(Color.GREEN);
			balanceMonitoringThread = new BalanceMonitoringThread(dlg, balance);
			balanceMonitoringThread.setDaemon(true);
			balanceMonitoringThread.start();
		}
	}
	public synchronized void stopBalance() {
		if(balanceMonitoringThread!=null) {
			balanceMonitoringThread.interrupt();
			balanceMonitoringThread=null;
		}
		balanceCheckBox.setSelected(false);
		balanceCheckBox.setOpaque(false);
		balanceCheckBox.setBorder(BorderFactory.createLineBorder(Color.GRAY));
	}

	

	/**
	 * @return the balanceCheckBox
	 */
	public JPanel getBalanceCheckBox() {
		JPanel panel = new JPanel(new GridLayout(1,1));
		panel.setMinimumSize(new Dimension(135, 24));		
		panel.setPreferredSize(new Dimension(135, 24));		
		panel.setMaximumSize(new Dimension(135, 24));		

		panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(1,1,1,1), BorderFactory.createLineBorder(Color.GRAY)));
		panel.add(balanceCheckBox);

		return panel;
	}
	

}
