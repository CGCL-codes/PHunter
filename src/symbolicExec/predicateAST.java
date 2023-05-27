package symbolicExec;

import soot.Value;
import soot.jimple.internal.JimpleLocal;

import java.util.List;


public class predicateAST {
    private Object symbol;
    private predicateAST leftOp;
    private predicateAST rightOp;

    public predicateAST(Object symbol, Object leftOp, Object rightOp) {
        setSymbol(symbol);
        setLeftOp(leftOp);
        setRightOp(rightOp);
    }

    public predicateAST(String symbol, JimpleLocal leftOp, List<Value> values) { //Handle the case when assignStmt contains invokeExpr, when the parameters of the call need to be extracted as well.
        setSymbol(symbol);
        setLeftOp(leftOp);
        this.rightOp = setRightOp(values);
    }

    private void setSymbol(Object symbol) {
        if (symbol instanceof predicateAST) {
            predicateAST ast = (predicateAST) symbol;
            this.symbol = ast.symbol; //I don't think cloning is needed here
            this.leftOp = ast.leftOp;
            this.rightOp = ast.rightOp;
            return;
        }
        this.symbol = symbol;
    }

    public Object getSymbol() {
        return this.symbol;
    }

    private void setLeftOp(Object leftOp) {
        if (leftOp == null) {
            return;
        }
        this.leftOp = new predicateAST(leftOp, null, null);
    }

    public predicateAST getLeftOp() {
        return this.leftOp;
    }

    private void setRightOp(Object rightOp) {
        if (rightOp == null) {
            return;
        }
        this.rightOp = new predicateAST(rightOp, null, null);
    }

    private predicateAST setRightOp(List<Value> values) {
        if (values.isEmpty())
            return null;
        Value first = values.get(0);
        values.remove(0);
        return new predicateAST("args", first, values);
    }

    public predicateAST getRightOp() {
        return this.rightOp;
    }

    public void traverse(predicateAST ast, Object pre, Object post) {
        if (ast != null) {
            if (ast.getSymbol().equals(pre)) {
                ast.setSymbol(post);
            } else {
                traverse(ast.getLeftOp(), pre, post);
                traverse(ast.getRightOp(), pre, post);
            }
        }
    }

    public String optimize(predicateAST ast) {
        if (ast != null) {
            return optimize(ast.getLeftOp()) + ast.getSymbol() + optimize(ast.getRightOp());
        }
        return "";
    }
}
