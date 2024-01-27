package org.example;

import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class Data {

    final double startTime;
    int urlsCrawled;
    int urlsToCrawl;
    int urlsAdded;
    int totalNumKeywords;
    int curNumKeywords;
    double minuteCounter;
    double urlsForMin;
    List<Point> points = new LinkedList<>();
    List<Double> pagesPerMin = new LinkedList<>();

    public Data() {
        startTime = millisToSecs(System.currentTimeMillis());
        minuteCounter = startTime;
        urlsCrawled = 0;
        urlsToCrawl = 1;
        totalNumKeywords = 0;
        curNumKeywords = 0;
        urlsForMin = 0;
    }

    public void collectData() {
        Point curPoint = new Point();
        curPoint.curTime = millisToSecs(System.currentTimeMillis()) - startTime;
        urlsCrawled++;
        urlsForMin++;
        curPoint.totalUrlsCrawled = urlsCrawled;
        curPoint.numAddedUrls = urlsAdded;
        urlsToCrawl += urlsAdded - 1;
        urlsAdded = 0;
        curPoint.totalUrlsToCrawl = urlsToCrawl;
        curPoint.numAddedKeywords = curNumKeywords;
        totalNumKeywords += curNumKeywords;
        curNumKeywords = 0;
        curPoint.totalKeywordsExtracted = totalNumKeywords;
        curPoint.crawlToBeCrawled = (double) urlsCrawled / (double) urlsToCrawl;
        double timePassed = millisToSecs(System.currentTimeMillis()) - minuteCounter;
        if (timePassed >= 60) {
            getPagesPerMin(timePassed);
        }
        points.add(curPoint);

    }

    public void getPagesPerMin(double timePassed) {
        double minRatio = timePassed / 60.0;
        double newPoint = urlsForMin / minRatio;
        pagesPerMin.add(newPoint);

        minuteCounter = millisToSecs(System.currentTimeMillis());
        urlsForMin = 0;
    }

    public void sendNumKeywords(int numKeywords) {
        curNumKeywords = numKeywords;
    }

    public void sendNumUrlsAdded(int numUrls) {
        urlsAdded = numUrls;
    }

    private static double millisToSecs(double millis) {
        return millis / 1000;
    }

    public void writeToFiles() throws Exception {

        try {
            FileWriter timeFile = new FileWriter("timestamp.txt");
            FileWriter totalUrlsCrawled = new FileWriter("totalUrlsCrawled.txt");
            FileWriter urlsAddedToSearch = new FileWriter("urlsAddedToSearch.txt");
            FileWriter totalUrlsToSearch = new FileWriter("totalUrlsToSearch.txt");
            FileWriter numAddedKeywords = new FileWriter("numAddedKeywords.txt");
            FileWriter totalKeywordsExtracted = new FileWriter("totalKeywordsExtracted.txt");
            FileWriter crawlToBeCrawled = new FileWriter("crawlToBeCrawled.txt");

            for (Point point : points) {
                timeFile.append(Double.toString(point.curTime)).append("\n");
                totalUrlsCrawled.append(Integer.toString(point.totalUrlsCrawled)).append("\n");
                urlsAddedToSearch.append(Integer.toString(point.numAddedUrls)).append("\n");
                totalUrlsToSearch.append(Integer.toString(point.totalUrlsToCrawl)).append("\n");
                numAddedKeywords.append(Integer.toString(point.numAddedKeywords)).append("\n");
                totalKeywordsExtracted.append(Integer.toString(point.totalKeywordsExtracted)).append("\n");
                crawlToBeCrawled.append(Double.toString(point.crawlToBeCrawled)).append("\n");
            }

            FileWriter pagesPerMinFile = new FileWriter("pagesPerMin.txt");

            for (double point : pagesPerMin) {
                pagesPerMinFile.append(Double.toString(point)).append("\n");
            }

            timeFile.close();
            totalUrlsCrawled.close();
            urlsAddedToSearch.close();
            totalUrlsToSearch.close();
            numAddedKeywords.close();
            totalKeywordsExtracted.close();
            crawlToBeCrawled.close();
            pagesPerMinFile.close();

        } catch (IOException e) {
            throw new Exception("Something went wrong writing to files.");
        }
    }

    private static class Point {
        double curTime;
        int totalUrlsCrawled;
        int numAddedUrls;
        int totalUrlsToCrawl;
        int numAddedKeywords;
        int totalKeywordsExtracted;
        double crawlToBeCrawled;

    }

}
