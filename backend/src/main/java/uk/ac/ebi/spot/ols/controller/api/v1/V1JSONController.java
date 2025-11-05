package uk.ac.ebi.spot.ols.controller.api.v1;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriUtils;
import uk.ac.ebi.spot.ols.repository.v1.*;

/**
 * @author Erhun Giray TUNCAY
 * @email giray.tuncay@tib.eu
 * TIB-Leibniz Information Center for Science and Technology
 */
@Tag(name = "JSON Controller", description = "NOTE: For IRI parameters, the value must be URL encoded. " +
        "For example, the IRI http://purl.obolibrary.org/obo/DUO_0000017 should be encoded as http%3A%2F%2Fpurl.obolibrary.org%2Fobo%2FDUO_0000017.")
@RestController
@RequestMapping("/api/fulljson")
public class V1JSONController {

    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    V1GraphRepository graphRepository;

    @RequestMapping(path = "/{onto}/entity/{iri}/find", produces = {MediaType.APPLICATION_JSON_VALUE,
            MediaTypes.HAL_JSON_VALUE}, method = RequestMethod.GET)
    HttpEntity<String> getJson(
            @PathVariable("onto")
            @Parameter(name = "onto",
                    description = "The ID of the ontology. For example for Data Use Ontology, the ID is duo.",
                    example = "duo") String ontologyId,
            @PathVariable("iri")
            @Parameter(name = "iri",
                    description = "The IRI of the term, this value must be single URL encoded",
                    example = "http%3A%2F%2Fpurl.obolibrary.org%2Fobo%2FDUO_0000017") String termId,
            @Parameter(description = "entity type", required = true)
            @RequestParam(value = "entity_type", required = true, defaultValue = "TERM") EntityType entityType) {

        ontologyId = ontologyId.toLowerCase();

        String decoded = UriUtils.decode(termId, "UTF-8");
        String entityId = ontologyId+"+class+"+decoded;
        String json = graphRepository.getEntityJson(entityId,ontologyId,entityType);
        if (json == null)
            throw  new ResourceNotFoundException("No _json could be found for " + ontologyId
                    + " and " + termId);

        return new ResponseEntity<>( json, HttpStatus.OK);
    }

    @RequestMapping(path = "/entities", produces = {MediaType.APPLICATION_JSON_VALUE,
            MediaTypes.HAL_JSON_VALUE}, method = RequestMethod.GET)
    HttpEntity<Page<String>>getEntities(
            @RequestParam(value = "onto", required = false)
            @Parameter(name = "onto",
                    description = "The ID of the ontology. For example for Data Use Ontology, the ID is duo.",
                    example = "duo") String ontologyId,
            @Parameter(description = "entity type", required = true)
            @RequestParam(value = "entity_type", required = true, defaultValue = "TERM") EntityType entityType,
            @Parameter(hidden = true) Pageable pageable){
        return new ResponseEntity<>(graphRepository.getEntitiesJson(ontologyId, entityType,pageable), HttpStatus.OK);
    }

}
