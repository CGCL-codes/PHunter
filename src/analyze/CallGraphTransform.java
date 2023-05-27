package analyze;

import soot.*;
import soot.jimple.IfStmt;
import soot.jimple.Jimple;
import soot.jimple.Stmt;

import java.util.*;

public class CallGraphTransform extends BodyTransformer {
    private static
    SootCallGraph cg;


    public CallGraphTransform(SootCallGraph cg) {
        this.cg = cg;
    }

    @Override
    protected void internalTransform(Body b, String phaseName, Map<String, String> options) {

        SootMethod method = b.getMethod();
//        if (SootCallGraph.isAndroidFrameworkCall(method.getDeclaringClass().getName()))
//            return;
//        if (method.getDeclaringClass().isApplicationClass())
//            return;
        MethodAttr methodAttr = new MethodAttr(b);
//        if (cg.isAPK && method.isStaticInitializer()) {
//            handleStaticInitial(b);
//        }

        boolean flag = false;
        UnitPatchingChain units = b.getUnits();
        for (Unit u : units) {
            if (!cg.isAPK) {
                int l = u.getJavaSourceStartLineNumber();
                if (l > -1 && !flag) {
                    flag = true;
                    methodAttr.startLinenumber = l;
                }
            }
            if (!(u instanceof Stmt))
                continue;
            Stmt t = (Stmt) u;
            if (t.containsFieldRef()) {
                SootField field = t.getFieldRef().getField();
                methodAttr.fieldRef.add(field);
            } else if (t.containsInvokeExpr()) {
                SootMethod callee = t.getInvokeExpr().getMethod();
                cg.callSites.putIfAbsent(method, new ArrayList<>());
                cg.callSites.get(method).add(callee);
            }
        }
        if (!cg.isAPK)
            methodAttr.endLinenumber = b.getUnits().getLast().getJavaSourceStartLineNumber();
        methodAttr.getFieldFuzzyForm();
        cg.sootMethodMethodAttrMap.put(method, methodAttr);
    }

    private Stmt handleDashOBug(Iterator<Unit> itrUnit, SootMethod m) {
        itrUnit.next();
        Unit unit = itrUnit.next();
//        System.out.println(m.getName() + " " + m.getDeclaringClass().getName() + " " + unit.toString());
//        System.out.println(unit.toString());
        IfStmt t = (IfStmt) unit;
        Unit target = t.getTarget();
//        Value value = t.getCondition();
//        ValueBox conbox = t.getConditionBox();
//        Value value = conbox.getValue();
//        List<ValueBox> list = value.getUseBoxes();
//        ValueBox left = list.get(0);
//        Value leftV = left.getValue();
////        System.out.println(leftV.toString());
//        ValueBox right = list.get(1);
//        Value rightV = right.getValue();
////        System.out.println(rightV.toString());
//        GtExpr gtExpr = Jimple.v().newGtExpr(leftV, rightV);
        return Jimple.v().newGotoStmt(target);
//        t.setTarget(expr);

//        GtExpr gt = new GeExpr()
//        Value newcon =  ConditionExpr;
//        con.getType()
    }

    private void handleStaticInitial(Body b) {
        Unit begin = null, end = null;
        SootClass runtimeClass = null;
        Trap first = null;
        boolean flag = false;
        List<Trap> tmpTrap = new LinkedList<>();
        for (Trap trap : b.getTraps()) {
            SootClass clazz = trap.getException();
            SootClass superClazz = clazz.getSuperclass();
            if (superClazz.getName().equals("java.lang.RuntimeException")) {
                runtimeClass = superClazz;
                flag = true;
                if (begin == null) {
                    begin = trap.getBeginUnit();
                }
                end = trap.getEndUnit();
                tmpTrap.add(trap);
            }
        }
        if (flag) {
            for (Trap trap : tmpTrap) {
                trap.setException(runtimeClass);
                trap.setBeginUnit(begin);
                trap.setEndUnit(end);
            }
        }

//        if (flag) {
//            for (Unit unit : b.getUnits()) {
//                if (!(unit instanceof Stmt))
//                    continue;
//                Stmt t = (Stmt) unit;
//                if(t instanceof IdentityStmt){
//                    System.out.println(1);
//                }else if(t instanceof AssignStmt)
//                    System.out.println(2);
////                if (t.containsInvokeExpr()) {
////                    SootClass runtimeEx = t.getInvokeExpr().getMethod().getDeclaringClass();
////                    if (runtimeEx.getName().equals("java.lang.RuntimeException")) {
////                        b.getUnits().remove(unit);
////                        break;
////                    }
////                }
//            }
//        }

    }

//    private void handleField(MethodAttr methodAttr, Value v, boolean isLeft) {
//        String sig = null;
//        if (v instanceof StaticFieldRef) {
//            StaticFieldRef staticFieldRef = (StaticFieldRef) v;
//            sig = staticFieldRef.getField().getSignature();
//        } else if (v instanceof InstanceFieldRef) {
//            InstanceFieldRef instanceFieldRef = (InstanceFieldRef) v;
//            sig = instanceFieldRef.getField().getSignature();
//        }
//        if (isLeft)
//            methodAttr.addWriteField(sig);
//        else methodAttr.addReadField(sig);
//    }

//    public void getStartEndNumber(Body body, MethodAttr method) {
//        int s = -1;
//        int e = -1;
////        if (body == null) return line;
//        PatchingChain<Unit> units = body.getUnits();
//        for (Unit unit : units) {
//            int l = unit.getJavaSourceStartLineNumber();
//            if (l > -1) {
//                method.setStartLinenumber(l);
//                break;
//            }
//        }
//        method.setEndLinenumber(units.getLast().getJavaSourceStartLineNumber());
//    }
}

