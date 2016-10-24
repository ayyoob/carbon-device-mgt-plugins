package org.wso2.carbon.device.mgt.iot.virtualfirealarm.api.listener;

import org.json.JSONObject;
import org.wso2.carbon.base.MultitenantConstants;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.device.mgt.iot.input.adapter.extension.ContentTransformer;
import org.wso2.carbon.device.mgt.iot.virtualfirealarm.api.exception.VirtualFireAlarmException;
import org.wso2.carbon.device.mgt.iot.virtualfirealarm.api.constants.VirtualFireAlarmConstants;

import java.security.PublicKey;
import java.util.Map;

public class VirtualFirealarmXmppContentTransformer implements ContentTransformer {

    @Override
    public Object transform(Object message, Map<String, Object> dynamicProperties) {
        JSONObject jsonPayload = new JSONObject((String) message);
        try {
            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext ctx = PrivilegedCarbonContext.getThreadLocalCarbonContext();
            ctx.setTenantDomain(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME, true);
            Integer serialNo = (Integer) jsonPayload.get(VirtualFireAlarmConstants.JSON_SERIAL_KEY);
            // the hash-code of the deviceId is used as the alias for device certificates during SCEP enrollment.
            // hence, the same is used here to fetch the device-specific-certificate from the key store.
            PublicKey clientPublicKey = VirtualFireAlarmUtils.getDevicePublicKey("" + serialNo);

            // the MQTT-messages from VirtualFireAlarm devices are in the form {"Msg":<MESSAGE>, "Sig":<SIGNATURE>}
            String actualMessage = VirtualFireAlarmUtils.extractMessageFromPayload((String) message,
                                                                                   clientPublicKey);
            return actualMessage;
        } catch (VirtualFireAlarmException e) {
            return "";
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }
    }
}
