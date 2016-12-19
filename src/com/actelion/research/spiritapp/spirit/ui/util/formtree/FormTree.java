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

package com.actelion.research.spiritapp.spirit.ui.util.formtree;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.util.LinkedList;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;

public class FormTree extends JPanel {
		
	public static final String PROPERTY_SUBMIT_PERFORMED = "action";
	public static final String PROPERTY_CHANGED = "change";
	private AbstractNode<?> root;
	private boolean rootVisible = true;
	private boolean rightClickEnabled = true;
	private Component lastComponent = null;
	
	
	
	/**
	 * Constructor
	 */	
	public FormTree() {		
		setOpaque(true);
		setBackground(Color.WHITE);
		setLayout(new GridBagLayout());
		initLayout();
		
		
		addAncestorListener(new AncestorListener() {			
			@Override
			public void ancestorRemoved(AncestorEvent arg0) {}			
			@Override
			public void ancestorMoved(AncestorEvent arg0) {}			
			@Override
			public void ancestorAdded(AncestorEvent arg0) {
				//Update the mousewheel scroll if enabled
				if(FormTree.this.getParent() instanceof JScrollPane) {
					((JScrollPane) FormTree.this.getParent()).getVerticalScrollBar().setUnitIncrement(22);					
				} else if(FormTree.this.getParent() instanceof JViewport && ((JViewport) FormTree.this.getParent()).getParent() instanceof JScrollPane) {
					((JScrollPane)((JViewport) FormTree.this.getParent()).getParent()).getVerticalScrollBar().setUnitIncrement(22);										
				}
			}
		});
		
	}
	
	public synchronized void expandAll(boolean expand) {
		expandAll(root, expand);
		root.recomputeProperties();
		initLayout();
	}
	
	public synchronized void expandAll(AbstractNode<?> node, boolean expand) {
		if(node.isCanExpand()) {
			node.setExpanded(expand);
		}
		for (AbstractNode<?> child : node.getChildren()) {
			expandAll(child, expand);
		}
	}
	
	protected void initLayout() {
		if(!SwingUtilities.isEventDispatchThread()) {
			SwingUtilities.invokeLater(new Runnable() {				
				@Override
				public void run() {
					initLayout();
				}
			});
			return;
		}
		//Memorize the focus		
		final Component comp = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
		
		//Reset the layout
		removeAll();
		if(root!=null) {
			root.recomputeProperties();
			GridBagConstraints c = new GridBagConstraints();
			c.gridy = 5000;
			c.weighty = c.weightx = 1;
			add(new JLabel(), c);
			
			if(rootVisible) {
				initRec(root);
			} else {
				for (AbstractNode<?> node : root.getChildren()) {				
					initRec(node);
				}
			}
		}
		//Request the focus later
		if(comp!=null) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					comp.requestFocusInWindow();
				}
			});
		}
		
		
		if(getParent()!=null && getParent() instanceof JComponent) ((JComponent)getParent()).revalidate(); //update scrollpane if available
		else validate();
		repaint();
	}	

	private void initRec(final AbstractNode<?> node) {
		if(node==null || !node.isVisible()) return;
		
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(0, 0, 0, 0);
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.WEST;
		c.gridx = 0;
		c.gridy = node.row;
		c.weightx = 1;
		c.weighty = 0; 

		if(node.isFilledRec()) {
			node.setExpanded(true);
		}
		
		add(node.getView(), c);		
		if(node.isExpanded()) {
			for (AbstractNode<?> child : node.getChildren()) {
				initRec(child);
			}
		}

	}
	
	
	protected synchronized void expand(AbstractNode<?> node) {
		final Component toFocus = lastComponent;
		setSelection(node);
		if(node.isFilledRec() && node.isExpanded()) {
			//Nothing
		} else {
			node.setExpanded(!node.isExpanded());
			if(!node.isExpanded() && node.getExpandStrategy()!=null) node.getExpandStrategy().onExpand();
			if(node.isExpanded() && node.getExpandStrategy()!=null) node.getExpandStrategy().onCollapse();
		}
		
		initLayout();
		if(toFocus!=null) {
			SwingUtilities.invokeLater(new Runnable() {		
				@Override
				public void run() {
					toFocus.requestFocusInWindow();
				}
			});
		}
	}		

	protected void setLastComponent(Component lastComponent) {
		this.lastComponent = lastComponent;
	}


	public synchronized void updateView() {
		if(root!=null) {
			root.updateViewRec();
			root.recomputeProperties(0,0);
		}
		initLayout();
	}
	
	public synchronized void updateModel() {
		if(root!=null) root.updateModelRec();
	}
	
	public synchronized void setRoot(AbstractNode<?> root) {
		this.root = root;
		root.updateViewRec();
		initLayout();
	}	
	

	public AbstractNode<?> getRoot() {
		return root;
	}
	
	/**
	 * @param rootVisible the rootVisible to set
	 */
	public void setRootVisible(boolean rootVisible) {
		this.rootVisible = rootVisible;
	}
	
	/**
	 * @return the rootVisible
	 */
	public boolean isRootVisible() {
		return rootVisible;
	}

	public synchronized void setSelection(final AbstractNode<?> n) {
		//Expand the node if not expanded
		if(n!=null) {
			boolean modified = false;
			AbstractNode<?> node = n.getParent();
			while(node!=null) {
				if(!node.isExpanded()) {node.setExpanded(true); modified = true;}
				node = node.getParent();
			}
			if(modified) {
				initLayout();
			}
		}

		//Scroll to visible
		if(n!=null) {
			Rectangle r = n.getView().getBounds();
			scrollRectToVisible(r);
		}
		//Call the onSelect
		if(n!=null && n.strategy!=null) {
			n.strategy.onFocus();
		}
	}
	public synchronized void setFocus(final AbstractNode<?> n) {
		if(n==null || n.getFocusable()==null) return;
		SwingUtilities.invokeLater(new Runnable() {					
			@Override
			public void run() {
				n.getFocusable().requestFocusInWindow();
			}
		});
	}
	
	public synchronized void setSelectionRow(int row) {
		updateUI();
		LinkedList<AbstractNode<?>> nodes = new LinkedList<AbstractNode<?>>();
		nodes.add(root);
		while(!nodes.isEmpty()) {
			final AbstractNode<?> n = nodes.pop();
			if(n==null) continue;
			if(n.row==row) {
				setSelection(n);
				return;
			}
			for (AbstractNode<?> child : n.getChildren()) {
				nodes.addFirst(child);				
			}
		}
	}
	
	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
			
			UIManager.put("nimbusSelectionBackground", Color.LIGHT_GRAY);
		} catch (Exception e) {
			e.printStackTrace();
		}

		
		FormTree tree = new FormTree();
		tree.setRootVisible(false);
		LabelNode root = new LabelNode(tree, "TEST");
		
		LabelNode cat1 = new LabelNode(tree, "CAT1");
		LabelNode cat2 = new LabelNode(tree, "CAT2");
		LabelNode cat3 = new LabelNode(tree, "CAT3");
		root.add(cat1);
		cat1.add(cat2);
		cat1.add(cat3);
		
		InputNode in0 = new InputNode(tree, null, "INPUT", null);
		InputNode in1 = new InputNode(tree, null, "INPUT1", null);
		InputNode in2 = new InputNode(tree, null, "INPUT2", null);
		InputNode in3 = new InputNode(tree, null, "INPUT3", null);
		InputNode in4 = new InputNode(tree, null, "SUBINPUT4", null);
		InputNode in5 = new InputNode(tree, null, "SUBINPUT5", null);
		InputNode in6 = new InputNode(tree, null, "SUBINPUT6", null);
		InputNode in7 = new InputNode(tree, null, "SUBINPUT7", null);		
		root.add(in0);
		in0.add(in1);
		in0.add(in2);
		in0.add(in3);
		in2.add(in4);
		in2.add(in5);
		in3.add(in6);
		in3.add(in7);
		
		CheckboxNode cb = new CheckboxNode(tree, "CB");
		CheckboxNode cb1 = new CheckboxNode(tree, "CB1");
		CheckboxNode cb2 = new CheckboxNode(tree, "CB2");
		CheckboxNode cb3 = new CheckboxNode(tree, "CB3");
		CheckboxNode cb4 = new CheckboxNode(tree, "CB3");
		root.add(cb);
		cb.add(cb1);
		cb.add(cb2);
		cb2.add(cb3);
		cb2.add(cb4);
		
		
		tree.setRoot(root);
		tree.expandAll(false);
		tree.setSelectionRow(4);
		
		JFrame f = new JFrame();
		f.setContentPane(tree);
		f.setSize(400,400);
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.setVisible(true);
		
	}

	/**
	 * @param rightClickEnabled the rightClickEnabled to set
	 */
	public void setRightClickEnabled(boolean rightClickEnabled) {
		this.rightClickEnabled = rightClickEnabled;
	}

	/**
	 * @return the rightClickEnabled
	 */
	public boolean isRightClickEnabled() {
		return rightClickEnabled;
	}

}
