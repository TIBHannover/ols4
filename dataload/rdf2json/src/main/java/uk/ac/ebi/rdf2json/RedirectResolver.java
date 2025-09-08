package uk.ac.ebi.rdf2json;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 *@author Deepan Anbalagan
 *@email deepan.anbalagan@tib.eu
 *TIB-Leibniz Information Center for Science and Technology
*/
public class RedirectResolver {
	
	private static final Map<String, String> redirectMap = new HashMap<>();
    
	//This will be removed after the solution is finalized
    /*static {
        // Add known redirects here
        redirectMap.put("http://www.opengis.net/ont/sf",
                        "https://opengeospatial.github.io/ogc-geosparql/geosparql11/sf_geometries.ttl");
        redirectMap.put("http://www.opengis.net/ont/geosparql",
                "https://opengeospatial.github.io/ogc-geosparql/geosparql11/geo.ttl");
    }*/
    
   static {
        Properties properties = new Properties();

        try (InputStream inputStream = RedirectResolver.class.getClassLoader()
                                                   .getResourceAsStream("redirect.properties")) {
            if (inputStream == null) {
                throw new RuntimeException("config.properties not found in classpath");
            }

            properties.load(inputStream);

            for (String key : properties.stringPropertyNames()) {
            	redirectMap.put(key, properties.getProperty(key));
            }

            System.out.println("Properties loaded: " + redirectMap);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String resolve(String iri) {
        return redirectMap.getOrDefault(iri, iri);
    }

}
