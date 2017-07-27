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

package com.actelion.research.spiritapp.spirit.ui.biosample.selector;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.UIManager;

import com.actelion.research.spiritapp.spirit.ui.biosample.BiosampleActions;
import com.actelion.research.spiritapp.spirit.ui.biosample.BiosampleTable;
import com.actelion.research.spiritapp.spirit.ui.util.POIUtils;
import com.actelion.research.spiritapp.spirit.ui.util.POIUtils.ExportMode;
import com.actelion.research.spiritapp.spirit.ui.util.editor.ImageEditorPane;
import com.actelion.research.spiritapp.spirit.ui.util.lf.LF;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.BiosampleLinker;
import com.actelion.research.spiritcore.business.biosample.BiosampleLinker.LinkerMethod;
import com.actelion.research.spiritcore.business.biosample.BiosampleLinker.LinkerType;
import com.actelion.research.spiritcore.business.biosample.BiosampleQuery;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.biosample.BiotypeMetadata;
import com.actelion.research.spiritcore.services.dao.DAOBiosample;
import com.actelion.research.spiritcore.services.dao.DAOBiotype;
import com.actelion.research.spiritcore.util.ListHashMap;
import com.actelion.research.util.ui.JEscapeDialog;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.JGenericComboBox;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.iconbutton.IconType;
import com.actelion.research.util.ui.iconbutton.JIconButton;

public class SelectorDlg extends JEscapeDialog {

	private Biotype biotype;
	private List<Biosample> pool;

	//topPanel
	private JGenericComboBox<BiosampleLinker> queryOnComboBox;
	private JGenericComboBox<BiosampleLinker> discriminatorComboBox;
	private JGenericComboBox<BiosampleLinker> displayComboBox;

	private JEditorPane editorPane = new ImageEditorPane();

	//selectorPanel
	private final JPanel selectorPanel = new JPanel();
	private List<SelectorPanel> selectorPanels = new ArrayList<SelectorPanel>();
	private JButton excelButton = new JIconButton(IconType.EXCEL, "Export to Excel");

	//resultPanel
	private final BiosampleTable biosampleTable = new BiosampleTable();


	public SelectorDlg(List<Biosample> pool) {
		this(pool, null, null, null);
	}

	public SelectorDlg(List<Biosample> pool, BiosampleLinker querySel, BiosampleLinker discriminatorSel, BiosampleLinker displaySel) {
		super(UIUtils.getMainFrame(), "Help me select some biosamples");

		LF.initComp(editorPane);

		//Validate input
		try {
			if(pool.size()<2) throw new Exception("You must select a pool of biosamples to pick from");
			this.pool = pool;

			Set<Biotype> biotypes = Biosample.getBiotypes(pool);
			if(biotypes.size()!=1)  throw new Exception("The biosamples must all have the same biotype");
			biotype = biotypes.iterator().next();
		} catch(Exception e) {
			JExceptionDialog.showError(e);
			return;
		}

		//Create components
		Set<BiosampleLinker> directLinkers = BiosampleLinker.getLinkers(pool, LinkerMethod.DIRECT_LINKS);
		Set<BiosampleLinker> indirectLinkers = BiosampleLinker.getLinkers(pool, LinkerMethod.ALL_LINKS);
		queryOnComboBox = new JGenericComboBox<BiosampleLinker>(directLinkers, false);
		queryOnComboBox.setPreferredWidth(220);

		discriminatorComboBox = new JGenericComboBox<BiosampleLinker>(indirectLinkers, true);
		discriminatorComboBox.setPreferredWidth(220);

		displayComboBox = new JGenericComboBox<BiosampleLinker>(indirectLinkers, true);
		displayComboBox.setPreferredWidth(220);

		if(querySel!=null) queryOnComboBox.setSelection(querySel);
		if(discriminatorSel!=null) discriminatorComboBox.setSelection(discriminatorSel);
		if(displaySel!=null) displayComboBox.setSelection(displaySel);

		queryOnComboBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				reset();
			}
		});
		discriminatorComboBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				updatePanelLayout();
			}
		});
		displayComboBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				updatePanelLayout();
			}
		});

		//northWestPanel
		JPanel northWestPanel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.WEST;
		c.weightx = 0;
		c.gridx = 0; c.gridy = 1; northWestPanel.add(new JLabel("Query on: "), c);
		c.gridx = 0; c.gridy = 2; northWestPanel.add(new JLabel("Discriminator: "), c);
		c.gridx = 0; c.gridy = 3; northWestPanel.add(new JLabel("Display: "), c);

		c.weightx = 1;
		c.gridx = 1; c.gridy = 1; northWestPanel.add(queryOnComboBox, c);
		c.gridx = 1; c.gridy = 2; northWestPanel.add(discriminatorComboBox, c);
		c.gridx = 1; c.gridy = 3; northWestPanel.add(displayComboBox, c);

		c.gridwidth = 2;

		//northEastPanel
		JPanel northEastPanel = new JPanel(new BorderLayout());
		northEastPanel.add(BorderLayout.CENTER, new JScrollPane(editorPane));
		editorPane.setEditable(false);


		//northPanel
		JPanel northPanel = new JPanel(new GridBagLayout());
		c.gridwidth = 1; c.fill = GridBagConstraints.BOTH;
		if(getQueryLinker()==null || getDiscriminatorLinker()==null) {
			c.weightx = 0; c.gridx = 0; c.gridy = 1; northPanel.add(northWestPanel, c);
		}
		c.weightx = 1; c.gridx = 1; c.gridy = 1; northPanel.add(northEastPanel, c);


		//splitPane
		JPanel splitPaneNorth = new JPanel(new BorderLayout());
		splitPaneNorth.add(BorderLayout.NORTH, northPanel);
		splitPaneNorth.add(BorderLayout.CENTER, UIUtils.createTitleBox("Query", new JScrollPane(selectorPanel)));

		BiosampleActions.attachPopup(biosampleTable);

		JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, splitPaneNorth, UIUtils.createTitleBox("Selection", new JScrollPane(biosampleTable)));
		splitPane.setDividerLocation(460);
		splitPane.setPreferredSize(new Dimension(1050, 750));

		//button
		excelButton.addActionListener(e-> {
			try {
				POIUtils.exportToExcel(biosampleTable.getTabDelimitedTable(), ExportMode.HEADERS_TOP);

			} catch(Exception ex) {
				JExceptionDialog.showError(ex);
			}
		});

		//ContentPane
		JPanel contentPane = new JPanel(new BorderLayout());
		contentPane.add(BorderLayout.CENTER, splitPane);
		contentPane.add(BorderLayout.SOUTH, UIUtils.createHorizontalBox(Box.createHorizontalGlue(), excelButton));

		//Init
		reset();
		updatePanelLayout();


		setContentPane(contentPane);
		pack();
		setLocationRelativeTo(UIUtils.getMainFrame());
		setVisible(true);



	}

	/**
	 * Reset all the panels
	 */
	private void reset() {
		selectorPanels.clear();
		selectorPanels.add(new SelectorPanel(this));

		updatePanelLayout();
		selectionQueryChanged();
		selectionBiosampleChanged();
		selectorPanel.validate();
	}

	/**
	 * Update layout of the selectorPanels
	 */
	private void updatePanelLayout() {
		Component comp = getFocusOwner();
		selectorPanel.removeAll();
		selectorPanel.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.gridy = 0;
		c.gridx = GridBagConstraints.RELATIVE;
		c.weightx = 0;
		c.weighty = 1;

		//Add SelectorPanels
		for (SelectorPanel sp : selectorPanels) {
			selectorPanel.add(sp, c);
			sp.updateView();
		}
		c.weightx = 1;
		selectorPanel.add(new JLabel(), c);

		validate();

		if(comp!=null) comp.requestFocusInWindow();
	}

	public List<Biosample> getPool() {
		return pool;
	}
	public BiosampleLinker getQueryLinker() {
		return queryOnComboBox.getSelection();
	}
	public BiosampleLinker getDiscriminatorLinker() {
		return discriminatorComboBox.getSelection();
	}
	public BiosampleLinker getDisplayLinker() {
		return displayComboBox.getSelection()!=null? displayComboBox.getSelection(): discriminatorComboBox.getSelection();
	}

	public List<String> getQueryValues() {
		List<String> res = new ArrayList<String>();
		if(getQueryLinker()!=null) {
			for(Biosample b: pool) {
				res.add(getQueryLinker().getValue(b));
			}
		}
		return res;
	}

	public List<Biosample> getBiosamples(String queryValue) {
		List<Biosample> res = new ArrayList<Biosample>();
		if(getQueryLinker()!=null) {
			for(Biosample b: pool) {
				if(queryValue==null || queryValue.equals(getQueryLinker().getValue(b))) {
					res.add(b);
				}
			}
		}
		return res;
	}

	private Set<String> getUsedDiscriminators(){
		BiosampleLinker linker2 = getDiscriminatorLinker();
		Set<String> used = new TreeSet<String>();
		if(linker2!=null) {
			for (Biosample b : getSelectedBiosamples()) {
				String val = linker2.getValue(b);
				if(val!=null && val.length()>0) used.add(val);
			}
		}
		return used;
	}

	private void updateDiscriminatorsPanel() {
		StringBuilder sb = new StringBuilder();
		sb.append("<html><div style='font-size:10px'><b>" + pool.size() + " " + biotype.getName()+"</b>: ");

		BiosampleLinker linker1 = getQueryLinker();
		if(linker1!=null) {
			Set<String> allQuerable = new TreeSet<String>();
			for(Biosample b: pool) {
				String val = linker1.getValue(b);
				if(val!=null && val.length()>0) allQuerable.add(val);
			}
			sb.append(allQuerable.size() + " " + linker1.getLabelShort());


			BiosampleLinker linker2 = getDiscriminatorLinker();
			if(linker2!=null) {
				Set<String> allDiscriminators = new TreeSet<String>();
				for(Biosample b: pool) {
					String val = linker2.getValue(b);
					if(val!=null && val.length()>0) allDiscriminators.add(val);
				}
				Set<String> used = getUsedDiscriminators();

				sb.append(" and " + allDiscriminators.size() + " " + linker2.getLabelShort()+":");
				sb.append("<div style='padding-left:20px;font-size:9px'>");
				boolean first = true;
				for (String string : allDiscriminators) {
					if(first) first = false;
					else sb.append(", ");
					sb.append("<span style='color:" + (used.contains(string)?"#CCCCCC": "#000066") + "'>" + string + "</span>");
				}
				sb.append("</div>");
			}
		}
		sb.append("</div>");

		editorPane.setText(sb.toString());


	}

	/**
	 * Return the selected biosamples (including nulls)
	 * @return a list [0->sample of 1st panel, 1->sample of 2nd panel, ...]
	 */
	public List<Biosample> getSelectedBiosamples() {
		List<Biosample> res = new ArrayList<Biosample>();
		for (SelectorPanel panel : selectorPanels) {
			res.add(panel.getSelection());
		}
		return res;
	}

	/**
	 * Return the selected String (including nulls)
	 * @return a list [0->query of 1st panel, 1->query of 2nd panel, ...]
	 */
	public List<String> getSelectedQueries() {
		List<String> res = new ArrayList<String>();
		for (SelectorPanel panel : selectorPanels) {
			res.add(panel.getQuery());
		}
		return res;
	}


	public boolean isGray(SelectorPanel panel, Biosample biosample) {
		BiosampleLinker linker = getDiscriminatorLinker();
		if(linker==null) return false;
		String linkedValue = linker.getValue(biosample);
		if(linkedValue==null) return true;

		for (int i = 0; i < selectorPanels.size(); i++) {
			SelectorPanel p = selectorPanels.get(i);
			if(p==panel) continue;
			Biosample sel = p.getSelection();
			if(sel==null) continue;
			String value = linker.getValue(sel);
			if(linkedValue.equals(value)) return true;
		}
		return false;
	}

	/**
	 * To be called when one of the queryItem changes.
	 * Used to add a panel if all are taken, to remove those that are empty and to gray out items
	 */
	public void selectionQueryChanged() {
		boolean changed = false;
		for (int i = 0; i < selectorPanels.size(); i++) {
			SelectorPanel sp = selectorPanels.get(i);
			if(i<selectorPanels.size()-1) {
				if(sp.getQuery().length()==0) {
					selectorPanels.remove(sp);
					changed = true;
					i--;
				}
			} else {
				if(sp.getQuery().length()>0) {
					selectorPanels.add(new SelectorPanel(SelectorDlg.this));
					changed = true;
				}
			}
		}
		if(changed) {
			updatePanelLayout();
		}
	}

	/**
	 * To be called when one of the selected Biosample changes
	 */
	public void selectionBiosampleChanged() {
		List<Biosample> sel = new ArrayList<Biosample>();
		for (Biosample b : getSelectedBiosamples()) {
			if(b!=null) sel.add(b);
		}
		biosampleTable.setRows(sel);
		excelButton.setEnabled(sel.size()>0);
		updateDiscriminatorsPanel();

		//Calculates possible choices
		BiosampleLinker l1 = getQueryLinker();
		BiosampleLinker l2 = getDiscriminatorLinker();
		canBeChosen.clear();
		if(l1!=null && l2!=null) {
			ListHashMap<String, String> query2Discriminators = new ListHashMap<String, String>();
			for (Biosample b : pool) {
				String s1 = l1.getValue(b);
				String s2 = l2.getValue(b);

				if(s1!=null && s1.length()>0 && s2!=null && s2.length()>0) query2Discriminators.add(s1, s2);
			}

			Set<String> selectedQueries = new HashSet<String>(getSelectedQueries());

			Set<String> used = getUsedDiscriminators();


			loop: for (String q : query2Discriminators.keySet()) {
				if(selectedQueries.contains(q)) {
					//ok
				} else {
					for (String d : query2Discriminators.get(q)) {
						if(!used.contains(d)) {
							canBeChosen.add(q);
							continue loop;
						}
					}
				}
			}
		}

		repaint();

	}


	private Set<String> canBeChosen = new HashSet<String>();
	public boolean isCompatibleWithSelected(String query) {
		return canBeChosen.contains(query);
	}


	public static void main(String[] args) throws Exception {

		try {
			UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
			UIManager.put("nimbusSelectionBackground", new Color(173,207,231));
		} catch (Exception e) {
			e.printStackTrace();
		}


		BiosampleQuery q = new BiosampleQuery();
		q.setBiotype(DAOBiotype.getBiotype("Antibody"));
		List<Biosample> res = DAOBiosample.queryBiosamples(q, null);
		//		DAOBiosample.fullLoad(res);


		Biotype antibodyType = DAOBiotype.getBiotype("Antibody");
		BiotypeMetadata antibodyAggMetadata = antibodyType.getMetadata("Fluorophore");
		if(antibodyAggMetadata==null) throw new Exception("Antibody.Fluorophore does not exist");

		Biotype fluorophoreType = DAOBiotype.getBiotype("Fluorophore");
		if(fluorophoreType==null) throw new Exception("Fluorophore does not exist");

		BiosampleLinker querySel = new BiosampleLinker(LinkerType.SAMPLENAME, antibodyType);
		BiosampleLinker discrimSel = new BiosampleLinker(antibodyAggMetadata, fluorophoreType.getMetadata("Type"));
		BiosampleLinker displaySel = new BiosampleLinker(antibodyAggMetadata, LinkerType.SAMPLEID);

		new SelectorDlg(res, querySel, discrimSel, displaySel);
	}
}
