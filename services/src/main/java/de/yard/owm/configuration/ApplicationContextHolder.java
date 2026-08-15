package de.yard.owm.configuration;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class ApplicationContextHolder implements ApplicationContextAware {
    private static ApplicationContext applicationContext;
    public static <T> T getBean(Class<T> type) {
        return applicationContext.getBean(type);
    }
    public static <T> T getBean(String name, Class<T> type) {
        return applicationContext.getBean(name, type);
    }
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        ApplicationContextHolder.applicationContext = applicationContext;
    }
}
