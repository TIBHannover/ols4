package uk.ac.ebi.spot.ols.reststatistics.entity;

import java.util.Objects;

import javax.persistence.Transient;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class RestCallParameter {
    private String name;
    private String value;
    private RestCallParameterType parameterType;

    public RestCallParameter() {
    }

    public RestCallParameter(String name, String value, RestCallParameterType parameterType) {
        this.name = name;
        this.value = value;
        this.parameterType = parameterType;
    }

    public RestCallParameter(String name, String value, RestCallParameterType parameterType, RestCall restCall) {
        this.name = name;
        this.value = value;
        this.parameterType = parameterType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public RestCallParameterType getParameterType() {
        return parameterType;
    }

    public void setParameterType(RestCallParameterType parameterType) {
        this.parameterType = parameterType;
    }

    @Override
    public String toString() {
        return "RestCallParameter{" +
            "parameterType='" + parameterType + '\'' +
            ", name='" + name + '\'' +
            ", value='" + value + '\'' +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RestCallParameter that = (RestCallParameter) o;
        return name.equals(that.name) && value.equals(that.value) && parameterType == that.parameterType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, value, parameterType);
    }

    @Transient
    @JsonIgnore
    public boolean isPathType() {
        return RestCallParameterType.PATH.equals(this.parameterType);
    }

    @Transient
    @JsonIgnore
    public boolean isQueryType() {
        return RestCallParameterType.QUERY.equals(this.parameterType);
    }

    @Transient
    @JsonIgnore
    public boolean isHeaderType() {
        return RestCallParameterType.HEADER.equals(this.parameterType);
    }
}
