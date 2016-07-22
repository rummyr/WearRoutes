package uk.me.ponies.wearroutes;


import android.location.Location;

import com.google.android.gms.maps.model.LatLng;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Created by rummy on 19/05/2016.
 */
public class GPXFileParser {
        public static List<LatLng> decodeGPX(File file) {
            FileInputStream in = null;
            try {
                in = new FileInputStream(file);
                return decodeGPX(in);

            } catch (FileNotFoundException fnfe) {
                fnfe.printStackTrace();
                return null;
            }
            finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        public static List<LatLng> decodeGPX(InputStream in) {
            List<LatLng> list = new ArrayList<LatLng>();

            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            try {
                DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
                Document document = documentBuilder.parse(in);
                Element elementRoot = document.getDocumentElement();

                NodeList nodelist_trkpt = elementRoot.getElementsByTagName("trkpt");

                for(int i = 0; i < nodelist_trkpt.getLength(); i++){

                    Node node = nodelist_trkpt.item(i);
                    NamedNodeMap attributes = node.getAttributes();

                    String newLatitude = attributes.getNamedItem("lat").getTextContent();
                    Double newLatitude_double = Double.parseDouble(newLatitude);

                    String newLongitude = attributes.getNamedItem("lon").getTextContent();
                    Double newLongitude_double = Double.parseDouble(newLongitude);

                    String newLocationName = newLatitude + ":" + newLongitude;
                    LatLng ll = new LatLng(newLatitude_double,newLongitude_double);

                    list.add(ll);

                }


            } catch (ParserConfigurationException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (SAXException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            return list;
        }


    }
