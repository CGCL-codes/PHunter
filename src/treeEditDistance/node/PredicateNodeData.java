package treeEditDistance.node;

import soot.SootField;
import soot.Type;
import soot.Value;
import soot.jimple.*;
import treeEditDistance.costmodel.NodeType;

public class PredicateNodeData {
    /* Field = type. eg, int, some_class
        Constant = type#value. eg, int#2, string#abcde
        Comparator = <, <=, ==, !=
        BinOperators = +, -, *, /, %, <<, &, | and so on
        Invoke = sig, eg, retrofit2.RequestFactory parseAnnotations(retrofit2.Retrofit, java.lang.reflect.Method)
        Parameter = index. eg, 1
      */
    private NodeType nodeType;

    private String data;

    private Value value = null;

    public void setNodeType(NodeType nodeType) {
        this.nodeType = nodeType;
    }

    public void setData(String data) {
        this.data = data;
    }

    public Value getValue() {
        return value;
    }

    public void setNode(PredicateNodeData data) {
        this.nodeType = data.getNodeType();
        this.data = data.getData();
    }

    public void setValue(Object v) {
        if (v instanceof IntConstant) {
            int num = ((IntConstant) v).value;
            setNodeType(NodeType.Constant);
            setData("int#" + num);
        } else if (v instanceof StringConstant) {
            String str = ((StringConstant) v).value;
            setNodeType(NodeType.Constant);
            if (str.equals(""))
                str = " ";
            setData("String#" + str);
        } else if (v instanceof LongConstant) {
            long num = ((LongConstant) v).value;
            setNodeType(NodeType.Constant);
            setData("long#" + num);
        } else if (v instanceof FloatConstant) {
            float num = ((FloatConstant) v).value;
            setNodeType(NodeType.Constant);
            setData("float#" + num);
        } else if (v instanceof DoubleConstant) {
            double num = ((DoubleConstant) v).value;
            setNodeType(NodeType.Constant);
            setData("double#" + num);
        } else if (v instanceof NullConstant) {
            setNodeType(NodeType.Constant);
            setData("null#null");
        } else if (v instanceof ClassConstant) {
            setNodeType(NodeType.Class);
            Type classType = ((ClassConstant) v).getType();
            setData(classType.toString());
        } else if (v instanceof CaughtExceptionRef) {
            setNodeType(NodeType.CaughtException);
            setData("@caughtexception");
        } else if (v instanceof FieldRef) {
            SootField field = ((FieldRef) v).getField();
            StringBuilder fieldSb = new StringBuilder();
            fieldSb.append(field.getDeclaringClass().getName()).append("#");
            setNodeType(NodeType.Field);
            fieldSb.append(field.getType());
            setData(fieldSb.toString());
        } else if (v instanceof ParameterRef) {
            int index = ((ParameterRef) v).getIndex();
            setNodeType(NodeType.Parameter);
            setData("" + index);
        } else if (v instanceof ThisRef) {
            setNodeType(NodeType.Parameter);
            setData("this");
        } else if (v instanceof NewExpr) {
            setNodeType(NodeType.Class);
            setData(((NewExpr) v).getType().toString());
        } else
            this.value = (Value) v;
    }


    public PredicateNodeData(Value value) {
        setValue(value);
    }

    public PredicateNodeData(NodeType type, String data) {
        this.nodeType = type;
        this.data = data;
    }

    public String getData() {
        return data;
    }

    public NodeType getNodeType() {
        return nodeType;
    }
}


