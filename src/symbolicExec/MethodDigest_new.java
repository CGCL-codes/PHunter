package symbolicExec;

import soot.*;
import soot.jimple.*;
import soot.jimple.internal.JCastExpr;
import soot.jimple.internal.JInvokeStmt;
import soot.jimple.internal.JLookupSwitchStmt;
import soot.jimple.internal.JTableSwitchStmt;
import soot.toolkits.graph.Block;
import soot.toolkits.graph.BriefBlockGraph;
import soot.util.Chain;
import treeEditDistance.costmodel.NodeType;
import treeEditDistance.node.Node;
import treeEditDistance.node.PredicateNodeData;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class MethodDigest_new {
    public List<List<Unit>> unitsPaths = new LinkedList<>();
    public List<List<Unit>> realUnitsPaths = new LinkedList<>();//Ensure that the numerical relations of the predicates in the path are all satisfied

    public List<List<String>> signatures = new LinkedList<>();//Record the function signature on the path
    public List<List<String>> variables = new LinkedList<>();//Record the variables on the path
    public List<List<String>> realVariables = new LinkedList<>();

    public List<Unit> patchRelatedUnits;
    public List<List<Node<PredicateNodeData>>> predicates = new LinkedList<>();//Store the predicates on each path
    public List<List<Node<PredicateNodeData>>> realPredicates = new LinkedList<>();
    public List<List<String>> realSignatures = new LinkedList<>();
    //    public boolean[][] flags;
    //    public List<List<Integer>> blockIndices = new LinkedList<>();
    //    public List<List<Boolean>> branches = new LinkedList<>();//Store the branches executed by path

    public MethodDigest_new(Body body, List<Integer> patchRelatedUnits) {
        this.patchRelatedUnits = patchRelatedUnits == null ? null
                : body.getUnits()
                .stream()
                .filter(unit -> patchRelatedUnits.contains(unit.getJavaSourceStartLineNumber()))
                .collect(Collectors.toList());
        enumMethodPath(body);
        getPredicate(new LinkedList<>());//collect the predicate of each path
        int a = 0;
    }

    public void enumMethodPath(Body body) {
        BriefBlockGraph bg = new BriefBlockGraph(body);
//        modifyBlockGraph(bg, body); //ignore catch block
        int num = bg.getBlocks().size();
        boolean[][] flags = new boolean[num][num];
        List<Block> heads = bg.getHeads();
        Block head = heads.get(0);
        List<Unit> unitPath = new ArrayList<>();
        traverseBlock(unitPath, head, flags);
    }

    private void modifyBlockGraph(BriefBlockGraph bg, Body body) {
        List<Block> blocks = bg.getBlocks();
        Chain<Trap> traps = body.getTraps();
        for (Trap trap : traps) {
//            Unit begin = trap.getBeginUnit();
            Unit end = trap.getEndUnit();
            Unit handler = trap.getHandlerUnit();
            //the first integer is the index of the endBlock and the second integer is the index of the handlerBlock
            int[] blockIndex = getBlockIndexOfUnit(blocks, end, handler);
            Block endBlock = blocks.get(blockIndex[0]);
            Block handlerBlock = blocks.get(blockIndex[1]);
            List<Block> preds = new ArrayList<>(handlerBlock.getPreds());
            if (!preds.contains(endBlock))
                preds.add(endBlock);
            List<Block> succs = new ArrayList<>(endBlock.getSuccs());
            if (!succs.contains(handlerBlock))
                succs.add(handlerBlock);
            endBlock.setSuccs(succs);
            handlerBlock.setPreds(preds);
        }
    }

    private int[] getBlockIndexOfUnit(List<Block> blocks, Unit end, Unit handler) {
        int[] index = new int[2];
        for (Block block : blocks) {
            for (Unit value : block) {
                if (value.equals(end)) {
                    index[0] = block.getIndexInMethod();
                }
                if (value.equals(handler)) {
                    index[1] = block.getIndexInMethod();
                }
            }
        }
        return index;
    }

    public void traverseBlock(List<Unit> unitPath, Block head,
                              boolean[][] flags) {
        unitPath.addAll(getBlockUnits(head));//Add the information in the head to the list
        List<Block> nextBlocks = head.getSuccs();
        if (nextBlocks.isEmpty()) {
            this.unitsPaths.add(unitPath);
            return;
        }
        for (Block block : nextBlocks) {
            if (checkCycle(head, block, flags) == 1) {
                if (nextBlocks.size() == 1) {
                    this.unitsPaths.add(unitPath);
                }
                continue;
            }
            List<Unit> newUnitPath = new ArrayList<>(unitPath);
            traverseBlock(newUnitPath, block, flags);
            flags[head.getIndexInMethod()][block.getIndexInMethod()] = false;
        }
    }

    public List<Unit> getBlockUnits(Block block) {
        List<Unit> paths = new ArrayList<>();
        for (Unit unit : block) {
            paths.add(unit);
        }
        return paths;
    }

    public List<String> getSignatures(Block block) {
        List<String> signature = new ArrayList<>();
        for (Unit unit : block) {
//            if (unit.toString().contains("invoke")){
//                signature.add(unit.toString());
//            }
            if (unit instanceof InvokeStmt) {
                signature.add(((JInvokeStmt) unit).getInvokeExpr().getMethod().getSignature());
            } else if (unit.toString().contains("invoke") && unit instanceof AssignStmt) {
                AssignStmt stmt = (AssignStmt) unit;
                if (stmt.getRightOp() instanceof InvokeExpr) {
                    InvokeExpr expr = (InvokeExpr) stmt.getRightOp();
                    signature.add(expr.getMethod().getSignature());
                }
            }
        }
        return signature;
    }

    public List<String> getVariable(Block block) {
        List<String> variable = new ArrayList<>();
        for (Unit unit : block) {
            for (ValueBox value : unit.getUseAndDefBoxes()) {
                variable.add(value.getValue().getType().toString() + " " + value.getValue().toString());
            }
        }
        return variable;
    }


    public int checkCycle(Block head, Block nextBlock, boolean[][] flags) {//Determine if the loop needs to be terminated
        int headIndex = head.getIndexInMethod();
        int nextIndex = nextBlock.getIndexInMethod();
        if (flags[headIndex][nextIndex])
            return 1;
        flags[headIndex][nextIndex] = true;
        return 0;
    }

    public void getPredicate(List<List<Boolean>> branches) {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine se = manager.getEngineByName("js");
        for (List<Unit> unit : this.unitsPaths) {
            backwardAnalysis(unit, branches, se);
        }
    }

    private void backwardAnalysis(List<Unit> units, List<List<Boolean>> branches, ScriptEngine se) {
        List<Node<PredicateNodeData>> predicate = new ArrayList<>();
        List<String> signature = new ArrayList<>();
        List<String> variable = new ArrayList<>();
        List<Boolean> branch = new ArrayList<>();

        for (int index = units.size() - 1; index >= 0; index--) {
            Stmt stmt = (Stmt) units.get(index);
            if (stmt.containsFieldRef()) {
                SootField field = stmt.getFieldRef().getField();
                Type type = field.getType();
                if (type instanceof ArrayType)
                    variable.add(field.getDeclaringClass().getName() + "," + ((ArrayType) type).baseType + ",");
                else variable.add(field.getDeclaringClass().getName() + "," + field.getType() + ",");
            } else if (stmt.containsArrayRef()) {
                ArrayRef arrayRef = stmt.getArrayRef();
                variable.add(arrayRef.getType().toString());
            } else if (stmt.containsInvokeExpr()) {
                SootMethod method = stmt.getInvokeExpr().getMethod();
                if (method.isStatic())
                    signature.add(method.getDeclaringClass().getName() + ",static," + getParamForm(method));
                else signature.add(method.getDeclaringClass().getName() + "," + getParamForm(method));
            }

            if (stmt instanceof IfStmt) {
                ConditionExpr condition = (ConditionExpr) ((IfStmt) stmt).getCondition();
                Stmt target = ((IfStmt) stmt).getTarget();
                Stmt nextStmt = (Stmt) units.get(index + 1);
                if (nextStmt == target) {
                    branch.add(Boolean.TRUE);
                } else {
                    branch.add(Boolean.FALSE);
                }
                String symbol = condition.getSymbol().trim();
                Value left = condition.getOp1();
                Value right = condition.getOp2();
                if (symbol.equals(">=")) {
                    Value tmp = left;
                    left = right;
                    right = tmp;
                    symbol = "<=";
                } else if (symbol.equals(">")) {
                    Value tmp = left;
                    left = right;
                    right = tmp;
                    symbol = "<";
                }
                PredicateNodeData data = new PredicateNodeData(NodeType.Comparator, symbol);
                Node<PredicateNodeData> node = new Node<>(data);
                node.addChild(left);
                node.addChild(right);
                predicate.add(node);
            } else if (stmt instanceof JTableSwitchStmt) {
                JTableSwitchStmt table = (JTableSwitchStmt) stmt;
                Stmt nextStmt = (Stmt) units.get(index + 1);
                Value key = table.getKey();
                int low = table.getLowIndex();
                int high = table.getHighIndex();
                for (int i = low; i <= high; i++) {
                    if (table.getTarget(i) == nextStmt) {
                        PredicateNodeData data = new PredicateNodeData(NodeType.Comparator, "==");
                        Node<PredicateNodeData> node = new Node<>(data);
                        node.addChild(key);
                        node.addChild(IntConstant.v(i));
                        predicate.add(node);
                    }
                }
                if (table.getDefaultTarget() == nextStmt) {
                    Value DEFAULT = IntConstant.v(Integer.MAX_VALUE);
                    PredicateNodeData data = new PredicateNodeData(NodeType.Comparator, "==");
                    Node<PredicateNodeData> node = new Node<>(data);
                    node.addChild(key);
                    node.addChild(DEFAULT);
                    predicate.add(node);
                }
            } else if (stmt instanceof JLookupSwitchStmt) {
                JLookupSwitchStmt look = (JLookupSwitchStmt) stmt;
                Stmt nextStmt = (Stmt) units.get(index + 1);
                Value key = look.getKey();
                List<IntConstant> caseList = look.getLookupValues();
                for (int i = 0; i < caseList.toArray().length; i++) {
                    if (look.getTarget(i) == nextStmt) {
                        PredicateNodeData data = new PredicateNodeData(NodeType.Comparator, "==");
                        Node<PredicateNodeData> node = new Node<>(data);
                        node.addChild(key);
                        node.addChild(caseList.get(i));
                        predicate.add(node);
                    }
                }
                if (look.getDefaultTarget() == nextStmt) {
                    Value DEFAULT = IntConstant.v(Integer.MAX_VALUE);
                    PredicateNodeData data = new PredicateNodeData(NodeType.Comparator, "==");
                    Node<PredicateNodeData> node = new Node<>(data);
                    node.addChild(key);
                    node.addChild(DEFAULT);
                    predicate.add(node);
                }
            } else if (stmt instanceof IdentityStmt) {
                IdentityStmt identityStmt = (IdentityStmt) stmt;
                Value left = identityStmt.getLeftOp();
                Value right = identityStmt.getRightOp();
                PredicateNodeData data = new PredicateNodeData(right);
                Node<PredicateNodeData> rightNode = new Node<>(data);
                for (Node<PredicateNodeData> condition : predicate) {
                    condition.traverse(condition, left, rightNode);
                }
            } else if (stmt instanceof AssignStmt) {
                AssignStmt assignStmt = (AssignStmt) stmt;
                Value left = assignStmt.getLeftOp();
                Value right = assignStmt.getRightOp();
                if (right instanceof InvokeExpr) {
                    //Handle the case when assignStmt contains invokeExpr, when the parameters of the call need to be extracted as well.
                    InvokeExpr expr = (InvokeExpr) right;
                    SootMethod method = expr.getMethod();
                    List<Value> values = expr.getArgs();
                    Node<PredicateNodeData> node;
                    if (expr instanceof StaticInvokeExpr) {//TODO: NewInvokeExpr is instanceof InvokeExpr, but it is not solved.
                        PredicateNodeData data = new PredicateNodeData(NodeType.Invoke,
                                method.getDeclaringClass().getName() + ",static," + getParamForm(method));
                        node = new Node<>(data);
                        for (Value value : values) {
                            node.addChild(value);
                        }
                    } else {
                        Value caller = ((InstanceInvokeExpr) expr).getBase();
                        PredicateNodeData data = new PredicateNodeData(NodeType.Invoke,
                                method.getDeclaringClass().getName() + "," + getParamForm(method));
                        node = new Node<>(data);
                        node.addChild(caller);
                        for (Value value : values) {
                            node.addChild(value);
                        }
                    }
                    for (Node<PredicateNodeData> condition : predicate) {
                        condition.traverse(condition, left, node);
                    }
                } else if (right instanceof BinopExpr) {
                    BinopExpr expr = (BinopExpr) right;
                    if (checkSymbolToOptimize(expr, units.get(index - 1))) {
                        PredicateNodeData data = new PredicateNodeData(IntConstant.v(0));
                        Node<PredicateNodeData> rightNode = new Node<>(data);
                        for (Node<PredicateNodeData> condition : predicate) {
                            condition.traverse(condition, left, rightNode);
                        }
                    } else {
                        String symbol = expr.getSymbol().trim();
                        PredicateNodeData data = new PredicateNodeData(NodeType.BinOperators, symbol);
                        Node<PredicateNodeData> node = new Node<>(data);
                        node.addChild(expr.getOp1());
                        node.addChild(expr.getOp2());
                        for (Node<PredicateNodeData> condition : predicate) {
                            condition.traverse(condition, left, node);
                        }
                    }
                } else if (right instanceof JCastExpr) {
                    JCastExpr expr = (JCastExpr) right;
                    Type type = expr.getCastType();
                    PredicateNodeData data;
                    Node<PredicateNodeData> node;
                    if (expr.getOp() instanceof Constant) {
                        Constant c = (Constant) expr.getOp();
                        data = new PredicateNodeData(c);
                    } else {
                        Local afterCast = (Local) expr.getOp();
                        afterCast.setType(type);
                        data = new PredicateNodeData(afterCast);
                    }
                    node = new Node<>(data);
                    for (Node<PredicateNodeData> condition : predicate) {
                        condition.traverse(condition, left, node);
                    }
                } else if (right instanceof UnopExpr) {
                    UnopExpr expr = (UnopExpr) right;
                    Value v = expr.getOp();
                    PredicateNodeData data;
                    if (expr instanceof LengthExpr) {
                        data = new PredicateNodeData(NodeType.UnaryOperators, "Length");
                    } else {
                        data = new PredicateNodeData(NodeType.UnaryOperators, "Negative");
                    }
                    Node<PredicateNodeData> node = new Node<>(data);
                    node.addChild(v);
                    for (Node<PredicateNodeData> condition : predicate) {
                        condition.traverse(condition, left, node);
                    }
                } else if (right instanceof AnyNewExpr) {
                    PredicateNodeData data;
                    Node<PredicateNodeData> node;
                    if (right instanceof NewExpr) {
                        NewExpr expr = (NewExpr) right;
                        data = new PredicateNodeData(NodeType.Class, expr.getType().toString());
                        node = new Node<>(data);
                    } else {//expr instanceof NewArrayExpr
                        //TODO handle NewMultiArrayExpr
                        NewArrayExpr expr = (NewArrayExpr) right;
                        data = new PredicateNodeData(NodeType.Array, expr.getBaseType().toString());
                        Value v = expr.getSize();//record the new array's length
                        node = new Node<>(data);
                        node.addChild(v);
                    }
                    for (Node<PredicateNodeData> condition : predicate) {
                        condition.traverse(condition, left, node);
                    }
                } else if (right instanceof ArrayRef) {
                    ArrayRef ar = (ArrayRef) right;
                    PredicateNodeData data = new PredicateNodeData(NodeType.Array, ar.getBase().toString());
                    Value v = ar.getIndex();
                    Node<PredicateNodeData> node = new Node<>(data);
                    node.addChild(v);
                    for (Node<PredicateNodeData> condition : predicate) {
                        condition.traverse(condition, left, node);
                    }
                } else if (right instanceof InstanceOfExpr) {
                    InstanceOfExpr expr = (InstanceOfExpr) right;
                    Type type = expr.getCheckType();
                    String clazz;
                    if (type instanceof ArrayType)
                        clazz = ((ArrayType) type).baseType.toString();
                    else clazz = type.toString();

                    PredicateNodeData data = new PredicateNodeData(NodeType.InstanceOf, clazz);
                    Value v = expr.getOp();
                    Node<PredicateNodeData> node = new Node<>(data);
                    node.addChild(v);
                    for (Node<PredicateNodeData> condition : predicate) {
                        condition.traverse(condition, left, node);
                    }
                } else {//The right-hand side is an immediate number or object
                    PredicateNodeData data = new PredicateNodeData(right);
                    Node<PredicateNodeData> rightNode = new Node<>(data);
                    for (Node<PredicateNodeData> condition : predicate) {
                        condition.traverse(condition, left, rightNode);
                    }
                }
            }
        }
        branches.add(branch);
        optimizePredicate(predicate, units, branch, variable, signature, se);
//        predicates.add(predicate);
//        signatures.add(signature);
//        variables.add(variable);
    }

    private boolean checkSymbolToOptimize(BinopExpr expr, Unit unit) {
        if (expr.getSymbol().trim().equals("%")) {
            if (unit instanceof AssignStmt) {
                AssignStmt stmt = (AssignStmt) unit;
                Value left = stmt.getLeftOp();
                if (stmt.getRightOp() instanceof BinopExpr) {
                    BinopExpr right = (BinopExpr) stmt.getRightOp();
                    if (right.getSymbol().trim().equals("*")) {
                        if (left == expr.getOp1()) {
                            return expr.getOp2().equals(right.getOp1()) || expr.getOp2().equals(right.getOp2());
                        } else if (left == expr.getOp2()) {
                            return expr.getOp1().equals(right.getOp1()) || expr.getOp1().equals(right.getOp2());
                        }
                    }
                }
            }
        }
        return false;
    }


    private String getParamForm(SootMethod method) {
        StringBuilder sb = new StringBuilder();
        Type ret = method.getReturnType();
        if (ret instanceof RefType)
            sb.append(ret).append(",");
        else if (ret instanceof ArrayType)
            sb.append(((ArrayType) ret).baseType).append(","); // ignore array
        else sb.append(ret).append(",");

        for (Type t : method.getParameterTypes()) {
            if (t instanceof RefType)
                sb.append(t).append(",");
            else if (t instanceof ArrayType)
                sb.append(((ArrayType) t).baseType).append(","); // ignore array
            else sb.append(t).append(",");
        }
        return sb.toString();
    }

    private void optimizePredicate(List<Node<PredicateNodeData>> predicate, List<Unit> units, List<Boolean> branch,
                                   List<String> variable, List<String> signature, ScriptEngine se) { //Clear paths that do not satisfy the predicate
        for (Node<PredicateNodeData> ast : predicate) {
            String str = ast.extractNodeData(ast);
            try {
                Boolean result = (Boolean) se.eval(str);
                if (result != branch.get(predicate.indexOf(ast))) {
                    return;
                }
            } catch (ScriptException ignored) {
            }
        }
        realPredicates.add(predicate);
        realUnitsPaths.add(units);
        realVariables.add(variable);
        realSignatures.add(signature);
    }
}