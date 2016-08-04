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
import java.awt.Font;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import com.actelion.research.util.ui.JCustomLabel;

public class LabelNode extends AbstractNode<Boolean> {
	
	private final JLabel lbl;
	
	public LabelNode(FormTree tree, String label) {
		this(tree, label, null);
		setBold(true);
	}
	public LabelNode(FormTree tree, String label, Strategy<Boolean> accessor) {
		super(tree, label, accessor);
		lbl = new JCustomLabel(label);
		lbl.setBorder(BorderFactory.createEmptyBorder(0, 0, 2, 0));

		lbl.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {		
				if(!isCanExpand()) return;
				if(e.getButton()==MouseEvent.BUTTON1 && e.getClickCount()==2) {
					getTree().expand(LabelNode.this);
				}
			}
		});		
		
		setExpandStrategy(new ExpandStrategy() {
			@Override
			public boolean shouldExpand() {
				for(AbstractNode<?> n: getChildren()) {
					if(n.isFilled() || (n.getExpandStrategy()!=null && n.getExpandStrategy().shouldExpand())) return true;
				}
				return false;
			}
			
		});
		
		addEventsToComponent();
	}
	
	public void setLabel(String label) {
		lbl.setText(label);
	}
	
	public void setIcon(Icon icon) {
		lbl.setIcon(icon);
	}
	
	public Icon getIcon() {
		return lbl.getIcon();
	}
	
	@Override
	public JComponent getComponent() {
		return lbl;
	}
	
	@Override
	protected void updateModel() {}
	@Override
	protected void updateView() {}
	@Override
	protected boolean isFilled() {
		return false;
	}

	@Override
	public JComponent getFocusable() {
		return lbl;
	}
	
	public void setForeground(Color color) {
		lbl.setForeground(color);
	}
	
	public LabelNode setBold(boolean bold) {
		lbl.setFont(lbl.getFont().deriveFont(bold ? Font.BOLD: Font.PLAIN));
		return this;
	}
	
	public LabelNode setImage(Image img) {
		lbl.setIcon(new ImageIcon(img));
		lbl.setHorizontalTextPosition(SwingUtilities.RIGHT);
		return this;
	}
	

	@Override
	public String toString() {
		return "LabelNode-"+label;
	}
}
