package com.fire.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

/**
 * @author zhuhuogeng
 * @date 16/7/24.
 */
@Slf4j
public class SendRequestUtil {

    private static final String ENCODING = "UTF-8";
    private static final int MAX_ERROR_COUNT = 3;

    /**
     * 发送get请求,返回String结果
     */
    public static String sendGetRequest(HttpClient httpClient, String url) {
        return sendGetRequest(httpClient, url, null, null);
    }

    /**
     * 发送get请求,返回String结果,包含cookie和referer
     */
    private static String sendGetRequest(HttpClient httpClient, String url, String cookie, String referer) {
        String result = "";
        int errorCount = 0;
        while (errorCount < MAX_ERROR_COUNT) {
            try {
                HttpGet httpGet =new HttpGet(url);
                if (StringUtils.isNotBlank(cookie)) {
                    httpGet.addHeader("Cookie", cookie);
                }
                if (StringUtils.isNotBlank(referer)) {
                    httpGet.addHeader("Referer", referer);
                }
                HttpResponse httpResponse = httpClient.execute(httpGet);
                HttpEntity entity = httpResponse.getEntity();
                result = EntityUtils.toString(entity, ENCODING);
                EntityUtils.consume(entity);
                httpGet.releaseConnection();
                break;
            } catch (Exception e) {
                log.error("get请求异常:", e);
                errorCount++;
                continue;
            }
        }
        return result;
    }

    public static String sendPostRequest(HttpClient httpClient, String url, Map<String, String> params) {
        return sendPostRequest(httpClient, url, params, null, null);
    }

    public static String sendPostRequest(HttpClient httpClient, String url, Map<String, String> params, String cookie, String referer) {
        String result = "";
        int errorCount = 0;
        while (errorCount < MAX_ERROR_COUNT) {
            try {
                HttpPost httpPost = new HttpPost(url);
                if (StringUtils.isNotBlank(cookie)) {
                    httpPost.addHeader("Cookie", cookie);
                }
                if (StringUtils.isNotBlank(referer)) {
                    httpPost.addHeader("Referer", referer);
                }
                List<NameValuePair> nameValuePairList = params.keySet().stream().map(param -> new BasicNameValuePair(param, params.get(param))).collect(toList());
                httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairList, ENCODING));
                HttpResponse httpResponse = httpClient.execute(httpPost);
                int status = httpResponse.getStatusLine().getStatusCode();
                HttpEntity entity = httpResponse.getEntity();
                result = EntityUtils.toString(entity, ENCODING);
                EntityUtils.consume(entity);
                httpPost.releaseConnection();
                if (status == 404 || status == 500) {
                    throw new Exception(String.valueOf(status));
                }
                if (status == 301 || status == 302) {
                    String forwardUrl = httpResponse.getFirstHeader("Location").getValue();
                    result = sendGetRequest(httpClient, forwardUrl);
                }
                break;
            } catch (Exception e) {
                log.error("post请求异常:", e);
                errorCount++;
                continue;
            }
        }
        return result;
    }

}
