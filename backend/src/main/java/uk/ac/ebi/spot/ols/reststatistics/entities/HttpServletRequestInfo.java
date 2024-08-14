package uk.ac.ebi.spot.ols.reststatistics.entities;

import java.util.Objects;
import java.util.Set;

public class HttpServletRequestInfo {
    private String url;
    private Set<RestCallParameter> pathVariables;
    private Set<RestCallParameter> queryParameters;
    private Set<RestCallParameter> headers;

    public HttpServletRequestInfo() {
    }

    public HttpServletRequestInfo(String url,
                                  Set<RestCallParameter> pathVariables,
                                  Set<RestCallParameter> queryParameters,
                                  Set<RestCallParameter> headers) {
        this.url = url;
        this.pathVariables = pathVariables;
        this.queryParameters = queryParameters;
        this.headers = headers;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Set<RestCallParameter> getPathVariables() {
        return pathVariables;
    }

    public void setPathVariables(Set<RestCallParameter> pathVariables) {
        this.pathVariables = pathVariables;
    }

    public Set<RestCallParameter> getQueryParameters() {
        return queryParameters;
    }

    public void setQueryParameters(Set<RestCallParameter> queryParameters) {
        this.queryParameters = queryParameters;
    }

    public Set<RestCallParameter> getHeaders() {
        return headers;
    }

    public void setHeaders(Set<RestCallParameter> headers) {
        this.headers = headers;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HttpServletRequestInfo that = (HttpServletRequestInfo) o;
        return url.equals(that.url) && Objects.equals(pathVariables, that.pathVariables) && Objects.equals(queryParameters, that.queryParameters) && Objects.equals(headers, that.headers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, pathVariables, queryParameters, headers);
    }
}
