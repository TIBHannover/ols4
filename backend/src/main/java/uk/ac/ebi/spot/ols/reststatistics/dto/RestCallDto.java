package uk.ac.ebi.spot.ols.reststatistics.dto;

import java.time.LocalDateTime;
import java.util.Set;

import uk.ac.ebi.spot.ols.reststatistics.entity.RestCall;
import uk.ac.ebi.spot.ols.reststatistics.entity.RestCallParameter;

public class RestCallDto {
    private String id;
    private String url;
    private Set<RestCallParameter> parameters;
    private LocalDateTime createdAt;

    public RestCallDto() {
    }

    public RestCallDto(String id,
                       String url,
                       Set<RestCallParameter> parameters,
                       LocalDateTime createdAt) {
        this.id = id;
        this.url = url;
        this.parameters = parameters;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Set<RestCallParameter> getParameters() {
        return parameters;
    }

    public void setParameters(Set<RestCallParameter> parameters) {
        this.parameters = parameters;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public static RestCallDto of(RestCall restCall) {
        return new RestCallDto(
            restCall.getId(),
            restCall.getUrl(),
            restCall.getParameters(),
            restCall.getCreatedAt()
        );
    }
}
