package com.actelion.research.spirit.test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JPanel;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.actelion.research.spiritapp.ui.SpiritFrame;
import com.actelion.research.spiritcore.business.pivot.analyzer.Analyzer;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.ResultQuery;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.dao.DAOResult;
import com.actelion.research.spiritcore.services.dao.DAOStudy;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.util.ui.SwingWorkerExtended;

public class SwingWorkerTest extends AbstractSpiritTest {


	@BeforeClass
	public static void init() throws Exception {
		initDemoExamples(user);
		SwingWorkerExtended.SHOW_EXCEPTION = false;
	}


	/**
	 * Tests that there is only one active worker thread per component, and only the last is executed
	 */
	@Test
	public void testThreadOverwrite() {
		JPanel panel = new JPanel();
		final long s = System.currentTimeMillis();
		List<Integer> finished = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			final int index = i;
			new SwingWorkerExtended(panel, SwingWorkerExtended.FLAG_ASYNCHRONOUS20MS) {
				@Override
				protected void doInBackground() throws Exception {
					Thread.sleep(200);
				}
				@Override
				protected void done() {
					System.out.println("SwingWorkerThreadTest.testSequentialOrder()");
				}
			}.afterDone(() -> {
				finished.add(index);
				System.out.println("Thread-"+index+" completed in "+(System.currentTimeMillis()-s)+"ms");
			});

		}
		//Initially, nothing is fininshed
		Assert.assertEquals(0, finished.size());

		//After 500ms, the last thread should have been executed, and the others not
		try{Thread.sleep(500);} catch (Exception e) {}
		Assert.assertEquals(1, finished.size());
		Assert.assertEquals((Integer)9, finished.get(0));
	}


	@Test
	public void testAnalyzerInWorkerThreads() throws Exception {
		AtomicInteger n = new AtomicInteger();
		List<Study> studies = DAOStudy.getStudies();
		studies = studies.subList(0, 2);
		Assert.assertTrue(studies.size()>0);
		SpiritFrame.clearAll();
		for(Study s: studies) {
			List<Result> results = DAOResult.queryResults(ResultQuery.createQueryForStudyIds(s.getStudyId()), user);

			new SwingWorkerExtended(null, SwingWorkerExtended.FLAG_ASYNCHRONOUS20MS) {
				@Override
				protected void doInBackground() throws Exception {
					System.out.println("SwingWorkerTest.testAnalyzerInWorkerThreads() "+s+">"+results.size());
					new Analyzer(JPAUtil.reattach(results), user);
				}
			}.afterDone(() -> {n.incrementAndGet();});
		}

		//Wait for termination
		int timeout = 0;
		do {
			try{Thread.sleep(200);} catch (Exception e) {break;}
			if(timeout++>=250) throw new Exception("TimeOut");
		} while(n.get()!=studies.size());
		System.out.println("SwingWorkerTest.testAnalyzerInWorkerThreads() done");
	}
}
