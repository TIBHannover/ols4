package uk.ac.ebi.spot.ols.reststatistics.service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import uk.ac.ebi.spot.ols.reststatistics.abstraction.service.RestCallService;
import uk.ac.ebi.spot.ols.reststatistics.abstraction.service.RestCallStatisticsService;
import uk.ac.ebi.spot.ols.reststatistics.dto.KeyValueResultDto;
import uk.ac.ebi.spot.ols.reststatistics.dto.RestCallDto;
import uk.ac.ebi.spot.ols.reststatistics.dto.RestCallRequest;
import uk.ac.ebi.spot.ols.reststatistics.entities.RestCallParameter;

@Service
public class RestCallStatisticsServiceImpl implements RestCallStatisticsService {
	private final RestCallService restCallService;

	@Autowired
	public RestCallStatisticsServiceImpl(RestCallService restCallService) {
		this.restCallService = restCallService;
	}

	@Override
	public Page<KeyValueResultDto> getRestCallsCountsByAddress(RestCallRequest request,
			List<RestCallParameter> parameters, boolean intersection, Pageable pageable) {
		Page<RestCallDto> page = restCallService.getList(request, parameters, intersection, pageable);

		Map<String, Long> countsMap = getCountsMap(page);

		List<KeyValueResultDto> list = countsMap.entrySet().stream()
				.map(entry -> new KeyValueResultDto(entry.getKey(), entry.getValue())).collect(Collectors.toList());

		return new PageImpl<>(list, pageable, list.size());
	}

	@Override
	public KeyValueResultDto getRestCallsTotalCount(RestCallRequest request, List<RestCallParameter> parameters,
			boolean intersection) {
		Long count = restCallService.count(request, parameters, intersection);
		Long value = Optional.ofNullable(count).orElse(0L);

		return new KeyValueResultDto("total", value);
	}

	@Override
	public Page<KeyValueResultDto> getStatisticsByParameter(RestCallRequest request, List<RestCallParameter> parameters,
			boolean intersection, Pageable pageable) {
		Page<RestCallDto> page = restCallService.getList(request, parameters, intersection, pageable);

		Map<String, Long> parametersWithCountsMap = page.getContent().stream()
				.flatMap(restCallDto -> restCallDto.getParameters().stream())
				.filter(request.getParameterNamePredicate()).filter(request.getParameterTypePredicate())
				.collect(Collectors.groupingBy(RestCallParameter::getValue, Collectors.counting())).entrySet().stream()
				.sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).collect(Collectors.toMap(
						Map.Entry::getKey, Map.Entry::getValue, (oldValue, newValue) -> oldValue, LinkedHashMap::new));

		List<KeyValueResultDto> list = parametersWithCountsMap.entrySet().stream()
				.map(entry -> new KeyValueResultDto(entry.getKey(), entry.getValue())).collect(Collectors.toList());

		return new PageImpl<>(list, pageable, list.size());
	}

	@Override
	public Page<KeyValueResultDto> getStatisticsByDate(RestCallRequest request, List<RestCallParameter> parameters,
			boolean intersection, Pageable pageable) {
		Page<RestCallDto> page = restCallService.getList(request, parameters, intersection, pageable);

		LinkedHashMap<String, Long> map = page.getContent().stream()
				.collect(Collectors.groupingBy(restCallDto -> restCallDto.getCreatedAt().toLocalDate().toString(),
						Collectors.counting()))
				.entrySet().stream().sorted(Map.Entry.comparingByKey()).collect(Collectors.toMap(Map.Entry::getKey,
						Map.Entry::getValue, (oldValue, newValue) -> oldValue, LinkedHashMap::new));

		List<KeyValueResultDto> list = map.entrySet().stream()
				.map(entry -> new KeyValueResultDto(entry.getKey(), entry.getValue())).collect(Collectors.toList());

		return new PageImpl<>(list, pageable, list.size());
	}

	private Map<String, Long> getCountsMap(Page<RestCallDto> page) {
		Map<String, Long> addressesWithCountsMap = page.getContent().stream()
				.collect(Collectors.groupingBy(RestCallDto::getUrl, Collectors.counting()));

		return addressesWithCountsMap.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (oldValue, newValue) -> oldValue,
						LinkedHashMap::new));
	}

}
