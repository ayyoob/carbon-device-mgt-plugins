package org.wso2.carbon.device.mgt.iot.virtualfirealarm.api.listener;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.FrameworkUtil;
import org.wso2.carbon.core.ServerStartupObserver;


import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class EventListener implements ServletContextListener {
    private VirtualFirealarmStartupListener virtualFirealarmStartupListener;

    private static final Log log = LogFactory.getLog(VirtualFirealarmSecurityManager.class);
    private boolean initialized = false;
    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        virtualFirealarmStartupListener = new VirtualFirealarmStartupListener();
        FrameworkUtil.getBundle(ServerStartupObserver.class).getBundleContext().registerService(
                ServerStartupObserver.class.getName(), virtualFirealarmStartupListener, null);
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        if (virtualFirealarmStartupListener != null && virtualFirealarmStartupListener.isInialized()) {
            VirtualFireAlarmUtils.destroyMqttInputAdapter();
            VirtualFireAlarmUtils.destroyXmppInputAdapter();
        }
    }
}
