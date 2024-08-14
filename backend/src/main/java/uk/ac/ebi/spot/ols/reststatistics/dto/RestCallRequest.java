package uk.ac.ebi.spot.ols.reststatistics.dto;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.function.Predicate;

import uk.ac.ebi.spot.ols.reststatistics.entities.RestCallParameter;
import uk.ac.ebi.spot.ols.reststatistics.entities.RestCallParameterType;

public class RestCallRequest {
    private String url;
    private Optional<RestCallParameterType> type;
    private Optional<String> parameterName;

    private LocalDateTime dateTimeFrom;
    private LocalDateTime dateTimeTo;

    public RestCallRequest() {
    }

    public RestCallRequest(String url, LocalDateTime dateTimeFrom, LocalDateTime dateTimeTo) {
        this.url = url;
        this.dateTimeFrom = dateTimeFrom;
        this.dateTimeTo = dateTimeTo;
    }

    public RestCallRequest(String url,
                           Optional<RestCallParameterType> type,
                           Optional<String> parameterName,
                           LocalDateTime dateTimeFrom,
                           LocalDateTime dateTimeTo) {
        this.url = url;
        this.type = type;
        this.parameterName = parameterName;
        this.dateTimeFrom = dateTimeFrom;
        this.dateTimeTo = dateTimeTo;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public LocalDateTime getDateTimeFrom() {
        return dateTimeFrom;
    }

    public void setDateTimeFrom(LocalDateTime dateTimeFrom) {
        this.dateTimeFrom = dateTimeFrom;
    }

    public LocalDateTime getDateTimeTo() {
        return dateTimeTo;
    }

    public void setDateTimeTo(LocalDateTime dateTimeTo) {
        this.dateTimeTo = dateTimeTo;
    }

    public Optional<RestCallParameterType> getType() {
        return type;
    }

    public void setType(Optional<RestCallParameterType> type) {
        this.type = type;
    }

    public Optional<String> getParameterName() {
        return parameterName;
    }

    public void setParameterName(Optional<String> parameterName) {
        this.parameterName = parameterName;
    }

    public Predicate<RestCallParameter> getParameterNamePredicate() {
        return parameterName.isPresent()
            ? parameter -> parameterName.get().equalsIgnoreCase(parameter.getName())
            : parameter -> true;
    }

    public Predicate<RestCallParameter> getParameterTypePredicate() {
        return type.isPresent()
            ? type.get().getRestCallParameterPredicate()
            : parameter -> true;
    }
}
