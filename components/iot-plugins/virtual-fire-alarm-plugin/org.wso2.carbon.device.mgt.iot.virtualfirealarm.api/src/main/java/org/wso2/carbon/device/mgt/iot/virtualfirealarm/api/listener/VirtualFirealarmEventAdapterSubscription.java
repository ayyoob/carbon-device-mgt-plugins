package org.wso2.carbon.device.mgt.iot.virtualfirealarm.api.listener;

import org.wso2.carbon.device.mgt.iot.input.adapter.extension.ContentTransformer;
import org.wso2.carbon.device.mgt.iot.virtualfirealarm.api.config.mqtt.MqttConfig;
import org.wso2.carbon.device.mgt.iot.virtualfirealarm.api.config.xmpp.XmppConfig;
import org.wso2.carbon.event.input.adapter.core.InputEventAdapterSubscription;

public class VirtualFirealarmEventAdapterSubscription implements InputEventAdapterSubscription {

    private ContentTransformer contentTransformer;

    public VirtualFirealarmEventAdapterSubscription() {
        //added contentTransformer here due to class not visible to input event adapter bundle.
        if (MqttConfig.getInstance().isEnabled()) {
            contentTransformer = new VirtualFirealarmMqttContentTransformer();
        } else if (XmppConfig.getInstance().isEnabled()) {
            contentTransformer = new VirtualFirealarmXmppContentTransformer();
        }
    }

    @Override
    public void onEvent(Object o) {
        String msg = (String) o;
        if (contentTransformer != null) {
            msg = (String) contentTransformer.transform(o, null);
        }
        if (msg != null && !msg.isEmpty()) {
            String[] messages = (msg).split(",");
            String deviceId = messages[0];
            String actualMessage = messages[1];
            if (actualMessage.contains("PUBLISHER")) {
                float temperature = Float.parseFloat(actualMessage.split(":")[2]);
                VirtualFireAlarmUtils.publishToDAS(deviceId, temperature);
            } else {
                float temperature = Float.parseFloat(actualMessage.split(":")[1]);
                VirtualFireAlarmUtils.publishToDAS(deviceId, temperature);
            }
        }
    }
}
