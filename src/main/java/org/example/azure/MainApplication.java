package org.example.azure;

import com.microsoft.azure.sdk.iot.service.exceptions.IotHubException;
import com.microsoft.azure.sdk.iot.service.twin.Twin;
import com.microsoft.azure.sdk.iot.service.twin.TwinClient;
import com.microsoft.azure.sdk.iot.service.twin.TwinCollection;

import java.io.IOException;

public class MainApplication {

    public static String iotHubConnectionString = "REPLACE_ME";

    public static void main(String[] args) throws Exception {
        patchDeviceTwin("dishwasher_2");
        getTwinTags("dishwasher_2");
    }
    public static void patchDeviceTwin(String deviceId) throws IOException, IotHubException {
        TwinClient twinClient = new TwinClient(iotHubConnectionString);
        Twin twin = twinClient.get(deviceId);
        TwinCollection twinCollection = new TwinCollection();
        twinCollection.put("customerId", "null");
        twinCollection.put("country", "usa");
        twinCollection.put("SoftwareId", "xe123");
        //twin.setTags(twinCollection);
        twinClient.patch(twin);
    }
    public static void getTwinTags(String deviceId) throws IOException, IotHubException {
        TwinClient twinClient = new TwinClient(iotHubConnectionString);
        Twin twin = twinClient.get(deviceId);
        TwinCollection twinCollection = twin.getTags();
        String countryValue  = twinCollection.get("country").toString();
        System.out.println("Country value of the twin is:" + countryValue);
        if(!countryValue.equals("usa")){
            System.out.println("Updating country tag has failed");
        }
    }
}
