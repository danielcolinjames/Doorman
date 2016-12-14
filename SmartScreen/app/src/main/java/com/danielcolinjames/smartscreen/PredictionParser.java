package com.danielcolinjames.smartscreen;

import android.os.AsyncTask;
import android.util.Log;
import android.util.Xml;
import android.webkit.WebView;
import android.widget.TextView;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Daniel on 2016-12-12.
 */

public class PredictionParser {

    // Implementation of AsyncTask used to download XML feed from stackoverflow.com.

    // ns = namespace which we aren't using
    // because this page said not to https://developer.android.com/training/basics/network-ops/xml.html#choose
    private static final String ns = null;


    public List parse(InputStream in) throws XmlPullParserException, IOException {
        try {
            XmlPullParser parser = Xml.newPullParser();

//            try {
//                URL url = new URL("http://webservices.nextbus.com/service/publicXMLFeed?command=predictions&a=ttc&stopId=3197");
//                parser.setInput(new InputStreamReader(getUrlData(url));
//            } catch (Throwable t) {
//                //Toast.makeText(t.toString(), Toast.LENGTH_LONG);
//                Log.i("Info", "COULDN'T GET THE URL");
//            }

            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();
            return readFeed(parser);
        } finally {
            in.close();
        }
    }


//    public class RetrieveFeed extends AsyncTask {
//        protected Object doInBackground(Object[] objects) {
//            //URL url = new URL("http://webservices.nextbus.com");
//
//
////        }
////        public InputStream getUrlData(String url) throws URISyntaxException, IOException {
////            DefaultHttpClient client
//            return url;
//        }
//    }

    /*

    Sample XML output

    <body copyright="All data copyright Toronto Transit Commission 2016.">
        <predictions agencyTitle="Toronto Transit Commission" routeTitle="93-Parkview Hills" routeTag="93" stopTitle="Woodbine Ave At Barker Ave South Side" stopTag="9185">
            <direction title="South - South - 93 Parkview Hills towards Woodbine Station">
                <prediction epochTime="1481521876336" seconds="394" minutes="6" isDeparture="false" branch="93" dirTag="93_0_93" vehicle="7750" block="93_1_10" tripTag="33184263"/>
                <prediction epochTime="1481522941011" seconds="1458" minutes="24" isDeparture="false" affectedByLayover="true" branch="93" dirTag="93_0_93" vehicle="7750" block="93_1_10" tripTag="33184264"/>
                <prediction epochTime="1481524005686" seconds="2523" minutes="42" isDeparture="false" affectedByLayover="true" branch="93" dirTag="93_0_93" vehicle="7750" block="93_1_10" tripTag="33184265"/>
            </direction>
        </predictions>
        <predictions agencyTitle="Toronto Transit Commission" routeTitle="91-Woodbine" routeTag="91" stopTitle="Woodbine Ave At Barker Ave South Side" stopTag="9185">
            <direction title="South - South - 91 Woodbine towards Woodbine Station">
                <prediction epochTime="1481522553254" seconds="1070" minutes="17" isDeparture="false" branch="91" dirTag="91_0_91C" vehicle="7765" block="91_1_10" tripTag="33184028"/>
                <prediction epochTime="1481524017249" seconds="2534" minutes="42" isDeparture="false" affectedByLayover="true" branch="91" dirTag="91_0_91C" vehicle="8113" block="91_2_20" tripTag="33184029"/>
            </direction>
        </predictions>
    </body>
     */


    private List readFeed(XmlPullParser parser) throws XmlPullParserException, IOException {
        List predictions = new ArrayList();

        parser.require(XmlPullParser.START_TAG, ns, "body");
        int count = 1;

        while (parser.next() != XmlPullParser.END_DOCUMENT) {

            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            String name = parser.getName();
            Log.d("DEBUG", "RFCOUNT = " + count + ", current tag = " + parser.getName());
            count++;
            Log.i("INFO", "readFeed, looking at this tag: " + name);// + parser.toString());
            // starts by looking for the predictions tag

            if (name.equals("predictions")) {

                Log.i("INFO", "STOP NAME: " + parser.getAttributeValue(ns, "stopTitle"));

                Prediction newPrediction = readPrediction(parser);

                // sometimes the XML document has <predictions> tags with nothing in them
                if (!newPrediction.nextBusTimes.isEmpty()) {
                    predictions.add(newPrediction);
                }
                //

            } else {
//                skip(parser);
                parser.nextTag();
            }
            parser.nextTag();
        }

        Log.i("INFO", "Finished readFeed() while loop");

        for (int i = 0; i < predictions.size(); i++) {
            Log.d("DEBUG", "predictions(" + i + ") = " + predictions.get(i).toString());
        }
        return predictions;
    }

    public static class Prediction {
        public final String routeTag;
        ArrayList<Integer> nextBusTimes;

        private Prediction(String routeTag, ArrayList<Integer> nextBusTimes) {
            this.routeTag = routeTag;
            this.nextBusTimes = nextBusTimes;
        }
    }

    private Prediction readPrediction(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "predictions");

        String routeTag = "";
        ArrayList<Integer> nextBusTimes = new ArrayList<Integer>();

        //Log.i("depth", String.valueOf(parser.getDepth()));

        int count = 1;

        while (parser.next() != XmlPullParser.END_TAG) {
            Log.i("INFO", "RPCount = " + count + ", Current tag being analyzed: " + parser.getName());
            count++;

            if (parser.getEventType() != XmlPullParser.START_TAG) {
                Log.d("DEBUG", "readPrediction: Skipping: " + parser.getName());
                continue;
            }

            String tagName = parser.getName();

            if (tagName.equals("prediction")) {
                routeTag = readRouteTag(parser);
                Log.i("INFO", "branch = " + routeTag);
//            } else if (tagName.equals("prediction")) {
                int seconds = readSeconds(parser);
                Log.i("INFO", "seconds = " + String.valueOf(seconds));
                nextBusTimes.add(seconds);
                //parser.nextTag();
            }
        }

//        for (int i = 0; i < nextBusTimes.size(); i++) {
//            Log.d("DEBUG", "NEXTBUSTIMES(" + i + "): " + nextBusTimes.get(i));
//        }

        return new Prediction(routeTag, nextBusTimes);
    }

    //public static class readStop

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

        //parser.require(XmlPullParser.END_TAG, ns, "prediction");
        return seconds;
    }

    private String readRouteTag(XmlPullParser parser) throws XmlPullParserException, IOException {
        String routeTag = "";

        parser.require(XmlPullParser.START_TAG, ns, "prediction");
        String tagName = parser.getName();

        Log.i("INFO", "readRouteTag current tag = " + tagName);

        if (tagName.equals("prediction")) {
            routeTag = parser.getAttributeValue(null, "branch");
            //parser.nextTag();
        }

        //parser.require(XmlPullParser.END_TAG, ns, "prediction");
        return routeTag;
    }

    private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalArgumentException();
        }

        Log.d("DEBUG", "skip() called on " + parser.getName());

        int depth = 1;
        while (depth != 0) {
            //Log.i("current depth", String.valueOf(depth));

            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }
}