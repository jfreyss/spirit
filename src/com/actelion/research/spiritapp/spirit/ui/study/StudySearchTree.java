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

package com.actelion.research.spiritapp.spirit.ui.study;

import com.actelion.research.spiritapp.spirit.ui.lf.StudyNode;
import com.actelion.research.spiritapp.spirit.ui.util.formtree.FormTree;
import com.actelion.research.spiritapp.spirit.ui.util.formtree.InputNode;
import com.actelion.research.spiritapp.spirit.ui.util.formtree.LabelNode;
import com.actelion.research.spiritapp.spirit.ui.util.formtree.Strategy;
import com.actelion.research.spiritapp.spirit.ui.util.formtree.AbstractNode.FieldType;
import com.actelion.research.spiritcore.business.RightLevel;
import com.actelion.research.spiritcore.business.study.StudyQuery;

/**
 * The Study Search tree is the tree used to query a study (by id or by keyword)
 * 
 * @author Joel Freyss
 *
 */
public class StudySearchTree extends FormTree {

	private final StudyQuery query = new StudyQuery();
	private final LabelNode root = new LabelNode(this, "Study:").setBold(true);
	
	private StudyNode studyNode = new StudyNode(this, RightLevel.WRITE, true, new Strategy<String>() {
		@Override
		public String getModel() {
			return query.getStudyIds();
		}
		@Override
		public void setModel(String modelValue) {
			if(modelValue!=null && modelValue.length()>0) {
				StudySearchTree.this.query.copyFrom(new StudyQuery());
			}
			query.setStudyIds(modelValue);
		}
		@Override
		public void onChange() {
			if(studyNode.getSelection().length()>0) {
				StudySearchTree.this.firePropertyChange(FormTree.PROPERTY_SUBMIT_PERFORMED, "", null);
			}
		}
	});
		
	public StudySearchTree() {
		setRootVisible(false);

		root.add(studyNode);
		
		root.add(new InputNode(this, FieldType.OR_CLAUSE, "Study Search", new Strategy<String>() {
			@Override
			public String getModel() {
				return query.getKeywords();
			}
			@Override
			public void setModel(String modelValue) {
				query.setKeywords(modelValue);
			}
		}));		

		setRoot(root);		
	}
	
	public StudyQuery getQuery() {
		updateModel();
		query.setUser(null);
		return query;
	}

	public void setQuery(StudyQuery query) {
		this.query.copyFrom(query);
		updateView();
	}

	public String getStudyIds() {
		return studyNode.getSelection();
	}

	public void setStudyIds(String studyIds) {
		studyNode.setSelection(studyIds);
		updateView();
	}


}
