package com.danielcolinjames.smartscreen;

import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Daniel on 2016-12-15.
 */

public class ParsePredictions {
    // ns = namespace which we aren't using
    // because this page said not to https://developer.android.com/training/basics/network-ops/xml.html#choose
    private static final String ns = null;

    // Prediction class
    public static class Prediction {
        public final String routeTag;
        ArrayList<Integer> nextBusTimes;

        private Prediction(String routeTag, ArrayList<Integer> nextBusTimes) {
            this.routeTag = routeTag;
            this.nextBusTimes = nextBusTimes;
        }
    }


    public List parse(InputStream in) throws XmlPullParserException, IOException {
        try {
            XmlPullParser parser = Xml.newPullParser();

            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();
            return readFeed(parser);
        } finally {
            in.close();
        }
    }


    private List readFeed(XmlPullParser parser) throws XmlPullParserException, IOException {
        List predictions = new ArrayList();

        parser.require(XmlPullParser.START_TAG, ns, "body");
        while (parser.next() != XmlPullParser.END_DOCUMENT) {

            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            String name = parser.getName();

            if (name.equals("predictions")) {
                Prediction newPrediction = readPrediction(parser);

                // sometimes the XML document has <predictions> tags with nothing in them
                if (!newPrediction.nextBusTimes.isEmpty()) {
                    predictions.add(newPrediction);
                }
            } else {
                parser.nextTag(); //skip(parser);
            }
            parser.nextTag();
        }
        return predictions;
    }



    private Prediction readPrediction(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "predictions");

        String routeTag = "";
        ArrayList<Integer> nextBusTimes = new ArrayList<Integer>();

        while (parser.next() != XmlPullParser.END_TAG) {

            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            String tagName = parser.getName();

            if (tagName.equals("prediction")) {
                routeTag = readRouteTag(parser);
                int seconds = readSeconds(parser);
                nextBusTimes.add(seconds);
                //parser.nextTag();
            }
        }
        return new Prediction(routeTag, nextBusTimes);
    }



    private int readSeconds(XmlPullParser parser) throws XmlPullParserException, IOException {
        // set seconds to -1 in case it can't parse it for some reason
        int seconds = -1;

        parser.require(XmlPullParser.START_TAG, ns, "prediction");
        String tagName = parser.getName();

        if (tagName.equals("prediction")) {
            try {
                seconds = Integer.parseInt(parser.getAttributeValue(null, "seconds"));
            } catch (Exception e) {
                Log.e("Error", "Couldn't parse the \"seconds\" attribute from the XML feed.");
                Log.i("Error message given:", e.getMessage());
            }
            parser.nextTag();
        }

        return seconds;
    }

    private String readRouteTag(XmlPullParser parser) throws XmlPullParserException, IOException {
        String routeTag = "";

        parser.require(XmlPullParser.START_TAG, ns, "prediction");
        String tagName = parser.getName();

        if (tagName.equals("prediction")) {
            routeTag = parser.getAttributeValue(null, "branch");
        }
        return routeTag;
    }
}
