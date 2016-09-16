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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import com.actelion.research.spiritcore.business.study.AttachedBiosample;
import com.actelion.research.spiritcore.business.study.Group;
import com.actelion.research.spiritcore.util.StatUtils;
import com.actelion.research.util.CompareUtils;
import com.actelion.research.util.FormatterUtils;
import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.JCustomLabel;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.JInfoLabel;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.iconbutton.JIconButton.IconType;

public class GroupTab extends WizardPanel {
	
	public static final int PANEL_WIDTH = 310;

	private RandomizationDlg dlg;

	private List<GroupPanel> groupPanels = new ArrayList<GroupPanel>();
	private List<Group> groups = new ArrayList<Group>();
	private JPanel centerPanel = new JPanel();	
	
	private JSlider factorSlider = new JSlider(JSlider.HORIZONTAL, 0, 10, 5);
	private JPanel factorBox = UIUtils.createHorizontalBox(new JLabel("  Randomize more on "), UIUtils.createHorizontalBox(BorderFactory.createEtchedBorder(), new JCustomLabel("Weight ", FastFont.SMALL), factorSlider, new JCustomLabel(" Data", FastFont.SMALL)));

	private JCheckBox heaviestReserveCheckBox = new JCheckBox("Put Heaviest to Reserve ", true);
	private JCheckBox lightestReserveCheckBox = new JCheckBox("Put Lightest to Reserve ", true);
	
	
//	private JLabel scoreLabel = new JLabel();
	
	public GroupTab(final RandomizationDlg dlg) {
		this.dlg = dlg;
		factorSlider.setPreferredSize(new Dimension(90, 24));
		factorSlider.setMajorTickSpacing(5);
		factorSlider.setMinorTickSpacing(1);
		factorSlider.setPaintTicks(true);
		factorSlider.setSnapToTicks(true);
		
		JButton randomizeButton = new JButton("Randomize");
		randomizeButton.setToolTipText("Randomize so that each group has the same weighing and data distribution");
		randomizeButton.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					randomize();
					refreshTables();
				} catch (Exception ex) {
					JExceptionDialog.showError(GroupTab.this, ex);
				}
			}
		});
				
		final JButton helpLabel = new JButton(IconType.HELP.getIcon()) {
			@Override
			public String getToolTipText() {				
				return "<html>The score equals:<br>" +
						"<b>Score</b> = weightFactor * <b>ScoreWeight</b> + (1-weightFactor) * <b>ScoreData</b><br>" +
						"<br>" +												
						"<b>ScoreWeight</b>=[SUM((AvgWeight(groupi)-AvgWeight(all))^2) + SUM((StdWeight(groupi)-StdWeight(all)^2)]<br>" + 
						"<b>ScoreWeight</b>=<b>" + FormatterUtils.format2(score(dlg.getRandomization().getSamples(), true)) + "</b><br>" + 
						"<br>" +												
						"<b>ScoreData</b>=[SUM((AvgData(groupi)-AvgData(all))^2) + SUM((StdData(groupi)-StdData(all)^2)]<br>" +
						"<b>ScoreData</b>=<b>" + FormatterUtils.format2(score(dlg.getRandomization().getSamples(), false)) + "</b><br>" + 
						"<br>" +
						" <i> where AvgWeight(groupi) is the avgWeight of the group i<br>" + 
						" <i> where AvgWeight(all) is the avgWeight of all groups<br>" + 
						" <i> where StdWeight(groupi) is the standard deviation the group i<br>" + 
						" <i> where StdWeight(all) is the standard deviation of all groups<br></html>"; 				
			}
		};
		helpLabel.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				JOptionPane.showMessageDialog(GroupTab.this, helpLabel.getToolTipText(), "Scoring Function", JOptionPane.INFORMATION_MESSAGE);				
			}
		});
		
		JComponent topPanel = UIUtils.createTitleBox("Automatic Randomization", UIUtils.createVerticalBox(
				new JInfoLabel("You can perfom automatic randomization by clicking here, or use drag and drop to assign manually"),
				UIUtils.createHorizontalBox(lightestReserveCheckBox, heaviestReserveCheckBox, Box.createHorizontalStrut(30), factorBox, Box.createHorizontalGlue()),
				UIUtils.createHorizontalBox(randomizeButton, /*Box.createHorizontalStrut(30), scoreLabel,*/ helpLabel, Box.createHorizontalGlue()))); 
		
		
		add(BorderLayout.NORTH, topPanel);
		add(BorderLayout.CENTER, centerPanel);
		add(BorderLayout.SOUTH, UIUtils.createHorizontalBox(new JCustomLabel("Drag animals to the desired groups or click automatic randomization", Font.ITALIC), Box.createHorizontalGlue(), getNextButton()));
	}
	
	
	
	
	
	
	@Override
	public void updateModel(boolean allowDialogs) throws Exception {
		// Nothing
		
	}

	@Override
	public void updateView() {
		
		//Create groupPanels
		groupPanels.clear();
		groups.clear();
		
		JPanel nonReservePanel = new JPanel();
		for (Group gr : dlg.getGroups()) {			
			if(!dlg.getPhase().equals(gr.getFromPhase())) continue;
			if(gr.getFromGroup()!=null && !groups.contains(gr.getFromGroup())) {
				Group toAdd = gr.getFromGroup();
				GroupPanel grPanel = new GroupPanel(dlg, toAdd, false, true);
				nonReservePanel.add(grPanel);
				groupPanels.add(grPanel);
				groups.add(toAdd);
			}

			Group toAdd = gr;
			GroupPanel grPanel = new GroupPanel(dlg, toAdd, false, false);
			nonReservePanel.add(grPanel);
			groupPanels.add(grPanel);
			groups.add(toAdd);
		}
		
		//Layout
		int cols = Math.max(1, (centerPanel.getWidth()-PANEL_WIDTH)/PANEL_WIDTH);	
		while(nonReservePanel.getComponentCount()<cols) nonReservePanel.add(new JPanel());
		nonReservePanel.setLayout(new GridLayout(0, cols));
		
		factorBox.setVisible(dlg.getRandomization().getNData()>0);

		
		//Reserve
		GroupPanel reservePanel = new GroupPanel(dlg, null, false, false);			
		groupPanels.add(reservePanel);

		centerPanel.removeAll();
		centerPanel.setLayout(new BorderLayout());
		JScrollPane sp = new JScrollPane(nonReservePanel);
		sp.setAutoscrolls(true);
		nonReservePanel.setAutoscrolls(true);
		centerPanel.add(BorderLayout.CENTER, sp);
		centerPanel.add(BorderLayout.EAST, reservePanel);
		

	
		
		for (GroupPanel gr : groupPanels) {
			gr.getRndTable().getModel().addTableModelListener(new TableModelListener() {				
				@Override
				public void tableChanged(TableModelEvent e) {
					refreshScore();					
				}
			});			
		}

		
		//Finally refresh the view
		validate();
		refreshTables();
		refreshScore();
		
		
	}
	private void refreshTables() {
		for (GroupPanel groupPanel : groupPanels) {
			Group g = groupPanel.getGroup();
			List<AttachedBiosample> rows = new ArrayList<AttachedBiosample>();
			for (AttachedBiosample s : dlg.getRandomization().getSamples()) {
				if(CompareUtils.compare(g, s.getGroup())==0) {
					rows.add(s);
				}
			}
			groupPanel.setRows(dlg.getRandomization(), rows);			
		}
	}
	
	public void randomize() throws Exception {

		dlg.setMustAskForExit(true);
		List<AttachedBiosample> samples = new ArrayList<AttachedBiosample>(dlg.getRandomization().getSamples());
		List<Double> weights = AttachedBiosample.getData(samples, -1);

		int neededAnimals = 0;
		for (Group gr : groups) neededAnimals+=gr.getNAnimals(dlg.getPhase());
		
		if(samples.size()<neededAnimals) throw new Exception("You need to have at least "+neededAnimals+" animals with measured weights");
		
		

		if(heaviestReserveCheckBox.isSelected() && !lightestReserveCheckBox.isSelected()) {
			//Sort ascending order
			Collections.sort(samples, new Comparator<AttachedBiosample>() {
				@Override
				public int compare(AttachedBiosample o1, AttachedBiosample o2) {
					if(o1.getWeight()==null) return 1;
					if(o2.getWeight()==null) return 1;
					double d1 = o1.getWeight();
					double d2 = o2.getWeight();				
					return d1-d2<0?-1:1;					
				}
			});
			
		} else if(!heaviestReserveCheckBox.isSelected() && lightestReserveCheckBox.isSelected()) {
			//Sort decending order
			Collections.sort(samples, new Comparator<AttachedBiosample>() {
				@Override
				public int compare(AttachedBiosample o1, AttachedBiosample o2) {
					if(o1.getWeight()==null) return 1;
					if(o2.getWeight()==null) return 1;
					double d1 = o1.getWeight();
					double d2 = o2.getWeight();				
					return d1-d2<0?1:-1;					
				}
			});			
		} else {
			//Sort by proximity to mean		
			final Double wMean = StatUtils.getMean(weights);
			Collections.sort(samples, new Comparator<AttachedBiosample>() {
				@Override
				public int compare(AttachedBiosample o1, AttachedBiosample o2) {
					if(o1.getWeight()==null) return 1;
					if(o2.getWeight()==null) return 1;
					double d1 = Math.abs(wMean - o1.getWeight());
					double d2 = Math.abs(wMean - o2.getWeight());				
					return d1-d2<0?-1:1;					
				}
			});
		}
		
		
		//Initialize the groups
		Map<Group, Integer> group2Left = new HashMap<Group, Integer>();
		for (Group gr : groups) {
			int n = gr.getNAnimals(dlg.getPhase());
			group2Left.put(gr, n>=0? n: 100);
		}
		for (AttachedBiosample rndSample : samples) {
			if(!rndSample.isSkipRando()) {
				rndSample.setGroup(null);
			} else if(rndSample.getGroup()!=null) {
				group2Left.put(rndSample.getGroup(), group2Left.get(rndSample.getGroup())==null?0: group2Left.get(rndSample.getGroup())-1);				
			}
		}
		
		
		/////////////////////////////////////////////////////////
		//First basic assignment
		// Data   -> GroupIndex
		// 1      -> 0
		// 2      -> 1
		// 3      -> 2
		// 4      -> 2
		// 5      -> 1
		// 6      -> 0
		//
		int groupIndex = -1;
		int direction = 1;
		
		if(groups.size()>1) {
			for (AttachedBiosample s : samples) {
				if(s.isSkipRando()) continue;
				for (int i = 0; i < groups.size()*5; i++) {
					
					groupIndex += direction;
					if(groupIndex>=groups.size()) {groupIndex = groups.size()-1; direction=-1;}
					if(groupIndex<0) {groupIndex = 0; direction= 1;}
	
					Group group = groups.get(groupIndex);
					if(group.getFromGroup()!=null) {
						if(!canAssign(s, group.getFromGroup())) continue;
					}
					if(group2Left.get(group)<=0) continue;
						
					
					group2Left.put(group, group2Left.get(group)-1);
					
					s.setGroup(group);
					break;
				}
			}		
		}
		
		Collections.sort(samples, new Comparator<AttachedBiosample>() {
			@Override
			public int compare(AttachedBiosample o1, AttachedBiosample o2) {
				return CompareUtils.compare(o1.getNo(), o2.getNo());
			}
		});

		
		List<Group> bestGroups = store(samples);
		double bestScore = score(samples);
		System.out.println("Start with score: "+bestScore);
		
		///////////////////////////////////
		//Simulated Annealing to optimize 
		for(int step=0; step<30000; step++) {
			double T = 400;
			int i1 = (int) (Math.random()*samples.size());
			int i2 = (int) (Math.random()*samples.size());			
			if(samples.get(i1).isSkipRando()) continue;
			if(samples.get(i2).isSkipRando()) continue;
			
			Group g1 = samples.get(i1).getGroup();
			Group g2 = samples.get(i2).getGroup();
			if(g1==g2) continue;
			if((heaviestReserveCheckBox.isSelected() || lightestReserveCheckBox.isSelected()) && (g1==null || g2==null)) continue;
			
			//In case of subgroups, make sure that the we mix compatible samples
			if(!canAssign(samples.get(i1), g2)) continue;
			if(!canAssign(samples.get(i2), g1)) continue;
			
			samples.get(i1).setGroup(g2);
			samples.get(i2).setGroup(g1);
			
			double score = score(samples);
			if(score<bestScore) {
				bestScore = score;
				bestGroups = store(samples);
				System.out.println(step + ". Accept better score: "+score);
			} else {
				double p = Math.exp(-(score-bestScore)/T);
				if(p<Math.random()) {
					restore(samples, bestGroups);
					System.out.println(step+". restore best "+bestScore + " p = "+Math.exp(-(score-bestScore)/T));
				}
			}			
		}
		restore(samples, bestGroups);

		refreshScore();
	}
	
	private boolean canAssign(AttachedBiosample r, Group g) {
		if(r==null || r.getBiosample()==null) return true; //Should not happen, but...
		if(g==null) return true; //all can go to reserve
		
		Group formerGroup = r.getBiosample().getInheritedGroup();
		if(g.equals(formerGroup)) return true;	//can move back in hierarchy			
		if(g.getFromGroup()!=null && g.getFromGroup().equals(formerGroup)) return true;	//can move back in hierarchy
		if(formerGroup!=null && g.equals(formerGroup.getFromGroup())) return true;	//can move back in hierarchy
		if(g.getFromGroup()!=null && formerGroup!=null && g.getFromGroup().equals(formerGroup.getFromGroup())) return true;	//can move back in hierarchy
		
		if(r.getBiosample()!=null && r.getBiosample().isDeadAt(dlg.getPhase())) return true; //Cannot move dead animals to subsequent groups
		
		if(formerGroup==null) {
			return g.getFromGroup()==null;
		} else {
			return formerGroup.equals(g.getFromGroup());
		}
	}
	
	private void refreshScore() {
//		double score = 
		score(dlg.getRandomization().getSamples());
//		scoreLabel.setText("<html>Randomization Score: <b>"+FormatterUtils.format2(score)+"</b>  <i>(lower is better)</i></html>");
	}
	
	public double score(List<AttachedBiosample> list) {
		return  (1-factorSlider.getValue()/10.0) * score(list, true) +  (factorSlider.getValue()/10.0) * score(list, false);
	}
	public double score(List<AttachedBiosample> list, boolean forWeights) {
		int minIndex = forWeights? -1: 0;
		int maxIndex = forWeights? -1: dlg.getRandomization().getNData()-1;
		
		double res = 0;
		for(int index = minIndex; index<=maxIndex; index++) {
			List<Double> doubles2 = AttachedBiosample.getData(list, index);
			Double mRef = StatUtils.getMean(doubles2);
			Double sRef = StatUtils.getStandardDeviation(doubles2, mRef);
			
			List<Integer> groupIds = new ArrayList<Integer>();
			for (Group g : groups) groupIds.add((int)g.getId());
			
			Map<Integer, List<AttachedBiosample>> splits = RandomizationDlg.splitByGroup(list);
			double tot = 0;
			for(Integer groupId: groupIds) {
				List<AttachedBiosample> l = splits.get(groupId);
				if(l==null || l.get(0).getGroup()==null) continue;
				List<Double> doubles = AttachedBiosample.getData(l, index);
				Double m = StatUtils.getMean(doubles);
				if(m==null) continue;
				tot += ((m-mRef) * (m-mRef)) / (mRef>0? mRef*mRef: 1);
	
				if(doubles.size()>=1) {
					Double s = StatUtils.getStandardDeviation(doubles, m);
					if(s!=null && sRef!=null && sRef>0 && s>0) tot += (m/s-mRef/sRef) * (m/s-mRef/sRef);
				}
			}
			res += tot / (maxIndex - minIndex + 1);
		}
		return res;
	}
	
	public List<Group> store(List<AttachedBiosample> samples) {
		List<Group> res = new ArrayList<Group>();
		for (AttachedBiosample s : samples) {
			res.add(s.getGroup());
		}		
		return res;
	}
	
	public void restore(List<AttachedBiosample> samples, List<Group> memo) {
		if(memo.size()!=samples.size()) throw new IllegalArgumentException("Invalid size in samples or memo");
		for (int i = 0; i < samples.size(); i++) {
			samples.get(i).setGroup(memo.get(i));
		}
	}

//	public static void main(String[] args) {
//		System.out.println("Question 1: " + ((Integer) 5 == (Integer) 5));
//		System.out.println("Question 2: " + (new Integer(5)==new Integer(5)));		
//		System.out.println("Question 3: " + (Integer.valueOf(5) == Integer.valueOf(5)));
//		System.out.println("Question 4: " + (double) 1.3f);
//	}
	
}
