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

package com.actelion.research.spiritapp.spirit.ui.biosample;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComponent;

import com.actelion.research.spiritapp.spirit.ui.SpiritFrame;
import com.actelion.research.spiritapp.spirit.ui.container.ContainerTypeComboBox;
import com.actelion.research.spiritapp.spirit.ui.icons.ImageFactory;
import com.actelion.research.spiritapp.spirit.ui.lf.BiotypeNode;
import com.actelion.research.spiritapp.spirit.ui.lf.BiotypeToggleNode;
import com.actelion.research.spiritapp.spirit.ui.lf.CreDateNode;
import com.actelion.research.spiritapp.spirit.ui.lf.CreUserNode;
import com.actelion.research.spiritapp.spirit.ui.lf.DepartmentNode;
import com.actelion.research.spiritapp.spirit.ui.lf.GroupNode;
import com.actelion.research.spiritapp.spirit.ui.lf.PhaseNode;
import com.actelion.research.spiritapp.spirit.ui.lf.QualityComboBox;
import com.actelion.research.spiritapp.spirit.ui.lf.StudyNode;
import com.actelion.research.spiritapp.spirit.ui.lf.UpdDateNode;
import com.actelion.research.spiritapp.spirit.ui.lf.UpdUserNode;
import com.actelion.research.spiritapp.spirit.ui.location.LocationFormNode;
import com.actelion.research.spiritapp.spirit.ui.util.formtree.AbstractNode.FieldType;
import com.actelion.research.spiritapp.spirit.ui.util.formtree.CheckboxNode;
import com.actelion.research.spiritapp.spirit.ui.util.formtree.ComboBoxNode;
import com.actelion.research.spiritapp.spirit.ui.util.formtree.FormTree;
import com.actelion.research.spiritapp.spirit.ui.util.formtree.InputNode;
import com.actelion.research.spiritapp.spirit.ui.util.formtree.LabelNode;
import com.actelion.research.spiritapp.spirit.ui.util.formtree.MultiNode;
import com.actelion.research.spiritapp.spirit.ui.util.formtree.Strategy;
import com.actelion.research.spiritapp.spirit.ui.util.formtree.TextComboBoxOneNode;
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
import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.business.study.StudyQuery;
import com.actelion.research.spiritcore.services.dao.DAOBiotype;
import com.actelion.research.spiritcore.services.dao.DAOStudy;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.spiritcore.util.ListHashMap;
import com.actelion.research.spiritcore.util.Pair;
import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.UIUtils;

public class BiosampleSearchTree extends FormTree {	
	
	
	private final BiosampleQuery query = new BiosampleQuery();
	private final LabelNode top = new LabelNode(this, "Query Biosamples:");
	private final ContainerTypeComboBox containerTypeComboBox = new ContainerTypeComboBox();
	
	private final LabelNode stuNode = new LabelNode(this, "Study");
	private final LabelNode conNode = new LabelNode(this, "Container");
	private final LabelNode bioNode = new LabelNode(this, "Biosample");
	
	private final LabelNode moreNode = new LabelNode(this, "Filters");
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

	private final JCheckBox exactCheckBox = new JCheckBox("Exact Match", false);
	
	private final InputNode keywordsNode = new InputNode(this, FieldType.AND_CLAUSE, "Keywords", new Strategy<String>() {
		@Override
		public String getModel() {
			String s = query.getKeywords();
			if(exactCheckBox.isSelected()) {
				if(s!=null && s.startsWith("\"") && s.endsWith("\"")) s = s.substring(1, s.length()-1);
			}
			return s;
		}
		@Override
		public void setModel(String modelValue) {
			if(exactCheckBox.isSelected()) {
				modelValue = modelValue.trim();
				if(modelValue!=null && modelValue.length()>0 && !modelValue.startsWith("\"")) modelValue = "\""+modelValue+"\"";
			}
			query.setKeywords(modelValue);
		}		
	}) {
		
		@Override
		public JComponent getComponent() {
			exactCheckBox.setIconTextGap(0);
			exactCheckBox.setFont(FastFont.SMALL);
			textField.setColumns(13);
			return UIUtils.createHorizontalBox(textField, UIUtils.createVerticalBox(exactCheckBox), Box.createHorizontalGlue());
		}
	};
	
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
	
	private final ComboBoxNode<Quality> minQualityNode = new ComboBoxNode<Quality>(this, new QualityComboBox(), "Min Quality", new Strategy<Quality>() {
		@Override public Quality getModel() {return query.getMinQuality();}
		@Override public void setModel(Quality modelValue) {query.setMinQuality(modelValue);}						
	});
	
	private final ComboBoxNode<Quality> maxQualityNode = new ComboBoxNode<Quality>(this, new QualityComboBox(), "Max Quality", new Strategy<Quality>() {
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
		exactCheckBox.setOpaque(false);
		
		//Study Category
		if(frame==null) {
			stuNode.setCanExpand(false);
			stuNode.add(studyNode);
			stuNode.add(groupNode);
			stuNode.add(phaseNode);
			top.add(stuNode);
		}
		

		//Container
		conNode.setCanExpand(false);
		conNode.add(new ComboBoxNode<ContainerType>(this, containerTypeComboBox, "", new Strategy<ContainerType>() {
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
				public void onAction() {	
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
					if(bioTypeNode.getSelection()==null || !bioTypeNode.getSelection().getName().equals(query.getBiotype())) {
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
				
		top.add(moreNode);

		if(frame!=null) {
			advancedNode.add(groupNode);
			advancedNode.add(phaseNode);
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
		advancedNode.add(new DepartmentNode(this, new Strategy<String>() {
			@Override
			public String getModel() {
				return query.getDepartment();
			}
			@Override
			public void setModel(String modelValue) {
				query.setDepartment(modelValue);
			}			
		}));
		advancedNode.add(catSelectOneNode);
//		advancedNode.add(minQualityNode);	
//		advancedNode.add(maxQualityNode);	
		advancedNode.add(onlyContainerCheckbox);
		advancedNode.add(onlyLocationCheckbox);
		
		//Quality
		minQualityNode.getComboBox().setEditable(false);
		maxQualityNode.getComboBox().setEditable(false);

		
		//Trashed
		
		moreNode.setCanExpand(false);
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
		
	
	public void eventStudyChanged() {		
				
		List<Study> studies = new ArrayList<>();
		try {
			if(query.getStudyIds()!=null && query.getStudyIds().length()>0) studies = DAOStudy.queryStudies(StudyQuery.createForStudyIds(query.getStudyIds()), SpiritFrame.getUser());
		}catch(Exception e) {
			e.printStackTrace();
		}

		//Update biotype/container filters
		if(studies.size()>0) {
			Set<ContainerType> allContainerTypes = new TreeSet<ContainerType>();
			Set<Biotype> allBioTypes = new TreeSet<Biotype>();
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
		
		//Update group, phases
		Study study = studies.size()==1? studies.get(0) : null;
		study = JPAUtil.reattach(study);
		groupNode.setStudy(study);
		phaseNode.setStudy(study);


		//filter available containers
		containerTypeComboBox.setVisible(containerTypeComboBox.getValues().size()>0);

		eventBiotypeChanged();
		bioNode.setVisible(bioTypeNode.getComboBox().getValues().size()>0);
	}
	

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

		moreNode.clearChildren();
		if(type!=null) {	
			//
			//Gets linkers
			ListHashMap<Pair<String, Biotype>, BiosampleLinker> linkers = getLinkers(type);
			List<Pair<String, Biotype>> keys = new ArrayList<Pair<String, Biotype>>(linkers.keySet());
			Collections.sort(keys, new Comparator<Pair<String, Biotype>>() {
				@Override
				public int compare(Pair<String, Biotype> o1, Pair<String, Biotype> o2) {
					return o1.getSecond().compareTo(o2.getSecond());
				}
			});
			for (Pair<String, Biotype> key : keys) {
				final Biotype biotype2 = key.getSecond();
				LabelNode linkerNode = new LabelNode(this, key.getFirst());
				linkerNode.setIcon(new ImageIcon(ImageFactory.getImage(biotype2, 26)));
				linkerNode.setCanExpand(true);
				linkerNode.setExpanded(false);
				moreNode.add(linkerNode);
				for (final BiosampleLinker linker : linkers.get(key)) {
					String label = linker.getLabelShort();
					if(linker.getType()==LinkerType.SAMPLEID && biotype2.getCategory()==BiotypeCategory.LIBRARY) {
						linkerNode.add(new TextComboBoxOneNode(this, label, false, new Strategy<String>() {
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
					} else if(linker.getBiotypeMetadata()!=null && (linker.getBiotypeMetadata().getDataType()==DataType.AUTO || linker.getBiotypeMetadata().getDataType()==DataType.LIST /* || linker.getBiotypeMetadata().getDataType()==DataType.DICO*/)) {
						linkerNode.add(new TextComboBoxOneNode(this, label, false, new Strategy<String>() {
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
					} else if(linker.getBiotypeMetadata()!=null && linker.getBiotypeMetadata().getDataType()==DataType.D_FILE) {
						continue;
					} else if(linker.getBiotypeMetadata()!=null && linker.getBiotypeMetadata().getDataType()==DataType.FILES) {
						continue;
					} else if(linker.getBiotypeMetadata()!=null && linker.getBiotypeMetadata().getDataType()==DataType.MULTI) {					
						linkerNode.add(new MultiNode(this, label, linker.getBiotypeMetadata().extractChoices(), new Strategy<String>() {
							@Override public String getModel() {return query.getLinker2values().get(linker);}
							@Override public void setModel(String modelValue) {
								if(modelValue==null || modelValue.length()==0) query.getLinker2values().remove(linker);
								else query.getLinker2values().put(linker, modelValue);
							}
						}));							
					} else if(linker.getType()==LinkerType.SAMPLENAME && biotype2.isNameAutocomplete()) {
						linkerNode.add(new TextComboBoxOneNode(this, label, false, new Strategy<String>() {
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
					} else if(linker.getType()==LinkerType.COMMENTS) {
						linkerNode.add(new TextComboBoxOneNode(this, label, false, new Strategy<String>() {
							@Override public String getModel() { return query.getLinker2values().get(linker);}
							@Override public void setModel(String modelValue) {
								if(modelValue==null || modelValue.length()==0) query.getLinker2values().remove(linker);
								else query.getLinker2values().put(linker, modelValue);
							}						
						}) {
							@Override public Collection<String> getChoices() {return DAOBiotype.getAutoCompletionFieldsForComments(biotype2, frame==null? studyNode.getStudy(): frame.getStudy());} 			
						});
					} else {							
						linkerNode.add(new InputNode(this, FieldType.AND_CLAUSE, label, new Strategy<String>() {
							@Override public String getModel() {return query.getLinker2values().get(linker);}
							@Override public void setModel(String modelValue) {
								if(modelValue==null || modelValue.length()==0) query.getLinker2values().remove(linker);
								else query.getLinker2values().put(linker, modelValue);
							}
						}));
					}
				}											
			}
		}
		
		if(selectableBiotypes==null || selectableBiotypes.length>1 || !selectableBiotypes[0].isAbstract()) {
			locationNode.setExpanded(false);
			moreNode.add(locationNode);
		}
		
		advancedNode.setExpanded(false);
		moreNode.add(advancedNode);
		
		moreNode.add(filterTrashNode);
		
		updateView();
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
	

	public static ListHashMap<Pair<String, Biotype>, BiosampleLinker> getLinkers(Biotype biotype) {
		ListHashMap<Pair<String, Biotype>, BiosampleLinker> res = new ListHashMap<Pair<String, Biotype>, BiosampleLinker>();

		//Look at own metadata
		{
			
			Pair<String, Biotype> key = new Pair<String, Biotype>(biotype.getName(), biotype);
			if(!biotype.isHideSampleId()) {
				res.add(key, new BiosampleLinker(LinkerType.SAMPLEID, biotype));				
			} 
			if(biotype.getSampleNameLabel()!=null) {
				res.add(key, new BiosampleLinker(LinkerType.SAMPLENAME, biotype));
			}
			for(BiotypeMetadata mt2: biotype.getMetadata()) {
				res.add(key, new BiosampleLinker(mt2));						
			}
			res.add(key, new BiosampleLinker(LinkerType.COMMENTS, biotype));
		}
		
		
		//Look at aggregated Data
		for(BiotypeMetadata mt: biotype.getMetadata()) {
			if(mt.getDataType()!=DataType.BIOSAMPLE) continue;
			if(mt.getParameters()==null) continue;
			Biotype biotype2 = DAOBiotype.getBiotype(mt.getParameters());
			if(biotype2==null) continue;
			String label = mt.getName();
			Pair<String, Biotype> key = new Pair<String, Biotype>(label, biotype2);
			
			if(!biotype2.isHideSampleId()) {
				res.add(key, new BiosampleLinker(mt, LinkerType.SAMPLEID));
			}
			if(biotype2.getSampleNameLabel()!=null) {
				res.add(key, new BiosampleLinker(mt, LinkerType.SAMPLENAME, biotype2));
			}
			for(BiotypeMetadata mt2: biotype2.getMetadata()) {
				res.add(key, new BiosampleLinker(mt, mt2));						
			}
			res.add(key, new BiosampleLinker(mt, LinkerType.COMMENTS, biotype2));
		}
		
		//Look at parent types
		Biotype b = biotype.getParent();
		while(b!=null) {
			String label = b.getName();
			Pair<String, Biotype> key = new Pair<String, Biotype>(label, b);
			
			if(!b.isHideSampleId()) {
				res.add(key, new BiosampleLinker(b, LinkerType.SAMPLEID));
			}

			if(b.getSampleNameLabel()!=null) {
				res.add(key, new BiosampleLinker(b, LinkerType.SAMPLENAME));
			}
			for(BiotypeMetadata mt2: b.getMetadata()) {
				res.add(key, new BiosampleLinker(mt2));						
			}
			res.add(key, new BiosampleLinker(b, LinkerType.COMMENTS));
			
			b = b.getParent();
		}
		return res;
	}
}
