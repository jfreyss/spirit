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

package com.actelion.research.spiritcore.services.helper;

import java.util.HashSet;
import java.util.Set;

import com.actelion.research.spiritcore.business.DataType;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.biosample.BiotypeMetadata;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.ResultValue;
import com.actelion.research.spiritcore.business.result.Test;
import com.actelion.research.spiritcore.business.result.TestAttribute;
import com.actelion.research.spiritcore.util.MiscUtils;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import net.objecthunter.exp4j.function.Function;

public class ExpressionHelper {

	/**
	 * Create a new ExpressionBuilder, while adding new functions:
	 * - 
	 * @param expr
	 * @return
	 * @throws Exception
	 */
	public static ExpressionBuilder createExpressionBuilder(String expr) throws Exception {
		ExpressionBuilder e = new ExpressionBuilder(expr);
		Function round = new Function("round", 2) {
		    @Override
		    public double apply(double... args) {
		        return Math.round(args[0] * Math.pow(10, args[1])) / Math.pow(10, args[1]);
		    }
		};
		e.function(round);
		return e;		
	}
	
	/**
	 * Creates an expression using I1 for the input attributes and O1, O2 for the output
	 * @param expr
	 * @param test
	 * @return
	 * @throws Exception
	 */
	public static Expression createExpression(String expr, Test test) throws Exception {
		ExpressionBuilder e = createExpressionBuilder(expr);
		
		if(test!=null) {
			Set<String> variables = new HashSet<>();
			int i = 0;
			for(TestAttribute a: test.getInputAttributes()) {
				i++;
				if(a.getDataType()==DataType.NUMBER) variables.add("I"+i);
			}
			i = 0;
			for(TestAttribute a: test.getOutputAttributes()) {
				i++;
				if(a.getDataType()==DataType.NUMBER) variables.add("O"+i);
			}
			e = e.variables(variables);
		}
		return e.build();
	}
	
	/**
	 * Evaluate the given expression on the result. The Expression must have been created through createExpression
	 * @param expr
	 * @param result
	 * @return
	 * @throws Exception
	 */
	public static double evaluate(Expression expr, Result result) throws Exception {
		Test test = result.getTest();
		int i = 0;
		for(TestAttribute a: test.getInputAttributes()) {
			i++;
			ResultValue rv = result.getResultValue(a);						
			if(a.getDataType()==DataType.NUMBER && rv.getDoubleValue()!=null) {
				expr.setVariable("I" + i, rv.getDoubleValue());
			}
		}
		i = 0;
		for(TestAttribute a: test.getOutputAttributes()) {
			i++;
			ResultValue rv = result.getResultValue(a);			
			if(a.getDataType()==DataType.NUMBER && rv.getDoubleValue()!=null) {
				expr.setVariable("O" + i, rv.getDoubleValue());
			}
		}
		return expr.evaluate();
	}

	/**
	 * Creates an expression using M1, M2 for the metadata
	 * @param expr
	 * @param test
	 * @return
	 * @throws Exception
	 */
	public static Expression createExpression(String expr, Biotype biotype) throws Exception {
		ExpressionBuilder e = createExpressionBuilder(expr);
		
		if(biotype!=null) {
			Set<String> variables = new HashSet<>();
			int i = 0;
			for(BiotypeMetadata a: biotype.getMetadata()) {
				i++;
				if(a.getDataType()==DataType.NUMBER) {
					variables.add("M"+i);
				}
			}
			e.variables(variables);
		}
		return e.build();
	}
	
	/**
	 * Evaluate the given expression on the biosample. The Expression must have been created through createExpression
	 * @param expr
	 * @param result
	 * @return
	 * @throws Exception
	 */
	public static double evaluate(Expression expr, Biosample biosample) throws Exception {
		Biotype biotype = biosample.getBiotype();
		int i = 0;
		for(BiotypeMetadata bm: biotype.getMetadata()) {
			i++;
			String m = biosample.getMetadataValue(bm);		
			if(bm.getDataType()==DataType.NUMBER && m!=null  && m.length()>0) {
				Double d = MiscUtils.parseDouble(m);
				if(d!=null) {
					expr.setVariable("M" + i, d);
				} else {
					return Double.NaN;
				}
			}
		}
		return expr.evaluate();
	}

	
}
