/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cc.bliss.match3.service.gamemanager.util;

import org.eclipse.jetty.http.HttpMethod;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

/**
 * @author khoatd
 */
public class HttpUtils {

    public static String post(String url, String queries) {
        return request(url, queries, HttpMethod.POST);
    }

    public static String get(String url, String queries) {
        return request(url, queries, HttpMethod.GET);
    }

    /**
     * Call restful get user info
     *
     * @param url
     * @param queries
     * @param method:GET:POST
     * @return
     */
    public static String request(String url, String queries, HttpMethod method) {
        String result = "";
        if (url.contains("?") && !queries.isEmpty()) {
            url = String.format("%s/%s", url, queries);
        } else {
            url = String.format("%s%s", url, queries);
        }
        if (method == HttpMethod.GET) {
            if (url.startsWith("https://") || url.startsWith("http://")) {
                // Send a GET request to the servlet
                try {
                    URL urlcnn = new URL(url);
                    // Send data                                                        
                    URLConnection urlConnection = urlcnn.openConnection();
                    ((HttpURLConnection) urlConnection).setRequestMethod("GET");
                    // Get the response
                    BufferedReader rd = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = rd.readLine()) != null) {
                        sb.append(line);
                    }
                    rd.close();
                    result = sb.toString();
                } catch (Exception ex) {

                }
            }
            return result;
        }
        if (method == HttpMethod.POST) {
            if (url.startsWith("https://") || url.startsWith("http://")) {
                try {
                    URL urlcnn = new URL(url);
                    URLConnection urlConnection;
                    DataOutputStream outStream;
                    DataInputStream inStream;

                    // Create connection                           
                    urlConnection = urlcnn.openConnection();
                    ((HttpURLConnection) urlConnection).setRequestMethod("POST");
                    urlConnection.setDoInput(true);
                    urlConnection.setDoOutput(true);
                    urlConnection.setUseCaches(false);
                    urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                    // Create I/O streams
                    outStream = new DataOutputStream(urlConnection.getOutputStream());
                    inStream = new DataInputStream(urlConnection.getInputStream());

                    // Send request
                    outStream.writeBytes(queries);
                    outStream.flush();
                    outStream.close();

                    // Get Response       
                    String buffer;
                    StringBuilder sb = new StringBuilder();
                    while ((buffer = inStream.readLine()) != null) {
                        sb.append(buffer);
                    }

                    // Close I/O streams
                    inStream.close();
                    outStream.close();
                    result = sb.toString();
                } catch (Exception ex) {

                }
            }
        }
        return result;
    }
}
