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

package com.actelion.research.spiritapp.ui.util.formtree;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
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
import javax.swing.SwingConstants;
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
		for (AbstractNode child : new ArrayList<>( children)) {
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
		for (AbstractNode child : new ArrayList<>( children)) {
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
		for (AbstractNode c : getChildren()) {
			if(c.isFilled()) return true;
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
				SwingUtilities.invokeLater(()-> {
					if(getFocusable()!=null) getFocusable().requestFocusInWindow();
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
		boolean addExpandButton = depth>0 && canExpand && (getChildren().size()>0 || expandStrategy!=null);

		if(panel==null) {

			//Add spacer and expand button
			int n = depth  - (tree.isRootVisible()?0:1);

			button.addFocusListener(new FocusAdapter() {
				@Override
				public void focusLost(FocusEvent e) {}
				@Override
				public void focusGained(FocusEvent e) {
					tree.setLastComponent((Component)e.getSource());
				}
			});
			button.addActionListener(e-> {
				tree.expand(AbstractNode.this);
			});

			//Add component
			JComponent component = getComponent();
			int marginPerLevel = button.getPreferredSize().width;
			int marginLeft = 1 + n * marginPerLevel /*- (addExpandButton? button.getPreferredSize().width-marginPerLevel:0)*/;
			JPanel buttonPanel = UIUtils.createCenterPanel(button, SwingConstants.CENTER);
			panel = UIUtils.createHorizontalBox(BorderFactory.createEmptyBorder(row>1 && depth<=1 && (getChildren().size()>0 || expandStrategy!=null)? 6:0, marginLeft, 0, 0),
					buttonPanel,
					component,
					Box.createHorizontalGlue());
			panel.setOpaque(false);
			//			getComponent().setPreferredSize(new Dimension(200, component.getPreferredSize().height));
		}

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

	public void setEnabled(boolean enabled) {
		getComponent().setEnabled(enabled);
	}

	public boolean isEnabled() {
		return getComponent().isEnabled();
	}

}
