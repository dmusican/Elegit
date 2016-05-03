package main.java.elegit;

/**
 * Class for uploading logged data
 */

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

import java.util.UUID;

public class DataSubmitter {
    private static final String submitUrl = "http://elegit.mathcs.carleton.edu/logging/upload.php"; //for testing, keeping the local one
    private static final Logger logger = LogManager.getLogger();
    public DataSubmitter() {
    }

    public void submitData() {
        logger.info("Submit data called");

        File logFolder = new File("logs/");
        File[] logsToUpload = logFolder.listFiles();

        for (File logFile: logsToUpload) {
            if (!logFile.isFile() || logFile.getName().equals("elegit.log")) {
                if (logsToUpload.length == 1) logger.info("No new logs to upload today");
                break;
            }
            String newUUID = UUID.randomUUID().toString();
            File UUIDfile = new File("logs/"+newUUID+".log");
            logFile.renameTo(UUIDfile);
            logFile=UUIDfile;

            logger.info("Attempting to upload log: {}",logFile.getName());
            CloseableHttpClient httpclient = HttpClients.createDefault();
            try {
                HttpPost httppost = new HttpPost(submitUrl);

                MultipartEntityBuilder builder = MultipartEntityBuilder.create();
                builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
                FileBody fileBody = new FileBody(logFile);

                builder.addPart("fileToUpload",fileBody);
                HttpEntity builtEntity = builder.build();

                httppost.setEntity(builtEntity);

                logger.info(httppost.getRequestLine());
                CloseableHttpResponse response = httpclient.execute(httppost);
                try {
                    logger.info("Executing request: " + response.getStatusLine());
                    logger.info(EntityUtils.toString(response.getEntity()));
                } catch (Exception e) {
                    logger.error("Response status check failed.");
                    response.close();
                    return;
                }
            } catch (Exception e) {
                logger.error("Failed to execute request. Attempting to close client.");
                try {
                    httpclient.close();
                } catch (Exception f) {
                    logger.error("Failed to close client.");
                    return;
                }
                return;
            }
            if (logFile.delete()) {
                logger.info("Succesfully deleted {}", logFile.getName());
            }
            //TODO: deal with the possibility of files not being correctly deleted
            logger.info("File upload was successful");
        }

    }
}