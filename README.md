# Bug Report

## How to reproduce the bug

1. Create an IoTHub in azure
   1. Replace the `IOT_HUB_NAME` with the created iothub name
   2. Replace the `IOT_HUB_CONNECTION_STRING` with connection string of the creted Iot Hub ( iothub owner policy)

2. Create a Storage Account in azure
   1. Create a container in the storage account
   2. Replace the `CONTAINER_NAME` in `MainApplication.java` with the name of container that is created.
   3. Set the value of the `STORAGE_CONNECTION_STRING` in `MainApplication.java`with the connection string of the storage account.( Security+Network/AccessKeys)

3. Create blob file `devices.txt` and register devices from blob to iot hub by setting the

   In order to do that set the `main()` method as below and run `./gradlew run`.
   ````  
   public static void main(String[] args) throws Exception {
      createDevicesInBlob(DEVICE_PREFIX, DEVICE_COUNT, AuthenticationType.SELF_SIGNED);
      //createDevicesInBlob(DEVICE_PREFIX, DEVICE_COUNT, AuthenticationType.SAS);
      registerDevicesFromBlobToIoTHub(IOT_HUB_NAME);
   }
   ````
   After running this command a file called `devices.txt` will be created in the container. Below is `devices.txt` when the authentication type is set to SELF_SIGNED

   ````
   {"id":"evehicle_0","importMode":"createOrUpdate","status":"Enabled","authentication":{"thumbprint":{"primaryThumbprint":"DE89B7BBD215E7E47ECD372F61205712D71DD521","secondaryThumbprint":"DE89B7BBD215E7E47ECD372F61205712D71DD521"},"type":"SELF_SIGNED"}}
   {"id":"evehicle_1","importMode":"createOrUpdate","status":"Enabled","authentication":{"thumbprint":{"primaryThumbprint":"DE89B7BBD215E7E47ECD372F61205712D71DD521","secondaryThumbprint":"DE89B7BBD215E7E47ECD372F61205712D71DD521"},"type":"SELF_SIGNED"}}
   {"id":"evehicle_2","importMode":"createOrUpdate","status":"Enabled","authentication":{"thumbprint":{"primaryThumbprint":"DE89B7BBD215E7E47ECD372F61205712D71DD521","secondaryThumbprint":"DE89B7BBD215E7E47ECD372F61205712D71DD521"},"type":"SELF_SIGNED"}}
   {"id":"evehicle_3","importMode":"createOrUpdate","status":"Enabled","authentication":{"thumbprint":{"primaryThumbprint":"DE89B7BBD215E7E47ECD372F61205712D71DD521","secondaryThumbprint":"DE89B7BBD215E7E47ECD372F61205712D71DD521"},"type":"SELF_SIGNED"}}
   {"id":"evehicle_4","importMode":"createOrUpdate","status":"Enabled","authentication":{"thumbprint":{"primaryThumbprint":"DE89B7BBD215E7E47ECD372F61205712D71DD521","secondaryThumbprint":"DE89B7BBD215E7E47ECD372F61205712D71DD521"},"type":"SELF_SIGNED"}}
   ````

4. In the created container, read the `importErrors.log`. A sample of the `importErrors.log` is below;

   ```
   {"errorCode":400012,"errorStatus":"Invalid device format in line: {\"id\":\"evehicle_0\",\"importMode\":\"createOrUpdate\",\"status\":\"Enabled\",\"authentication\":{\"thumbprint\":{\"primaryThumbprint\":\"DE89B7BBD215E7E47ECD372F61205712D71DD521\",\"secondaryThumbprint\":\"DE89B7BBD215E7E47ECD372F61205712D71DD521\"},\"type\":\"SELF_SIGNED\"}}. Error converting value \"SELF_SIGNED\" to type 'System.Nullable`1[Microsoft.Azure.Devices.Cloud.AuthenticationType]'. Path 'authentication.type', line 1, position 249."}
   {"errorCode":400012,"errorStatus":"Invalid device format in line: {\"id\":\"evehicle_1\",\"importMode\":\"createOrUpdate\",\"status\":\"Enabled\",\"authentication\":{\"thumbprint\":{\"primaryThumbprint\":\"DE89B7BBD215E7E47ECD372F61205712D71DD521\",\"secondaryThumbprint\":\"DE89B7BBD215E7E47ECD372F61205712D71DD521\"},\"type\":\"SELF_SIGNED\"}}. Error converting value \"SELF_SIGNED\" to type 'System.Nullable`1[Microsoft.Azure.Devices.Cloud.AuthenticationType]'. Path 'authentication.type', line 1, position 249."}
   {"errorCode":400012,"errorStatus":"Invalid device format in line: {\"id\":\"evehicle_2\",\"importMode\":\"createOrUpdate\",\"status\":\"Enabled\",\"authentication\":{\"thumbprint\":{\"primaryThumbprint\":\"DE89B7BBD215E7E47ECD372F61205712D71DD521\",\"secondaryThumbprint\":\"DE89B7BBD215E7E47ECD372F61205712D71DD521\"},\"type\":\"SELF_SIGNED\"}}. Error converting value \"SELF_SIGNED\" to type 'System.Nullable`1[Microsoft.Azure.Devices.Cloud.AuthenticationType]'. Path 'authentication.type', line 1, position 249."}
   {"errorCode":400012,"errorStatus":"Invalid device format in line: {\"id\":\"evehicle_3\",\"importMode\":\"createOrUpdate\",\"status\":\"Enabled\",\"authentication\":{\"thumbprint\":{\"primaryThumbprint\":\"DE89B7BBD215E7E47ECD372F61205712D71DD521\",\"secondaryThumbprint\":\"DE89B7BBD215E7E47ECD372F61205712D71DD521\"},\"type\":\"SELF_SIGNED\"}}. Error converting value \"SELF_SIGNED\" to type 'System.Nullable`1[Microsoft.Azure.Devices.Cloud.AuthenticationType]'. Path 'authentication.type', line 1, position 249."}
   {"errorCode":400012,"errorStatus":"Invalid device format in line: {\"id\":\"evehicle_4\",\"importMode\":\"createOrUpdate\",\"status\":\"Enabled\",\"authentication\":{\"thumbprint\":{\"primaryThumbprint\":\"DE89B7BBD215E7E47ECD372F61205712D71DD521\",\"secondaryThumbprint\":\"DE89B7BBD215E7E47ECD372F61205712D71DD521\"},\"type\":\"SELF_SIGNED\"}}. Error converting value \"SELF_SIGNED\" to type 'System.Nullable`1[Microsoft.Azure.Devices.Cloud.AuthenticationType]'. Path 'authentication.type', line 1, position 249."}
   ```

## How to fix the bug
By changing the `com.microsoft.azure.sdk.iot.service.auth.AuthenticationType.SELF_SIGNED` to `com.microsoft.azure.sdk.iot.service.auth.AuthenticationType.selfSigned` is fixing the problem.

One quick way to test it simply replacing the `"type":"SELF_SIGNED"` json attributes with `"type":"selfSigned"` in the `devices.txt`.

After editing the `devices.txt` you can run again the `main()`as below to try again to register the devices from blob to iothub
```
    public static void main(String[] args) throws Exception {
        //createDevicesInBlob(DEVICE_PREFIX, DEVICE_COUNT, AuthenticationType.SELF_SIGNED);
        //createDevicesInBlob(DEVICE_PREFIX, DEVICE_COUNT, AuthenticationType.SAS);
        registerDevicesFromBlobToIoTHub(IOT_HUB_NAME);
    }
```



## How to run
Run the command `./gradlew run ` in the project root directory

## Note 
The bug is only happening if the authentication type is `SELF_SIGNED`. 