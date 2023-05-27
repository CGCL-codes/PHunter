package dataflow;

import soot.Local;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.*;
import soot.jimple.internal.JLookupSwitchStmt;
import soot.jimple.internal.JTableSwitchStmt;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.scalar.BackwardFlowAnalysis;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PIAnalysis extends BackwardFlowAnalysis<Unit, PredicateFlowSet> {


    /**
     * Construct the analysis from a DirectedGraph representation of a Body.
     */
    public PIAnalysis(DirectedGraph<Unit> graph) {
        super(graph);
        // do fixed-point
        doAnalysis();
    }

    @Override
    protected void flowThrough(PredicateFlowSet in, Unit unit, PredicateFlowSet out) {
        copy(in, out);
        if (unit instanceof IfStmt || unit instanceof SwitchStmt) {
            genBranch(unit, out);
        } else if (unit instanceof IdentityStmt || unit instanceof AssignStmt) {
            handleAssign(unit, out);
        } else {
//            I(x) = x
            Identity();
        }

    }

    private void genBranch(Unit unit, PredicateFlowSet out) {
        Set<ConditionExpr> predicateSet = new HashSet<>();
        if (unit instanceof IfStmt) {
            IfStmt ifStmt = (IfStmt) unit;
            ConditionExpr conValue = (ConditionExpr) ifStmt.getCondition();
            predicateSet.add(conValue);
        } else if (unit instanceof JTableSwitchStmt) {
            JTableSwitchStmt table = (JTableSwitchStmt) unit;
            Value key = table.getKey();
            int low = table.getLowIndex();
            int high = table.getHighIndex();
            for (int i = low; i <= high; i++) {
                Value cases = IntConstant.v(i);
                ConditionExpr newExpr = Jimple.v().newEqExpr(key, cases);
                predicateSet.add(newExpr);
            }
            // we use Integer.MAX_VALUE to represent the default
            Value DEFAULT = IntConstant.v(Integer.MAX_VALUE);
            ConditionExpr defaultExpr = Jimple.v().newEqExpr(key, DEFAULT);
            predicateSet.add(defaultExpr);
        } else if (unit instanceof JLookupSwitchStmt) {
            JLookupSwitchStmt look = (JLookupSwitchStmt) unit;
            Value key = look.getKey();
            List<IntConstant> caseList = look.getLookupValues();
            for (IntConstant constant : caseList) {
                ConditionExpr newExpr = Jimple.v().newEqExpr(key, constant);
                predicateSet.add(newExpr);
            }
            // we use Integer.MAX_VALUE to represent the default
            Value DEFAULT = IntConstant.v(Integer.MAX_VALUE);
            ConditionExpr defaultExpr = Jimple.v().newEqExpr(key, DEFAULT);
            predicateSet.add(defaultExpr);
        }
        out.addSet(predicateSet);
    }

    private void handleAssign(Unit unit, PredicateFlowSet out) {
        if (unit instanceof IdentityStmt) { //used to assign parameters to the local
            IdentityStmt identityStmt = (IdentityStmt) unit;
            Value leftLocal = identityStmt.getLeftOp();
            Value rightParameter = identityStmt.getRightOp();
            for (ConditionExpr expr : out.predicateSet) {
                if (expr.getOp1().equals(leftLocal))
                    expr.setOp1(rightParameter);
                else if (expr.getOp2().equals(leftLocal))
                    expr.setOp2(rightParameter);
            }
        } else if (unit instanceof AssignStmt) {
            AssignStmt assignStmt = (AssignStmt) unit;
            Value left = assignStmt.getLeftOp();
            Value right = assignStmt.getRightOp();

            for (ConditionExpr expr : out.predicateSet) {
                if (left instanceof ArrayRef || right instanceof AnyNewExpr) {
                    // array[i]=g(i,y1,y2,y3) will remain
                    // arr1=new int[2] will remain
                    return;
                } else {
                    if (left instanceof Local) {
                        List<ValueBox> uses = expr.getUseBoxes();
                        Local local = (Local) left;
                        for (ValueBox use : uses) {
                            if (use.getValue() instanceof Local && use.getValue().equals(local))
                                use.setValue(right);
                        }
                    }
                }
            }
        }
    }

    private void findAndSet(Local local,
                            ValueBox target,
                            Value fun) {
        Value targetV = target.getValue();
        List<ValueBox> uses = targetV.getUseBoxes();
        for (ValueBox use : uses) {
            if (use.getValue() instanceof Local && use.getValue().equals(local))
                use.setValue(fun);
        }
    }


    private void handleInvokeStmt() {

    }

    private void Identity() {
        //do nothing
    }

    @Override
    protected PredicateFlowSet newInitialFlow() {
        return new PredicateFlowSet();
    }

    @Override
    protected void merge(PredicateFlowSet in1, PredicateFlowSet in2, PredicateFlowSet out) {
        // may analysis
        PredicateFlowSet source = in1.duplicate();
        source.union(in2);
        copy(source, out);
    }

    @Override
    protected void copy(PredicateFlowSet source, PredicateFlowSet dest) {
        dest.setTo(source);
    }
}
