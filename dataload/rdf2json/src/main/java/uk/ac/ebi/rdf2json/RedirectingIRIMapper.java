package uk.ac.ebi.rdf2json;

import org.semanticweb.owlapi.model.*;

/**
 *@author Deepan Anbalagan
 *@email deepan.anbalagan@tib.eu
 *TIB-Leibniz Information Center for Science and Technology
*/
public class RedirectingIRIMapper implements OWLOntologyIRIMapper {
	
    @Override
    public IRI getDocumentIRI(IRI ontologyIRI) {
        return IRI.create(RedirectResolver.resolve(ontologyIRI.toString()));
    }

}
