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

package com.actelion.research.spiritapp.bioviewer;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ComponentInputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

import com.actelion.research.spiritapp.spirit.ui.IBiosampleTab;
import com.actelion.research.spiritapp.spirit.ui.SpiritFrame;
import com.actelion.research.spiritapp.spirit.ui.SpiritTab;
import com.actelion.research.spiritapp.spirit.ui.biosample.BiosampleActions;
import com.actelion.research.spiritapp.spirit.ui.biosample.BiosampleMetadataPanel;
import com.actelion.research.spiritapp.spirit.ui.biosample.BiosampleOrRackTab;
import com.actelion.research.spiritapp.spirit.ui.biosample.BiosampleTabbedPane;
import com.actelion.research.spiritapp.spirit.ui.util.POIUtils;
import com.actelion.research.spiritapp.spirit.ui.util.POIUtils.ExportMode;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritAction;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeType;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritContextListener;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.BiosampleQuery;
import com.actelion.research.spiritcore.business.biosample.Container;
import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.spiritcore.services.dao.DAOBiosample;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.util.CSVUtils;
import com.actelion.research.util.ui.EasyClipboard;
import com.actelion.research.util.ui.JCustomTextField;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.iconbutton.IconType;

public class BioViewerTab extends SpiritTab implements IBiosampleTab {

	private BiosampleOrRackTab biosampleTab = new BiosampleOrRackTab();
	private BiosampleTabbedPane biosampleDetailPane = new BiosampleTabbedPane();
	private BiosampleMetadataPanel biosampleEditorPane = new BiosampleMetadataPanel();
	private final JTextField scanTextField = new JCustomTextField(12, "", "Scan...");


	private KeyListener keyListener = new KeyAdapter() {
		@Override
		public void keyTyped(KeyEvent e) {
			if ((e.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) > 0 && e.getKeyChar() == 22) {

				// Copy-Paste: don't update the last read
				// startedTyping = 0;

				// Paste Tubes
				String paste = EasyClipboard.getClipboard();
				if (paste == null)
					return;
				StringTokenizer st = new StringTokenizer(paste, " \n\t,");
				while (st.hasMoreTokens()) {
					String token = st.nextToken();
					newScan(token);
				}
				e.consume();
			} else if (e.getKeyChar() == 127) {
				// Delete
				// plateOverview.deleteSelection();
				e.consume();

			} else if ((e.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) == 0) {

				String text = scanTextField.getText();

				// if(text.length()==0) startedTyping =
				// System.currentTimeMillis();

				if (e.getKeyChar() == 8) {
					if (scanTextField.isFocusOwner())
						return;
					if (text.length() > 0)
						text = text.substring(0, text.length() - 1);
				} else if (e.getKeyChar() >= 32 && e.getKeyChar() <= 122) {
					if (scanTextField.isFocusOwner())
						return;
					text += e.getKeyChar();
				} else if (e.getKeyChar() == 10 && text.length() > 0) {
					newScan(text);
					text = "";
				}
				scanTextField.setText(text);
				e.consume();
			}
		}
	};

	public BioViewerTab(SpiritFrame frame) {
		super(frame, "BioViewer", IconType.SCANNER.getIcon());

		// Scan Panel
		scanTextField.enableInputMethods(false);
		scanTextField.addKeyListener(keyListener);
		scanTextField.addFocusListener(new FocusAdapter() {
			@Override
			public void focusGained(FocusEvent e) {
				scanTextField.selectAll();
			}
		});
		scanTextField.addActionListener(e-> {
			newScan(scanTextField.getText());
		});

		JButton b1 = new JButton(new ClearAction());
		JButton b2 = new JButton(new SpiritAction.Action_Scan());
		b1.setText("");
		b2.setText("");

		scanTextField.setMaximumSize(new Dimension(120, 26));
		scanTextField.setPreferredSize(new Dimension(120, 26));

		JPanel north = UIUtils.createHorizontalBox(b1, b2, scanTextField, Box.createHorizontalGlue(), new JButton(new ExportCSVAction()), new JButton(new ExportExcelAction()));
		north.setBorder(BorderFactory.createEtchedBorder());




		// Left Panel
		JPanel listPanel = new JPanel(new BorderLayout());
		listPanel.add(BorderLayout.CENTER, biosampleTab);
		biosampleTab.linkBiosamplePane(biosampleDetailPane);
		biosampleTab.linkBiosamplePane(biosampleEditorPane);

		// Right Panel
		biosampleDetailPane.setSelectedTab(BiosampleTabbedPane.HISTORY_TITLE);

		JPanel detailPanel = new JPanel(new BorderLayout());
		JSplitPane detailPanes = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(biosampleEditorPane), biosampleDetailPane);
		detailPanes.setDividerLocation(360);

		// Listeners
		biosampleTab.getBiosampleTable().resetPreferredColumnWidth();
		biosampleTab.getBiosampleTable().getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete");
		biosampleTab.getBiosampleTable().getActionMap().put("delete", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				List<Biosample> rows = biosampleTab.getBiosamples();
				rows.removeAll(biosampleTab.getBiosampleTable().getSelection());
				biosampleTab.setBiosamples(rows);
			}
		});


		detailPanel.add(BorderLayout.CENTER, detailPanes);

		// CenterPanel
		JSplitPane mainSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, listPanel, detailPanel);
		mainSplitPane.setDividerLocation(340);

		add(BorderLayout.NORTH, north);
		add(BorderLayout.CENTER, mainSplitPane);


		BiosampleActions.attachPopup(biosampleTab.getBiosampleTable());
		addKeyListener(this, keyListener);
		ComponentInputMap inputMap = (ComponentInputMap) getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		inputMap.put(KeyStroke.getKeyStroke("pressed F5"), "refresh");
		inputMap.put(KeyStroke.getKeyStroke("ctrl released A"), "selectAll");

		setInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW, inputMap);
		getActionMap().put("refresh", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				refresh();
			}
		});
		getActionMap().put("selectAll", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				biosampleTab.getBiosampleTable().selectAll();
				selectionChanged();
			}
		});

	}


	private void refresh() {
		List<Biosample> before = new ArrayList<>(biosampleTab.getBiosamples());
		List<Biosample> sel = biosampleTab.getBiosampleTable().getSelection();
		biosampleTab.clear();
		JPAUtil.closeFactory();
		List<Biosample> reloaded = JPAUtil.reattach(before);
		biosampleTab.setBiosamples(reloaded);
		biosampleTab.setSelectedBiosamples(sel);
		selectionChanged();
	}

	private void selectionChanged() {
		Collection<Biosample> selBiosamples = biosampleTab.getSelection(Biosample.class);
		List<Container> selContainers = Biosample.getContainers(selBiosamples);
		SpiritContextListener.setStatus(selContainers.size() + "/" + biosampleTab.getBiosampleTable().getRowCount() + " Biosamples");
		Collection<Biosample> highlighted = biosampleTab.getBiosampleTable().getHighlightedSamples();
		biosampleEditorPane.setBiosamples(highlighted.size()==1? highlighted: null);
		biosampleDetailPane.setBiosamples(highlighted.size()==1? highlighted: null, false);
	}

	private void newScan(String sampleId) {
		if(sampleId==null) return;
		if(sampleId.length()==0) return;
		try {
			List<Biosample> biosamples = DAOBiosample.queryBiosamples(BiosampleQuery.createQueryForSampleIdOrContainerIds(sampleId), null);
			if(biosamples.size()==0) {
				newScan(Collections.singletonList(new Container(sampleId)));
			} else {
				newScan(Biosample.getContainers(biosamples, true));
			}
		} catch(Exception e) {
			JExceptionDialog.showError(this, e);
		}
	}

	/**
	 * NewScan with loaded containers
	 * @param containers
	 */
	private void newScan(final Collection<Container> containers) {
		scanTextField.requestFocusInWindow();
		scanTextField.selectAll();

		//				Map<String, Container> id2Container = new HashMap<String, Container>();
		//				for (Container c : Biosample.getContainers(biosamples, true)) {
		//					for(Biosample b: c.getBiosamples()) {
		//						id2Container.put(b.getSampleId(), c);
		//					}
		//					if(c.getContainerId()!=null) {
		//						id2Container.put(c.getContainerId(), c);
		//					}
		//				}
		//
		//				//Add Unknown for not found tubes
		//				for (ScannedTube scannedTube : scannedTubes) {
		//					Container c = id2Container.get(scannedTube.getTubeId());
		//					if(c==null) {
		//						c = new Container(scannedTube.getTubeId());
		//						Biosample notFound = new Biosample("NOTFOUND");
		//						notFound.setContainer(c);
		//						biosamples.add(notFound);
		//					}
		//					if(scannedTube.getPosition()!=null) c.setScannedPosition(scannedTube.getPosition());
		//				}

		//Filter only the new ones
		List<Biosample> biosamples = Container.getBiosamples(containers, true);
		List<Biosample> toAdd = new ArrayList<Biosample>(biosamples);
		toAdd.removeAll(biosampleTab.getBiosamples());

		if(toAdd.size()>0) {
			//Update the new rows
			List<Biosample> rows = new ArrayList<Biosample>(biosampleTab.getBiosamples());
			rows.addAll(toAdd);
			biosampleTab.setBiosamples(rows);
		}
		biosampleTab.setSelectedBiosamples(biosamples);
		selectionChanged();
		Toolkit.getDefaultToolkit().beep();
		//			}
		//		};
	}


	private class ClearAction extends AbstractAction {
		public ClearAction() {
			super("Clear", IconType.CLEAR.getIcon());
			putValue(AbstractAction.SHORT_DESCRIPTION, "Clear List");
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			scanTextField.setText("");
			biosampleTab.clear();
			selectionChanged();
		}
	}

	private class ExportCSVAction extends AbstractAction {
		public ExportCSVAction() {
			super("Export to CSV", IconType.CSV.getIcon());
			putValue(AbstractAction.SHORT_DESCRIPTION, "Export to Excel");
		}
		@Override
		public void actionPerformed(ActionEvent arg0) {
			try {
				CSVUtils.exportToCsv(biosampleTab.getBiosampleTable().getTabDelimitedTable());
			} catch (Exception e) {
				JExceptionDialog.showError(e);
			}
		}
	}

	private class ExportExcelAction extends AbstractAction {
		public ExportExcelAction() {
			super("Export to Excel", IconType.EXCEL.getIcon());
			putValue(AbstractAction.SHORT_DESCRIPTION, "Export to Excel");
		}
		@Override
		public void actionPerformed(ActionEvent arg0) {
			try {
				POIUtils.exportToExcel(biosampleTab.getBiosampleTable().getTabDelimitedTable(), ExportMode.HEADERS_TOP);
			} catch (Exception e) {
				JExceptionDialog.showError(e);
			}
		}
	}

	public static void addKeyListener(Component c, KeyListener kl) {
		boolean hasListenerAlready = false;
		for (KeyListener l : c.getKeyListeners()) {
			if (l == kl)
				hasListenerAlready = true;
		}
		if (!hasListenerAlready)
			c.addKeyListener(kl);

		if (c instanceof java.awt.Container) {
			for (Component child : ((java.awt.Container) c).getComponents()) {
				addKeyListener(child, kl);
			}
		}
	}
	@SuppressWarnings("unchecked")
	@Override
	public<T> void fireModelChanged(SpiritChangeType action, Class<T> what, List<T> details) {
		if(!isShowing()) return;
		if(what==Biosample.class) {
			List<Biosample> biosamples = (List<Biosample>) details;
			Biosample.clearAuxInfos(biosamples);
			if(action==SpiritChangeType.MODEL_ADDED) {
				biosampleTab.setBiosamples(biosamples);
				biosampleTab.setSelectedBiosamples(biosamples);
			} else if(action==SpiritChangeType.MODEL_UPDATED) {
				if(biosampleTab.getBiosamples().size()>200 || !biosampleTab.getBiosamples().containsAll(biosamples)) {
					//The table is too big or some edited or some samples are contained in the table, sets only the biosamples that were edited
					biosampleTab.setBiosamples(JPAUtil.reattach(biosamples));
					System.out.println("BioViewer.actionModelChanged()1");
				} else {
					//Refresh the table
					biosampleTab.setBiosamples(JPAUtil.reattach(biosampleTab.getBiosamples()));
					System.out.println("BioViewer.actionModelChanged()2");
				}
				biosampleTab.setSelectedBiosamples(biosamples);
			} else if(action==SpiritChangeType.MODEL_DELETED) {
				List<Biosample> rows = biosampleTab.getBiosamples();
				rows.removeAll(biosamples);
				biosampleTab.setBiosamples(JPAUtil.reattach(rows));
			}
		}
		repaint();
	}


	@Override
	public void setRack(Location rack) {
		biosampleTab.setRack(rack);
	}


	@Override
	public void setBiosamples(List<Biosample> biosamples) {
		biosampleTab.setBiosamples(biosamples);
	}


	@Override
	public void setSelectedBiosamples(List<Biosample> biosamples) {
		biosampleTab.setSelectedBiosamples(biosamples);
	}


	@Override
	public List<Biosample> getBiosamples() {
		return biosampleTab.getBiosamples();
	}


	@Override
	public void onTabSelect() {
	}

	@Override
	public void onStudySelect() {
	}

}
