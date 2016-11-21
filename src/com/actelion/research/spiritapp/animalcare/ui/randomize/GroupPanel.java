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

package com.actelion.research.spiritapp.animalcare.ui.randomize;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritapp.spirit.ui.study.GroupLabel;
import com.actelion.research.spiritapp.spirit.ui.study.edit.AttachedBiosampleTable;
import com.actelion.research.spiritapp.spirit.ui.study.edit.AttachedBiosampleTableModel;
import com.actelion.research.spiritapp.spirit.ui.study.edit.AttachedBiosampleTableModel.Mode;
import com.actelion.research.spiritcore.business.study.AttachedBiosample;
import com.actelion.research.spiritcore.business.study.Group;
import com.actelion.research.spiritcore.business.study.Randomization;
import com.actelion.research.spiritcore.util.StatUtils;
import com.actelion.research.util.FormatterUtils;
import com.actelion.research.util.ui.JCustomTextField;
import com.actelion.research.util.ui.PopupAdapter;
import com.actelion.research.util.ui.TextChangeListener;
import com.actelion.research.util.ui.UIUtils;

public class GroupPanel extends JPanel {

	private boolean forCages;
	private AttachedBiosampleTable table;
	private JLabel infoLabel = new JLabel();
	private JLabel nLabel = new JLabel();
	private final Group group;
	
	

	public GroupPanel(RandomizationDlg dlg, final Group gp, boolean forCages, boolean hideFrom) {
		
		String title = gp==null?"NoGroup": 
			gp.getBlindedName(Spirit.getUser().getUsername()) + (hideFrom || gp.getFromGroup()==null?"": " (from "+gp.getFromGroup().getBlindedName(Spirit.getUser().getUsername())+")");

		this.group = gp;
		this.forCages = forCages;

		
		//Create table
		table = new AttachedBiosampleTable(new AttachedBiosampleTableModel(forCages? Mode.RND_SETCAGES: Mode.RND_SETGROUPS, dlg.getStudy(),  group, dlg.getPhase()), true);
		table.setCanSort(true);
		table.getModel().addTableModelListener(new TableModelListener() {			
			@Override
			public void tableChanged(TableModelEvent e) {
				refreshStats();				
			}
		});
		table.createDefaultColumnsFromModel();
		
		
		if(!forCages) {
			table.addMouseListener(new PopupAdapter() {				
				@Override
				protected void showPopup(MouseEvent e) {
					boolean skipRando = false;
					for(AttachedBiosample r: table.getSelection()) {
						if(r.isSkipRando()) skipRando = true;
					}
					
					final JRadioButtonMenuItem button = new JRadioButtonMenuItem("Fix group (ignore automatic rando)");
					button.setSelected(skipRando);
					button.addActionListener(new ActionListener() {
						
						@Override
						public void actionPerformed(ActionEvent e) {
							for(AttachedBiosample r: table.getSelection()) {
								r.setSkipRando(button.isSelected());
							}
						}
					});
					JPopupMenu menu = new JPopupMenu();
					menu.add(button);
					menu.show(table, e.getX(), e.getY());
					
				}
			});			
		}
		
		//Create Panel
//		final JPanel contentPanel = new JPanel(new GridBagLayout());
		setBackground(group==null? Color.LIGHT_GRAY: group.getColor());
//		setTitleFont(new Font(Font.DIALOG, Font.BOLD, 13));
//		setTitlePainter(new Painter() {			
//			@Override
//			public void paint(Graphics2D g, Object object, int width, int height) {
//				g.setPaint(new GradientPaint(0, 0, Color.LIGHT_GRAY, 0, height, contentPanel.getBackground()));
//				g.fillRect(0, 0, width, height);
//				
//			}
//		});
		
		setBorder(BorderFactory.createRaisedBevelBorder());
		
		
		int n = gp==null? 0: gp.getNAnimals(dlg.getPhase());
		final JCustomTextField nTextField = new JCustomTextField(JCustomTextField.INTEGER);
		if(gp==null) {
			
		} else {
			nTextField.setTextInteger(n<=0? null: n);
			nTextField.addTextChangeListener(new TextChangeListener() {	
				@Override
				public void textChanged(JComponent src) {
					if(nTextField.getTextInt()==null || nTextField.getTextInt()<=0) return;
					int[] subGroupSizes = gp.getSubgroupSizes();
					int leftToAdd = nTextField.getTextInt() - gp.getNAnimals();
					if(leftToAdd>0) {
						for (int i = subGroupSizes.length-1; leftToAdd>0 && i >=0; i--) {
							int tmp = leftToAdd / (i+1);
							subGroupSizes[i] += tmp;
							leftToAdd -= tmp;
						}
					} else if(leftToAdd<0) {
						for (int i = subGroupSizes.length-1; leftToAdd<0 && i >=0; i--) {
							int tmp = Math.min(subGroupSizes[i], -leftToAdd);
							subGroupSizes[i] -= tmp;
							leftToAdd += tmp;
						}
					}
					System.out.println(Arrays.toString(subGroupSizes));
					gp.setSubgroupSizes(subGroupSizes);
					
				}
			});
		}
		//Create ui
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;		
		c.fill = GridBagConstraints.BOTH;		
		c.insets = new Insets(1, 2, 1, 2);
		
		infoLabel.setOpaque(false);
		
		setLayout(new BorderLayout());
		GroupLabel groupLabel = new GroupLabel(title, group);
		groupLabel.setOpaque(group!=null);
		add(BorderLayout.NORTH, UIUtils.createHorizontalBox(groupLabel, Box.createHorizontalStrut(5), new JLabel("N ="), nLabel, gp==null || forCages? null: UIUtils.createHorizontalBox(new JLabel(" / "), nTextField), Box.createHorizontalGlue()));
		add(BorderLayout.CENTER, new JScrollPane(table));
		if(!forCages) {
			add(BorderLayout.SOUTH, infoLabel);
		}
		
		prefHeight = Math.min(320,  (n>0? n: 6) * 26 + 60 + (forCages?0:50)) ;
	}
	
	
	
	
	private int prefHeight;
	
	@Override
	public Dimension getPreferredSize() {
		return new Dimension(forCages?CageTab.PANEL_WIDTH: GroupTab.PANEL_WIDTH, prefHeight);
	}
	@Override
	public Dimension getMinimumSize() {
		return super.getPreferredSize();
	}
	@Override
	public Dimension getMaximumSize() {
		return super.getPreferredSize();
	}
	
	public AttachedBiosampleTable getRndTable() {
		return table;
	}
	
	public void setRows(Randomization rnd, final List<AttachedBiosample> rows) {
		table.getModel().setNData(rnd.getNData());
		table.setRows(rows);
		
	}
		
	public void refreshStats() {
		nLabel.setText(""+table.getRowCount());
		if(forCages) {		
		} else {
			List<Double> weights = table.getDoubles(-1);
			
			StringBuilder sb = new StringBuilder();
			
			Double mean;
			for(int i=0; i<=10; i++) {
				List<Double> datas = table.getDoubles(i);
				mean = StatUtils.getMean(datas);
				if(mean!=null) sb.append("D"+(i+1)+"=<b>"+FormatterUtils.format3(mean) + "</b> (std=" + FormatterUtils.format1(StatUtils.getStandardDeviation(datas, mean))+")<br>");
			}
			
			mean = StatUtils.getMean(weights);
			if(mean!=null) sb.append("Weight=<b>"+FormatterUtils.format2(mean) + "</b> (std=" + FormatterUtils.format1(StatUtils.getStandardDeviation(weights, mean))+")<br>");
			

			infoLabel.setText("<html>" + sb + "</html>");
		
		}
	}
	
	public Group getGroup() {
		return group;
	}
	
	public AttachedBiosampleTable getTable() {
		return table;
	}
	
	
}
