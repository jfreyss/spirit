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
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.net.URL;
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
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

import com.actelion.research.spiritapp.bioviewer.ui.batchaliquot.BatchAliquotDlg;
import com.actelion.research.spiritapp.bioviewer.ui.batchassign.BatchAssignDlg;
import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritapp.spirit.SpiritMenu;
import com.actelion.research.spiritapp.spirit.ui.SpiritAction;
import com.actelion.research.spiritapp.spirit.ui.biosample.BiosampleActions;
import com.actelion.research.spiritapp.spirit.ui.biosample.BiosampleMetadataPanel;
import com.actelion.research.spiritapp.spirit.ui.biosample.BiosampleOrRackTab;
import com.actelion.research.spiritapp.spirit.ui.biosample.BiosampleTabbedPane;
import com.actelion.research.spiritapp.spirit.ui.result.ResultActions;
import com.actelion.research.spiritapp.spirit.ui.scanner.SpiritScanner;
import com.actelion.research.spiritapp.spirit.ui.util.ISpiritChangeObserver;
import com.actelion.research.spiritapp.spirit.ui.util.ISpiritContextObserver;
import com.actelion.research.spiritapp.spirit.ui.util.POIUtils;
import com.actelion.research.spiritapp.spirit.ui.util.POIUtils.ExportMode;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeListener;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeType;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritContextListener;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.BiosampleQuery;
import com.actelion.research.spiritcore.business.biosample.Container;
import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.spiritcore.business.pivot.PivotTemplate;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.ResultQuery;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.dao.DAOBiosample;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.spiritcore.util.CSVUtils;
import com.actelion.research.util.ui.EasyClipboard;
import com.actelion.research.util.ui.JCustomTextField;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.JStatusBar;
import com.actelion.research.util.ui.SplashScreen2;
import com.actelion.research.util.ui.SplashScreen2.SplashConfig;
import com.actelion.research.util.ui.SwingWorkerExtended;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.iconbutton.JIconButton;
import com.actelion.research.util.ui.iconbutton.JIconButton.IconType;

public class BioViewer extends JFrame implements ISpiritChangeObserver, ISpiritContextObserver {

	private static SplashConfig splashConfig = new SplashConfig(BioViewer.class.getResource("bioviewer.jpg"), "BioViewer", "BioViewer v" + Spirit.class.getPackage().getImplementationVersion() + "<br> (C) Actelion - J.Freyss");
	
	private BiosampleOrRackTab biosampleTab = new BiosampleOrRackTab();
	private BiosampleTabbedPane biosampleDetailPane = new BiosampleTabbedPane();
	private BiosampleMetadataPanel biosampleEditorPane = new BiosampleMetadataPanel();
	private final JTextField scanTextField = new JCustomTextField(12, "", "Scan...");
	private final JStatusBar statusBar = new JStatusBar();

	public BioViewer() {
		super("BioViewer");

		SpiritContextListener.register(this);
		SpiritChangeListener.register(this);
		
		URL url = getClass().getResource("ico.png");
		if (url != null)
			setIconImage(Toolkit.getDefaultToolkit().createImage(url));

		// Scan Panel
		scanTextField.enableInputMethods(false);
		scanTextField.addKeyListener(keyListener);
		scanTextField.addFocusListener(new FocusAdapter() {
			@Override
			public void focusGained(FocusEvent e) {
				scanTextField.selectAll();
			}
		});
		scanTextField.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				newScan(scanTextField.getText());
			}
		});

		JButton b1 = new JButton(new ClearAction());
		JButton b2 = new JButton(new Action_Scanner());
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
		biosampleDetailPane.setSelectedTab(BiosampleTabbedPane.HIERARCHY_TITLE);

		JPanel detailPanel = new JPanel(new BorderLayout());
		JSplitPane detailPanes = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(biosampleEditorPane), biosampleDetailPane);
		detailPanel.add(BorderLayout.CENTER, detailPanes);

		// CenterPanel
		JSplitPane mainSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, listPanel, detailPanel);
		mainSplitPane.setDividerLocation(340);

		JPanel centerPanel = new JPanel(new BorderLayout());
		centerPanel.add(BorderLayout.NORTH, north);
		centerPanel.add(BorderLayout.CENTER, mainSplitPane);

		// ContentPane
		statusBar.setCopyright("BioViewer - (C) Joel Freyss - Actelion 2012");
		statusBar.setBorder(BorderFactory.createLoweredBevelBorder());
		JPanel contentPanel = new JPanel(new BorderLayout());
		contentPanel.add(BorderLayout.CENTER, centerPanel);
		contentPanel.add(BorderLayout.SOUTH, statusBar);
		setContentPane(contentPanel);

		createMenu();

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


		BiosampleActions.attachPopup(biosampleTab.getBiosampleTable());
		addKeyListener(this, keyListener);
		ComponentInputMap inputMap = (ComponentInputMap) contentPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		inputMap.put(KeyStroke.getKeyStroke("pressed F5"), "refresh");
		inputMap.put(KeyStroke.getKeyStroke("ctrl released A"), "selectAll"); 

		contentPanel.setInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW, inputMap);
		contentPanel.getActionMap().put("refresh", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				refresh();
			}
		});
		contentPanel.getActionMap().put("selectAll", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				biosampleTab.getBiosampleTable().selectAll();
				selectionChanged();
			}
		});

		SpiritChangeListener.register(this);
		
		
		UIUtils.adaptSize(this, 1600, 1200);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setVisible(true);

		
//		if(System.getProperty("user.name").equals("freyssj")) {
//			try {
//				SpiritUser user = DAOSpiritUser.loadUser("freyssj");
//				if(user==null) throw new Exception("Could not load user freyssj");
//				Spirit.setUser(user);
//				
//			} catch (Exception e) {
//				System.err.println(e);
//			}
//		} 
		
		//Login
		if(Spirit.getUser()==null) {
			eventUserChanged();
		}		

		toFront();
	}

	private void refresh() {
		List<Biosample> before = new ArrayList<Biosample>(biosampleTab.getBiosamples());
		List<Biosample> sel = biosampleTab.getBiosampleTable().getSelection();
		biosampleTab.clear();
		JPAUtil.close();
		List<Biosample> reloaded = JPAUtil.reattach(before);
		biosampleTab.setBiosamples(reloaded);
		biosampleTab.setSelectedBiosamples(sel);
		selectionChanged();
	}
	
	final AbstractAction LOGIN_ACTION = new SpiritAction.Action_Relogin(this, "BioViewer");
	
	public void eventUserChanged() {
		LOGIN_ACTION.actionPerformed(null);
	}

	public void createMenu() {
		final JMenuBar menuBar = new JMenuBar();

		JMenu edit = new JMenu("Edit");
		edit.setMnemonic('e');
		menuBar.add(edit);
		JMenuItem selectAll = new JMenuItem("Select All");
		selectAll.setAccelerator(KeyStroke.getKeyStroke("ctrl released A"));
		selectAll.setMnemonic('a');
		selectAll.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				biosampleTab.getBiosampleTable().selectAll();
				selectionChanged();
			}
		});
		edit.add(selectAll);

		JMenuItem clearAll = new JMenuItem(new ClearAction());
		clearAll.setMnemonic('c');
		edit.add(clearAll);
		JMenuItem refreshAll = new JMenuItem("Refresh", IconType.REFRESH.getIcon());
		refreshAll.setMnemonic('r');
		refreshAll.setAccelerator(KeyStroke.getKeyStroke("F5"));
		refreshAll.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				refresh();
			}
		});
		edit.add(refreshAll);
		edit.add(new JSeparator());
		edit.add(new BiosampleActions.Action_NewBatch());
		edit.add(new ResultActions.Action_New());
		
		menuBar.add(SpiritMenu.getToolsMenu());
		menuBar.add(SpiritMenu.getHelpMenu(splashConfig));

		setJMenuBar(menuBar);
	}

	private void selectionChanged() {
		Collection<Biosample> selBiosamples = biosampleTab.getSelection(Biosample.class);
		List<Container> selContainers = Biosample.getContainers(selBiosamples);
		statusBar.setInfos(selContainers.size() + "/" + biosampleTab.getBiosampleTable().getRowCount() + " Biosamples");
		Collection<Biosample> highlighted = biosampleTab.getBiosampleTable().getHighlightedSamples();
		biosampleEditorPane.setBiosamples(highlighted.size()==1? highlighted: null);
		biosampleDetailPane.setBiosamples(highlighted.size()==1? highlighted: null, false);
	}

	private void newScan(Location rack) {
		

		boolean clearList = true;
		if(biosampleTab.getBiosamples().size()>0) {
			clearList = false;
			int res = JOptionPane.showConfirmDialog(this, "Do you want to clear your previous list?", "Scan Plate", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
			if(res==JOptionPane.YES_OPTION) clearList = true;
		}
		
		if(clearList) {			
			biosampleTab.setRack(rack);
			selectionChanged();
			Toolkit.getDefaultToolkit().beep();
		} else {
			newScan(rack.getContainers());
		}		
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

	/**
	 * 
	 */
	private KeyListener keyListener = new KeyListener() {
		@Override
		public void keyPressed(KeyEvent e) {
		}
		@Override
		public void keyReleased(KeyEvent e) {
		}
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
//				scanTextField.setText("");
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

	@Override
	public <T> void actionModelChanged(SpiritChangeType action, Class<T> what, List<T> details) {
		biosampleTab.reload();
	}
	
//	public <T> void actionModelChanged(final ChangeType action, final Class<T> what, final List<T> details) {
//		SwingUtilities.invokeLater(new Runnable() {
//			@SuppressWarnings("unchecked")
//			@Override
//			public void run() {
//
//				if (action == ChangeType.LOGIN) {
//					refresh();
//				} else {
//
//					biosampleDetailPane.setBiosamples(null);
//					biosampleEditorPane.setBiosample(null);
//
//					if (what == Biosample.class && (action == ChangeType.MODEL_ADDED || action == ChangeType.MODEL_UPDATED)) {
//						List<Biosample> biosamples = biosampleTab.getBiosamples();
//						biosamples.removeAll((List<Biosample>) details);
//						biosamples.addAll(JPAUtil.reload((List<Biosample>) details));
//						biosampleTab.setBiosamples(biosamples);
//						biosampleTab.setSelectedBiosamples((List<Biosample>) details);
//					}
//
//					refresh();
//				}
//			}
//		});
//	}

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

	public static class Action_BatchAssign extends AbstractAction {
		public Action_BatchAssign() {
			super("Scan & Assign Tubes");
			putValue(AbstractAction.SHORT_DESCRIPTION, "Scan Tubes in order to assign existing biosamples to containerIds");
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			new BatchAssignDlg().setVisible(true);
		}
	}

	public static class Action_BatchAliquot extends AbstractAction {
		public Action_BatchAliquot() {
			super("Scan & Create Aliquots");
			putValue(AbstractAction.SHORT_DESCRIPTION, "Scan Tubes in order to create aliquots and assign them to containerIds");
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			new BatchAliquotDlg().setVisible(true);
		}
	}

	public class Action_Scanner extends AbstractAction {
		public Action_Scanner() {
			super("Scan Rack");
			putValue(AbstractAction.SMALL_ICON, JIconButton.IconType.SCANNER.getIcon());
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				
				SpiritScanner scanner = new SpiritScanner();
				Location rack = scanner.scan(null, true, null);
				if(rack==null) return;
				newScan(rack);
			} catch (Exception ex) {
				JExceptionDialog.showError(ex);
			}

		}
	}

	public static void main(String[] args) {

		Spirit.initUI();
		SplashScreen2.show(splashConfig);
		
		new SwingWorkerExtended() {
			@Override
			protected void doInBackground() throws Exception {
				SpiritAction.logUsage("BioViewer");					
				JPAUtil.getManager();
			}
			@Override
			protected void done() {
				new BioViewer();
			}
		};
	
	}
	
	
	public static void open() {
		BioViewer app = new BioViewer();
		app.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	}

	@Override
	public void setStudy(Study study) {
		//Not supported
	}

	@Override
	public void setBiosamples(List<Biosample> biosamples) {
		biosampleTab.setBiosamples(biosamples);		
	}

	@Override
	public void setRack(Location rack) {
		biosampleTab.setRack(rack);		
	}

	@Override
	public void setLocation(Location location, int pos) {
	}

	@Override
	public void setResults(List<Result> results, PivotTemplate template) {
	}

	@Override
	public void query(BiosampleQuery q) {
	}

	@Override
	public void query(ResultQuery q) {
	}

	@Override
	public void setStatus(String status) {
		statusBar.setInfos(status);
	}
	@Override
	public void setUser(String status) {
		statusBar.setUser(status);
	}
}
