package edu.northwestern.ssa;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpMethod;

public class ElasticSearch extends AwsSignedRestRequest {
    ElasticSearch() {
        super("es");
    }

    public void createIndex(SdkHttpMethod method, String host, String path, Optional<Map<String, String>> params) {
        try {
            restRequest(method, host, path, params);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void postDoc(SdkHttpMethod method, String host, String path, Optional<JSONObject> body, Optional<Map<String, String>> params) {
        path += "/_doc";
        try {
            HttpExecuteResponse response = restRequest(method, host, path, params, body);
            while (!response.httpResponse().isSuccessful()) {
                response.responseBody().get().close();
                response = restRequest(method, host, path, params, body);
            }
            response.responseBody().get().close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}