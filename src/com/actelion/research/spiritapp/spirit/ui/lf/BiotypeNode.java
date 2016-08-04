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

package com.actelion.research.spiritapp.spirit.ui.lf;

import java.util.Collection;

import com.actelion.research.spiritapp.spirit.ui.util.formtree.ComboBoxNode;
import com.actelion.research.spiritapp.spirit.ui.util.formtree.FormTree;
import com.actelion.research.spiritapp.spirit.ui.util.formtree.Strategy;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.services.dao.DAOBiotype;

/**
 * Wrapper to a BioTypeComboBox
 * @author freyssj
 *
 */
public class BiotypeNode extends ComboBoxNode<Biotype> {
	
	public BiotypeNode(FormTree tree, Strategy<Biotype> strategy) {
		super(tree, new BiotypeComboBox(DAOBiotype.getBiotypes()), "", null, strategy);
		getComboBox().setEditable(false);
	}
	
	public BiotypeNode(FormTree tree, Collection<Biotype> fixedChoices, Strategy<Biotype> strategy) {
		super(tree, new BiotypeComboBox(fixedChoices, "..."), "", null, strategy);
		getComboBox().setEditable(false);
	}
	
	
	@Override
	public BiotypeComboBox getComboBox() {
		return (BiotypeComboBox) super.getComboBox();
	}
	
	@Override
	protected void updateModel() {
		if(strategy==null) {
			System.err.println("No Strategy defined for "+getLabel());
			return;
		}
		strategy.setModel(getComboBox().getSelection());
	}
	@Override
	protected void updateView() {
		Biotype model = strategy.getModel();		
		((BiotypeComboBox) getComboBox()).setSelection(model);
	}
	
	public void setEnabled(boolean val) {
		((BiotypeComboBox) getComboBox()).setEnabled(val);
	}
	public boolean  isEnabled() {
		return ((BiotypeComboBox) getComboBox()).isEnabled();
	}
	

	
}
