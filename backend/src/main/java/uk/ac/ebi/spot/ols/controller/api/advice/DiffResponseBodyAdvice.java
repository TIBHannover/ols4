package uk.ac.ebi.spot.ols.controller.api.advice;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@ControllerAdvice
public class DiffResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean supports(MethodParameter returnType, Class converterType) {
        return returnType.getContainingClass().getName().endsWith("Controller");
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType,
                                  MediaType selectedContentType, Class selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {

        String ifNoneMatch = request.getHeaders().getFirst(HttpHeaders.IF_NONE_MATCH);
        String currentETag = generateETag(body);
        String cleanIfNoneMatch = (ifNoneMatch != null) ? ifNoneMatch.replace("\"", "").trim() : null;
        String cleanCurrentETag = currentETag.replace("\"", "").trim();

        if (cleanIfNoneMatch != null && cleanIfNoneMatch.equals(cleanCurrentETag)) {
            //response.getHeaders().clear();
            //response.setStatusCode(HttpStatus.NOT_MODIFIED);
            response.getHeaders().setETag(currentETag);
            response.getHeaders().add("X-Data-Status", "UNCHANGED");
            return body;
            //return ResponseEntity.noContent().build();

        } else if (cleanIfNoneMatch != null) {
            //response.getHeaders().clear();
            response.getHeaders().setETag(currentETag);
            response.getHeaders().add("X-Data-Status", "CHANGED");
            return body;
        }

        response.getHeaders().setETag(currentETag);
        response.getHeaders().add("X-Data-Status", "INITIAL");
        return body;
    }

    private String generateETag(Object body) {
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(body);
            return "\"" + DigestUtils.md5DigestAsHex(bytes) + "\"";
        } catch (Exception e) {
            return "\"" + Integer.toHexString(body.hashCode()) + "\"";
        }
    }
}
