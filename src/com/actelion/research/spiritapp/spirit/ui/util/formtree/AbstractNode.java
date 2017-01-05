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

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.PopupAdapter;
import com.actelion.research.util.ui.UIUtils;

@SuppressWarnings("rawtypes")
public abstract class AbstractNode<T>  {

	public enum FieldType {
		EXACT,
		AND_CLAUSE,
		OR_CLAUSE
	}
	protected final FormTree tree;
    
	protected AbstractNode parent;
	protected List<AbstractNode> children = new ArrayList<AbstractNode>();
	protected Strategy<T> strategy;
	protected ExpandStrategy expandStrategy;	
	protected final String label;
	
	protected int row;
	protected int depth;
	protected boolean expanded = false;

	protected Font editFont = FastFont.REGULAR;
	
	private JPanel panel = null;
	private final JToggleButton button = new JToggleButton();
	private boolean canExpand = true;
	private boolean visible = true; 
	
    protected abstract void updateView();
	protected abstract void updateModel();
	protected abstract boolean isFilled();

	public abstract JComponent getComponent();
	public abstract JComponent getFocusable();	

	
	static {
		UIManager.put("FormTree.collapsed", new ImageIcon(AbstractNode.class.getResource("collapsed.png")));
		UIManager.put("FormTree.expanded", new ImageIcon(AbstractNode.class.getResource("expanded.png")));		
	}
	
	public AbstractNode(FormTree tree, String label, Strategy<T> strategy) {
		this.tree = tree;
		this.label = label;
		this.strategy = strategy;
		
		button.setPreferredSize(new Dimension(19,19));
		button.setSelected(isExpanded());
		button.setBorder(BorderFactory.createEmptyBorder(4, 5, 4, 5));		
	}
	
	public String getLabel() {
		return label;	
	}
	
	public void add(AbstractNode<?> child) {
		child.parent = this;
		children.add(child);
	}
	
	protected final void updateViewRec() {		
		updateView();		
		for (AbstractNode child : new ArrayList<AbstractNode>( children)) {
			child.updateViewRec();
		}

		if(!isExpanded() && (isFilledRec() || (expandStrategy!=null && expandStrategy.shouldExpand()))) {			
			setExpanded(true);
			setVisible(true);
		}
		if(!isVisible()) return;
		
	}
	
	protected final void updateModelRec() {
		if(strategy!=null) updateModel();
		for (AbstractNode child : new ArrayList<AbstractNode>( children)) {
			child.updateModelRec();
		}		
	}
	
	public FormTree getTree() {
		return tree;
	}

    public void clearChildren() {
		children.clear();
	}

    public List<AbstractNode> getChildren() {
		return Collections.unmodifiableList(children);
	}
   
    protected void recomputeProperties() {
    	if(depth==0) setExpanded(true);
    	
    	recomputeProperties(depth, row);    	
    }
    
    protected final int recomputeProperties(int depth, int row) {
    	if(!isVisible()) return row;
    	this.depth = depth;
    	this.row = row;
    	row++;
    
    	for (AbstractNode child : getChildren()) {    		
    		row = child.recomputeProperties(depth+1, row);
		}
    	return row;   	
    }

	public Strategy getStrategy() {
		return strategy;
	}

	public void setStrategy(Strategy<T> strategy) {
		this.strategy = strategy;
	}
	
	protected boolean isFilledRec() {
		if(isFilled()) return true;
		for (AbstractNode c : getChildren()) {
			if(c.isFilledRec()) return true;
		}
		return false;
	}

	public AbstractNode getChildAt(int childIndex) {
		return children.get(childIndex);
	}

	public int getChildCount() {
		return children.size();
	}

	public AbstractNode getParent() {
		return parent;
	}

	protected void addEventsToComponent() {
		final Component component = getComponent();
		MouseAdapter ma = new PopupAdapter() {			
			@Override
			public void mouseClicked(MouseEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						if(getFocusable()!=null) getFocusable().requestFocusInWindow();
					}
				});				
			}
			@Override
			protected void showPopup(final MouseEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						if(getFocusable()!=null) getFocusable().requestFocusInWindow();
						if(strategy!=null && tree.isRightClickEnabled()) {										
							strategy.onRightClick(component, e.getX(), e.getY());
						}
					}
				});	
				
			}
		};
		FocusAdapter fa = new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
			}
			@Override
			public void focusGained(FocusEvent e) {
				tree.setSelection(AbstractNode.this);
				tree.setLastComponent((Component)e.getSource());
				if(strategy!=null) strategy.onFocus();
			}
		};
		
		component.addMouseListener(ma);
		component.addFocusListener(fa);
		if(component instanceof Container) {
			for (Component c : ((Container)component).getComponents()) {
				c.addMouseListener(ma);
				c.addFocusListener(fa);
			}
		}		
	}
	
	protected final Component getView() {

		if(panel==null) {
	
			//Add spacer and expand button
			int n = depth  - (tree.isRootVisible()?0:1);
						
			button.addFocusListener(new FocusAdapter() {
				@Override
				public void focusLost(FocusEvent e) {
				}
				@Override
				public void focusGained(FocusEvent e) {
					tree.setLastComponent((Component)e.getSource());
				}
			});
			button.addActionListener(new ActionListener() {				
				@Override
				public void actionPerformed(ActionEvent e) {
					tree.expand(AbstractNode.this);
				}
			});
			
			//Add component
			JComponent component = getComponent();
			
			panel = new JPanel(new GridBagLayout());
			GridBagConstraints c = new GridBagConstraints();
			c.gridy = 0;
			c.weighty = 0;
			c.gridx = 0; c.weightx = 0; panel.add(UIUtils.createHorizontalBox(Box.createHorizontalStrut(2 + Math.max(0, n) * 12), button), c);
			c.gridx = 1; c.weightx = 1; panel.add(component, c);
			c.gridx = 2; c.weightx = 0; panel.add(Box.createHorizontalGlue(), c);
			
			panel.setOpaque(false);
		}
		panel.setBorder(BorderFactory.createEmptyBorder(row>1 && depth<=1 && (getChildren().size()>0 || expandStrategy!=null)? 5:0, 0, 0, 0));
		
		boolean addExpandButton = depth>0 && canExpand && (getChildren().size()>0 || expandStrategy!=null);		
		button.setVisible(addExpandButton);
		
		button.setSelected(isExpanded());
		if(isExpanded()) {
			button.setIcon(UIManager.getIcon("FormTree.expanded"));
		} else {
			button.setIcon(UIManager.getIcon("FormTree.collapsed"));
		}
		return panel;
	}

	public ExpandStrategy getExpandStrategy() {
		return expandStrategy;
	}

	public void setExpandStrategy(ExpandStrategy expandStrategy) {
		this.expandStrategy = expandStrategy;
	}
	
	public void setExpanded(boolean expanded) {
		this.expanded = expanded;
	}

	public boolean isExpanded() {
		return expanded;
	}
	
	public void setCanExpand(boolean canExpand) {
		this.canExpand = canExpand;
		if(!canExpand) setExpanded(true);
	}
	
	public boolean isCanExpand() {
		return canExpand;
	}
	
	public void setVisible(boolean visible) {
		this.visible = visible;
	}
	
	public boolean isVisible() {
		return visible;
	}
}
