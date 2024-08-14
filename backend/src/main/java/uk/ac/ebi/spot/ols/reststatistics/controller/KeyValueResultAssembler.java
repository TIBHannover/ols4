package uk.ac.ebi.spot.ols.reststatistics.controller;

import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import uk.ac.ebi.spot.ols.reststatistics.dto.KeyValueResultDto;

@Component
public class KeyValueResultAssembler implements RepresentationModelAssembler<KeyValueResultDto, EntityModel<KeyValueResultDto>> {

    @Override
    public EntityModel<KeyValueResultDto> toModel(KeyValueResultDto document) {
    	EntityModel<KeyValueResultDto> resource = EntityModel.of(document);

        return resource;
    }
}