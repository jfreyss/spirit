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

package com.actelion.research.spiritapp.ui.biosample;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.actelion.research.spiritapp.ui.SpiritFrame;
import com.actelion.research.spiritapp.ui.biosample.edit.EditBiosampleDlg;
import com.actelion.research.spiritapp.ui.util.component.JHeaderLabel;
import com.actelion.research.spiritapp.ui.util.formtree.FormTree;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.BiosampleQuery;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.biosample.BiotypeCategory;
import com.actelion.research.spiritcore.services.dao.DAOBiosample;
import com.actelion.research.util.ui.JEscapeDialog;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.SwingWorkerExtended;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.exceltable.JSplitPaneWithZeroSizeDivider;
import com.actelion.research.util.ui.iconbutton.IconType;
import com.actelion.research.util.ui.iconbutton.JIconButton;

/**
 * Simple dialog to query for biosamples and return the selection Modal Mode:
 * Biosample res = new BiosampleFinder(null, type, true,
 * selection).showOpenDialog();
 *
 * Non Modal: new BiosampleFinder(null, type, true, selection) {
 * onSelect(Biosample b) {} }.setVisible(true)
 *
 * @author J.Freyss
 */
public abstract class BiosampleFinder extends JEscapeDialog {

	private BiosampleTable table = new BiosampleTable();
	private List<Biosample> choices = null;
	private Biosample currentSelection;
	private BiosampleTabbedPane detailPane = new BiosampleTabbedPane();
	private JLabel statusLabel = new JLabel();

	private BiosampleSearchTree searchTree;
	private final JPanel centerPane;
	private final JSplitPane westPane;

	public BiosampleFinder(JDialog top, String title, String text, BiosampleQuery query, List<Biosample> choices, final Biosample currentSelection, boolean canCreate) {
		super(top, title, top == null);
		this.choices = choices;
		this.currentSelection = currentSelection;

		// Non Modal -> close on lost focus
		addWindowFocusListener(new WindowFocusListener() {
			@Override
			public void windowGainedFocus(WindowEvent e) {
			}

			@Override
			public void windowLostFocus(WindowEvent e) {
				dispose();
			}
		});

		searchTree = new BiosampleSearchTree(null, null, true);
		searchTree.setQuery(query==null? new BiosampleQuery(): query);
		searchTree.addPropertyChangeListener(FormTree.PROPERTY_SUBMIT_PERFORMED, evt-> {
			search();
		});

		final JButton searchButton = new JIconButton(IconType.SEARCH, "Search");
		final JButton okButton = new JIconButton(IconType.BIOSAMPLE, "Select");
		okButton.setEnabled(false);

		JPanel box;
		if (canCreate && choices==null) {
			final JButton newButton = new JIconButton(IconType.NEW, "Create New");
			newButton.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					Biosample toCreate = new Biosample();
					Biotype biotype = searchTree.getQuery().getBiotype();
					if (biotype != null) {
						toCreate.setBiotype(biotype);
					}
					List<Biosample> list = new ArrayList<Biosample>();
					list.add(toCreate);
					try {
						EditBiosampleDlg dlg = EditBiosampleDlg.createDialogForEditInTransactionMode(list);
						dlg.setVisible(true);
						list = dlg.getSaved();
						if (list.size() == 1) {
							onSelect(list.get(0));
						} else {
							table.setRows(list);
						}
					} catch (Exception ex) {
						JExceptionDialog.showError(ex);
					}
				}
			});

			box = UIUtils.createHorizontalBox(Box.createHorizontalGlue(), newButton, searchButton);
		} else {
			box = UIUtils.createHorizontalBox(Box.createHorizontalGlue(), searchButton);
		}
		box.setBackground(Color.WHITE);
		box.setOpaque(true);

		JPanel filtersPanel = new JPanel(new BorderLayout());
		filtersPanel.add(BorderLayout.CENTER, new JScrollPane(searchTree));
		filtersPanel.add(BorderLayout.SOUTH, box);
		filtersPanel.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));

		westPane = new JSplitPaneWithZeroSizeDivider(JSplitPane.VERTICAL_SPLIT, filtersPanel, detailPane);
		westPane.setDividerLocation(1200);
		westPane.setBorder(BorderFactory.createLineBorder(Color.BLACK));

		centerPane = new JPanel(new BorderLayout());
		centerPane.add(BorderLayout.CENTER, new JScrollPane(table));
		centerPane.add(BorderLayout.SOUTH, UIUtils.createHorizontalBox(statusLabel, Box.createHorizontalGlue(), okButton));

		JSplitPane splitPane2 = new JSplitPaneWithZeroSizeDivider(JSplitPane.HORIZONTAL_SPLIT, westPane, centerPane);
		splitPane2.setDividerLocation(300);
		splitPane2.setBorder(BorderFactory.createLineBorder(Color.BLACK));

		JPanel contentPanel = new JPanel(new BorderLayout());
		if (text != null) {
			contentPanel.add(BorderLayout.NORTH, new JHeaderLabel(text));
		}
		contentPanel.add(BorderLayout.CENTER, splitPane2);

		setContentPane(contentPanel);

		searchButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				search();
			}
		});

		okButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				List<Biosample> l = table.getSelection();
				onSelect(l.size() == 1 ? l.get(0) : null);
			}
		});

		table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if(e.getValueIsAdjusting()) return;

				List<Biosample> l = table.getSelection();

				if(l.size()==1) {
					detailPane.setBiosamples(l);
					if(westPane.getDividerLocation()>westPane.getHeight()-20) westPane.setDividerLocation(480);
				} else {
					detailPane.setBiosamples(null);
					if(westPane.getDividerLocation()<westPane.getHeight()-20) westPane.setDividerLocation(westPane.getHeight());
				}

				okButton.setEnabled(l.size()==1);
			}
		});
		table.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() >= 2 && table.getSelection().size()==1) {
					Biosample b = table.getSelection().get(0);
					if(table.getModel().getTreeColumn()!=null) {
						int treeIndex = table.getModel().getColumns().indexOf(table.getModel().getTreeColumn());
						if(treeIndex<0) return;
						Rectangle rect = table.getCellRect(table.getSelectedRow(), treeIndex, true);
						if(e.getX()>=rect.x && e.getX()<rect.x+22) {
							//Expand
							return;
						}
					}
					onSelect(b);

				}
			}
		});
		java.awt.Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		if (top == null) {
			setSize(dim.width - 100, dim.height - 100);
		} else {
			setSize(Math.min(1350, dim.width - 100), Math.min(dim.height - 100, 800));
		}

		getRootPane().setDefaultButton(searchButton);
		setLocationRelativeTo(UIUtils.getMainFrame());

		// Init results
		if(choices!=null) {
			filtersPanel.setVisible(false);
			search();
		} else if (query.getBiotype()!=null && (query.getBiotype().getCategory() == BiotypeCategory.LIBRARY || query.getBiotype().getCategory() == BiotypeCategory.PURIFIED)) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					search();
				}
			});
		} else if (currentSelection != null) {
			table.setRows(Collections.singletonList(currentSelection));
			table.setSelection(Collections.singletonList(currentSelection));
			table.scrollTo(currentSelection);
		}
	}

	private void search() {
		if(choices!=null) {
			table.setRows(choices);
			statusLabel.setText(choices.size() + " biosamples");
			if (currentSelection != null) {
				table.setSelection(Collections.singletonList(currentSelection));
				table.scrollTo(currentSelection);
			}
		} else {
			final BiosampleQuery q = searchTree.getQuery();
			if (q.isEmpty())  return;


			new SwingWorkerExtended("Loading Biosamples", centerPane) {
				private List<Biosample> biosamples;

				@Override
				protected void doInBackground() throws Exception {

					biosamples = DAOBiosample.queryBiosamples(q, SpiritFrame.getUser());
					Collections.sort(biosamples);
				}

				@Override
				protected void done() {
					table.setRows(biosamples);
					statusLabel.setText(biosamples.size() + " biosamples");
					if (currentSelection != null) {
						table.setSelection(Collections.singletonList(currentSelection));
						table.scrollTo(currentSelection);
					}
				}

			};
		}
	}

	/**
	 * Must be overidden for custom behaviours on select. Don't forget to call
	 * dispose
	 *
	 * @param sel
	 */
	public abstract void onSelect(Biosample sel);

}