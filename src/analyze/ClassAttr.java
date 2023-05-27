package analyze;

import soot.SootClass;

import java.util.ArrayList;
import java.util.List;

public class ClassAttr {
    public int modifier;
    public String name;
    public String packageName;
    public List<MethodAttr> methods = new ArrayList<>();
    public List<ClassAttr> superClasses = new ArrayList<>();
    public boolean has_phantom_superclass_ = false;


    public ClassAttr(SootClass sc) {
        this.modifier = sc.getModifiers();
        this.name = sc.getName();
        this.packageName = sc.getPackageName();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }


    public void addMethod(MethodAttr methodAttr) {
        methods.add(methodAttr);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
//        sb.append("Class: ");
        sb.append(name);
//        sb.append("\tsuperClassName ");
//        sb.append(packageName);
//        sb.append("\n");

//        sb.append(String.format("method size:%s\n", methodAttrs.size()));
//        for (MethodAttr methodAttr : methods) {
//            sb.append(methodAttr.getSignature()).append(" ").append(methodAttr.getStartLinenumber()).append(" ").append(methodAttr.getEndLinenumber()).append("\n");
//            sb.append("xref call to:\n");
//            for (String s : methodAttr.getCallee()) {
//                sb.append(s).append("\n");
//            }
//            sb.append("read field:\n");
//            for (String s : methodAttr.getReadField()) {
//                sb.append(s).append("\n");
//            }
//            sb.append("write field:\n");
//            for (String s : methodAttr.getWriteField()) {
//                sb.append(s).append("\n");
//            }
//            sb.append("call java library api:\n");
//            for (String s : methodAttr.getJavaLibraryCall()) {
//                sb.append(s).append("\n");
//            }
//            sb.append("\n");
//        }
//        sb.append("=======================\n");
        return sb.toString();

    }

}
