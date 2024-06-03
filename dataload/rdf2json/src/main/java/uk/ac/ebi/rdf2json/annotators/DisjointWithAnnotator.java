package uk.ac.ebi.rdf2json.annotators;

import java.util.*;
import java.util.stream.Collectors;

import uk.ac.ebi.rdf2json.OntologyGraph;
import uk.ac.ebi.rdf2json.OntologyNode;
import uk.ac.ebi.rdf2json.OntologyNode.NodeType;
import uk.ac.ebi.rdf2json.annotators.helpers.PropertyCollator;
import uk.ac.ebi.rdf2json.helpers.RdfListEvaluator;
import uk.ac.ebi.rdf2json.properties.PropertyValue;
import uk.ac.ebi.rdf2json.properties.PropertyValueURI;

public class DisjointWithAnnotator {

	public static void annotateDisjointWith(OntologyGraph graph) {

		long startTime3 = System.nanoTime();

		for (String id : graph.nodes.keySet()) {
			OntologyNode c = graph.nodes.get(id);

			if (c.types.contains(OntologyNode.NodeType.ALL_DISJOINT_CLASSES)) {

				PropertyValue membersList = c.properties.getPropertyValue("http://www.w3.org/2002/07/owl#members");
				List<PropertyValue> members = RdfListEvaluator
						.evaluateRdfList(graph.getNodeForPropertyValue(membersList), graph);

				List<OntologyNode> classNodes = members.stream().map(val -> graph.getNodeForPropertyValue(val))
						.collect(Collectors.toList());

				for (OntologyNode classNodeA : classNodes) {
					for (OntologyNode classNodeB : classNodes) {
						if (classNodeA != null && classNodeB != null && classNodeB.uri != classNodeA.uri) {
							classNodeA.properties.addProperty("http://www.w3.org/2002/07/owl#disjointWith",
									PropertyValueURI.fromUri(classNodeB.uri));
						}
					}
				}

			} else if (c.types.contains(OntologyNode.NodeType.ALL_DISJOINT_PROPERTIES)) {

				PropertyValue membersList = c.properties.getPropertyValue("http://www.w3.org/2002/07/owl#members");
				List<PropertyValue> members = RdfListEvaluator
						.evaluateRdfList(graph.getNodeForPropertyValue(membersList), graph);

				List<OntologyNode> propertyNodes = members.stream().map(val -> graph.getNodeForPropertyValue(val))
						.collect(Collectors.toList());

				for (OntologyNode propertyNodeA : propertyNodes) {
					for (OntologyNode propertyNodeB : propertyNodes) {
						if (propertyNodeB.uri != propertyNodeA.uri) {
							propertyNodeA.properties.addProperty("http://www.w3.org/2002/07/owl#propertyDisjointWith",
									PropertyValueURI.fromUri(propertyNodeB.uri));
						}
					}
				}

			} else if (c.types.contains(OntologyNode.NodeType.ALL_DIFFERENT)) {

				PropertyValue membersList = c.properties
						.getPropertyValue("http://www.w3.org/2002/07/owl#distinctMembers");
				if (membersList != null) {
					List<PropertyValue> members = RdfListEvaluator
							.evaluateRdfList(graph.getNodeForPropertyValue(membersList), graph);

					List<OntologyNode> individualNodes = members.stream().map(val -> graph.getNodeForPropertyValue(val))
							.filter(val -> val != null).collect(Collectors.toList());

					for (OntologyNode individualNodeA : individualNodes) {
						for (OntologyNode individualNodeB : individualNodes) {
							if (individualNodeB.uri != individualNodeA.uri) {
								individualNodeA.properties.addProperty("http://www.w3.org/2002/07/owl#differentFrom",
										PropertyValueURI.fromUri(individualNodeB.uri));
							}
						}
					}
				}
			}
		}

		long endTime3 = System.nanoTime();
		System.out.println("annotate disjointWith: " + ((endTime3 - startTime3) / 1000 / 1000 / 1000));

	}
}
