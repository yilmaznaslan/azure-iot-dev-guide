package org.example.azure.resources.iotHub.devicemanagement.business;

import com.azure.core.util.Context;
import com.azure.resourcemanager.iothub.IotHubManager;
import com.azure.resourcemanager.iothub.models.ExportDevicesRequest;
import com.azure.resourcemanager.iothub.models.IotHubDescription;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.microsoft.azure.sdk.iot.service.auth.AuthenticationMechanism;
import com.microsoft.azure.sdk.iot.service.exceptions.IotHubException;
import com.microsoft.azure.sdk.iot.service.registry.*;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.*;
import org.example.azure.resources.iotHub.resourceManager.business.IoTHubBA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.EnumSet;
import java.util.Objects;


public class DeviceManagementBA {

    private static Logger LOGGER = LoggerFactory.getLogger(DeviceManagementBA.class);

    public static final String exportFileLocation = "/Users/yilmaznaci.aslan/azure";
    private static final Gson gson = new GsonBuilder().disableHtmlEscaping().create();
    private static String storageConnectionString = "DefaultEndpointsProtocol=https;AccountName=yilmazteststorageaccount;AccountKey=kB0HFufBBDwEZuFYP7B+gVHcfA48n8grtiiQcKtZDsxDElyVPTwvLHOQ+qySWbKnpw8CxwP/ZKc2+AStI/AU9A==;EndpointSuffix=core.windows.net";
    private static String containerName = "devicecontainer";
    private static boolean excludeKeys = false;
    private static String importBlobName = "devices.txt";
    private final IotHubManager iotHubManager;
    private final IoTHubBA ioTHubBA;

    private final String resourceGroupName;
    public DeviceManagementBA(IotHubManager iotHubManager, IoTHubBA ioTHubBA, String resourceGroupName) {
        this.iotHubManager = iotHubManager;
        this.ioTHubBA = ioTHubBA;
        this.resourceGroupName = resourceGroupName;
    }

    public Object iotHubResourceExportDevices(String iotHubName) {
        return iotHubManager
                .iotHubResources()
                .exportDevicesWithResponse(
                        resourceGroupName,
                        iotHubName,
                        new ExportDevicesRequest().withExportBlobContainerUri("testBlob").withExcludeKeys(true),
                        Context.NONE);
    }

    public void addSingleDevice(String iotHubName, String deviceId) {
        RegistryClient registryClient = new RegistryClient(ioTHubBA.getIotHubConnectionString(iotHubName));
        Device device = new Device(deviceId);
        try {
            device = registryClient.addDevice(device);
            System.out.println("Device created: " + device.getDeviceId());
            System.out.println("Device key: " + device.getPrimaryKey());
        } catch (IotHubException | IOException iote) {
            iote.printStackTrace();
        }
    }

    public void exportDevicesToBlobFromIotHub(String iotHubName) throws Exception {
        System.out.println("Starting export sample...");


        CloudStorageAccount storageAccount = CloudStorageAccount.parse(storageConnectionString);
        CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
        CloudBlobContainer container = blobClient.getContainerReference(containerName);
        container.createIfNotExists();
        String containerSasUri = getContainerSasUri(container);

        RegistryClient registryClient = new RegistryClient(ioTHubBA.getIotHubConnectionString(iotHubName));
        RegistryJob exportJob = registryClient.exportDevices(containerSasUri, excludeKeys);

        while (true) {
            exportJob = registryClient.getJob(exportJob.getJobId());
            if (exportJob.getStatus() == RegistryJob.JobStatus.COMPLETED) {
                break;
            }
            Thread.sleep(500);
        }


        String path = "org/example/azure/iotHub/DeviceManager/exportedDevices.json";

        File filePath = new File(Objects.requireNonNull(DeviceManagementBA.class.getClassLoader().getResource(path)).getPath());

        for (ListBlobItem blobItem : container.listBlobs()) {
            if (blobItem instanceof CloudBlob) {
                CloudBlob blob = (CloudBlob) blobItem;
                blob.download(new FileOutputStream(exportFileLocation + blob.getName()));
                blob.downloadToFile(filePath.getPath());
            }
        }


        System.out.println("Export job completed. Results are in " + exportFileLocation);
    }

    public void createAndImportDevicesToIotHub(String iotHubName, String devicePrefix, int deviceCount) throws Exception {
        IotHubDescription iotHubDescription = ioTHubBA.getIotHub(iotHubName);
        if (iotHubDescription != null) {
            importDevicesToBlob(devicePrefix, deviceCount);
            importDevicesFromBlobToIoTHub(iotHubName);

        }
    }

    private void importDevicesToBlob(String devicePrefix, int deviceCount) throws Exception {
        System.out.println("Starting importing devices to blob. ");

        // Creating Azure storage container and getting its URI
        CloudStorageAccount storageAccount = CloudStorageAccount.parse(storageConnectionString);
        CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
        CloudBlobContainer container = blobClient.getContainerReference(containerName);
        container.createIfNotExists();

        // Creating the list of devices to be submitted for import
        StringBuilder devicesToImport = new StringBuilder();
        for (int i = 0; i < deviceCount; i++) {
            String deviceId = devicePrefix + "_" + i;
            Device device = new Device(deviceId);
            AuthenticationMechanism authentication = new AuthenticationMechanism(device.getSymmetricKey());

            ExportImportDevice deviceToAdd = new ExportImportDevice();
            deviceToAdd.setId(deviceId);
            deviceToAdd.setAuthentication(authentication);
            deviceToAdd.setStatus(DeviceStatus.Enabled);
            deviceToAdd.setImportMode(ImportMode.CreateOrUpdate);

            devicesToImport.append(gson.toJson(deviceToAdd));

            if (i < deviceCount - 1) {
                devicesToImport.append("\r\n");
            }
        }

        byte[] blobToImport = devicesToImport.toString().getBytes(StandardCharsets.UTF_8);

        // Creating the Azure storage blob and uploading the serialized string of devices
        System.out.println("Uploading " + blobToImport.length + " bytes into Azure storage.");
        InputStream stream = new ByteArrayInputStream(blobToImport);
        CloudBlockBlob importBlob = container.getBlockBlobReference(importBlobName);
        importBlob.deleteIfExists();
        importBlob.upload(stream, blobToImport.length);

    }

    private void importDevicesFromBlobToIoTHub(String iotHubName) throws Exception {
        LOGGER.info("Importing devices from blob to iothub : {}", iotHubName);
        // Creating Azure storage container and getting its URI
        CloudStorageAccount storageAccount = CloudStorageAccount.parse(storageConnectionString);
        CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
        CloudBlobContainer container = blobClient.getContainerReference(containerName);
        String containerSasUri = getContainerSasUri(container);

        // Starting the import job
        String iotHubConnectionString = ioTHubBA.getIotHubConnectionString(iotHubName);
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

        //Cleaning up the blob
        /*
        for (ListBlobItem blobItem : container.listBlobs()) {
            if (blobItem instanceof CloudBlob) {
                CloudBlob blob = (CloudBlockBlob) blobItem;
                blob.deleteIfExists();
            }
        }
         */
    }

    private String getContainerSasUri(CloudBlobContainer container) throws InvalidKeyException, StorageException {
        // Set the expiry time and permissions for the container.
        // In this case no start time is specified, so the shared access signature becomes valid immediately.
        SharedAccessBlobPolicy sasConstraints = new SharedAccessBlobPolicy();
        Date expirationDate = Date.from(Instant.now().plus(Duration.ofDays(1)));
        sasConstraints.setSharedAccessExpiryTime(expirationDate);
        EnumSet<SharedAccessBlobPermissions> permissions = EnumSet.of(
                SharedAccessBlobPermissions.WRITE,
                SharedAccessBlobPermissions.LIST,
                SharedAccessBlobPermissions.READ,
                SharedAccessBlobPermissions.DELETE);
        sasConstraints.setPermissions(permissions);

        // Generate the shared access signature on the container, setting the constraints directly on the signature.
        String sasContainerToken = container.generateSharedAccessSignature(sasConstraints, null);

        // Return the URI string for the container, including the SAS token.
        return container.getUri() + "?" + sasContainerToken;
    }

}
