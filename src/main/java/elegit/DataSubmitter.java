package elegit;

/*
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

public class DataSubmitter {
    private static final String submitUrl = "http://elegit.mathcs.carleton.edu/logging/upload.php";
    private static final Logger logger = LogManager.getLogger();
    private static final String LOG_FILE_NAME = "elegit.log";
    public DataSubmitter() {
    }

    public String submitData(String uuid) {
        logger.info("Submit data called");
        String logPath = Paths.get("logs").toString();

        File logDirectory = new File(logPath);
        File[] logsToUpload=logDirectory.listFiles();

        String lastUUID="";
        if (uuid==null || uuid.equals("")) {
            uuid=UUID.randomUUID().toString();
            logger.info("Making a new uuid.");
        }

        for (File logFile: logsToUpload) {
            if (!logFile.isFile() || logFile.getName().equals(LOG_FILE_NAME)) {
                if (logsToUpload.length == 1) {
                    logger.info("No new logs to upload today");
                    break;
                }
            }

            // Move the file to a uuid filename for upload to the server.
            try {
                logFile = Files.copy(logFile.toPath(), logFile.toPath().resolveSibling(uuid+".log")).toFile();
            } catch (IOException e) {
                e.printStackTrace();
            }

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
                    lastUUID=uuid;
                } catch (Exception e) {
                    logger.error("Response status check failed.");
                    response.close();
                    return null;
                }
            } catch (Exception e) {
                logger.error("Failed to execute request. Attempting to close client.");
                try {
                    httpclient.close();
                } catch (Exception f) {
                    logger.error("Failed to close client.");
                    return null;
                }
                return null;
            }
            // Delete the log file as we might be uploading more!
            logFile.delete();

        }
        // Clean up the directory
        for (File file: logDirectory.listFiles()) {
            if (!file.getName().equals(LOG_FILE_NAME))
                file.delete();
        }

        return lastUUID;
    }
}
