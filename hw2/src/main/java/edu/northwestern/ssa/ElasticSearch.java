package edu.northwestern.ssa;

import org.json.JSONArray;
import org.json.JSONObject;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.utils.IoUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ElasticSearch extends AwsSignedRestRequest{
    public ElasticSearch (String serviceName) {
        super(serviceName);
    }

    private static String getParam(String paramName) {
        String prop = System.getProperty(paramName);
        return (prop != null)? prop : System.getenv(paramName);
    }

    public JSONObject searchRequest (String query, String lang, String date, String count, String offset) throws IOException {
        String host = getParam("ELASTIC_SEARCH_HOST");
        String path = getParam("ELASTIC_SEARCH_INDEX") + "/_search";

        Map<String, String> map = new HashMap<>();
        StringBuilder sb = new StringBuilder();
        sb.append("txt:(" + query.replace(" ", " AND ") + ")");
        if (lang != null){
            sb.append(" AND ");
            sb.append("lang:").append(lang);
        }
        if (date != null){
            sb.append(" AND ");
            sb.append("date:").append(date);
        }
//        System.out.println(sb.toString());
        map.put("q", sb.toString());

        if (count != null){
            map.put("size", count);
        }
        if (offset != null){
            map.put("from", offset);
        }
        map.put("track_total_hits", "true");

        JSONObject resultJSON = new JSONObject();
        HttpExecuteResponse response = restRequest(SdkHttpMethod.GET, host, path, Optional.of(map));
        if (response.httpResponse().isSuccessful()) {
            AbortableInputStream inputStream = response.responseBody().get();
            JSONObject obj = new JSONObject(IoUtils.toUtf8String(inputStream));

            resultJSON.put("total_results", obj.getJSONObject("hits").getJSONObject("total").getInt("value"));

            JSONArray hitsArray = obj.getJSONObject("hits").getJSONArray("hits");
            resultJSON.put("returned_results", hitsArray.length());
            JSONArray articles = new JSONArray();
            for (int i = 0; i < hitsArray.length(); i++) {
                articles.put(hitsArray.getJSONObject(i).getJSONObject("_source"));
            }
            resultJSON.put("articles", articles);
        }
        close();

        return resultJSON;
    }

}