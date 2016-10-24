/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.device.mgt.iot.virtualfirealarm.api.listener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.core.ServerStartupObserver;
import org.wso2.carbon.device.mgt.iot.virtualfirealarm.api.config.VirtualFirealarmConfig;
import org.wso2.carbon.device.mgt.iot.virtualfirealarm.api.exception.VirtualFirealarmConfigurationException;
import org.wso2.carbon.device.mgt.iot.virtualfirealarm.api.util.APIUtil;

import java.io.IOException;

public class VirtualFirealarmStartupListener implements ServerStartupObserver {
    private static final Log log = LogFactory.getLog(VirtualFirealarmSecurityManager.class);
    private boolean inialized = false;

    @Override
    public void completingServerStartup() {
    }

    @Override
    public void completedServerStartup() {
        try {
            VirtualFirealarmConfig.initialize();
            VirtualFireAlarmUtils.setupMqttInputAdapter();
            VirtualFireAlarmUtils.setupXmppInputAdapter();
            APIUtil.getInputEventAdapterService().start();
            inialized = true;
        } catch (IOException e) {
            log.error("failed to initialize the startup listener.", e);
        } catch (VirtualFirealarmConfigurationException e) {
            log.error("failed to initialize the configuration virtual-firealarm.xml", e);
        }
    }

    public boolean isInialized() {
        return inialized;
    }
}