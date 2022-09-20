package org.example.azure;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.microsoft.azure.sdk.iot.service.auth.AuthenticationMechanism;
import com.microsoft.azure.sdk.iot.service.auth.AuthenticationType;
import com.microsoft.azure.sdk.iot.service.registry.*;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.EnumSet;


public class MainApplication {

    // IoTHub constants
    public static String IOT_HUB_NAME = "REPLACE";
    public static String IOT_HUB_CONNECTION_STRING = "REPLACE";
    public static String DEVICE_PREFIX = "evehicle";
    public static Integer DEVICE_COUNT = 5;

    // Storage account constants
    public static String CONTAINER_NAME = "devicecontainer";
    public static String BLOB_NAME = "devices.txt";
    public static String STORAGE_CONNECTION_STRING = "REPLACE";

    private static final Gson gson = new GsonBuilder().disableHtmlEscaping().create();
    private static Logger LOGGER = LoggerFactory.getLogger(MainApplication.class);

    public static void main(String[] args) throws Exception {
        //createDevicesInBlob(DEVICE_PREFIX, DEVICE_COUNT, AuthenticationType.SELF_SIGNED);
        //createDevicesInBlob(DEVICE_PREFIX, DEVICE_COUNT, AuthenticationType.SAS);
        registerDevicesFromBlobToIoTHub(IOT_HUB_NAME);
    }

    private static void createDevicesInBlob(String devicePrefix, int deviceCount, AuthenticationType authenticationType) throws Exception {
        LOGGER.debug("Starting to create devices in blob. ");


        // Creating the list of devices to be submitted for import
        StringBuilder devicesToImport = new StringBuilder();
        for (int i = 0; i < deviceCount; i++) {
            String deviceId = devicePrefix + "_" + i;
            Device device = new Device(deviceId);

            ExportImportDevice deviceToAdd = new ExportImportDevice();
            deviceToAdd.setId(deviceId);
            if (authenticationType.equals(AuthenticationType.SAS)){
                AuthenticationMechanism authentication = new AuthenticationMechanism(device.getSymmetricKey());
                deviceToAdd.setAuthentication(authentication);
            } else if(authenticationType.equals(AuthenticationType.SELF_SIGNED)){
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

    private static void registerDevicesFromBlobToIoTHub(String iotHubName) throws Exception {
        LOGGER.info("Registering devices from blob to iothub : {}", iotHubName);
        // Creating Azure storage container and getting its URI
        CloudStorageAccount storageAccount = CloudStorageAccount.parse(STORAGE_CONNECTION_STRING);
        CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
        CloudBlobContainer container = blobClient.getContainerReference(CONTAINER_NAME);
        String blobSasUrl = getContainerSasUrl(container);
        LOGGER.info("ContainerSasURI: " +  blobSasUrl);

        // Starting the import job
        String iotHubConnectionString = IOT_HUB_CONNECTION_STRING;
        RegistryClient registryClient = new RegistryClient(iotHubConnectionString);
        RegistryJob importJob = registryClient.importDevices(blobSasUrl, blobSasUrl);

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
            LOGGER.info("Import job completed. The new devices are now added to the hub.");
        } else {
            System.out.println("Import job failed. Failure reason: " + importJob.getFailureReason());
        }

    }

    private static String getContainerSasUrl(CloudBlobContainer container) throws InvalidKeyException, StorageException {
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
