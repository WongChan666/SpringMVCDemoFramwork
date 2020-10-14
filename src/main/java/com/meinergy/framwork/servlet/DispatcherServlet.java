package com.meinergy.framwork.servlet;

import com.meinergy.framwork.annotion.Autowired;
import com.meinergy.framwork.annotion.Controller;
import com.meinergy.framwork.annotion.RequestMapping;
import com.meinergy.framwork.annotion.Service;
import com.meinergy.framwork.pojo.Handler;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DispatcherServlet
 *
 * @author chenwang
 * @date 2020/10/13
 */
public class DispatcherServlet extends HttpServlet {

    private final Properties properties = new Properties();

    /**
     * handler（缓存url和method的映射）
     */
    private List<Handler> handlerMapping = new ArrayList<>();

    /**
     * IOC容器
     */
    private final Map<String,Object> ioc = new HashMap<>();

    /**
     * 缓存扫描到的全限定类名
     */
    private final List<String> classNames = new ArrayList<>();

    @Override
    public void init(ServletConfig config){
        //1.加载配置文件 springmvc.properties
        String contextConfigLocation = config.getInitParameter("contextConfigLocation");
        doLoadConfig(contextConfigLocation);

        //2.扫描相关的注解
        doScan(properties.getProperty("scanPackage"));

        //3.初始化相关的benn，添加到ioc容器当中
        doInstance();

        //4.实现依赖注入
        doAutowired();

        //5.构造一个handlerMapping处理器映射器,将处理好的url和handler方法建立映射关系
        initHandlerMapping();

        System.out.println("springmvc，初始化完成--------");
    }

    /**
     * 构造一个handlermapping处理器映射器.将处理好的url和handler方法建立映射关系
     */
    private void initHandlerMapping() {
        if(ioc.size()==0){
            return;
        }
        //遍历ioc容器
        for(Map.Entry entry :ioc.entrySet()){
            //获取类对象
            Class<?> aClass = entry.getValue().getClass();
            //如果不是controller注解类，则跳过
            if(!aClass.isAnnotationPresent(Controller.class)){
                continue;
            }

            //类注解的value作为url前缀
            String baseUrl = "";
            if(aClass.isAnnotationPresent(RequestMapping.class)){
                //获取注解
                RequestMapping annotation = aClass.getAnnotation(RequestMapping.class);
                //获得注解的值（url前缀）
                baseUrl = annotation.value();
            }

            //获得方法
            Method[] methods = aClass.getMethods();
            for(Method method : methods){
                //判断是否有RequestMapping注解
                if(!method.isAnnotationPresent(RequestMapping.class)){
                    continue;
                }

                //若有RequestMapping注解
                //获取该注解
                RequestMapping annotation = method.getAnnotation(RequestMapping.class);
                //获取方法上的注解的值并拼接为最终url
                String url = baseUrl+annotation.value();


                //把method信息都封装到一个hander对象中。
                Handler handler = new Handler(entry.getValue(),method, Pattern.compile(url));

                //计算方法的参数位置信息
                Parameter[] parameters = method.getParameters();
                for (int j = 0; j < parameters.length; j++) {
                    Parameter parameter = parameters[j];
                    //如果是response或者request对象，则参数名写为HttpServletRequest或者HttpServletResponse
                    if (parameter.getType() == HttpServletRequest.class || parameter.getType() == HttpServletResponse.class) {
                        handler.getParamIndexMapping().put(parameter.getType().getSimpleName(), j);
                    } else {
                        handler.getParamIndexMapping().put(parameter.getName(),j);
                    }
                }
                handlerMapping.add(handler);
            }
        }
    }

    /**
     * 实现依赖注入
     */
    private void doAutowired() {

        if(ioc.size()==0){
            return;
        }
        //遍历ioc容器
        for(Map.Entry entry: ioc.entrySet()){
            //获取类的所有属性
            Field[] declaredFields = entry.getValue().getClass().getDeclaredFields();

            for(Field field :declaredFields){
                //判断属性是否有AutoWired注解
                if(!field.isAnnotationPresent(Autowired.class)){
                    continue;
                }
                //有该注解
                //获取该注解对象
                Autowired annotation = field.getAnnotation(Autowired.class);
                //注入的bean id
                String beanName = annotation.value();

                //没有配置bean id，则按照当前字段类型注入
                if("".equals(beanName.trim())){
                    beanName = field.getType().getName();
                }

                //打开访问权限
                field.setAccessible(true);
                try {
                    //设置当前的bean对象的属性值为ioc中的指定beanName的bean对象，实现依赖注入.
                    field.set(entry.getValue(),ioc.get(beanName));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * IOC容器
     */
    private void doInstance(){
        if(classNames.size()==0) {
            return;
        }
        try{
            for (String classname : classNames) {
                //反射
                Class<?> aClass = Class.forName(classname);
                //判断是否是controller注解的类
                if (aClass.isAnnotationPresent(Controller.class)) {
                    //获取该类的简单类名
                    String simpleName = aClass.getSimpleName();
                    //首字母小写
                    String lowerFirstSimpName = lowerFirst(simpleName);
                    //实例化
                    Object o = aClass.newInstance();
                    ioc.put(lowerFirstSimpName, o);
                    //service注解的类
                } else if (aClass.isAnnotationPresent(Service.class)) {
                    Service annotation = aClass.getAnnotation(Service.class);
                    //获取value的值
                    String beanName = annotation.value();

                    //如果value有值（指定了id），就以指定的为准
                    if (!"".equals(beanName.trim())) {
                        ioc.put(beanName, aClass.newInstance());
                    } else {
                        //没有指定，则以首字母小写作为id
                        ioc.put(lowerFirst(aClass.getSimpleName()), aClass.newInstance());
                    }

                    //为service的实现接口进行ioc管理，
                    Class<?>[] interfaces = aClass.getInterfaces();
                    for (Class<?> anInterface : interfaces) {
                        //接口的全限定类名作为id存入ioc容器进行管理
                        ioc.put(anInterface.getName(), aClass.newInstance());
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * 首字母小写
     * @param str
     * @return
     */
    public String lowerFirst(String str){
        char left = 'A',right = 'Z';
        char [] chars = str.toCharArray();
        if(chars[0]>=left&&chars[0]<=right){
            chars[0]+=32;
        }
        return String.valueOf(chars);
    }

    /**
     * 扫描注解类
     * @param scanPackage
     */
    private void doScan(String scanPackage) {
        //获取磁盘路径
        String scanPackagePath = Thread.currentThread().getContextClassLoader().getResource("")+scanPackage.replaceAll("\\.","/");
        scanPackagePath = scanPackagePath.substring(5);
        File pack = new File(scanPackagePath);
        //获取路径下的所有文件
        File[] files = pack.listFiles();
        for(File f :files){
            if(f.isDirectory()){
                //如果有子文件夹递归调用doScan方法
                doScan(scanPackage+"."+f.getName());
            }else if (f.getName().endsWith("class")){
                //若是以.class结尾的文件则获取其全限定类名
                String className = scanPackage+"."+f.getName().replaceAll(".class","");
                //添加到缓存中以便使用
                classNames.add(className);
            }

        }
    }

    /**
     * 加载配置文件
     * @param contextConfigLocation
     */
    private void doLoadConfig(String contextConfigLocation) {
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation);
        try {
            properties.load(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req,resp);
    }

    /**
     * 请求的处理
     * @param req
     * @param resp
     * @throws IOException
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        //1.根据请求的url获取对应的handler
        Handler handler = getHandler(req);

        //如果找不到匹配的handler，返回404
        if(handler==null){
            resp.getWriter().write("404 not found");
            return;
        }

        //2.参数绑定
        //获取所有的参数类型数组，数组长度即为最后传入反射调用handle方法的可变参数args数组长度
        Parameter[] parameters = handler.getMethod().getParameters();

        //创建一个新的数组，用于存储参数数组，
        Object[] paramValues = new Object[parameters.length];

        //向参数数组中请求传入方法值，并且保证参数顺序和方法中的参数顺序一致。
        //获取所有的参数map
        Map<String,String[]> parameterMap = req.getParameterMap();

        //遍历request中所有的参数
        for(Map.Entry<String,String[]> param:parameterMap.entrySet()){
            //拼接参数数组值为string字符串，并用逗号隔开
            String value = StringUtils.join(param.getValue(),",");

            //若请求参数名和handle方法参数名匹配上，则填充数据
            if(!handler.getParamIndexMapping().containsKey(param.getKey())){
                continue;
            }

            //找到该参数索引位置，并将参数值放入数组的对应位置中
            paramValues[handler.getParamIndexMapping().get(param.getKey())] = value;
        }

        //request和response特殊处理
        int requestIndex = handler.getParamIndexMapping().get(HttpServletRequest.class.getSimpleName());
        paramValues[requestIndex] = req;

        int responseIndex = handler.getParamIndexMapping().get(HttpServletResponse.class.getSimpleName());
        paramValues[responseIndex] = resp;

        //最终调用invoke方法执行对应的handle方法处理
        try {
            handler.getMethod().invoke(handler.getController(),paramValues);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException exception) {
            exception.printStackTrace();
        }

    }

    /**
     * 获取对应的handler
     * @param req
     * @return
     */
    private Handler getHandler(HttpServletRequest req) {
        if(handlerMapping.size()==0){return null;}

        //获取请求的url
        String url = req.getRequestURI();
        System.out.println(url);
        for(Handler handler:handlerMapping){
            Matcher matcher = handler.getPattern().matcher(url);
            //判断是否匹配url
            if(!matcher.matches()){
                continue;
            }
            return handler;
        }
        return null;
    }

}
