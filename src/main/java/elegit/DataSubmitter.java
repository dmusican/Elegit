package main.java.elegit;

/**
 * Created by Eric on 1/13/2016.
 */

import java.io.*;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DataSubmitter {
    private static final String submitUrl = "http://localhost:8080"; //for testing, keeping the local one
    private static final String filepath = "logfile.log";
    private static final Logger logger = LogManager.getLogger();
    public DataSubmitter() {
    }

    public void submitData() {
        logger.info("Starting data submit");
        CloseableHttpClient httpclient = HttpClients.createDefault();
        try {
            HttpPost httppost = new HttpPost(submitUrl);

            File file = new File(filepath);

            InputStreamEntity reqEntity = new InputStreamEntity(
                    new FileInputStream(file), -1, ContentType.APPLICATION_OCTET_STREAM);
            reqEntity.setChunked(true);

            httppost.setEntity(reqEntity);

            logger.info(httppost.getRequestLine());
            CloseableHttpResponse response = httpclient.execute(httppost);
            try {
                logger.info("Executing request: "+response.getStatusLine());
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
        logger.info("File upload was successful");
    }
}
