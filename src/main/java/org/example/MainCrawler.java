package org.example;

public class MainCrawler {


    public static void main(String[] args) throws Exception {

        String seedUrl = "https://gloutir.com";

        int numWords = 100;
        for (int i = 0; i < args.length; i++) {

            if ("-words".equals(args[i])) {
                try {
                    numWords = Integer.parseInt(args[i+1]);
                } catch (NumberFormatException e) {
                    throw new Exception("You must input a number with no strings.");
                } catch (ArrayIndexOutOfBoundsException e) {
                    throw new Exception("You must input a number after the '-words' parameter.");
                }
            }
        }

        Parser parser = new Parser(numWords);
        parser.startParsing(seedUrl);

    }



}