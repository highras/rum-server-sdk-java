package com.rum.json;

import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class JsonHelper {

    public interface IJson {

        Map toMap(Object javaBean);

        Map toMap(String jsonString);

        String toJSON(Object javaBean);

        String toJSON(Map map);

        Object toJavaBean(Object javabean, Map data);

        void toJavaBean(Object javabean, String jsonString);
    }

    private JsonHelper() {}

    private static class Singleton {

        private static final JsonHelper INSTANCE = new JsonHelper();
    }

    public static final JsonHelper getInstance() {

        return JsonHelper.Singleton.INSTANCE;
    }

    private IJson _iJson;

    public void setJson(IJson value) {

        this._iJson = value;
    }

    public IJson getJson() {

        if (this._iJson == null) {

            this._iJson = new JsonOrg();
        }

        return this._iJson;
    }
}

class JsonOrg implements JsonHelper.IJson {

    @Override
    public Map toMap(Object javaBean) {

        Map result = new HashMap();
        Method[] methods = javaBean.getClass().getDeclaredMethods();

        for (Method method : methods) {

            try {

                if (method.getName().startsWith("get")) {

                    String field = method.getName();
                    field = field.substring(field.indexOf("get") + 3);
                    field = field.toLowerCase().charAt(0) + field.substring(1);

                    Object value = method.invoke(javaBean, (Object[]) null);
                    result.put(field, null == value ? "" : value.toString());
                }
            } catch (Exception ex) {

                ex.printStackTrace();
            }
        }

        return result;
    }

    @Override
    public Map toMap(String jsonString) {

        JSONObject jsonObject = new JSONObject(jsonString);

        Map result = new HashMap();
        Iterator iterator = jsonObject.keys();
        String key = null;
        String value = null;

        while (iterator.hasNext()) {

            key = (String) iterator.next();
            value = jsonObject.getString(key);
            result.put(key, value);
        }

        return result;
    }

    @Override
    public String toJSON(Object javaBean) {

        return new JSONObject(toMap(javaBean)).toString();
    }

    @Override
    public String toJSON(Map map) {

        return new JSONObject(map).toString();
    }

    @Override
    public Object toJavaBean(Object javabean, Map data) {

        Method[] methods = javabean.getClass().getDeclaredMethods();

        for (Method method : methods) {

            try {

                if (method.getName().startsWith("set")) {

                    String field = method.getName();
                    field = field.substring(field.indexOf("set") + 3);
                    field = field.toLowerCase().charAt(0) + field.substring(1);
                    method.invoke(javabean, new Object[]{
                            data.get(field)
                    });
                }
            } catch (Exception ex) {

                ex.printStackTrace();
            }
        }

        return javabean;
    }

    @Override
    public void toJavaBean(Object javabean, String jsonString) {

        JSONObject jsonObject = new JSONObject(jsonString);

        Map map = toMap(jsonObject.toString());
        toJavaBean(javabean, map);
    }
}
