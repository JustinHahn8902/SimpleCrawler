package org.example;

import io.github.crew102.rapidrake.RakeAlgorithm;
import io.github.crew102.rapidrake.data.SmartWords;
import io.github.crew102.rapidrake.model.RakeParams;
import io.github.crew102.rapidrake.model.Result;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.codecs.simpletext.SimpleTextCodec;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.PriorityQueue;

public class Parser {

    MongoDBMS dbms;
    RakeAlgorithm rakeAlg;
    int maxWords;
    IndexWriter writer;
    Data data;

    public Parser(int numWords) throws IOException {
        data = new Data();
        dbms = new MongoDBMS();
        rakeAlg = initRaker();
        maxWords = numWords;

        String path = "index-directory";
        IndexWriterConfig config = new IndexWriterConfig(new StandardAnalyzer());
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        config.setCodec(new SimpleTextCodec());
        writer = new IndexWriter(FSDirectory.open(new File(path).toPath()), config);
    }

    public void startParsing(String seedUrl) throws Exception {

        int urlsParsed = 0;
        String curUrl = seedUrl;
        while (urlsParsed < 1050) {
            System.out.println("Currently parsing URL: " + curUrl);
            Document doc = getDocument(curUrl);
            if (doc != null) {
                System.out.println("Added to writer");
                writer.addDocument(doc);
                data.collectData();
                urlsParsed++;
            }
            curUrl = dbms.getNextURL();
        }
        writer.close();
        data.writeToFiles();
    }

    public Document getDocument(String url) throws Exception {
        Document doc = new Document();
        org.jsoup.nodes.Document jDoc;

        try {
            jDoc = Jsoup.connect(url).get();

            doc.add(new Field("path", url, setFieldType(true, false, IndexOptions.NONE, true)));

            String title = jDoc.title();
            doc.add(new Field("title", title, setFieldType(true, true, IndexOptions.DOCS, false)));

            doc.add(new Field("hashedUrl", Long.toString(MongoDBMS.hashed(MongoDBMS.URLFormatter(url))), setFieldType(true, false, IndexOptions.DOCS, true)));

            doc.add(new Field("body", getKeywords(jDoc), setFieldType(true, true, IndexOptions.DOCS, false)));

            data.sendNumUrlsAdded(dbms.addURLs_DB(jDoc.select("a[href]"), url));
            dbms.addURLSearched(url);

            return doc;
        } catch (Exception e) {
            System.out.println("Skipping URL: " + url);
            return null;
        }

    }

    private RakeAlgorithm initRaker() throws IOException {

        String[] stopWords = new SmartWords().getSmartWords();
        String[] stopPOS = {"VB", "VBD", "VBG", "VBN", "VBP", "VBZ"};
        int wordMinChar = 2;
        boolean stem = true;
        String phraseDelims = "[-,.?():;\"!/]";

        RakeParams rakeParams = new RakeParams(stopWords, stopPOS, wordMinChar, stem, phraseDelims);
        String taggerModelUrl = "model-bin/en-pos-maxent.bin";
        String sentDectModelUrl = "model-bin/en-sent.bin";
        return new RakeAlgorithm(rakeParams, taggerModelUrl, sentDectModelUrl);
    }

    private String getKeywords(org.jsoup.nodes.Document jDoc) {

        Result result = rakeAlg.rake(jDoc.body().text());
        PriorityQueue<WordFreq> freqWords = new PriorityQueue<>(Collections.reverseOrder());
        String[] keyws = result.getFullKeywords();
        float[] vals = result.getScores();

        for (int i = 0; i < result.getFullKeywords().length; i++) {
            if (keyws[i].length() < 75) {
                freqWords.add(new WordFreq(keyws[i], vals[i]));
            }
        }

        int numKeyws = 0;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < maxWords; i++) {
            sb.append(freqWords.remove().word);
            sb.append(" ");
            numKeyws++;
            if (freqWords.size() == 0) {
                break;
            }
        }
        data.sendNumKeywords(numKeyws);

        return sb.toString();

    }

    private static class WordFreq implements Comparable<WordFreq> {
        String word;
        float freq;

        public WordFreq(String word, float freq) {
            this.word = word;
            this.freq = freq;
        }

        @Override
        public int compareTo(@NotNull Parser.WordFreq o) {
            int val = 0;
            if (this.freq > o.freq) {
                val = 1;
            } else if (this.freq < o.freq) {
                val = -1;
            }
            return val;
        }

        @Override
        public String toString() {
            return this.word + " " + this.freq + ",";
        }
    }

    private FieldType setFieldType(boolean stored, boolean tokenized, IndexOptions indexed, boolean frozen) {
        FieldType newType = new FieldType();
        newType.setTokenized(tokenized);
        newType.setIndexOptions(indexed);
        newType.setStored(stored);
        if (frozen) { newType.freeze(); }
        return newType;
    }

}
