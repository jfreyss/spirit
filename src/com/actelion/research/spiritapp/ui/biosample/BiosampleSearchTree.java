/*
 * Spirit, a study/biosample management tool for research.
 * Copyright (C) 2018 Idorsia Pharmaceuticals Ltd., Hegenheimermattweg 91,
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

package com.actelion.research.spiritapp.ui.biosample;

import java.awt.Image;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.ImageIcon;

import com.actelion.research.spiritapp.ui.SpiritFrame;
import com.actelion.research.spiritapp.ui.location.ContainerTypeComboBox;
import com.actelion.research.spiritapp.ui.location.LocationFormNode;
import com.actelion.research.spiritapp.ui.util.component.BiotypeNode;
import com.actelion.research.spiritapp.ui.util.component.BiotypeToggleNode;
import com.actelion.research.spiritapp.ui.util.component.CreDateNode;
import com.actelion.research.spiritapp.ui.util.component.CreUserNode;
import com.actelion.research.spiritapp.ui.util.component.DepartmentNode;
import com.actelion.research.spiritapp.ui.util.component.GroupNode;
import com.actelion.research.spiritapp.ui.util.component.PhaseNode;
import com.actelion.research.spiritapp.ui.util.component.QualityComboBox;
import com.actelion.research.spiritapp.ui.util.component.StudyNode;
import com.actelion.research.spiritapp.ui.util.component.UpdDateNode;
import com.actelion.research.spiritapp.ui.util.component.UpdUserNode;
import com.actelion.research.spiritapp.ui.util.formtree.AbstractNode;
import com.actelion.research.spiritapp.ui.util.formtree.AbstractNode.FieldType;
import com.actelion.research.spiritapp.ui.util.formtree.CheckboxNode;
import com.actelion.research.spiritapp.ui.util.formtree.FormTree;
import com.actelion.research.spiritapp.ui.util.formtree.InputNode;
import com.actelion.research.spiritapp.ui.util.formtree.LabelNode;
import com.actelion.research.spiritapp.ui.util.formtree.MultiNode;
import com.actelion.research.spiritapp.ui.util.formtree.ObjectComboBoxNode;
import com.actelion.research.spiritapp.ui.util.formtree.Strategy;
import com.actelion.research.spiritapp.ui.util.formtree.TextComboBoxNode;
import com.actelion.research.spiritapp.ui.util.icons.ImageFactory;
import com.actelion.research.spiritcore.business.DataType;
import com.actelion.research.spiritcore.business.Quality;
import com.actelion.research.spiritcore.business.RightLevel;
import com.actelion.research.spiritcore.business.biosample.BiosampleLinker;
import com.actelion.research.spiritcore.business.biosample.BiosampleLinker.LinkerType;
import com.actelion.research.spiritcore.business.biosample.BiosampleQuery;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.biosample.BiotypeCategory;
import com.actelion.research.spiritcore.business.biosample.BiotypeMetadata;
import com.actelion.research.spiritcore.business.biosample.ContainerType;
import com.actelion.research.spiritcore.business.employee.EmployeeGroup;
import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.spiritcore.business.property.PropertyKey;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.business.study.StudyQuery;
import com.actelion.research.spiritcore.services.dao.DAOBiotype;
import com.actelion.research.spiritcore.services.dao.DAOStudy;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.spiritcore.services.dao.SpiritProperties;
import com.actelion.research.util.ui.JCustomTextField;

public class BiosampleSearchTree extends FormTree {


	private final BiosampleQuery query = new BiosampleQuery();
	private final LabelNode top = new LabelNode(this, "Query Biosamples:");
	private final ContainerTypeComboBox containerTypeComboBox = new ContainerTypeComboBox();

	private final LabelNode stuNode = new LabelNode(this, "Study");
	private final LabelNode conNode = new LabelNode(this, "Container");
	private final LabelNode bioNode = new LabelNode(this, "");

	private final LabelNode filtersNode = new LabelNode(this, "Other Filters");
	private final LabelNode locationNode = new LabelNode(this, "Location");
	private final LabelNode advancedNode = new LabelNode(this, "Advanced");
	private LabelNode catSelectOneNode = new LabelNode(this, "Select One Sample per TopParent");
	private CheckboxNode cb1, cb2;
	private Biotype[] selectableBiotypes;
	private SpiritFrame frame;


	private final StudyNode studyNode = new StudyNode(this, RightLevel.READ, true, new Strategy<String>() {
		@Override
		public String getModel() {
			return query.getStudyIds();
		}
		@Override
		public void setModel(String modelValue) {
			query.setStudyIds(modelValue);
		}
		@Override
		public void onAction() {
			updateModel();
			eventStudyChanged();
			initLayout();
			setFocus(studyNode);
		}
	});

	private final GroupNode groupNode = new GroupNode(this, new Strategy<String>() {
		@Override
		public String getModel() {
			return query.getGroup();
		}
		@Override
		public void setModel(String modelValue) {
			query.setGroup(modelValue);
		}
	});
	private final PhaseNode phaseNode = new PhaseNode(this, new Strategy<String>() {
		@Override
		public String getModel() {
			return query.getPhases();
		}
		@Override
		public void setModel(String modelValue) {
			query.setPhases(modelValue);
		}
	});
	private final InputNode quickSearchNode = new InputNode(this, FieldType.OR_CLAUSE, "SampleIds/ContainerIds", new Strategy<String>() {

		@Override
		public String getModel() {
			return query.getSampleIdOrContainerIds();
		}
		@Override
		public void setModel(String modelValue) {
			query.setSampleIdOrContainerIds(modelValue);
		}
	});

	private final BiotypeNode bioTypeNode;

	private final InputNode keywordsNode = new InputNode(this, FieldType.AND_CLAUSE, "Keywords", new Strategy<String>() {
		@Override
		public String getModel() {
			String s = query.getKeywords();
			return s;
		}
		@Override
		public void setModel(String modelValue) {
			query.setKeywords(modelValue);
		}
	});

	private final LocationFormNode locationFormNode = new LocationFormNode(this, "Location", new Strategy<Location>() {
		@Override
		public Location getModel() {
			return query.getLocationRoot();
		}
		@Override
		public void setModel(Location modelValue) {
			query.setLocationRoot(modelValue);
		}
	});

	private final ObjectComboBoxNode<Quality> minQualityNode = new ObjectComboBoxNode<Quality>(this, "Min Quality", new QualityComboBox(), new Strategy<Quality>() {
		@Override public Quality getModel() {return query.getMinQuality();}
		@Override public void setModel(Quality modelValue) {query.setMinQuality(modelValue);}
	});

	private final ObjectComboBoxNode<Quality> maxQualityNode = new ObjectComboBoxNode<Quality>(this, "Max Quality", new QualityComboBox(), new Strategy<Quality>() {
		@Override public Quality getModel() {return query.getMaxQuality();}
		@Override public void setModel(Quality modelValue) {query.setMaxQuality(modelValue);}
	});

	private final CheckboxNode onlyContainerCheckbox = new CheckboxNode(this, "Only in Containers", new Strategy<Boolean>() {
		@Override
		public Boolean getModel() {
			return query.isFilterNotInContainer()==Boolean.TRUE;
		}
		@Override
		public void setModel(Boolean modelValue) {
			query.setFilterNotInContainer(modelValue==Boolean.TRUE);
		}
	});

	private final CheckboxNode onlyLocationCheckbox = new CheckboxNode(this, "Only in Locations", new Strategy<Boolean>() {
		@Override
		public Boolean getModel() {
			return query.isFilterNotInLocation()==Boolean.TRUE;
		}
		@Override
		public void setModel(Boolean modelValue) {
			query.setFilterNotInLocation(modelValue==Boolean.TRUE);
		}
	});

	private final CheckboxNode filterTrashNode = new CheckboxNode(this, "Hide Trashed/Used Up", new Strategy<Boolean>() {
		@Override
		public Boolean getModel() {
			return query.isFilterTrashed();
		}
		@Override
		public void setModel(Boolean modelValue) {
			query.setFilterTrashed(modelValue==Boolean.TRUE);
		}
	});


	public BiosampleSearchTree(SpiritFrame frame) {
		this(frame, null, false);
	}

	public BiosampleSearchTree(SpiritFrame frame, final Biotype[] selectableBiotypes, final boolean autoQuery) {
		super();
		this.frame = frame;
		setRootVisible(false);
		this.selectableBiotypes = selectableBiotypes;
		query.setFilterTrashed(selectableBiotypes!=null && selectableBiotypes.length>0);
		//		exactCheckBox.setOpaque(false);

		//Study Category
		if(frame==null) {
			stuNode.setCanExpand(false);
			stuNode.add(studyNode);
			if(SpiritProperties.getInstance().isChecked(PropertyKey.STUDY_FEATURE_STUDYDESIGN)) {
				stuNode.add(groupNode);
				stuNode.add(phaseNode);
			}
			top.add(stuNode);
		}


		//Container
		if(SpiritProperties.getInstance().isChecked(PropertyKey.BIOSAMPLE_CONTAINERTYPES)) {
			conNode.setCanExpand(false);
			conNode.add(new ObjectComboBoxNode<ContainerType>(this, "ContainerType", containerTypeComboBox, new Strategy<ContainerType>() {
				@Override
				public ContainerType getModel() {
					return query.getContainerType();
				}
				@Override
				public void setModel(ContainerType modelValue) {
					query.setContainerType(modelValue);
				}
			}));
			top.add(conNode);
		}

		//Biosample
		if(selectableBiotypes==null) {
			bioTypeNode = new BiotypeNode(this, new Strategy<Biotype>() {
				@Override
				public Biotype getModel() {
					return query.getBiotypes()!=null && query.getBiotypes().length==1? query.getBiotypes()[0]: null;
				}
				@Override
				public void setModel(Biotype modelValue) {
					query.setBiotypes(modelValue==null? null: new Biotype[]{modelValue});
				}

				@Override
				public void onChange() {
					if((bioTypeNode.getSelection()==null && query.getBiotype()!=null) || (bioTypeNode.getSelection()!=null && !bioTypeNode.getSelection().equals(query.getBiotype()))) {
						updateModel();
						eventBiotypeChanged();
					}
				}
			});

		} else {
			bioTypeNode = new BiotypeToggleNode(this, Arrays.asList(selectableBiotypes),  new Strategy<Biotype>() {
				@Override
				public Biotype getModel() {
					return query.getBiotypes()!=null && query.getBiotypes().length==1? query.getBiotypes()[0]: null;
				}
				@Override
				public void setModel(Biotype modelValue) {
					query.setBiotypes(modelValue==null? selectableBiotypes: new Biotype[]{modelValue});
				}

				@Override
				public void onAction() {
					if(bioTypeNode.getSelection()==null || !bioTypeNode.getSelection().equals(query.getBiotype())) {
						updateModel();
						eventBiotypeChanged();
					}
				}
			});
			bioTypeNode.setVisible(selectableBiotypes.length>1);
		}
		bioNode.setCanExpand(false);
		top.add(bioNode);

		//Location
		locationNode.setCanExpand(true);
		locationNode.add(locationFormNode);

		////////////////////////////
		//Filters

		//OneSample
		cb1 = new CheckboxNode(this, "Select the most-left", new Strategy<Boolean>() {
			@Override
			public Boolean getModel() {
				return query.getSelectOneMode()==BiosampleQuery.SELECT_MOST_LEFT;
			}
			@Override
			public void setModel(Boolean modelValue) {
				query.setSelectOneMode(modelValue? BiosampleQuery.SELECT_MOST_LEFT: BiosampleQuery.SELECT_ALL);
				if(modelValue) {
					query.setSelectOneMode(BiosampleQuery.SELECT_MOST_LEFT);
				}
			}
			@Override
			public void onAction() {
				cb2.getCheckbox().setSelected(false);
			}

		});
		catSelectOneNode.add(cb1);

		cb2 = new CheckboxNode(this, "Select the most-right", new Strategy<Boolean>() {
			@Override
			public Boolean getModel() {
				return query.getSelectOneMode()==BiosampleQuery.SELECT_MOST_RIGHT;
			}
			@Override
			public void setModel(Boolean modelValue) {
				if(modelValue) {
					query.setSelectOneMode(BiosampleQuery.SELECT_MOST_RIGHT);
				}
			}
			@Override
			public void onAction() {
				cb1.getCheckbox().setSelected(false);
			}

		});
		catSelectOneNode.add(cb2);

		top.add(filtersNode);

		//Advanced node
		if(frame!=null) {
			if(SpiritProperties.getInstance().isChecked(PropertyKey.STUDY_FEATURE_STUDYDESIGN)) {
				advancedNode.add(groupNode);
				advancedNode.add(phaseNode);
			}
		}

		//Creation
		advancedNode.add(new CreUserNode(this, new Strategy<String>() {
			@Override
			public String getModel() {
				return query.getCreUser();
			}
			@Override
			public void setModel(String modelValue) {
				query.setCreUser(modelValue);
			}
		}));

		advancedNode.add(new CreDateNode(this, new Strategy<String>() {
			@Override
			public String getModel() {
				return query.getCreDays();
			}
			@Override
			public void setModel(String modelValue) {
				query.setCreDays(modelValue);
			}
		}));

		advancedNode.add(new UpdUserNode(this, new Strategy<String>() {
			@Override
			public String getModel() {
				return query.getUpdUser();
			}
			@Override
			public void setModel(String modelValue) {
				query.setUpdUser(modelValue);
			}
		}));

		advancedNode.add(new UpdDateNode(this, new Strategy<String>() {
			@Override
			public String getModel() {
				return query.getUpdDays();
			}
			@Override
			public void setModel(String modelValue) {
				query.setUpdDays(modelValue);
			}
		}));
		advancedNode.add(new DepartmentNode(this, new Strategy<EmployeeGroup>() {
			@Override
			public EmployeeGroup getModel() {
				return query.getDepartment();
			}
			@Override
			public void setModel(EmployeeGroup modelValue) {
				query.setDepartment(modelValue);
			}
		}));
		advancedNode.add(catSelectOneNode);
		advancedNode.add(minQualityNode);
		advancedNode.add(maxQualityNode);
		advancedNode.add(onlyContainerCheckbox);
		advancedNode.add(onlyLocationCheckbox);


		//Trashed
		filtersNode.setCanExpand(false);
		if(selectableBiotypes!=null && selectableBiotypes.length>0) {
			stuNode.setVisible(selectableBiotypes.length==1 && (selectableBiotypes[0].getCategory()==BiotypeCategory.LIVING || selectableBiotypes[0].getCategory()==BiotypeCategory.SOLID || selectableBiotypes[0].getCategory()==BiotypeCategory.LIQUID));

			boolean canSelectContainer = false;
			for(Biotype type: selectableBiotypes) {
				if(!type.isAbstract() && !type.isHideContainer() && type.getContainerType()==null) {
					canSelectContainer = true;
					break;
				}
			}
			conNode.setVisible(canSelectContainer);

			bioNode.setExpanded(true);
			catSelectOneNode.setVisible(false);
			if(selectableBiotypes.length==1) {
				query.setBiotype(selectableBiotypes[0]);
				bioTypeNode.setSelection(selectableBiotypes[0]);
			}
			containerTypeComboBox.setVisible(false);
		} else {
			bioTypeNode.setValues(DAOBiotype.getBiotypes());

		}

		setRoot(top);
		eventStudyChanged();
	}


	/**
	 * Updates the filters of the study and biotypes
	 */
	public void eventStudyChanged() {
		BiosampleQuery query = getQuery();
		List<Study> studies = new ArrayList<>();
		try {
			if(query.getStudyIds()!=null && query.getStudyIds().length()>0) {
				studies = DAOStudy.queryStudies(StudyQuery.createForStudyIds(query.getStudyIds()), SpiritFrame.getUser());
			}
		}catch(Exception e) {
			e.printStackTrace();
		}

		//Update biotype/container filters
		if(studies.size()>0) {
			Set<ContainerType> allContainerTypes = new TreeSet<>();
			Set<Biotype> allBioTypes = new TreeSet<>();
			for (Study study : studies) {
				allContainerTypes.addAll(DAOStudy.getContainerTypes(study));
				allBioTypes.addAll(DAOStudy.getBiotypes(study));
			}
			containerTypeComboBox.setValues(allContainerTypes);
			bioTypeNode.setValues(allBioTypes);
			conNode.setVisible(containerTypeComboBox.getValues().size()>0);
		} else {
			containerTypeComboBox.setValues(Arrays.asList(ContainerType.values()));
			if(selectableBiotypes==null || selectableBiotypes.length==0) {
				bioTypeNode.setValues(DAOBiotype.getBiotypes());
			}
		}
		if(bioTypeNode.getValues().size()==1) {
			bioTypeNode.setSelection(bioTypeNode.getValues().iterator().next());
			//			bioTypeNode.setVisible(false);
		} else {
			//			bioTypeNode.setVisible(true);
		}
		//Update group, phases
		Study study = studies.size()==1? studies.get(0) : null;
		study = JPAUtil.reattach(study);
		groupNode.setStudy(study);
		phaseNode.setStudy(study);


		//filter available containers
		containerTypeComboBox.setVisible(containerTypeComboBox.getValues().size()>0);

		eventBiotypeChanged();
		bioNode.setVisible(bioTypeNode.getValues().size()>0);
	}


	/**
	 * Updates the filters of the biotypes
	 */
	public void eventBiotypeChanged() {
		bioNode.clearChildren();
		query.getLinker2values().clear();

		bioTypeNode.setCanExpand(false);
		bioNode.add(bioTypeNode);
		bioNode.add(keywordsNode);
		bioNode.add(quickSearchNode);

		Biotype type = query.getBiotype();

		if(type==null || type.isAbstract()){
			query.setFilterNotInContainer(false);
		}

		filtersNode.clearChildren();
		if(type!=null) {

			//Add filter for parents biotypes
			List<Biotype> parentTypes = type.getHierarchy();
			for(int i=0; i<parentTypes.size()-1; i++) {
				Biotype b = parentTypes.get(i);

				LabelNode node = new LabelNode(this, b.getName());
				node.setIcon(new ImageIcon(ImageFactory.getImage(b, 26)));
				node.setCanExpand(true);
				node.setExpanded(false);
				filtersNode.add(node);

				if(!b.isHideSampleId()) {
					addFilter(node, new BiosampleLinker(b, LinkerType.SAMPLEID));
				}

				if(b.getSampleNameLabel()!=null) {
					addFilter(node, new BiosampleLinker(b, LinkerType.SAMPLENAME));
				}
				for(BiotypeMetadata mt2: b.getMetadata()) {
					addFilter(node, new BiosampleLinker(mt2));
				}
				addFilter(node, new BiosampleLinker(b, LinkerType.COMMENTS));
			}

			//Add filter for selected biotype
			LabelNode node =  bioNode;

			//Filter for sampleId
			if(!type.isHideSampleId()) {
				addFilter(node, new BiosampleLinker(LinkerType.SAMPLEID, type));
			}

			//Filter for sampleName
			if(type.getSampleNameLabel()!=null) {
				addFilter(node, new BiosampleLinker(LinkerType.SAMPLENAME, type));
			}

			//Filters for metadata
			for(BiotypeMetadata mt2: type.getMetadata()) {
				if(mt2.getDataType()==DataType.BIOSAMPLE && mt2.getParameters()!=null) {
					//aggregated biosample
					Biotype biotype2 = DAOBiotype.getBiotype(mt2.getParameters());
					if(SpiritProperties.getInstance().isAdvancedMode()) {
						if(biotype2==null) {
							addFilter(node, new BiosampleLinker(mt2));
						} else {
							AbstractNode<?> n;
							if(!biotype2.isHideSampleId()) {
								n = addFilter(node, new BiosampleLinker(mt2, LinkerType.SAMPLEID, biotype2), mt2.getName());
							} else {
								n = new LabelNode(this, mt2.getName());
							}

							if(biotype2.getSampleNameLabel()!=null) {
								addFilter(n, new BiosampleLinker(mt2, LinkerType.SAMPLENAME, biotype2));
							}
							for(BiotypeMetadata mt3: biotype2.getMetadata()) {
								addFilter(n, new BiosampleLinker(mt2, mt3));
							}
							addFilter(n, new BiosampleLinker(mt2, LinkerType.COMMENTS, biotype2));
						}
					} else {
						if(!biotype2.isHideSampleId()) {
							addFilter(node, new BiosampleLinker(mt2, LinkerType.SAMPLEID, biotype2), mt2.getName());
						} else if(biotype2.getSampleNameLabel()!=null) {
							addFilter(node, new BiosampleLinker(mt2, LinkerType.SAMPLENAME, biotype2), mt2.getName());
						}
					}
				} else {
					//regular metadata
					addFilter(node, new BiosampleLinker(mt2));
				}
			}

			//Filter for comments
			addFilter(node, new BiosampleLinker(LinkerType.COMMENTS, type));

		}

		if(selectableBiotypes==null || selectableBiotypes.length>1 || !selectableBiotypes[0].isAbstract()) {
			locationNode.setExpanded(false);
			filtersNode.add(locationNode);
		}

		advancedNode.setExpanded(false);
		if(SpiritProperties.getInstance().isAdvancedMode()) {
			filtersNode.add(advancedNode);
		}
		filtersNode.add(filterTrashNode);

		updateView();
	}

	private AbstractNode<?> addFilter(AbstractNode<?> linkerNode, BiosampleLinker linker) {
		return addFilter(linkerNode, linker, linker.getLabelShort());
	}
	private AbstractNode<?> addFilter(AbstractNode<?> linkerNode, BiosampleLinker linker, String label) {
		AbstractNode<?> res = null;
		Biotype biotype2 = linker.getBiotypeForLabel();
		if(biotype2==null) return null;
		if(linker.getType()==LinkerType.SAMPLEID && biotype2.getCategory()==BiotypeCategory.LIBRARY) {
			linkerNode.add(res = new TextComboBoxNode(this, label, false, new Strategy<String>() {
				@Override public String getModel() {return query.getLinker2values().get(linker);}
				@Override public void setModel(String modelValue) {
					if(modelValue==null || modelValue.length()==0) query.getLinker2values().remove(linker);
					else query.getLinker2values().put(linker, modelValue);
				}
			}) {
				@Override
				public Collection<String> getChoices() {
					return DAOBiotype.getAutoCompletionFieldsForSampleId(biotype2);
				}
			});
		} else if(linker.getType()==LinkerType.SAMPLENAME && biotype2.isNameAutocomplete()) {
			linkerNode.add(res = new TextComboBoxNode(this, label, new Strategy<String>() {
				@Override public String getModel() {return query.getLinker2values().get(linker);}
				@Override public void setModel(String modelValue) {
					if(modelValue==null || modelValue.length()==0) query.getLinker2values().remove(linker);
					else query.getLinker2values().put(linker, modelValue);
				}
			}) {
				@Override
				public Collection<String> getChoices() {
					return DAOBiotype.getAutoCompletionFieldsForName(biotype2, frame==null? studyNode.getStudy(): frame.getStudy());
				}
			});
		} else if(linker.getType()==LinkerType.SAMPLENAME || linker.getType()==LinkerType.SAMPLEID){
			linkerNode.add(res = new InputNode(this, label, new Strategy<String>() {
				@Override public String getModel() {return query.getLinker2values().get(linker);}
				@Override public void setModel(String modelValue) {
					if(modelValue==null || modelValue.length()==0) query.getLinker2values().remove(linker);
					else query.getLinker2values().put(linker, modelValue);
				}
			}));
		} else if(linker.getType()==LinkerType.COMMENTS) {
			linkerNode.add(res = new TextComboBoxNode(this, label, false, new Strategy<String>() {
				@Override public String getModel() { return query.getLinker2values().get(linker);}
				@Override public void setModel(String modelValue) {
					if(modelValue==null || modelValue.length()==0) query.getLinker2values().remove(linker);
					else query.getLinker2values().put(linker, modelValue);
				}
			}) {
				@Override public Collection<String> getChoices() {return DAOBiotype.getAutoCompletionFieldsForComments(biotype2, frame==null? studyNode.getStudy(): frame.getStudy());}
			});
		} else if(linker.getBiotypeMetadata()!=null) {
			if(linker.getBiotypeMetadata().getDataType()==DataType.AUTO || linker.getBiotypeMetadata().getDataType()==DataType.LIST) {

				linkerNode.add(res = new TextComboBoxNode(this, label, false, new Strategy<String>() {
					@Override public String getModel() {return query.getLinker2values().get(linker);}
					@Override public void setModel(String modelValue) {
						if(modelValue==null || modelValue.length()==0) query.getLinker2values().remove(linker);
						else query.getLinker2values().put(linker, modelValue);
					}
				}) {
					@Override
					public Collection<String> getChoices() {
						return DAOBiotype.getAutoCompletionFields(linker.getBiotypeMetadata(), frame==null? studyNode.getStudy(): frame.getStudy());
					}
				});
			} else if(linker.getBiotypeMetadata().getDataType()==DataType.D_FILE) {
				//skip
			} else if(linker.getBiotypeMetadata().getDataType()==DataType.FILES) {
				//skip
			} else if(linker.getBiotypeMetadata().getDataType()==DataType.MULTI) {
				linkerNode.add(res = new MultiNode(this, label, linker.getBiotypeMetadata().extractChoices(), new Strategy<String>() {
					@Override public String getModel() {return query.getLinker2values().get(linker);}
					@Override public void setModel(String modelValue) {
						if(modelValue==null || modelValue.length()==0) query.getLinker2values().remove(linker);
						else query.getLinker2values().put(linker, modelValue);
					}
				}));
			} else {
				linkerNode.add(res = new InputNode(this, label, new Strategy<String>() {
					@Override public String getModel() {return query.getLinker2values().get(linker);}
					@Override public void setModel(String modelValue) {
						if(modelValue==null || modelValue.length()==0) query.getLinker2values().remove(linker);
						else query.getLinker2values().put(linker, modelValue);
					}
				}));

			}
		}
		if(res!=null && linker.getBiotypeForLabel()!=null && linker.isLinked()) {
			Image icon = ImageFactory.getImage(linker.getBiotypeForLabel(), 22);
			if(icon!=null && (res.getComponent()) instanceof JCustomTextField) {
				((JCustomTextField) res.getComponent()).setIcon(new ImageIcon(icon));
			}
		}
		return res;
	}

	public BiosampleQuery getQuery() {
		query.setSelectOneMode(BiosampleQuery.SELECT_ALL);
		query.setStudyIds(frame==null? null: frame.getStudyId());

		updateModel();
		return query;
	}

	public void setQuery(BiosampleQuery query) {
		//update our model
		this.query.copyFrom(query);
		if(frame!=null) {
			frame.setStudyId(query.getStudyIds());
		}
		updateView();

		//recreate the study tab
		eventStudyChanged();

		//recreate the metadata tab
		eventBiotypeChanged();
	}

	public String getStudyId() {
		return frame==null? studyNode.getSelection(): frame.getStudyId();
	}

	public void setStudyId(String v) {
		if(v==null || v.equals(frame==null? studyNode.getSelection(): frame.getStudyId())) return;
		setQuery(BiosampleQuery.createQueryForStudyIds(v));
	}

	public Biotype getBiotype() {
		return bioTypeNode.getSelection();
	}

	public void setBiotype(Biotype v) {
		bioTypeNode.setSelection(v);
	}


}
