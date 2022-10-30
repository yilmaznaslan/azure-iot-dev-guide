package org.example.azure.services.iotHub.devicemanagement.service;

import com.codahale.metrics.annotation.Timed;
import com.microsoft.azure.sdk.iot.service.twin.Twin;
import org.example.azure.services.iotHub.devicemanagement.business.DeviceManagementBA;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.HashMap;

@Path("iothub/{iotHubName}/devices")
@Produces(MediaType.APPLICATION_JSON)
public class DeviceManagementService {

    private final DeviceManagementBA deviceManagementBA;

    public DeviceManagementService(DeviceManagementBA deviceManagementBA) {
        this.deviceManagementBA = deviceManagementBA;
    }

    @POST
    @Timed
    public void registerDevicesInBulk(@PathParam("iotHubName") String iotHubName,
                                      @QueryParam("devicePrefix") String devicePrefix,
                                      @QueryParam("deviceCount") Integer deviceCount,
                                      @QueryParam("authenticationType") String authenticationType) throws Exception {
        deviceManagementBA.createAndRegisterDevicesToIotHub(iotHubName,devicePrefix, deviceCount, authenticationType);
    }

    //@POST
    @Timed
    public void registerSingleDevice(@QueryParam("deviceId") String deviceId) throws Exception {
        deviceManagementBA.registerSingleDevice(deviceId);
    }


    @GET
    @Timed
    public HashMap<String, Twin> getDevices(@PathParam("iotHubName") String iotHubName) throws Exception {
        //deviceManagementBA.getDevicesFromIotHubToBlob(iotHubName);
        return deviceManagementBA.getDeviceTwins();
    }

    @GET
    @Timed
    @Path("/{deviceId}")
    public Twin getDeviceTwinByDeviceId(@PathParam("deviceId") String deviceId,
                                        @PathParam("iotHubName") String iotHubName) throws Exception {
        return deviceManagementBA.getDeviceTwinByDeviceId(deviceId);
    }

    @DELETE
    @Timed
    public void deleteDevices(@PathParam("iotHubName") String iotHubName) throws Exception {
        deviceManagementBA.deleteDeviceTwins();
    }
}