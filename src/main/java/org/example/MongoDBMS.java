package org.example;

import com.mongodb.ServerApi;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoException;
import com.mongodb.ConnectionString;
import com.mongodb.ServerApiVersion;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoClients;
import org.bson.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import static com.mongodb.client.model.Filters.eq;

public class MongoDBMS {

    private static final String dbConnStr = "mongodb+srv://";
    private MongoClient mongoClient;

    public MongoDBMS() {

        ServerApi serverApi = ServerApi.builder().version(ServerApiVersion.V1).build();
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(dbConnStr))
                .serverApi(serverApi).build();

        try {
            mongoClient = MongoClients.create(settings);
        } catch (MongoException e) {
            System.out.println("Failed to connect to MongoDB.");
            throw e;
        }
    }

    public int addURLs_DB(Elements urls, String curURL) throws Exception {

        MongoDatabase urldb = mongoClient.getDatabase("URL_DB");
        MongoCollection<Document> searchList = urldb.getCollection("search");
        int numUrlsAdded = 0;

        try {
            for (Element link : urls) {
                String url = link.attr("abs:href");
                url = URLFormatter(url);
                if (!url.startsWith("https://")
                 || url.equals(URLFormatter(curURL))) {
                    continue;
                }

                if (searchList.find(eq("_id", hashed(url))).first() == null) {
                    searchList.insertOne(new Document().append("_id", hashed(url)).append("url", url));
                    numUrlsAdded++;
                }
            }
            return numUrlsAdded;
        } catch (MongoException e) {
            throw new Exception("Failed to insert inlist of urls");
        }
    }

    public void addURLSearched(String url) throws Exception {

        MongoDatabase urldb = mongoClient.getDatabase("URL_DB");
        MongoCollection<Document> searchList = urldb.getCollection("search");
        MongoCollection<Document> searchedList = urldb.getCollection("searched");

        try {
            url = URLFormatter(url);
            if (searchedList.find(eq("_id", hashed(url))).first() != null) {
                throw new Exception("Somehow the just searched URL is already in the searched list");
            }
            searchedList.insertOne(new Document().append("_id", hashed(url)).append("url", url));

        } catch (MongoException e) {
            throw new Exception("Something went wrong with inserting the searched URL into the searched collection");
        }

    }

    public String getNextURL() throws Exception {

        MongoDatabase urldb = mongoClient.getDatabase("URL_DB");
        MongoCollection<Document> searchList = urldb.getCollection("search");

        try {
            Document next = searchList.find().first();
            searchList.findOneAndDelete(next);
            return (String) next.get("url");
        } catch (MongoException e) {
            throw new Exception("Error somewhere in getting the next url of search list.");
        }
    }

    public static String URLFormatter(String url) {
        if (url.endsWith("#")) {
            url = url.substring(0, url.length() - 1);
        }
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

    /**
     * Hashes the url using djb2 hash algorithm.
     * @param url to be hashed
     * @return long hashed url
     */
    public static long hashed(String url) {
        long hash = 5381;
        for (int i = 0; i < url.length(); i++) {
            hash = ((hash << 5) + hash) + url.charAt(i);
        }
        return hash;
    }
}
