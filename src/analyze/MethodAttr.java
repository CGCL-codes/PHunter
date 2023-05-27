package analyze;

import soot.*;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class MethodAttr implements Comparable<MethodAttr> {

    public static final String ClassRepresentation = "X";
    public static final String ArrayClassRepresentation = "X[]";

    public ClassAttr declaredClass;
    public String signature;
    public int modifiers;
    public String fuzzy;
    public String subSignature;
    public Body body;

    public int startLinenumber;
    public int endLinenumber;

    public final List<MethodAttr> callee = new LinkedList<>();
    public final List<MethodAttr> caller = new LinkedList<>();
    public final Set<SootField> fieldRef = new HashSet<>();
    public List<String> fuzzyFieldRef = null;

    //    private final Set<String> readField = new HashSet<>();
//    private final Set<String> writeField = new HashSet<>();
    public int getStartLinenumber() {
        return startLinenumber;
    }

    public void setStartLinenumber(int startLinenumber) {
        this.startLinenumber = startLinenumber;
    }

    public int getEndLinenumber() {
        return endLinenumber;
    }

    public void setEndLinenumber(int endLinenumber) {
        this.endLinenumber = endLinenumber;
    }

    public MethodAttr(Body body) { // for method
        this.body = body;
        SootMethod method = body.getMethod();
        signature = method.getSignature();
        fuzzy = getFuzzyForm(method);
        modifiers = method.getModifiers();
        subSignature = method.getSubSignature();
    }

    public MethodAttr(SootMethod method) { // for no body method
        signature = method.getSignature();
        fuzzy = getFuzzyForm(method);
        modifiers = method.getModifiers();
        subSignature = method.getSubSignature();
    }


    public void getFieldFuzzyForm() {
        if (fieldRef.isEmpty())
            return;
        this.fuzzyFieldRef = new LinkedList<>();
        for (SootField f : fieldRef) {
            StringBuilder sb = new StringBuilder();
            addType(f.getType(), sb);
            fuzzyFieldRef.add(sb.toString());
        }
    }


    private String getFuzzyForm(SootMethod m) {
        if (m == null)
            return null;
        StringBuilder sb = new StringBuilder();
        if (m.isStatic())
            sb.append("static,");
        addType(m.getReturnType(), sb);
        for (Type t : m.getParameterTypes())
            addType(t, sb);
        return sb.toString();
    }

    private void addType(Type t, StringBuilder sb) {
        if (t instanceof RefType) {
            RefType refType = (RefType) t;
            if (refType.getSootClass().isJavaLibraryClass()
                    || isAndroidClass(refType.getSootClass().getName()))
                sb.append(t).append(",");
            else if (refType.getSootClass().isApplicationClass()
                    || refType.getSootClass().isPhantomClass()) {
                sb.append("X,");
            } else {
                sb.append(t).append(",");
            }
            return;
        }
        if (t instanceof ArrayType &&
                ((ArrayType) t).baseType instanceof RefType) {
            RefType refType = (RefType) ((ArrayType) t).baseType;
            if (refType.getSootClass().isJavaLibraryClass()
                    || isAndroidClass(refType.getSootClass().getName()))
                sb.append(t).append(",");
            else if (refType.getSootClass().isApplicationClass()
                    || refType.getSootClass().isPhantomClass()) {
                sb.append("X[],");
            } else {
                sb.append(t).append(",");
            }
            return;
        }
        sb.append(t).append(",");
    }


    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }


    public void addCallee(MethodAttr callee) {
        this.callee.add(callee);
    }

    public void addCaller(MethodAttr caller) {
        this.caller.add(caller);
    }

    public static boolean isAndroidClass(String name) {
        return name.startsWith("android.") ||
                name.startsWith("androidx.") ||
                name.startsWith("dalvik.") ||
                name.startsWith("org.w3c.dom");
    }

//    public void addReadField(String fieldSignature) {
//        readField.add(fieldSignature);
//    }
//
//    public void addWriteField(String fieldSignature) {
//        writeField.add(fieldSignature);
//    }
//
//    public void addJavaLibraryCall(String methodSignature) {
//        javaLibraryCall.add(methodSignature);
//    }
//
//    public void addAndroidFrameworkCall(String methodSignature) {
//        androidFrameworkLibraryCall.add(methodSignature);
//    }

    @Override
    public int hashCode() {
        int hash = 0;
        String str = signature;
        int itr = str.length() / 32;
        if (str.length() % 32 != 0)
            itr += 1;
        for (int i = 0; i < itr; i++) {
            if (i != itr - 1)
                hash += str.substring(32 * i, 32 * i + 32).hashCode();
            else hash += str.substring(32 * i).hashCode();
        }
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        MethodAttr other = (MethodAttr) obj;
        if (this.signature == null) {
            return other.signature == null;
        } else return this.signature.equals(other.signature);
    }

    public String toString() {
        return this.signature;
    }

    @Override
    public int compareTo(MethodAttr o) {
        return this.signature.compareTo(o.signature);
    }
}

