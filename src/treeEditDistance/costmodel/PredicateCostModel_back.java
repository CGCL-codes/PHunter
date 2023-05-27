package treeEditDistance.costmodel;

import treeEditDistance.node.Node;
import treeEditDistance.node.PredicateNodeData;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class PredicateCostModel_back implements CostModel<PredicateNodeData> {
    @Override
    public float del(Node<PredicateNodeData> n) {
        return 1.25f;
    }

    @Override
    public float ins(Node<PredicateNodeData> n) {
        return 1.25f;
    }

    /*
        field:
        a field is like : type:name,
        Due to the confusion of variable names, only the type of field is used here,
        i.e., the data here only means type(int,byte,char,long,float,class)
        and if the type of field cannot be inferred, it is marked as UNKNOWN

        Immediate:
        immediate is like: type:value
    */
    @Override
    public float ren(Node<PredicateNodeData> n1, Node<PredicateNodeData> n2) {
        if (n1.getNodeData().getNodeType() == n2.getNodeData().getNodeType()) {
            String data1 = n1.getNodeData().getData();
            String data2 = n2.getNodeData().getData();
            switch (n1.getNodeData().getNodeType()) {
                case Field:
                    //form is class#type
                    String[] field1 = data1.split("#");
                    String[] field2 = data2.split("#");
                    float clazzDistance, typeDistance;
                    if (field1[0].startsWith("X") || field2[0].startsWith("X"))
                        clazzDistance = 0.25f;
                    else if (!field1[0].equals(field2[0]))
                        return 0.5f;
                    else clazzDistance = 0;

                    if (field1[1].startsWith("X") || field2[1].startsWith("X"))
                        typeDistance = 0.25f;
                    else if (!field1[1].equals(field2[1]))
                        return 0.5f;
                    else typeDistance = 0;
                    return clazzDistance + typeDistance;

                case Constant:
                    // form is type#value
                    String[] immediate1 = data1.split("#");
                    String[] immediate2 = data2.split("#");

                    if (immediate1[0].equals(immediate2[0])) {
                        return immediate1[1].equals(immediate2[1]) ? 0.0f : 0.25f;
                    } else {
                        return 0.5f;
                    }

                case Comparator:
                    // including <,<=,==,!=, note that there is no > or >=
                    if (data1.equals(data2))
                        return 0.0f;
                    if (data1.equals("<") || data1.equals("<=")
                            && data2.equals("<") || data2.equals("<="))
                        return 0.25f;
                    else if (data1.equals("==") || data1.equals("!=")
                            && data2.equals("==") || data2.equals("!="))
                        return 0.25f;
                    else return 0.5f;

                case BinOperators:
                    Set<String> arithmetic = new HashSet<>(Arrays.asList("+", "-", "*", "/", "%"));
//                    Set<String> bitOp= new HashSet<>(Arrays.asList("&", "|", "^", "<<",">>",""));
                    // form is +,-,*,/,%,,<<,>>,&,|,^ and so on
                    if (data1.equals(data2))
                        return 0.0f;
                    else if (arithmetic.contains(data1) && arithmetic.contains(data2))
                        return 0.25f;
                    else if (!arithmetic.contains(data1) && !arithmetic.contains(data2))
                        return 0.25f;
                    else return 0.5f;

                case Invoke:
                    // form is clazz,signature
                    String[] invoke1 = data1.split(",");
                    String[] invoke2 = data2.split(",");
                    if (invoke1.length != invoke2.length)
                        return 0.75f;
                    boolean flag = false; // exist X
                    for (int i = 0; i < invoke2.length; i++) {
                        if (!invoke2[i].startsWith("X") && !invoke1[i].startsWith(invoke2[i]))
                            return 0.75f;
                        if (invoke2[i].startsWith("X"))
                            flag = true;
                    }
                    if (flag)
                        return 0.25f;
                    else return 0.0f;

                case CaughtException:
                case Parameter:
                case UnaryOperators:
                    return data1.equals(data2) ? 0 : 0.5f;

                case MultiArray:
                case InstanceOf:
                case Array:
                case Class:
                    if (data1.startsWith("X") || data2.startsWith("X"))
                        return 0.25f;
                    else if (!data1.equals(data2))
                        return 0.5f;
                    else return 0;
            }
        }
        return 1.0f;
    }


    private boolean isBasicType(String data) {
        return data.equals("boolean") || data.equals("byte") || data.equals("char") ||
                data.equals("int") || data.equals("long") || data.equals("float") ||
                data.equals("double") || data.equals("short");
    }
}
