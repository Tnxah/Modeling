package zad1;


import zad1.models.Bind;

import javax.script.*;


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class Controller implements Bind {
    ScriptEngineManager manager;
    ScriptEngine engine;

    String modelName = "";

    public Class<?> modelClass;
    public Object model;


    Map<String, double[]> dataMap = new HashMap<String, double[]>();
    Map<String, double[]> resultDataMap = new LinkedHashMap<String, double[]>();

    int size = 15;
    int LL;

    double EKS[] = {2, 4, 6, 8, 10};
    double PKB[] = {2, 2, 2, 2, 2};

    List<String> finalResults[];
    Field[] fields;


    public void writeModelFields() {
        for (Field field : fields) {
            if (field.isAnnotationPresent(Bind.class) && !field.getName().equals("LL")) {
                field.setAccessible(true);
                try {
                    //System.out.println("Field before: " + field.get(model));
                    field.set(model, dataMap.get(field.getName()));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }

            }
            if (field.isAnnotationPresent(Bind.class) && field.getName().equals("LL")) {
                field.setAccessible(true);
                try {
                    field.set(model, LL);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void readModelFields() {
        double[] arr = new double[LL];

        for (Field field : fields) {
            if (field.isAnnotationPresent(Bind.class) && !field.getName().equals("LL")) {
                field.setAccessible(true);
                try {


                    arr = (double[]) field.get(model);
                    resultDataMap.put(field.getName(), arr);
                    //System.out.println(field.getName() + Arrays.toString(arr));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }

    }


    public Controller(String modelName) {
        this.modelName = modelName;
        findClass();

        manager = new ScriptEngineManager();
        engine = manager.getEngineByName("groovy");


    }

    public void findClass() {
        try {
            modelClass = Class.forName("zad1.models." + modelName);
            //System.out.println();
            this.model = modelClass.newInstance();

            fields = modelClass.getDeclaredFields();
        } catch (ClassNotFoundException e) {
            System.out.println("Model not found");
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }
    }

    public Controller runModel() {
        writeModelFields();
        Method run;
        try {
            run = modelClass.getDeclaredMethod("run");
            run.setAccessible(true);
            run.invoke(model);
        } catch (NoSuchMethodException e) {
            System.out.println("There is no \"run()\" method in this model");
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        readModelFields();
        return this;
    }

    public Controller readDataFrom(String s) {
        String[] lineValues;
        double[] values;
        size = 15;
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(s));
            String line = reader.readLine();
            while (line != null) {

                lineValues = line.split("\\s");
                if (lineValues[0].equals("LATA")) {
                    size = lineValues.length - 1;
                }
                values = new double[size];
                for (int i = 1; i < size + 1; i++) {
                    if (i < lineValues.length) {
                        values[i - 1] = Double.parseDouble(lineValues[i]);
                    }
                    if (lineValues.length - 1 < size && i >= lineValues.length) {
                        values[i - 1] = values[i - 2];
                    }
                }

                dataMap.put(lineValues[0], values);

                line = reader.readLine();
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        values = dataMap.get("LATA");
        resultDataMap.put("LATA", values);
        LL = values.length;

        return this;
    }

    public void runScript(String script){
        try {
            engine.put("LL", LL);
            for (Map.Entry<String, double[]> map : resultDataMap.entrySet()
            ) {
                engine.put(map.getKey(), resultDataMap.get(map.getKey()));
            }
            engine.eval(script);

            showBindings();
        } catch (ScriptException e) {
            System.out.println("There are some errors in script: " + e.getMessage());
        }
    }

    public void runScriptFromFile(String s) {
        String script = "";
        try {
            script = new String(Files.readAllBytes(Paths.get(s)));
        } catch (IOException e) {
            e.printStackTrace();
        }
        runScript(script);
    }

    private void showBindings() {

        ScriptContext ctx = engine.getContext();   // uzyskanie biezącego kontekstu
        List<Integer> scopes = ctx.getScopes();    // z tego kontekstu - jakie są zakresy
        for (Integer scope : scopes) {

            Bindings bnd = ctx.getBindings(scope);

            for (String key : bnd.keySet()) {
if(!resultDataMap.containsKey(key) && !key.equals("LL") && bnd.get(key).getClass().toString().equals("class [D"))
                resultDataMap.put(key, (double[])bnd.get(key));
            }
        }
    }


    public String getResultsAsTsv() {
        StringBuilder sb = new StringBuilder();
String helpS = "";
        double[] vars = new double[size];
        for (Map.Entry<String, double[]> map : resultDataMap.entrySet()
        ) {
            sb.append(map.getKey());

            vars = resultDataMap.get(map.getKey());
            for (int i = 0; i < vars.length; i++) {
                if (map.getKey().equals("LATA")) {
                    helpS = vars[i] + "";
                    helpS = helpS.replace(".0","");
                    sb.append("\t" + helpS);
                } else {
                    sb.append("\t" + vars[i]);
                }

            }
            sb.append("\n");
        }

        return sb.toString();
    }


    @Override
    public Class<? extends Annotation> annotationType() {
        return null;
    }
}
