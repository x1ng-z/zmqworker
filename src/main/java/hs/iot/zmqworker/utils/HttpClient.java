package hs.iot.zmqworker.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author zzx
 * @version 1.0
 * @date 2021/10/5 0:12
 */
@Slf4j
public class HttpClient {
    public static <T> T postForEntity(String url, Object myclass, Class<T> returnClass) {
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        HttpPost posturl = new HttpPost(url);
        RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(15 * 1000)
                .setConnectTimeout(10 * 1000)
                .setConnectionRequestTimeout(5 * 1000)
                .build();
        posturl.setConfig(requestConfig);
        String jsonSting = JSON.toJSONString(myclass);
        StringEntity entity = new StringEntity(jsonSting, "UTF-8");
        posturl.setEntity(entity);
        posturl.setHeader("Content-Type", "application/json;charset=utf8");
        // 响应模型
        CloseableHttpResponse response = null;
        try {
            // 由客户端执行(发送)Post请求
            response = httpClient.execute(posturl);
            // 从响应模型中获取响应实体
            HttpEntity responseEntity = response.getEntity();
            if (responseEntity != null) {
                String stringValue = EntityUtils.toString(responseEntity);
                return JSONObject.parseObject(stringValue, returnClass);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            try {
                // 释放资源
                if (httpClient != null) {
                    httpClient.close();
                }
                if (response != null) {
                    response.close();
                }
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
        return null;
    }

    public static String doget(String url, Map<String, String> map) {
        String result = null;
        CloseableHttpClient httpclient = HttpClients.createDefault();
        // 创建参数队列
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        if (!CollectionUtils.isEmpty(map)) {
            for (Map.Entry<String, String> entry : map.entrySet()) {
                String mapKey = entry.getKey();
                String mapValue = entry.getValue();
                params.add(new BasicNameValuePair(mapKey, mapValue));
            }
        }
        CloseableHttpResponse response = null;
        //参数转换为字符串
        try {
            String paramsStr = EntityUtils.toString(new UrlEncodedFormEntity(params, "UTF-8"));
            url = url + "?" + paramsStr;
            // 创建httpget.
            HttpGet httpget = new HttpGet(url);
            // 执行get请求.
            response = httpclient.execute(httpget);

            // 获取响应实体
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                result = EntityUtils.toString(entity);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            // 关闭连接,释放资源
            try {
                response.close();
            } catch (IOException e) {

            }
            try {
                httpclient.close();
            } catch (IOException e) {
            }
        }
        return result;

    }
}
