package com.meinergy.framwork.pojo;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Handler
 * 封装handle方法相关信息
 * @author chenwang
 * @date 2020/10/13
 */
public class Handler {

    /**
     * controller类对象
     */
    private Object controller;

    /**
     * handle方法
     */
    private Method method;

    /**
     * url
     */
    private Pattern pattern;

    /**
     * 参数顺序，便于进行参数绑定
     */
    private Map<String,Integer> paramIndexMapping;

    public Handler(Object controller, Method method, Pattern pattern) {
        this.controller = controller;
        this.method = method;
        this.pattern = pattern;
        this.paramIndexMapping = new HashMap<>();
    }

    public Object getController() {
        return controller;
    }

    public void setController(Object controller) {
        this.controller = controller;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public Pattern getPattern() {
        return pattern;
    }

    public void setPattern(Pattern pattern) {
        this.pattern = pattern;
    }

    public Map<String, Integer> getParamIndexMapping() {
        return paramIndexMapping;
    }

    public void setParamIndexMapping(Map<String, Integer> paramIndexMapping) {
        this.paramIndexMapping = paramIndexMapping;
    }
}
