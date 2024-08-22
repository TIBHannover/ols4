package uk.ac.ebi.spot.ols.reststatistics.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class UrlCyclicDecoder {
    public static final int URL_DECODE_TIMES = 3;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public String decode(String url) {
        if (!url.contains("%")) {
            return url;
        }

        int count = 0;
        String decoded = url;
        while (decoded.contains("%") && count < URL_DECODE_TIMES) {
            try {
                decoded = URLDecoder.decode(decoded, StandardCharsets.UTF_8.toString());
            } catch (UnsupportedEncodingException e) {
                logger.error("Could not get query parameters: {}", e.getLocalizedMessage());

                return url;
            }
            count++;
        }

        return decoded;
    }
}
