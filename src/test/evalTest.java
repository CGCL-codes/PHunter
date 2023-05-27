package test;


import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public class evalTest {
    public static void main(String[] args) {//test the class ScriptEngineManager

        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine se = manager.getEngineByName("JS");
        String str = "8>>2==2";
//        se.put("a",1);
        boolean result;
        try {
            result = (Boolean) se.eval(str);

            System.out.println(result);
        } catch (ScriptException e) {
            e.printStackTrace();
        }
    }
}
