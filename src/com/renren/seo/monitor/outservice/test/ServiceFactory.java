package com.renren.seo.monitor.outservice.test;

import java.util.Collection;

import com.renren.xoa.lite.annotation.XoaService;
@XoaService(serviceId = "zhan.xoa.renren.com")
public class ServiceFactory<E> {

    /**
     * 真正的工厂类,抽象IServiceFactory，目的在于封装创建Service实例的逻辑，与静态工厂类ServiceFactory解耦。
     * 
     * by xun.dai@renren-inc.com
     * 
     */

    public static <T extends Collection> T getService(Class<T> serviceClass) {
        return getService(serviceClass, 250);
    }

    public static <T extends Collection> T getService(Class<T> serviceClass, int timeout) {
    	return (T)new Object();
    }
}