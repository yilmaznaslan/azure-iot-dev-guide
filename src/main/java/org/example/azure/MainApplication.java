package org.example.azure;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.microsoft.azure.sdk.iot.service.auth.AuthenticationMechanism;
import com.microsoft.azure.sdk.iot.service.auth.AuthenticationType;
import com.microsoft.azure.sdk.iot.service.registry.*;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;


public class MainApplication {

    public static String IOT_HUB_NAME = "yilmaztestiothub";
    public static String IOT_HUB_CONNECTION_STRING = "HostName=iotbugtesthub.azure-devices.net;SharedAccessKeyName=iothubowner;SharedAccessKey=CDx25gJmYGkrdla1AcwQbw9c5zCWOLeBxXo2os+IKGU=";
    public static String DEVICE_PREFIX = "evehicle";
    public static Integer DEVICE_COUNT = 10;

    public static String CONTAINER_NAME = "devicecontainer";
    public static String BLOB_NAME = "devices.txt";
    public static String STORAGE_CONNECTION_STRING = "DefaultEndpointsProtocol=https;AccountName=storagetestyilmaz;AccountKey=vVAMzuVpSLuy0lvD+MitG4P8bXLpba77/S+jSGD6Ta9eX2CI9g0a3CpR2XERyR3axImnb7GWlHDU+AStaFD28A==;EndpointSuffix=core.windows.net";
    public static String CONTAINER_SAS_URI = "https://storagetestyilmaz.blob.core.windows.net/devicecontainer?sp=r&st=2022-09-19T22:48:40Z&se=2022-09-20T06:48:40Z&spr=https&sv=2021-06-08&sr=c&sig=TV70sbH4pyIH2h1x9x4gk9uQc0cpuQW05iq89E5GVv0%3D";

    private static final Gson gson = new GsonBuilder().disableHtmlEscaping().create();
    private static Logger LOGGER = LoggerFactory.getLogger(MainApplication.class);

    public static void main(String[] args) throws Exception {
        //createDevicesInBlob(DEVICE_PREFIX, DEVICE_COUNT, "SAS");
        importDevicesFromBlobToIoTHub(IOT_HUB_NAME);
    }

    private static void createDevicesInBlob(String devicePrefix, int deviceCount, String authenticationType) throws Exception {
        LOGGER.debug("Starting to create devices in blob. ");


        // Creating the list of devices to be submitted for import
        StringBuilder devicesToImport = new StringBuilder();
        for (int i = 0; i < deviceCount; i++) {
            String deviceId = devicePrefix + "_" + i;
            Device device = new Device(deviceId);

            ExportImportDevice deviceToAdd = new ExportImportDevice();
            deviceToAdd.setId(deviceId);
            if (authenticationType.equals(AuthenticationType.SAS.name())){
                AuthenticationMechanism authentication = new AuthenticationMechanism(device.getSymmetricKey());
                deviceToAdd.setAuthentication(authentication);
            } else if(authenticationType.equals(AuthenticationType.SELF_SIGNED.name())){
                String primaryThumbprint = "DE89B7BBD215E7E47ECD372F61205712D71DD521";
                String secondaryThumbprint = "DE89B7BBD215E7E47ECD372F61205712D71DD521";
                AuthenticationMechanism authentication = new AuthenticationMechanism(primaryThumbprint, secondaryThumbprint);
                deviceToAdd.setAuthentication(authentication);
            }
            deviceToAdd.setStatus(DeviceStatus.Enabled);
            deviceToAdd.setImportMode(ImportMode.CreateOrUpdate);

            devicesToImport.append(gson.toJson(deviceToAdd));

            if (i < deviceCount - 1) {
                devicesToImport.append("\r\n");
            }
        }

        byte[] blobToImport = devicesToImport.toString().getBytes(StandardCharsets.UTF_8);

        // Creating the Azure storage blob and uploading the serialized string of devices
        LOGGER.info("Uploading " + blobToImport.length + " bytes into Azure storage.");
        InputStream stream = new ByteArrayInputStream(blobToImport);
        CloudStorageAccount cloudStorageAccount = CloudStorageAccount.parse(STORAGE_CONNECTION_STRING);
        CloudBlobClient blobClient = cloudStorageAccount.createCloudBlobClient();
        CloudBlobContainer container = blobClient.getContainerReference(CONTAINER_NAME);
        CloudBlockBlob importBlob = container.getBlockBlobReference(BLOB_NAME);
        importBlob.deleteIfExists();
        importBlob.upload(stream, blobToImport.length);
    }

    private static void  importDevicesFromBlobToIoTHub(String iotHubName) throws Exception {
        LOGGER.info("Importing devices from blob to iothub : {}", iotHubName);
        CloudStorageAccount storageAccount = CloudStorageAccount.parse(STORAGE_CONNECTION_STRING);
        CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
        CloudBlobContainer container = blobClient.getContainerReference(CONTAINER_NAME);
        String containerSasUri = CONTAINER_SAS_URI;

        // Starting the import job
        String iotHubConnectionString = IOT_HUB_CONNECTION_STRING;
        RegistryClient registryClient = new RegistryClient(iotHubConnectionString);
        RegistryJob importJob = registryClient.importDevices(containerSasUri, containerSasUri);

        // Waiting for the import job to complete
        while (true) {
            importJob = registryClient.getJob(importJob.getJobId());
            if (importJob.getStatus() == RegistryJob.JobStatus.COMPLETED
                    || importJob.getStatus() == RegistryJob.JobStatus.FAILED) {
                break;
            }
            Thread.sleep(500);
        }

        // Checking the result of the import job
        if (importJob.getStatus() == RegistryJob.JobStatus.COMPLETED) {
            System.out.println("Import job completed. The new devices are now added to the hub.");
        } else {
            System.out.println("Import job failed. Failure reason: " + importJob.getFailureReason());
        }

    }

}
