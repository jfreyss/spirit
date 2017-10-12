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

package com.actelion.research.spiritapp.spirit.ui.result;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.actelion.research.spiritapp.spirit.ui.SpiritFrame;
import com.actelion.research.spiritapp.spirit.ui.util.formtree.AbstractNode;
import com.actelion.research.spiritapp.spirit.ui.util.formtree.AbstractNode.FieldType;
import com.actelion.research.spiritapp.spirit.ui.util.formtree.CheckboxNode;
import com.actelion.research.spiritapp.spirit.ui.util.formtree.FormTree;
import com.actelion.research.spiritapp.spirit.ui.util.formtree.InputNode;
import com.actelion.research.spiritapp.spirit.ui.util.formtree.LabelNode;
import com.actelion.research.spiritapp.spirit.ui.util.formtree.ObjectComboBoxNode;
import com.actelion.research.spiritapp.spirit.ui.util.formtree.Strategy;
import com.actelion.research.spiritapp.spirit.ui.util.formtree.TextComboBoxMultipleNode;
import com.actelion.research.spiritapp.spirit.ui.util.formtree.TextComboBoxNode;
import com.actelion.research.spiritapp.spirit.ui.util.lf.BiotypeNode;
import com.actelion.research.spiritapp.spirit.ui.util.lf.CreUserNode;
import com.actelion.research.spiritapp.spirit.ui.util.lf.PhaseNode;
import com.actelion.research.spiritapp.spirit.ui.util.lf.QualityComboBox;
import com.actelion.research.spiritapp.spirit.ui.util.lf.UpdDateNode;
import com.actelion.research.spiritcore.business.Quality;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.result.ResultQuery;
import com.actelion.research.spiritcore.business.result.Test;
import com.actelion.research.spiritcore.business.result.TestAttribute;
import com.actelion.research.spiritcore.business.study.Phase;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.business.study.StudyQuery;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.spiritcore.services.dao.DAOBiotype;
import com.actelion.research.spiritcore.services.dao.DAOResult;
import com.actelion.research.spiritcore.services.dao.DAOStudy;
import com.actelion.research.spiritcore.services.dao.DAOTest;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.SwingWorkerExtended;

public class ResultSearchTree extends FormTree {

	private final SpiritFrame frame;
	private final Biotype forcedBiotype;
	private List<Test> tests;

	private final ResultQuery query = new ResultQuery();
	private final LabelNode root = new LabelNode(this, "Query Results:");
	private final LabelNode rootTest = new LabelNode(this, "Test");
	private final LabelNode biotypeNode = new LabelNode(this, "Biotype");
	private final LabelNode inputNode = new LabelNode(this, "Input");
	//	private final LabelNode outputNode = new LabelNode(this, "Output");
	private final LabelNode advancedNode = new LabelNode(this, "Advanced");

	private TextComboBoxNode elbNode;
	private Map<String, ResultFilters> memoLastResultFilters = new HashMap<>();

	private int push = 0;

	private static class ResultFilters {
		public final Set<String> types = new HashSet<>();
		public final Map<TestAttribute, Set<String>> inputChoices = new LinkedHashMap<>();
		public final Set<TestAttribute> outputDisplays = new LinkedHashSet<>();

	}

	public ResultSearchTree(SpiritFrame frame, Biotype forcedBiotype) {
		super();
		this.frame = frame;
		this.forcedBiotype = forcedBiotype;
		setRootVisible(false);

		if(forcedBiotype==null) {
			//General Spirit Search
			//			root.add(studyNode);
		} else {
			//StockCare Biotype Search
			BiotypeNode biotypeComboNode = new BiotypeNode(this, Collections.singleton(forcedBiotype), new Strategy<Biotype>(){
				@Override
				public Biotype getModel() {return query.getBiotype()==null? null: DAOBiotype.getBiotype(query.getBiotype());};
				@Override
				public void setModel(Biotype modelValue) {query.setBiotype(modelValue==null? null: modelValue.getName());};
			});
			query.setBiotype(forcedBiotype.getName());
			biotypeComboNode.setSelection(forcedBiotype);
			biotypeComboNode.setEnabled(false);
			root.add(biotypeComboNode);


			InputNode sampleIdNode = new InputNode(this, FieldType.OR_CLAUSE, forcedBiotype.getName()+"Ids", new Strategy<String>(){
				@Override
				public String getModel() {return query.getSampleIds();};
				@Override
				public void setModel(String modelValue) {query.setSampleIds(modelValue);};
			});
			root.add(sampleIdNode);
		}



		root.add(new InputNode(this, FieldType.AND_CLAUSE, "ParticipantIds", new Strategy<String>() {
			@Override
			public String getModel() {
				return query.getTopSampleIds();
			}
			@Override
			public void setModel(String modelValue) {
				query.setTopSampleIds(modelValue);
			}
		}));


		root.add(new InputNode(this, FieldType.AND_CLAUSE, "Keywords", new Strategy<String>() {
			@Override
			public String getModel() {
				return query.getKeywords();
			}
			@Override
			public void setModel(String modelValue) {
				query.setKeywords(modelValue);
			}
		}));


		//TestNode
		root.add(rootTest);

		//biotypeNode
		biotypeNode.setCanExpand(false);
		root.add(biotypeNode);

		//inputNode
		root.add(inputNode);

		//		//outputNode
		//		root.add(outputNode);

		///////////////////////////////////////////////////////////////////////////////
		//advancedNode
		///////////////////////////////////////////////////////////////////////////////
		elbNode = new TextComboBoxNode(this, "ELBs", true, new Strategy<String>() {
			@Override
			public String getModel() {
				return query.getElbs();
			}
			@Override
			public void setModel(String modelValue) {
				query.setElbs(modelValue);
			}
			@Override
			public void onAction() {
				if(push>0) return;
				try {
					push++;
					refreshFilters(false);
					setSelection(elbNode);
					setFocus(elbNode);
				} finally {
					push--;
				}
			}
		}) {
			@Override
			public Collection<String> getChoices() {
				return DAOResult.getElbsForStudy(frame==null? null: frame.getStudyId());

			}
		};
		advancedNode.add(elbNode);

		PhaseNode phaseNode = new PhaseNode(this, new Strategy<String>() {
			@Override
			public String getModel() {
				return query.getPhases();
			}
			@Override
			public void setModel(String modelValue) {
				query.setPhases(modelValue);
			}

		}) {
			@Override
			public Collection<String> getChoices() {
				Set<String> res = new TreeSet<String>();
				try {
					for(Study s: DAOStudy.queryStudies(StudyQuery.createForStudyIds(frame==null? null: frame.getStudyId()), SpiritFrame.getUser())){
						for(Phase p: s.getPhases()) {
							res.add(p.getShortName());
						}
					}
				} catch(Exception e) {
					e.printStackTrace();
				}
				return new ArrayList<String>(res);

			}
		};
		advancedNode.add(phaseNode);

		advancedNode.add(new CreUserNode(this, new Strategy<String>() {
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
				return query.getUpdDate();
			}
			@Override
			public void setModel(String modelValue) {
				query.setUpdDate(modelValue);
			}
		}));

		ObjectComboBoxNode<Quality> qualityNode = new ObjectComboBoxNode<Quality>(this, "Min Quality", new QualityComboBox(), new Strategy<Quality>() {
			@Override
			public Quality getModel() {
				return query.getQuality();
			}
			@Override
			public void setModel(Quality modelValue) {
				query.setQuality(modelValue);
			}
		});
		advancedNode.add(qualityNode);

		root.add(advancedNode);


		setRoot(root);

		expandAll(false);

	}

	public SwingWorkerExtended refreshFilters(final boolean onlyFilters) {
		final SpiritUser user = SpiritFrame.getUser();
		if(user==null || frame==null) return new SwingWorkerExtended();

		return new SwingWorkerExtended("Updating Filters", this, SwingWorkerExtended.FLAG_ASYNCHRONOUS100MS) {
			ResultFilters inputKeywords;
			List<Study> studies;
			@Override
			protected void doInBackground() throws Exception {

				studies = query.getStudyIds()==null || query.getStudyIds().length()==0? null: DAOStudy.queryStudies(StudyQuery.createForStudyIds(query.getStudyIds()), user);
				inputKeywords = getResultFilters();
			}

			@Override
			protected void done() {
				synchronized (ResultSearchTree.class) {
					//Add the test nodes
					if(!onlyFilters) {
						String elbs = query.getElbs();

						Map<Test, Integer> counter = null;
						if(elbs!=null && elbs.length()>0) {
							tests = DAOTest.getTestsFromElbs(elbs);
						} else {
							counter = DAOStudy.countResults(studies, forcedBiotype);
							tests = studies!=null && studies.size()>0? new ArrayList<>(counter.keySet()): DAOTest.getTests();
						}

						rootTest.clearChildren();
						String oldCategory = null;
						LabelNode catNode = null;
						for (final Test test : tests) {
							String category = test.getCategory();
							if(category==null) {
								System.err.println("Null category for: "+test);
								continue;
							}

							//Define the Test Node
							final CheckboxNode testNode = new CheckboxNode(ResultSearchTree.this, test.getName() + (counter!=null && counter.get(test)!=null? " ("+counter.get(test)+")":""));

							//Define The Category Node
							if(catNode==null || oldCategory==null || !oldCategory.equals(category)) {
								final LabelNode n = new LabelNode(ResultSearchTree.this, category);
								n.setCanExpand(false);
								rootTest.add(n);
								oldCategory = category;
								catNode = n;
							}
							catNode.add(testNode);

							//Add the test strategy
							testNode.setCanExpand(false);
							testNode.setStrategy(new Strategy<Boolean>() {
								@Override
								public Boolean getModel() {
									return query.getTestIds().contains(test.getId());
								}
								@Override
								public void setModel(Boolean modelValue) {
									if(modelValue!=null && modelValue) {
										query.getTestIds().add(test.getId());
									} else {
										query.getTestIds().remove(test.getId());
									}
								}
								@Override
								public void onAction() {
									getQuery();
									refreshFilters(true);
								}
							});
						}
					}

					if(!onlyFilters) {
						rootTest.setCanExpand(false);
						rootTest.setVisible(rootTest.getChildren().size()>0);
						for(AbstractNode<?> node : rootTest.getChildren()) {
							node.setExpanded(true);
						}
					}
					biotypeNode.setVisible(inputKeywords.types.size()>0);
					inputNode.setVisible(inputKeywords.inputChoices.size()>0);

					query.getBiotypes().retainAll(inputKeywords.types);
					for(TestAttribute att: inputKeywords.inputChoices.keySet()) {
						if(query.getAttribute2Values().get(att)!=null) query.getAttribute2Values().get(att).retainAll(inputKeywords.inputChoices.get(att));
					}

					biotypeNode.clearChildren();
					for (final String biotype : new TreeSet<String>(inputKeywords.types)) {
						biotypeNode.add(new CheckboxNode(ResultSearchTree.this, biotype, new Strategy<Boolean>() {
							@Override
							public void setModel(Boolean modelValue) {
								if(modelValue==Boolean.TRUE) query.getBiotypes().add(biotype);
								else query.getBiotypes().remove(biotype);
							}
							@Override
							public Boolean getModel() {
								return query.getBiotypes().contains(biotype);
							}
						}));
					}
					inputNode.clearChildren();
					for(final TestAttribute ta: inputKeywords.inputChoices.keySet()) {
						if(inputKeywords.inputChoices.get(ta)==null || inputKeywords.inputChoices.get(ta).size()<=0) continue;

						//Add Input Node
						LabelNode inputNode2 = new LabelNode(ResultSearchTree.this, ta.getTest().getName()+"."+ta.getName());
						inputNode.add(inputNode2);

						final Set<String> choices = new TreeSet<String>(inputKeywords.inputChoices.get(ta));
						if(choices.size()<20) {
							for (final String input : choices) {
								inputNode2.add(new CheckboxNode(ResultSearchTree.this, input.equals("")? "NONE": input, new Strategy<Boolean>() {
									@Override
									public void setModel(Boolean modelValue) {
										if(modelValue==Boolean.TRUE) query.getAttribute2Values().add(ta, input);
										else query.getAttribute2Values().delete(ta, input);
									}
									@Override
									public Boolean getModel() {
										return query.getAttribute2Values().get(ta)!=null && query.getAttribute2Values().get(ta).contains(input);
									}
								}));
								inputNode2.setExpanded(true);
							}
						} else {
							inputNode2.add(new TextComboBoxMultipleNode(ResultSearchTree.this, FieldType.OR_CLAUSE, ta.getName(), new Strategy<String[]>() {
								@Override
								public void setModel(String[] inputs) {
									query.getAttribute2Values().remove(ta);
									for(String s: inputs) {
										query.getAttribute2Values().add(ta, s);
									}
								}
								@Override
								public String[] getModel() {
									if(query.getAttribute2Values().get(ta)!=null && query.getAttribute2Values().get(ta).size()>0) {
										return query.getAttribute2Values().get(ta).toArray(new String[0]);
									} else {
										return new String[0];
									}
								}
							}) {
								@Override
								public Collection<String> getChoices() {
									return choices;
								}
							});
						}
					}
					/*
					//Add Output Node
					outputNode.clearChildren();

					for(final TestAttribute ta: inputKeywords.outputDisplays) {
						CheckboxNode cb = new CheckboxNode(ResultSearchTree.this, ta.getTest().getName()+"."+ta.getName(), new Strategy<Boolean>() {
							@Override
							public void setModel(Boolean modelValue) {
								if(modelValue) {
									query.getSkippedOutputAttribute().remove(ta);
								} else {
									query.getSkippedOutputAttribute().add(ta);
								}
							}
							@Override
							public Boolean getModel() {
								return !query.getSkippedOutputAttribute().contains(ta);
							}
						});
						outputNode.add(cb);
					}
					 */
					inputNode.setExpanded(true);
					updateView();
				}
			}

		};
	}

	/**
	 * Gets the query and update the model
	 * @return
	 */
	public ResultQuery getQuery() {
		updateModel();
		return query;
	}

	/**
	 * Sets the query and update the view
	 * @param query
	 */
	public void setQuery(ResultQuery query) {
		setSelection(null);
		this.query.copyFrom(query);
		if(frame!=null) {
			frame.setStudyId(query.getStudyIds());
		}
		refreshFilters(false).afterDone(() -> updateView());
	}

	public void repopulate() {
		refreshFilters(false).afterDone(() -> {
			updateView();
			memoLastResultFilters.clear();
		});
	}

	/**
	 * Suggest possibilities for filters: biotype, gene
	 */
	private ResultFilters getResultFilters() {
		ResultQuery q = query;


		if(q.isEmpty()) return new ResultFilters();

		try {
			//For faster queries, memorize the last 5 entries
			String keyQuery = q.getQueryKey();
			ResultFilters res = memoLastResultFilters.get(keyQuery);
			if(res!=null) return res;

			res = new ResultFilters();
			Set<Integer> testIds = new HashSet<>(q.getTestIds());

			//Find possible biotypes
			if(forcedBiotype==null) {
				for (Biotype biotype : DAOResult.getBiotypes(q.getStudyIds(), testIds)) {
					res.types.add(biotype.getName());
				}
			}

			//Find possible inputs
			for(int testId: testIds) {
				Map<TestAttribute, Collection<String>> inputChoices = DAOTest.getInputFields(testId, q.getStudyIds());
				for (Map.Entry<TestAttribute, Collection<String>> e : inputChoices.entrySet()) {
					res.inputChoices.put(e.getKey(), new LinkedHashSet<>(e.getValue()));
				}
			}
			for(Test t: DAOTest.getTests(testIds)) {
				List<TestAttribute> tas = t.getOutputAttributes();
				if(tas.size()>1) res.outputDisplays.addAll(tas);

			}

			if(memoLastResultFilters.size()>5) memoLastResultFilters.clear();
			memoLastResultFilters.put(keyQuery, res);
			return res;

		} catch(Exception e) {
			JExceptionDialog.showError(e);
			return new ResultFilters();
		}
	}
}
