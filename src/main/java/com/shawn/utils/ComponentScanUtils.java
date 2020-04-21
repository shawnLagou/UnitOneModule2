package com.shawn.utils;

import com.shawn.factory.BeanFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public class ComponentScanUtils {

    /**
     * 扫描是否有Transactional注解
     * @param beanName
     * @return
     */
    public static boolean Scan(String beanName) {
        Class<?> clazz =  BeanFactory.getBean(beanName).getClass();
        Method[] declaredMethods = clazz.getDeclaredMethods();
        for (Method declaredMethod : declaredMethods) {
            Annotation[] annotations = declaredMethod.getAnnotations();
            for (Annotation annotation : annotations) {
                if (annotation.toString().contains("Transactional")) {
                    return true;
                }
            }
        }
        return false;
    }
}
