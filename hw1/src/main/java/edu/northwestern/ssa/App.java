package edu.northwestern.ssa;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;
import org.archive.io.warc.WARCReaderFactory;
import org.archive.io.ArchiveRecord;
import org.archive.io.ArchiveReader;
import org.apache.commons.io.IOUtils;
import org.jsoup.nodes.Document;
import org.jsoup.Jsoup;
import org.json.JSONArray;
import org.json.JSONObject;
import java.time.Duration;
import java.time.Instant;
import java.io.File;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Map;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Scanner;
import java.util.GregorianCalendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Optional;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Future;

//COMP_SCI 310 Scalable Software Architectures HW1
//Jing Jiang

public class App {
    public static void main(String[] args) {
        System.out.println("Hello world!");

        try{
            // Part 1
            String hostname = System.getenv("ELASTIC_SEARCH_HOST");
            String index = System.getenv("ELASTIC_SEARCH_INDEX");

            String bucket = "commoncrawl";

            // String key = "crawl-data/CC-NEWS/2017/02/CC-NEWS-20170202093341-00045.warc.gz";

            S3Client s3 = S3Client.builder()
                    .region(Region.US_EAST_1)
                    .overrideConfiguration(ClientOverrideConfiguration.builder()
                            .apiCallTimeout(Duration.ofMinutes(30)).build())
                    .build();

            // String key = System.getenv("COMMON_CRAWL_FILENAME");
            BufferedReader txtReader = new BufferedReader(new FileReader("latest-warc-name.txt"));
            String nameText = txtReader.readLine();
            System.out.println("latest warc key on record: " + nameText);
            txtReader.close();

            Date date = new Date();
            SimpleDateFormat year = new SimpleDateFormat("yyyy");
            String prefix = "crawl-data/CC-NEWS/" + year.format(date) + '/';
            ListObjectsV2Request listRequest = ListObjectsV2Request.builder().bucket(bucket).prefix(prefix).build();
            ListObjectsV2Iterable listResponse = s3.listObjectsV2Paginator(listRequest);
            Instant latest = null;
            String latestWarc = "";
            for (S3Object content : listResponse.contents()) {
                Instant current = content.lastModified();
                if (latest == null || current.compareTo(latest) > 0) {
                    latestWarc = content.key();
                    latest = current;
                }
            }
            System.out.println("latest warc key available: " + latestWarc);
            if (!latestWarc.equals(nameText)) {
                System.out.println("Found new Warc file.");
                s3.getObject(GetObjectRequest.builder().bucket(bucket).key(latestWarc).build(),
                        ResponseTransformer.toFile(Paths.get("WARC")));
                try {
                    PrintWriter txtWriter = new PrintWriter(new BufferedWriter(new FileWriter("latest-warc-name.txt")));
                    txtWriter.println(latestWarc);
                    txtWriter.flush();
                    txtWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                s3.close();

                // Part 2 - 4
                File warcFile = new File("WARC");
                ArchiveReader reader = WARCReaderFactory.get(warcFile);
                Iterator<ArchiveRecord> it = reader.iterator();
                ArchiveRecord record;
                Map<String, String> map = new HashMap<>();
                ElasticSearch es = new ElasticSearch();

                int count = 0;
                String s;
                byte[] bytes;
                String[] strings;
                es.createIndex(SdkHttpMethod.PUT, hostname, index, Optional.ofNullable(map));
                while (it.hasNext()) {
                    try {
                        record = it.next();
                        String url = record.getHeader().getUrl();
                        bytes = IOUtils.toByteArray(record);
                        s = new String(bytes);
                        strings = s.split("\r\n\r\n");

                        if (strings.length == 2){
                            s = strings[1];
                        }
                        Document doc = Jsoup.parse(s);
                        String text = doc.text();
                        String title = doc.title();

                        if (title.length() == 0) {
                            continue;
                        }
                        JSONObject body = new JSONObject();
                        body.put("url", url);
                        body.put("title", title);
                        body.put("txt", text);

                        es.postDoc(SdkHttpMethod.POST, hostname, index, Optional.ofNullable(body), Optional.ofNullable(map));
                        count++;
                        if (count % 1000 == 0) {
                            System.out.println("current count: " + count);
                        }
                    } catch (Exception e) {
                        System.out.println(e.toString());
                    }
                }
                warcFile.delete();
                es.close();
            } else {
                // If already imported the latest warc file, discard.
                System.out.println("Latest Warc file already imported!");
                s3.close();
            }

        } catch(Exception e) {
            System.out.println(e.toString());
        }

    }
}
