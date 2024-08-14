package uk.ac.ebi.spot.ols.reststatistics.entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Document(collection = "rest_call")
public class RestCall {
    @Id
    private String id;

    private String url;

    private Set<RestCallParameter> parameters = new HashSet<>();

    private LocalDateTime createdAt;

    public RestCall() {
    }

    public RestCall(String url) {
        this.url = url;
        this.createdAt = LocalDateTime.now();
    }

    public RestCall(String url,
                    Set<RestCallParameter> parameters) {
        this.url = url;
        this.parameters = parameters;
        this.createdAt = LocalDateTime.now();
    }

    public void addParameters(Set<RestCallParameter> set) {
        parameters.addAll(set);
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Set<RestCallParameter> getParameters() {
        return parameters;
    }

    public void setParameters(Set<RestCallParameter> parameters) {
        this.parameters = parameters;
    }

    @Override
    public String toString() {
        return "RestCall{" +
            "id=" + id +
            ", url='" + url + '\'' +
            ", parameters=" + parameters +
            ", createdAt=" + createdAt +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RestCall restCall = (RestCall) o;
        return id.equals(restCall.id) && url.equals(restCall.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, url);
    }
}
