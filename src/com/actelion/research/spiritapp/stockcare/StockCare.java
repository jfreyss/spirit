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

package com.actelion.research.spiritapp.stockcare;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.JPanel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritapp.spirit.ui.SpiritFrame;
import com.actelion.research.spiritapp.spirit.ui.SpiritTab;
import com.actelion.research.spiritapp.spirit.ui.biosample.BiosampleActions;
import com.actelion.research.spiritapp.spirit.ui.biosample.BiosampleOrRackTab;
import com.actelion.research.spiritapp.spirit.ui.biosample.BiosampleTab;
import com.actelion.research.spiritapp.spirit.ui.location.LocationTab;
import com.actelion.research.spiritapp.spirit.ui.location.depictor.RackDepictorListener;
import com.actelion.research.spiritapp.spirit.ui.result.ResultActions;
import com.actelion.research.spiritapp.spirit.ui.result.ResultTab;
import com.actelion.research.spiritapp.spirit.ui.util.ISpiritChangeObserver;
import com.actelion.research.spiritapp.spirit.ui.util.ISpiritContextObserver;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritAction;
import com.actelion.research.spiritapp.stockcare.ui.item.StockCareItem;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.biosample.Container;
import com.actelion.research.spiritcore.business.pivot.PivotTemplate;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.util.ui.SplashScreen2;
import com.actelion.research.util.ui.SplashScreen2.SplashConfig;
import com.actelion.research.util.ui.SwingWorkerExtended;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.iconbutton.JIconButton;

public class StockCare extends SpiritFrame implements ISpiritChangeObserver, ISpiritContextObserver {
	
	private static SplashConfig splashConfig = new SplashConfig(StockCare.class.getResource("stockcare.png"), "StockCare", "StockCare v" + Spirit.class.getPackage().getImplementationVersion() + "<br> (C) Actelion - J.Freyss");
	
	private StockCareItem currentItem;
	
	private Map<Biotype, BiosampleTab> biotype2tab = new HashMap<>();
	private BiosampleTab biosampleTab;
	private LocationTab inventoryTab;
	
	
	public StockCare() {
		super("StockCare", "StockCare - (C) Joel Freyss - Actelion 2013");
		setStudyLevel(null, true);
	}	
	
	@Override
	public List<SpiritTab> getTabs() {
		List<SpiritTab> tabs = new ArrayList<>();
		tabs.add(new StockCareHome(this));

		long s = System.currentTimeMillis();
		if(currentItem!=null) {		
			//Create the BiosampleTabs
			//one tab for the hierarchy and one per aggregated type
			Biotype[] biotypes = currentItem.getBiotypes();
			biosampleTab = new StockCareBiosampleTab(biotypes, false);		
			for(Biotype biotype: currentItem.getBiotypes()) {
				biotype2tab.put(biotype, biosampleTab);
			}
			tabs.add(biosampleTab);
//			tabbedPane.add(currentItem.getName(), biosampleTab);
//			tabbedPane.setIconAt(tabbedPane.getTabCount()-1, currentItem.getIcon());
			
			for(final Biotype biotype: currentItem.getAggregatedBiotypes()) {
				if(biotype.isHidden()) continue;
				BiosampleTab tab = new StockCareBiosampleTab(new Biotype[] {biotype}, true);
				tabs.add(tab);
//				tabbedPane.addTab(biotype.getName(), tab);
//				tabbedPane.setIconAt(tabbedPane.getTabCount()-1, new ImageIcon(ImageFactory.getImageThumbnail(biotype)));
			}
		
			// Create the InventoryTab
			if(biotypes.length>1 || !biotypes[0].isAbstract()) {
				inventoryTab = new LocationTab(StockCare.this, currentItem.getBiotypes()[currentItem.getBiotypes().length-1]) {
					@Override
					protected JPanel createButtonsPanel() {
						//Edit
						final JIconButton editButton = new JIconButton(new BiosampleActions.Action_BatchEdit() {
							@Override
							public java.util.List<Biosample> getBiosamples() {return new ArrayList<Biosample>();}
						});
		
						//Create				
						final JIconButton createButton = new JIconButton(new BiosampleActions.Action_New(currentItem.getMainBiotype()));
		
						//Checkin
						final JIconButton checkinButton = new JIconButton(new BiosampleActions.Action_Checkin(null));
						
						//Checkout
						final JIconButton checkoutButton = new JIconButton(new BiosampleActions.Action_Checkout(null));
		
						getLocationDepictor().addRackDepictorListener(new RackDepictorListener() {					
							@Override
							public void onSelect(Collection<Integer> pos, final Container lastContainer, boolean dblClick) {
								final List<Biosample> biosamples = Container.getBiosamples(getLocationDepictor().getSelectedContainers());
		
								editButton.setAction(new BiosampleActions.Action_BatchEdit(biosamples));
								checkinButton.setAction(new BiosampleActions.Action_Checkin(biosamples));
								checkoutButton.setAction(new BiosampleActions.Action_Checkout(biosamples));
		
							}
						});
		
						return UIUtils.createHorizontalBox(Box.createHorizontalGlue(), editButton, createButton, checkinButton, checkoutButton);
					}
				};
				tabs.add(inventoryTab);
//				tabbedPane.add("Location", inventoryTab);		
//				tabbedPane.setIconAt(tabbedPane.getTabCount()-1, IconType.LOCATION.getIcon());
			}
	
			//Create the ResultTab
			if(currentItem.hasResults()) {
				PivotTemplate[] tpls = currentItem.getDefaultTemplates();
				ResultTab resultTab = new ResultTab(StockCare.this, currentItem.getMainBiotype()) {
					@Override 
					protected JPanel createButtonsPanel() {
						//Edit button
						final JIconButton editButton = new JIconButton(new ResultActions.Action_Edit_Results() {
							@Override
							public List<Result> getResults() {
								return getSelection();
							}					
						});
						editButton.setEnabled(false);
	
						//Create button
						final JIconButton createButton = new JIconButton(new ResultActions.Action_New());
						
						//Enable edit button when selection is made
						ListSelectionListener listener = new ListSelectionListener() {						
							@Override
							public void valueChanged(ListSelectionEvent e) {
								List<Result> sel = getSelection();
								editButton.setEnabled(sel.size() > 0 && SpiritRights.canEditResults(sel, SpiritFrame.getUser()));
							}
						};
						getPivotCardPanel().getPivotTable().getSelectionModel().addListSelectionListener(listener);
						getPivotCardPanel().getPivotTable().getColumnModel().getSelectionModel().addListSelectionListener(listener);
						return UIUtils.createHorizontalBox(Box.createHorizontalGlue(), editButton, createButton);
					}		
				};
				if(tpls!=null && tpls.length>0) {
					resultTab.setCurrentPivotTemplate(currentItem.getDefaultTemplates()[0]);
					resultTab.setDefaultTemplates(currentItem.getDefaultTemplates());
				}
				tabs.add(resultTab);
//				tabbedPane.add("Result", resultTab);
//				tabbedPane.setIconAt(tabbedPane.getTabCount()-1, IconType.RESULT.getIcon());
			}
			
			
			
			System.out.println("StockCare.initTabs(2) "+(System.currentTimeMillis()-s)+"ms");
			
//			tabbedPane.setSelectedIndex(selectedTabIndex);
		}
		
		
		return tabs;
	}
	
//	public void eventUserChanged() {
//
//		SpiritUser user =  SpiritFrame.getUser();
//		String userStatus;
//		if(user==null) {
//			userStatus = "No user logged in";
//		} else {
//			userStatus = user.getUsername() + " ("+ (user.getMainGroup()==null?"NoDept":user.getMainGroup().getName())+ ") logged in";
//		}
//		statusBar.setUser(userStatus);
//		initMenuBar();
//	}
	
//	/**
//	 * Init Menu. To be called after a biotype is selected
//	 */
//	private void initMenuBar() {
//		JMenuBar menuBar = new JMenuBar();
//		
//		JMenu editMenu = new JMenu("Edit");
//		editMenu.setMnemonic('v');
//		if(currentItem!=null) {			
//			editMenu.add(new BiosampleActions.Action_New(currentItem.getMainBiotype()));
//			AbstractAction action = new ResultActions.Action_New(currentItem.getDefaultTest());
//			action.setEnabled(currentItem.hasResults());
//			editMenu.add(action);
//			editMenu.add(new JSeparator());
//		}
//		SpiritMenu.addEditMenuItems(editMenu, null);
//
//		menuBar.add(editMenu);
//		menuBar.add(SpiritMenu.getDevicesMenu());
//		menuBar.add(SpiritMenu.getToolsMenu());
//		menuBar.add(SpiritMenu.getAdminMenu());
//		menuBar.add(SpiritMenu.getHelpMenu(splashConfig));
//		
//		setJMenuBar(menuBar);
//	}
	
	private class StockCareBiosampleTab extends BiosampleTab {
		private final Biotype aggregatedBiotype;
		public StockCareBiosampleTab(Biotype[] biotypes, boolean aggregated) {
			super(StockCare.this, biotypes);
			this.aggregatedBiotype = aggregated? biotypes[0]: null; 					
		}
			
		@Override 
		protected JPanel createButtonsPanel() {
			//Edit button
			final JIconButton editButton = new JIconButton(new BiosampleActions.Action_BatchEdit() {
				@Override
				public List<Biosample> getBiosamples() {
					return getBiosampleOrRackTab().getSelection(Biosample.class);
				}					
			});
			editButton.setEnabled(false);


			final BiosampleActions.Action_New createAction = new BiosampleActions.Action_New(currentItem.getMainBiotype());
			final BiosampleActions.Action_NewChild childAction = new BiosampleActions.Action_NewChild(null);
			
			//Create button
			final JIconButton createButton = aggregatedBiotype==null? 
					new JIconButton(createAction):
					new JIconButton(new BiosampleActions.Action_New(aggregatedBiotype));
			final JIconButton childButton = aggregatedBiotype==null? 
					new JIconButton(childAction):
					new JIconButton(new BiosampleActions.Action_New(aggregatedBiotype));
			childButton.setVisible(currentItem.getBiotypes().length>1);
			//Specific action (only for the main item)
			Box actionsBox = Box.createHorizontalBox();
			if(aggregatedBiotype==null) {
				for(AbstractAction action: currentItem.getActions()) {
					JIconButton button = new JIconButton(action);
					actionsBox.add(button);
				}
			}
			
			//Enable edit button when selection is made
			getBiosampleOrRackTab().addPropertyChangeListener(BiosampleOrRackTab.PROPERTY_SELECTION, new PropertyChangeListener() {
				@Override
				public void propertyChange(PropertyChangeEvent evt) {
					List<Biosample> sel = getBiosampleOrRackTab().getSelection(Biosample.class);
//					createAction.setParent(sel.size()==1? sel.get(0): null);
					childAction.setParent(sel.size()==1? sel.get(0): null);
					editButton.setEnabled(sel.size() > 0 && SpiritRights.canEditBiosamples(sel, SpiritFrame.getUser()));
				}
			});
			
			return UIUtils.createHorizontalBox(Box.createHorizontalGlue(), editButton, childButton, createButton, actionsBox);
		}		
	}
	
//	private void initTabs() {
//		//Remove all tabs except the home
//		int selectedTabIndex = tabbedPane.getSelectedIndex();
//
//		for(int i=tabbedPane.getTabCount()-1; i>0; i--) {
//			tabbedPane.remove(i);
//		}
//		if(currentItem==null) return;
//		biotype2tab.clear();
//		
//		long s = System.currentTimeMillis();
//				
//		//Create the BiosampleTabs
//		//one tab for the hierarchy and one per aggregated type
//		Biotype[] biotypes = currentItem.getBiotypes();
//		biosampleTab = new StockCareBiosampleTab(biotypes, false);		
//		for(Biotype biotype: currentItem.getBiotypes()) {
//			biotype2tab.put(biotype, biosampleTab);
//		}
//		tabbedPane.add(currentItem.getName(), biosampleTab);
//		tabbedPane.setIconAt(tabbedPane.getTabCount()-1, currentItem.getIcon());
//		
//		for(final Biotype biotype: currentItem.getAggregatedBiotypes()) {
//			if(biotype.isHidden()) continue;
//			BiosampleTab tab = new StockCareBiosampleTab(new Biotype[] {biotype}, true); 
//			tabbedPane.addTab(biotype.getName(), tab);
//			tabbedPane.setIconAt(tabbedPane.getTabCount()-1, new ImageIcon(ImageFactory.getImageThumbnail(biotype)));
//		}
//	
//		// Create the InventoryTab
//		if(biotypes.length>1 || !biotypes[0].isAbstract()) {
//			inventoryTab = new LocationTab(StockCare.this, currentItem.getBiotypes()[currentItem.getBiotypes().length-1]) {
//				@Override
//				protected JPanel createButtonsPanel() {
//					//Edit
//					final JIconButton editButton = new JIconButton(new BiosampleActions.Action_BatchEdit() {
//						@Override
//						public java.util.List<Biosample> getBiosamples() {return new ArrayList<Biosample>();}
//					});
//	
//					//Create				
//					final JIconButton createButton = new JIconButton(new BiosampleActions.Action_New(currentItem.getMainBiotype()));
//	
//					//Checkin
//					final JIconButton checkinButton = new JIconButton(new BiosampleActions.Action_Checkin(null));
//					
//					//Checkout
//					final JIconButton checkoutButton = new JIconButton(new BiosampleActions.Action_Checkout(null));
//	
//					getLocationDepictor().addRackDepictorListener(new RackDepictorListener() {					
//						@Override
//						public void onSelect(Collection<Integer> pos, final Container lastContainer, boolean dblClick) {
//							final List<Biosample> biosamples = Container.getBiosamples(getLocationDepictor().getSelectedContainers());
//	
//							editButton.setAction(new BiosampleActions.Action_BatchEdit(biosamples));
//							checkinButton.setAction(new BiosampleActions.Action_Checkin(biosamples));
//							checkoutButton.setAction(new BiosampleActions.Action_Checkout(biosamples));
//	
//						}
//					});
//	
//					return UIUtils.createHorizontalBox(Box.createHorizontalGlue(), editButton, createButton, checkinButton, checkoutButton);
//				}
//			};
//			tabbedPane.add("Location", inventoryTab);		
//			tabbedPane.setIconAt(tabbedPane.getTabCount()-1, IconType.LOCATION.getIcon());
//		}
//
//		//Create the ResultTab
//		if(currentItem.hasResults()) {
//			PivotTemplate[] tpls = currentItem.getDefaultTemplates();
//			ResultTab resultTab = new ResultTab(StockCare.this, currentItem.getMainBiotype()) {
//				@Override 
//				protected JPanel createButtonsPanel() {
//					//Edit button
//					final JIconButton editButton = new JIconButton(new ResultActions.Action_Edit_Results() {
//						@Override
//						public List<Result> getResults() {
//							return getSelection();
//						}					
//					});
//					editButton.setEnabled(false);
//
//					//Create button
//					final JIconButton createButton = new JIconButton(new ResultActions.Action_New());
//					
//					//Enable edit button when selection is made
//					ListSelectionListener listener = new ListSelectionListener() {						
//						@Override
//						public void valueChanged(ListSelectionEvent e) {
//							List<Result> sel = getSelection();
//							editButton.setEnabled(sel.size() > 0 && SpiritRights.canEditResults(sel, SpiritFrame.getUser()));
//						}
//					};
//					getPivotCardPanel().getPivotTable().getSelectionModel().addListSelectionListener(listener);
//					getPivotCardPanel().getPivotTable().getColumnModel().getSelectionModel().addListSelectionListener(listener);
//					return UIUtils.createHorizontalBox(Box.createHorizontalGlue(), editButton, createButton);
//				}		
//			};
//			if(tpls!=null && tpls.length>0) {
//				resultTab.setCurrentPivotTemplate(currentItem.getDefaultTemplates()[0]);
//				resultTab.setDefaultTemplates(currentItem.getDefaultTemplates());
//			}
//			tabbedPane.add("Result", resultTab);
//			tabbedPane.setIconAt(tabbedPane.getTabCount()-1, IconType.RESULT.getIcon());
//		}
//		
//		
//		
//		System.out.println("StockCare.initTabs(2) "+(System.currentTimeMillis()-s)+"ms");
//		
//		tabbedPane.setSelectedIndex(selectedTabIndex);
//	}
	
//	@SuppressWarnings("unchecked")
//	@Override 
//	public <T> void actionModelChanged(final SpiritChangeType action, final Class<T> w, final List<T> details) {
//		if(action==SpiritChangeType.MODEL_ADDED && w==Biosample.class){
//			List<Biosample> biosamples = (List<Biosample>) details;
//			Map<Biotype, List<Biosample>> map = Biosample.mapBiotype(biosamples);
//			for (Biotype biotype : map.keySet()) {
//				BiosampleTab tab = biotype2tab.get(biotype);
//				tabbedPane.setSelectedComponent(tab);
//				tab.setBiosamples(biosamples);				
//			}
//		} else {
//			SwingUtilities.invokeLater(new Runnable() {			
//				@Override
//				public void run() {
//					Component c = tabbedPane.getSelectedComponent();
//					if(c instanceof SpiritTab) {
//						((SpiritTab) c).fireModelChanged(action, w, details);
//					}								
//				}
//			});
//		}
//	}
//	
//	
//	@Override
//	public void setStudy(Study study) {
//	}
//
//	@Override
//	public void setBiosamples(List<Biosample> biosamples) {
//		tabbedPane.setSelectedComponent(biosampleTab);
//		biosampleTab.setBiosamples(biosamples);
//	}
//	
//	@Override
//	public void setRack(Location rack) {
//		tabbedPane.setSelectedComponent(biosampleTab);
//		biosampleTab.setRack(rack);
//	}
//
//	@Override
//	public void setLocation(Location location, int pos) {
//		tabbedPane.setSelectedComponent(inventoryTab);
//		inventoryTab.setBioLocation(location, pos);
//		if(location!=null) statusBar.setInfos(location + " selected");
//	}
//
//	@Override
//	public void setResults(List<Result> results) {
//	}
//
//	@Override
//	public void query(BiosampleQuery q) {
//	}
//
//	@Override
//	public void query(ResultQuery q, int graphIndex) {
//	}
//
//	@Override
//	public void setStatus(String status) {
//		statusBar.setInfos(status);
//	}
//	@Override
//	public void setUser(String status) {
//		statusBar.setUser(status);
//	}
//	
	
	public void setItem(StockCareItem item) {
		this.currentItem = item;
//		initMenuBar();
//		initTabs();
		recreateUI();
		getTabbedPane().setSelectedIndex(1);
	}

	
	public static void main(String[] args) {

		// Splash
		SplashScreen2.show(splashConfig);
		new SwingWorkerExtended() {			
			@Override
			protected void doInBackground() throws Exception {
				SpiritAction.logUsage("StockCare");					
				JPAUtil.getManager();
			}
			@Override
			protected void done() {
				Spirit.initUI();
				new StockCare();				
			}			
		};
	}
	

}
