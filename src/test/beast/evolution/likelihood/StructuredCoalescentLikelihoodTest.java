/*
 * Copyright (C) 2012 Tim Vaughan
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package test.beast.evolution.likelihood;

import beast.core.Operator;
import beast.core.parameter.IntegerParameter;
import beast.core.parameter.RealParameter;
import beast.evolution.likelihood.StructuredCoalescentLikelihood;
import beast.evolution.migrationmodel.MigrationModel;
import beast.evolution.tree.ColouredTree;
import beast.evolution.tree.ColouredTreeFromNewick;
import beast.evolution.tree.Tree;
import beast.util.TreeParser;
import org.junit.*;
import static org.junit.Assert.*;

/**
 * Tests for StructuredCoalescentLikelihood class methods.  Must extend
 * operator, as only operators are allowed to edit state nodes and current
 * approach to building coloured tree involves multiple calls to the
 * setValue() method of the state nodes representing the colouring information.
 * 
 * Possible way around this problem would be to have the initialiser work on
 * temporary arrays would then be used to initialise the state nodes in
 * one go.  (This is what Denise did earlier on..)
 *
 * @author Tim Vaughan
 */
public class StructuredCoalescentLikelihoodTest extends Operator {

	/**
	 * Test of calculateLogP method, of class StructuredCoalescentLikelihood.
	 */
	@Test
	public void testCalculateLogP() throws Exception {
		System.out.println("calculateLogP");

		// Assemble test ColouredTree:
		TreeParser parser = new TreeParser("", false);
		String newickStr =
				"(((A[&state=1]:0.25)[&state=0]:0.25,B[&state=0]:0.5)[&state=0]:1.5,"
				+ "(C[&state=0]:1.0,D[&state=0]:1.0)[&state=0]:1.0)[&state=0]:0.0;";
		parser.initByName(
				"adjustTipHeights", true,
				"singlechild", true,
				"newick", newickStr);
		Tree flatTree = parser;

		ColouredTree ctree = new ColouredTree();
		ctree.initByName(
				"nColours", 2,
				"maxBranchColours", 10,
				"tree", new Tree(),
				"changeColours", new IntegerParameter("0"),
				"changeTimes", new RealParameter("0.0"),
				"changeCounts", new IntegerParameter("0"),
				"nodeColours", new IntegerParameter("0"));
		ctree.initFromFlatTree(flatTree);

		// Assemble migration model:
		RealParameter rateMatrix = new RealParameter();
		rateMatrix.initByName(
				"minordimension",2,
				"dimension",4,
				"value","0.0 1.0 2.0 0.0");
		RealParameter popSizes = new RealParameter();
		popSizes.initByName(
				"dimension",2,
				"value","5.0 10.0");
		MigrationModel migrationModel = new MigrationModel();
		migrationModel.initByName(
				"rateMatrix", rateMatrix,
				"popSizes", popSizes);

		// Set up likelihood instance:
		StructuredCoalescentLikelihood likelihood = new StructuredCoalescentLikelihood();
		likelihood.initByName(
				"migrationModel", migrationModel,
				"colouredTree", ctree);

		double expResult = -12.43961; // Calculated by hand
		double result = likelihood.calculateLogP();

		System.out.println(result);
		assertEquals(expResult, result, 1e-5);
	}

	@Override
	public double proposal() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

}
