package uk.ac.ebi.spot.ols.reststatistics.controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import uk.ac.ebi.spot.ols.reststatistics.entity.RestCall;
import uk.ac.ebi.spot.ols.reststatistics.service.RestCallService;
import uk.ac.ebi.spot.ols.reststatistics.service.RestCallStatisticsService;
import uk.ac.ebi.spot.ols.reststatistics.dto.KeyValueResultDto;
import uk.ac.ebi.spot.ols.reststatistics.dto.RestCallDto;
import uk.ac.ebi.spot.ols.reststatistics.dto.RestCallRequest;
import uk.ac.ebi.spot.ols.reststatistics.entity.RestCallParameter;
import uk.ac.ebi.spot.ols.reststatistics.entity.RestCallParameterType;

@RestController
@RequestMapping("/api/rest/statistics")
public class RestCallStatistics {
	public static final int DEFAULT_PAGE_SIZE = 20;

	private final RestCallService restCallService;
    private final RestCallStatisticsService restCallStatisticsService;
    private final RestCallAssembler restCallAssembler;
    private final KeyValueResultAssembler keyValueResultAssembler;

    @Autowired
    public RestCallStatistics(RestCallService restCallService,
                                        RestCallStatisticsService restCallStatisticsService,
                                        RestCallAssembler restCallAssembler,
                                        KeyValueResultAssembler keyValueResultAssembler) {
        this.restCallService = restCallService;
        this.restCallStatisticsService = restCallStatisticsService;
        this.restCallAssembler = restCallAssembler;
        this.keyValueResultAssembler = keyValueResultAssembler;
    }

    @Operation(summary = "REST Calls List")
    @RequestMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
    public HttpEntity<PagedModel<RestCallDto>> getList(
        @RequestParam(name = "url", required = false) String url,
        @RequestParam(name = "dateTimeFrom", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTimeFrom,
        @RequestParam(name = "dateTimeTo", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTimeTo,
        @RequestParam(name="intersection", required=true,defaultValue="false") boolean intersection,
        @RequestBody(required = false) List<RestCallParameter> parameterList,
        @Parameter(hidden = true) PagedResourcesAssembler assembler,
        @PageableDefault(size = DEFAULT_PAGE_SIZE)
        @Parameter(hidden = true) Pageable pageable
    ) {

		RestCallRequest request = new RestCallRequest(url, dateTimeFrom, dateTimeTo);

		List<RestCallParameter> parameters = new ArrayList<RestCallParameter>();

        if(parameterList != null) {
            parameters.addAll(parameterList);
        }

		Page<RestCallDto> page = restCallService.getList(request, parameters, intersection, pageable);

		return new ResponseEntity<>(assembler.toModel(page, restCallAssembler), HttpStatus.OK);
    }

    @Operation(summary = "REST Calls statistics by URL")
    @RequestMapping(value = "/byUrl", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
    public HttpEntity<PagedModel<KeyValueResultDto>> getStatisticsByUrl(
        @RequestParam(name = "url", required = false) String url,
        @RequestParam(name = "dateTimeFrom", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTimeFrom,
        @RequestParam(name = "dateTimeTo", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTimeTo,
        @RequestParam(name="intersection", required=true,defaultValue="false") boolean intersection,
        @RequestBody(required = false) List<RestCallParameter> parameterList,
        @Parameter(hidden = true) PagedResourcesAssembler assembler,
        @PageableDefault(size = DEFAULT_PAGE_SIZE)
        @Parameter(hidden = true) Pageable pageable
    ) {
        RestCallRequest request = new RestCallRequest(
            url,
            dateTimeFrom,
            dateTimeTo
        );

        List<RestCallParameter> parameters = new ArrayList<RestCallParameter>();

        if(parameterList != null) {
            parameters.addAll(parameterList);
        }

        Page<KeyValueResultDto> page = restCallStatisticsService.getRestCallsCountsByAddress(request, parameters, intersection, pageable);

        return new ResponseEntity<>(assembler.toModel(page, keyValueResultAssembler), HttpStatus.OK);
    }

    @Operation(summary = "REST Calls total count")
    @RequestMapping(value = "/count", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
    public HttpEntity<KeyValueResultDto> getTotalCount(
        @RequestParam(name = "url", required = false) String url,
        @RequestParam(name = "dateTimeFrom", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTimeFrom,
        @RequestParam(name = "dateTimeTo", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTimeTo,
        @RequestParam(name="intersection", required=true,defaultValue="false") boolean intersection,
        @RequestBody(required = false) List<RestCallParameter> parameterList
    ) {
        RestCallRequest request = new RestCallRequest(
            url,
            dateTimeFrom,
            dateTimeTo
        );


        List<RestCallParameter> parameters = new ArrayList<RestCallParameter>();

        if(parameterList != null) {
            parameters.addAll(parameterList);
        }

        KeyValueResultDto counts = restCallStatisticsService.getRestCallsTotalCount(request,parameters,intersection);

        return new ResponseEntity<>(counts, HttpStatus.OK);
    }

    @Operation(summary = "REST Calls statistics by query parameters and path variables")
    @RequestMapping(value = "/byParameter", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
    public HttpEntity<PagedModel<KeyValueResultDto>> getStatisticsByParameter(
        @Parameter(description = "Parameter type")
        @RequestParam(name = "type", required = false) RestCallParameterType type,
        @RequestParam(name = "url", required = false) String url,
        @Parameter(description = "Parameter name")
        @RequestParam(name = "parameter", required = false) String parameter,
        @RequestParam(name = "dateTimeFrom", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTimeFrom,
        @RequestParam(name = "dateTimeTo", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTimeTo,
        @RequestParam(name="intersection", required=true,defaultValue="false") boolean intersection,
        @RequestBody(required = false) List<RestCallParameter> parameterList,
        @Parameter(hidden = true) PagedResourcesAssembler assembler,
        @PageableDefault(size = DEFAULT_PAGE_SIZE)
        @Parameter(hidden = true) Pageable pageable
    ) {
        RestCallRequest request = new RestCallRequest(
            url,
            Optional.ofNullable(type),
            Optional.ofNullable(parameter),
            dateTimeFrom,
            dateTimeTo
        );

        List<RestCallParameter> parameters = new ArrayList<RestCallParameter>();

        if(parameterList != null) {
            parameters.addAll(parameterList);
        }

        Page<KeyValueResultDto> page = restCallStatisticsService.getStatisticsByParameter(request, parameters, intersection,pageable);

        return new ResponseEntity<>(assembler.toModel(page, keyValueResultAssembler), HttpStatus.OK);
    }

    @Operation(summary = "REST Calls statistics by date")
    @RequestMapping(value = "/byDate", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
    public HttpEntity<PagedModel<KeyValueResultDto>> getStatisticsByDate(
    	@Parameter(description = "Parameter type")
        @RequestParam(name = "type", required = false) RestCallParameterType type,
        @RequestParam(name = "url", required = false) String url,
        @Parameter(description = "Parameter name")
        @RequestParam(name = "parameter", required = false) String parameter,
        @RequestParam(name = "dateTimeFrom", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTimeFrom,
        @RequestParam(name = "dateTimeTo", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTimeTo,
        @RequestParam(name="intersection", required=true,defaultValue="false") boolean intersection,
        @RequestBody(required = false) List<RestCallParameter> parameterList,
        @Parameter(hidden = true) PagedResourcesAssembler assembler,
        @PageableDefault(size = DEFAULT_PAGE_SIZE)
        @Parameter(hidden = true) Pageable pageable
    ) {
        RestCallRequest request = new RestCallRequest(
            url,
            Optional.ofNullable(type),
            Optional.ofNullable(parameter),
            dateTimeFrom,
            dateTimeTo
        );

        List<RestCallParameter> parameters = new ArrayList<RestCallParameter>();

        if(parameterList != null) {
            parameters.addAll(parameterList);
        }

        Page<KeyValueResultDto> page = restCallStatisticsService.getStatisticsByDate(request, parameters, intersection, pageable);

        return new ResponseEntity<>(assembler.toModel(page, keyValueResultAssembler), HttpStatus.OK);
    }
}
