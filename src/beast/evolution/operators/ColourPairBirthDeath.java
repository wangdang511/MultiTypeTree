/*
 * Copyright (C) 2013 Tim Vaughan <tgvaughan@gmail.com>
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
package beast.evolution.operators;

import beast.core.Description;
import beast.evolution.tree.Node;
import beast.util.Randomizer;

/**
 * @author Tim Vaughan <tgvaughan@gmail.com>
 */
@Description("Implements colour change (migration) pair birth/death move "
        + "described by Ewing et al., Genetics (2004).")
public class ColourPairBirthDeath extends ColouredTreeOperator {

    @Override
    public void initAndValidate() { }
    
    @Override
    public double proposal() {
        cTree = colouredTreeInput.get();
        tree = cTree.getUncolouredTree();
        
        int n = tree.getLeafNodeCount();
        int m = cTree.getTotalNumberofChanges();
        
        // Select sub-edge at random:
        int edgeNum = Randomizer.nextInt(2*n - 2 + m);
        
        // Find edge that sub-edge lies on:
        Node selectedNode = null;
        for (Node node : tree.getNodesAsArray()) {
            if (node.isRoot())
                continue;

            if (edgeNum<cTree.getChangeCount(node)+1) {
                selectedNode = node;
                break;
            }
            edgeNum -= cTree.getChangeCount(node)+1;
        }
        
        // Complete either pair birth or pair death proposal:
        if (Randomizer.nextDouble()<0.5)
            return birthProposal(selectedNode, edgeNum, n, m);
        else
            return deathProposal(selectedNode, edgeNum, n, m);

    }
    
    /**
     * Colour change pair birth proposal.
     * 
     * @param node Node above which selected edge lies
     * @param edgeNum Number of selected edge
     * @param n Number of nodes on tree.
     * @param m Number of colour changes currently on tree.
     * @return log of Hastings factor of move.
     */
    private double birthProposal(Node node, int edgeNum, int n, int m) {
        
        int ridx = edgeNum;
        int sidx = edgeNum-1;
        
        double ts, tr;
        int oldEdgeColour;
        if (sidx<0) {
            ts = node.getHeight();
            oldEdgeColour = cTree.getNodeColour(node);
        } else {
            ts = cTree.getChangeTime(node, sidx);
            oldEdgeColour = cTree.getChangeColour(node, sidx);
        }

        if (ridx>cTree.getChangeCount(node)-1)
            tr = node.getParent().getHeight();
        else
            tr = cTree.getChangeTime(node, ridx);

        int newEdgeColour;
        do {
            newEdgeColour = Randomizer.nextInt(cTree.getNColours());
        } while (newEdgeColour == oldEdgeColour);
        
        double tau1 = Randomizer.nextDouble()*(tr-ts) + ts;
        double tau2 = Randomizer.nextDouble()*(tr-ts) + ts;
        double tauMin = Math.min(tau1, tau2);
        double tauMax = Math.max(tau1, tau2);
        
        try {
            insertChange(node, edgeNum, oldEdgeColour, tauMax);
            insertChange(node, edgeNum, newEdgeColour, tauMin);
        } catch (RecolouringException ex) {
            if (cTree.discardWhenMaxExceeded()) {
                ex.discardMsg();
                return Double.NEGATIVE_INFINITY;
            } else
                ex.throwRuntime();
        }
        
        return Math.log((cTree.getNColours()-1)*(m + 2*n - 2)*(tr-ts)*(tr-ts))
                - Math.log(2*(m + 2*n));
    }
    
    /**
     * Colour change pair death proposal.
     * 
     * @param node Node above which selected edge lies
     * @param edgeNum Number of selected edge
     * @param n Number of nodes on tree
     * @param m Number of colour changes currently on tree
     * @return log of Hastings factor of move.
     */
    private double deathProposal(Node node, int edgeNum, int n, int m) {
        
        int idx = edgeNum-1;
        int sidx = edgeNum-2;
        int ridx = edgeNum+1;
        
        if (sidx<-1 || ridx > cTree.getChangeCount(node))
            return Double.NEGATIVE_INFINITY;
        
        double ts, tr;
        int is, ir;
        if (sidx<0) {
            ts = node.getHeight();
            is = cTree.getNodeColour(node);
        } else {
            ts = cTree.getChangeTime(node, sidx);
            is = cTree.getChangeColour(node, sidx);
        }
        
        if (ridx>cTree.getChangeCount(node)-1)
            tr = node.getParent().getHeight();
        else
            tr = cTree.getChangeTime(node, ridx);
        ir = cTree.getChangeColour(node, ridx-1);
        
        if (is != ir)
            return Double.NEGATIVE_INFINITY;
        
        removeChange(node, idx);
        removeChange(node, idx);
        
        return Math.log(2*(m + 2*n - 2))
                - Math.log((cTree.getNColours()-1)*(m+2*n-4)*(tr-ts)*(tr-ts));
    }
    
}