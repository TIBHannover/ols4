import { FormControl, RadioGroup, FormControlLabel, Radio } from "@mui/material";
import { Map as ImmutableMap, Set as ImmutableSet } from "immutable";
import { Fragment, useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { useAppDispatch, useAppSelector } from "../../app/hooks";
import { randomString } from "../../app/util";
import LoadingOverlay from "../../components/LoadingOverlay";
import Node from "../../components/Node";
import Entity from "../../model/Entity";
import Ontology from "../../model/Ontology";
import extractEntityHierarchy from "./extractEntityHierarchy";
import {
  getAncestors,
  getNodeChildren,
  getRootEntities,
  TreeNode
} from "./ontologiesSlice";

export default function EntityTree({
  ontology,
  entityType,
  selectedEntity,
  lang
}: {
  ontology:Ontology,
  selectedEntity?: Entity;
  entityType: "entities" | "classes" | "properties" | "individuals",
  lang:string
}) {
  const dispatch = useAppDispatch();
  const ancestors = useAppSelector((state) => state.ontologies.ancestors);
  const children = useAppSelector((state) => state.ontologies.nodeChildren);
  const rootEntities = useAppSelector((state) => state.ontologies.rootEntities);
  const loading = useAppSelector(
    (state) => state.ontologies.loadingNodeChildren
  );

  const [rootNodes, setRootNodes] = useState<TreeNode[]>();
  const [nodeChildren, setNodeChildren] = useState<
    ImmutableMap<String, TreeNode[]>
  >(ImmutableMap());
  const [expandedNodes, setExpandedNodes] = useState<ImmutableSet<String>>(
    ImmutableSet()
  );
  const [preferredRoots, setPreferredRoots] = useState<boolean>(ontology.getPreferredRoots().length > 0);

  const populateTreeFromEntities = useCallback(
    (entities: Entity[]) => {
      let { rootEntities, uriToChildNodes } =
        extractEntityHierarchy(entities);

	if(preferredRoots) {
		let preferred = ontology.getPreferredRoots()
		if(preferred.length > 0) {
			let preferredRootEntities = preferred.map(iri => entities.filter(entity => entity.getIri() === iri)[0])
			rootEntities = preferredRootEntities.filter(entity => !!entity)
		}
	}

      const newNodeChildren = new Map<String, TreeNode[]>();
      const newExpandedNodes = new Set<String>();

      setRootNodes(
        rootEntities.map((rootEntity) =>
          createTreeNode(rootEntity, undefined, 0)
        )
      );

      setNodeChildren(ImmutableMap(newNodeChildren));
      setExpandedNodes(ImmutableSet(newExpandedNodes));

      function createTreeNode(
        node: Entity,
        parent: TreeNode | undefined,
        debugNumIterations: number
      ): TreeNode {
        if (debugNumIterations > 100) {
          throw new Error("probable cyclic tree (createTreeNode)");
        }
        const childNodes = uriToChildNodes.get(node.getIri()) || [];

        const treeNode: TreeNode = {
          absoluteIdentity: parent
            ? parent.absoluteIdentity + ";" + node.getIri()
            : node.getIri(),
          iri: node.getIri(),
          title: node.getName(),
          expandable: node.hasChildren(),
          entity: node,
          numDescendants: node.getNumHierarchicalDescendants() || node.getNumDescendants()
        };

        newNodeChildren.set(
          treeNode.absoluteIdentity,
          childNodes.map((childNode) =>
            createTreeNode(childNode, treeNode, debugNumIterations + 1)
          )
        );

        if (treeNode.iri !== selectedEntity?.getIri()) {
          newExpandedNodes.add(treeNode.absoluteIdentity);
        }

        return treeNode;
      }
    },
    [selectedEntity,preferredRoots,lang]
  );
  const toggleNode = (node: any) => {
    if (expandedNodes.has(node.absoluteIdentity)) {
      // closing a node
      setExpandedNodes(expandedNodes.delete(node.absoluteIdentity));
    } else {
      // opening a node
      setExpandedNodes(expandedNodes.add(node.absoluteIdentity));
      const entityIri = node.iri;
      const absoluteIdentity = node.absoluteIdentity;
      dispatch(
        getNodeChildren({
          ontologyId: ontology.getOntologyId(),
          entityTypePlural: entityType,
          entityIri,
          absoluteIdentity,
	  lang
        })
      );
    }
  };

  useEffect(() => {
    if (selectedEntity) {
      const entityIri = selectedEntity.getIri();
      dispatch(getAncestors({ ontologyId: ontology.getOntologyId(), entityType, entityIri, lang }));
    } else {
      dispatch(getRootEntities({ ontologyId: ontology.getOntologyId(), entityType, preferredRoots, lang }));
    }

    // for(let alreadyExpanded of expandedNodes.toArray()) {
    //   dispatch(
    //     getNodeChildren({
    //       ontologyId: ontology.getOntologyId(),
    //       entityTypePlural: entityType,
    //       entityIri: alreadyExpanded.split(';').pop(),
    //       absoluteIdentity: alreadyExpanded,
	  // lang
    //     })
    //   )
    // }
  }, [dispatch, entityType, selectedEntity, ontology, preferredRoots, lang]);

  useEffect(() => {
    if (selectedEntity) {
      populateTreeFromEntities([selectedEntity, ...ancestors]);
    }
  }, [populateTreeFromEntities, ancestors, selectedEntity, lang]);

  useEffect(() => {
    setNodeChildren(nodeChildren.merge(children));
  }, [nodeChildren, children, lang]);

  useEffect(() => {
    setRootNodes(
      rootEntities.map((entity: Entity) => {
        return {
          iri: entity.getIri(),
          absoluteIdentity: entity.getIri(),
          title: entity.getName(),
          expandable: entity.hasChildren(),
          entity: entity,
          numDescendants: entity.getNumHierarchicalDescendants() || entity.getNumDescendants()
        };
      })
    );
  }, [rootEntities, lang]);

  function renderNodeChildren(
    children: TreeNode[],
    debugNumIterations: number
  ) {
    if (debugNumIterations > 100) {
      throw new Error("probable cyclic tree (renderNodeChildren)");
    }
    const childrenCopy = [...children];
    childrenCopy.sort((a, b) => {
      const titleA = a?.title ? a.title.toString().toUpperCase() : "";
      const titleB = b?.title ? b.title.toString().toUpperCase() : "";
      return titleA === titleB ? 0 : titleA > titleB ? 1 : -1;
    });
    const childrenSorted = childrenCopy.filter(
      (child, i, arr) =>
        !i || child.absoluteIdentity !== arr[i - 1].absoluteIdentity
    );
    return (
      <ul
        role="group"
        className="jstree-container-ul jstree-children jstree-no-icons"
      >
        {childrenSorted.map((childNode: TreeNode, i) => {
          const isExpanded = expandedNodes.has(childNode.absoluteIdentity);
          const isLast = i === childrenSorted!.length - 1;
          const termUrl = encodeURIComponent(encodeURIComponent(childNode.iri));
          const highlight =
            (selectedEntity && childNode.iri === selectedEntity.getIri()) ||
            false;
          return (
            <Node
              expandable={childNode.expandable}
              expanded={isExpanded}
              highlight={highlight}
              isLast={isLast}
              onClick={() => {
                toggleNode(childNode);
              }}
              key={randomString()}
            >
              <Link
                to={`/ontologies/${ontology.getOntologyId()}/${childNode.entity.getTypePlural()}/${termUrl}`}
              >
                {childNode.title}
              </Link>
              {
                childNode.numDescendants > 0 &&
                  <span style={{color:'gray'}}>{' (' + childNode.numDescendants.toLocaleString() + ')'}</span>
              }
              {isExpanded &&
                renderNodeChildren(
                  nodeChildren.get(childNode.absoluteIdentity) || [],
                  debugNumIterations + 1
                )}
            </Node>
          );
        })}
      </ul>
    );
  }
  return <Fragment>
    <div style={{position: 'relative'}}>
	{ontology.getPreferredRoots().length > 0 &&
	<div style={{position:'absolute', right:0, top:0}}>
		<FormControl>
			<RadioGroup
				name="radio-buttons-group"
				value={preferredRoots ? "true": "false"}
			>
				<FormControlLabel value="true" onClick={() => setPreferredRoots(true)} control={<Radio  />} label="Preferred roots" />
				<FormControlLabel value="false"  onClick={() => setPreferredRoots(false)}  control={<Radio/>} label="All classes" />
			</RadioGroup>
		</FormControl>
		</div>}
      {rootNodes ? (
        <div className="px-3 jstree jstree-1 jstree-proton" role="tree">
          {renderNodeChildren(rootNodes, 0)}
        </div>
      ) : null}
      {loading ? <LoadingOverlay message="Loading children..." /> : null}
    </div>
  </Fragment>
}
