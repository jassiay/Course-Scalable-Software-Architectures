package edu.northwestern.ssa.api;

import edu.northwestern.ssa.ElasticSearch;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.io.IOException;

//COMP_SCI 310 Scalable Software Architectures HW2
//Jing Jiang

@Path("/search")
public class Search {
    /** search the elastic search and return results **/
    @GET
    public Response getMsg(@QueryParam("query") String q,
                           @QueryParam("language") String lang,
                           @QueryParam("date") String date,
                           @DefaultValue("10") @QueryParam("count") String count,
                           @QueryParam("offset") String offset)
            throws IOException {

        if (q == null) {
            return Response.status(400).header("Access-Control-Allow-Origin", "*").build();
        }

        ElasticSearch es = new ElasticSearch("es");
        JSONObject searchResponseResults = es.searchRequest(q, lang, date, count, offset);

        return Response.status(200).type("application/json").entity(searchResponseResults.toString(4))
                // below header is for CORS
                .header("Access-Control-Allow-Origin", "*").build();
    }
}
