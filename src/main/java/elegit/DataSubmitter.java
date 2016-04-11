package main.java.elegit;

/**
 * Class for uploading logged data
 */

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;

public class DataSubmitter {
    private static final String submitUrl = "http://elegit.mathcs.carleton.edu/logging"; //for testing, keeping the local one
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
            logger.info("Attempting to upload log: {}",logFile.getName());
            CloseableHttpClient httpclient = HttpClients.createDefault();
            try {
                HttpPost httppost = new HttpPost(submitUrl);

                InputStreamEntity reqEntity = new InputStreamEntity(
                        new FileInputStream(logFile), -1, ContentType.APPLICATION_OCTET_STREAM);
                reqEntity.setChunked(true);

                httppost.setEntity(reqEntity);

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
