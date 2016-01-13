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

public class DataSubmitter {
    private static final String submitUrl = "http://localhost:8080"; //for testing, keeping the local one
    private static final String filepath = "filename.log";
    public DataSubmitter() {

    }

    public void submitData() {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        try {
            HttpPost httppost = new HttpPost(submitUrl);

            File file = new File(filepath);

            InputStreamEntity reqEntity = new InputStreamEntity(
                    new FileInputStream(file), -1, ContentType.APPLICATION_OCTET_STREAM);
            reqEntity.setChunked(true);

            httppost.setEntity(reqEntity);

            System.out.println("Executing request: " + httppost.getRequestLine());
            CloseableHttpResponse response = httpclient.execute(httppost);
            try {
                System.out.println("----------------------------------------");
                System.out.println(response.getStatusLine());
                System.out.println(EntityUtils.toString(response.getEntity()));
            } catch (Exception e) {
                response.close();
            }
        } catch (Exception e) {
            try {
                httpclient.close();
            } catch (Exception f) {
            }
        }
    }
}
