package uk.ac.ebi.spot.ols.reststatistics.dto;

import java.util.List;

public class RestCallCountResultDto {
    List<KeyValueResultDto> result;

    public RestCallCountResultDto() {
    }

    public RestCallCountResultDto(List<KeyValueResultDto> result) {
        this.result = result;
    }

    public List<KeyValueResultDto> getResult() {
        return result;
    }

    public void setResult(List<KeyValueResultDto> result) {
        this.result = result;
    }
}
