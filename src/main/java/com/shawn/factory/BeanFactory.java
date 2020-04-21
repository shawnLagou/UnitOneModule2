package com.shawn.factory;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;

public class BeanFactory {

    /**
     * 任务一：读取解析xml，通过反射技术实例化对象并且存储待用（map集合）
     * 任务二：对外提供获取实例对象的接口（根据id获取）
     */

    private static Map<String,Object> map = new HashMap<>();  // 存储对象

    static {
        // 扫描注解
        InputStream resourceAsStream = BeanFactory.class.getClassLoader().getResourceAsStream("applicationContext.xml");

        // 解析xml
        SAXReader saxReader = new SAXReader();
        try {
            Document document = saxReader.read(resourceAsStream);
            Element rootElement = document.getRootElement();
            List<Element> beanList = rootElement.selectNodes("//context");
            for (int i = 0; i < beanList.size(); i++) {
                Element element =  beanList.get(i);
                // 处理每个bean元素，获取到该元素的id 和 package-base 属性
                String id = element.attributeValue("id");        // packageContext
                String packageName = element.attributeValue("package-base");  // com.shawn

                // 注解映射
                Map<String, Class<?>> annotationMap = new HashMap<>();
                Set<Class<?>> allClasses = getAllClasses(packageName, annotationMap);

                // 实例化之后的对象集合
                for (Class<?> aClass : allClasses) {
                    Annotation[] annotations = aClass.getAnnotations();
                    for (Annotation annotation : annotations) {
                        if (annotationMap.containsValue(annotation.annotationType())) {
                            Object o = aClass.newInstance();
                            String name = "";
                            Class<?>[] interfaces = aClass.getInterfaces();

                            // 不存在value值，则使用类名
                            if (getMemberValue(annotation).equals("")) {
                                if (interfaces.length == 0) {
                                    int index = aClass.getName().lastIndexOf(".");
                                    name = aClass.getName().substring(index + 1);
                                } else {
                                    // 如果实现接口，则使用接口名
                                    for (Class<?> anInterface : interfaces) {
                                        int index = anInterface.getName().lastIndexOf(".");
                                        name = anInterface.getName().substring(index + 1);
                                    }
                                }
                            } else {
                                // 获取value值
                                name = (String) getMemberValue(annotation);
                            }
                            // 存储到map中待用
                            map.put(name,o);
                        }

                    }
                }

                // 反射调用属性的set方法
                for (Map.Entry<String, Object> entry : map.entrySet()) {
                    Object object = entry.getValue();
                    Class<?> clazz = object.getClass();
                    Field[] fields= clazz.getDeclaredFields();
                    for (Field field : fields) {
                        Annotation[] annotations = field.getAnnotations();
                        String beanName = "";
                        for (Annotation annotation : annotations) {
                            // 实现自动装配
                            if (annotation.toString().contains("Autowired")) {
                                String name = field.getGenericType().getTypeName();
                                int index = name.lastIndexOf(".");
                                beanName = name.substring(index + 1);
                            }
                            // 如果注入bean使用value属性，则Qualifier需要指定相同名字
                            if (annotation.toString().contains("Qualifier")) {
                                beanName = (String) getMemberValue(annotation);
                            }
                        }
                        if (!beanName.equals("")) {
                            field.setAccessible(true);
                            field.set(object, map.get(beanName));
                        }
                    }
                }
            }
        } catch (DocumentException e) {
            e.printStackTrace();
        }  catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * 获取注解中的value
     * @param annotation
     * @return
     * @throws Exception
     */
    private static Object getMemberValue(Annotation annotation) throws Exception {
        InvocationHandler invocationHandler = Proxy.getInvocationHandler(annotation);
        Field declaredField = invocationHandler.getClass().getDeclaredField("memberValues");
        declaredField.setAccessible(true);
        Map memberValues = (Map) declaredField.get(invocationHandler);
        return memberValues.get("value");
    }

    private static Set<Class<?>> getAllClasses(String packageName, Map<String, Class<?>> annotationMap) {
        // 第一个class类的集合
        Set<Class<?>> classes = new LinkedHashSet<Class<?>>();

        // 是否循环迭代
        boolean recursive = true;
        // 获取包的名字 并进行替换
        String packageDirName = packageName.replace('.', '/');
        // 定义一个枚举的集合 并进行循环来处理这个目录下的things
        Enumeration<URL> dirs;
        try {
            dirs = Thread.currentThread().getContextClassLoader().getResources(packageDirName);
            // 循环迭代下去
            while (dirs.hasMoreElements()) {
                // 获取下一个元素
                URL url = dirs.nextElement();
                // 得到协议的名称
                String protocol = url.getProtocol();
                // 如果是以文件的形式保存在服务器上
                if ("file".equals(protocol)) {
                    // 获取包的物理路径
                    String filePath = URLDecoder.decode(url.getFile(), "UTF-8");
                    // 以文件的方式扫描整个包下的文件 并添加到集合中
                    findClassesInPackageByFile(packageName, filePath, recursive, classes, annotationMap);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return classes;
    }

    /**
     * 以文件的形式来获取包下的所有Class
     *
     * @param packageName
     * @param packagePath
     * @param recursive
     * @param classes
     */
    private static void findClassesInPackageByFile(String packageName, String packagePath, final boolean recursive, Set<Class<?>> classes, Map<String, Class<?>> annotationMap) {
        // 获取此包的目录 建立一个File
        File dir = new File(packagePath);
        // 如果不存在或者 也不是目录就直接返回
        if (!dir.exists() || !dir.isDirectory()) {
            // log.warn("用户定义包名 " + packageName + " 下没有任何文件");
            return;
        }
        // 如果存在 就获取包下的所有文件 包括目录
        File[] dirfiles = dir.listFiles(new FileFilter() {
            // 自定义过滤规则 如果可以循环(包含子目录) 或则是以.class结尾的文件(编译好的java类文件)
            @Override
            public boolean accept(File file) {
                return (recursive && file.isDirectory()) || (file.getName().endsWith(".class"));
            }
        });
        // 循环所有文件
        for (File file : dirfiles) {
            // 如果是目录 则继续扫描
            if (file.isDirectory()) {
                findClassesInPackageByFile(packageName + "." + file.getName(), file.getAbsolutePath(), recursive, classes, annotationMap);
            } else {
                // 如果是java类文件 去掉后面的.class 只留下类名
                String className = file.getName().substring(0, file.getName().length() - 6);
                try {
                    // 添加到集合中去
                    Class<?> clazz = Thread.currentThread().getContextClassLoader().loadClass(packageName + '.' + className);
                    if (clazz.isAnnotation() && !annotationMap.containsKey(className)) {
                        annotationMap.put(className, clazz);
                    } else if (!clazz.isInterface() && !clazz.isAnonymousClass()) {
                        classes.add(Thread.currentThread().getContextClassLoader().loadClass(packageName + '.' + className));
                    }
                } catch (ClassNotFoundException e) {
                    // log.error("添加用户自定义视图类错误 找不到此类的.class文件");
                    e.printStackTrace();
                }
            }
        }
    }


    // 任务二：对外提供获取实例对象的接口（根据id获取）
    public static Object getBean(String id) {
        return map.get(id);
    }

}
