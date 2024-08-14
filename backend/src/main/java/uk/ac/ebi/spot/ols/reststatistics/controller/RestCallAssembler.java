package uk.ac.ebi.spot.ols.reststatistics.controller;

import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import uk.ac.ebi.spot.ols.reststatistics.dto.RestCallDto;

@Component
public class RestCallAssembler implements RepresentationModelAssembler<RestCallDto, EntityModel<RestCallDto>> {

    @Override
    public EntityModel<RestCallDto> toModel(RestCallDto document) {
    	EntityModel<RestCallDto> resource = EntityModel.of(document);

        return resource;
    }
}