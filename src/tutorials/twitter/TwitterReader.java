package tutorials.twitter;

import twitter4j.Status;
import twitter4j.TwitterException;
import twitter4j.TwitterObjectFactory;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class TwitterReader {

    public static List<Status> ReadFile(String fileName) throws TwitterException, IOException {
        FileInputStream inputStream = null;
        Scanner sc = null;
        List<Status> tweets = new ArrayList<>();

        try {
            inputStream = new FileInputStream(fileName);
            sc = new Scanner(inputStream, "UTF-8");
            while (sc.hasNextLine()) {
                String line = sc.nextLine();
                tweets.add((Status)TwitterObjectFactory.createObject(line));
            }
            if (sc.ioException() != null) {
                throw sc.ioException();
            }
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
            if (sc != null) {
                sc.close();
            }
        }

        return tweets;
    }


}
