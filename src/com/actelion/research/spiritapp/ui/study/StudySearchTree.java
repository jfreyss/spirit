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

package com.actelion.research.spiritapp.ui.study;

import java.util.Arrays;
import java.util.Collection;

import com.actelion.research.spiritapp.ui.SpiritFrame;
import com.actelion.research.spiritapp.ui.util.formtree.FormTree;
import com.actelion.research.spiritapp.ui.util.formtree.InputNode;
import com.actelion.research.spiritapp.ui.util.formtree.LabelNode;
import com.actelion.research.spiritapp.ui.util.formtree.Strategy;
import com.actelion.research.spiritapp.ui.util.formtree.TextComboBoxNode;
import com.actelion.research.spiritapp.ui.util.formtree.AbstractNode.FieldType;
import com.actelion.research.spiritcore.business.DataType;
import com.actelion.research.spiritcore.business.property.PropertyKey;
import com.actelion.research.spiritcore.business.study.StudyQuery;
import com.actelion.research.spiritcore.services.dao.DAOStudy;
import com.actelion.research.spiritcore.services.dao.SpiritProperties;

/**
 * The Study Search tree is the tree used to query a study (by id or by keyword)
 *
 * @author Joel Freyss
 *
 */
public class StudySearchTree extends FormTree {

	private final StudyQuery query = new StudyQuery();
	private final LabelNode root = new LabelNode(this, "Study:").setBold(true);

	public StudySearchTree(SpiritFrame frame) {
		setRootVisible(false);

		root.add(new InputNode(this, FieldType.OR_CLAUSE, "Keywords", new Strategy<String>() {
			@Override
			public String getModel() {
				return query.getKeywords();
			}
			@Override
			public void setModel(String modelValue) {
				query.setKeywords(modelValue);
			}
		}));

		if(SpiritProperties.getInstance().getValues(PropertyKey.STUDY_STATES).length>0) {
			root.add(new TextComboBoxNode(this, "State", Arrays.asList(SpiritProperties.getInstance().getValues(PropertyKey.STUDY_STATES)), new Strategy<String>() {
				@Override
				public String getModel() {
					return query.getState();
				}
				@Override
				public void setModel(String modelValue) {
					query.setState(modelValue);
				}
			}));
		}

		if(SpiritProperties.getInstance().getValues(PropertyKey.STUDY_TYPES).length>0) {
			root.add(new TextComboBoxNode(this, "Type", Arrays.asList(SpiritProperties.getInstance().getValues(PropertyKey.STUDY_TYPES)), new Strategy<String>() {
				@Override
				public String getModel() {
					return query.getType();
				}
				@Override
				public void setModel(String modelValue) {
					query.setType(modelValue);
				}
			}));
		}

		for (final String metadata : SpiritProperties.getInstance().getValues(PropertyKey.STUDY_METADATA)) {
			String label = SpiritProperties.getInstance().getValue(PropertyKey.STUDY_METADATA_NAME, metadata);
			String dataType = SpiritProperties.getInstance().getValue(PropertyKey.STUDY_METADATA_DATATYPE, metadata);
			if(DataType.AUTO.name().equals(dataType)) {
				root.add(new TextComboBoxNode(this, label, new Strategy<String>() {
					@Override
					public String getModel() {
						return query.getMetadata(metadata);
					}
					@Override
					public void setModel(String modelValue) {
						query.setMetadata(metadata, modelValue);
					}
				}) {
					@Override
					public Collection<String> getChoices() {
						return DAOStudy.getMetadataValues(metadata);
					}
				});
			} else if(DataType.LIST.name().equals(dataType)) {
				root.add(new TextComboBoxNode(this, label, Arrays.asList(SpiritProperties.getInstance().getValues(PropertyKey.STUDY_METADATA_PARAMETERS, metadata)), new Strategy<String>() {
					@Override
					public String getModel() {
						return query.getMetadata(metadata);
					}
					@Override
					public void setModel(String modelValue) {
						query.setMetadata(metadata, modelValue);
					}
				}));
			}

		}


		setRoot(root);
	}

	public StudyQuery getQuery() {
		updateModel();
		query.setUser(null);
		//		query.setStudyIds(frame==null? null: frame.getStudyId());
		return query;
	}

	public void setQuery(StudyQuery query) {
		this.query.copyFrom(query);
		updateView();
	}

	//	public String getStudyIds() {
	//		return studyNode.getSelection();
	//	}
	//
	//	public void setStudyIds(String studyIds) {
	//		studyNode.setSelection(studyIds);
	//		updateView();
	//	}


}
