import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAppDispatch, useAppSelector } from "../../../app/hooks";
import DataTable, { Column } from "../../../components/DataTable";
import Entity from "../../../model/Entity";
import { getEntities } from "../ontologiesSlice";
import Individual from "../../../model/Individual";
import Property from "../../../model/Property";

export default function EntityList({
  ontologyId,
  entityType,
}: {
  ontologyId: string;
  entityType: "entities" | "classes" | "properties" | "individuals";
}) {
  const dispatch = useAppDispatch();
  const entities = useAppSelector((state) => state.ontologies.entities);
  // const loading = useAppSelector((state) => state.ontologies.loadingEntities);
  const totalEntities = useAppSelector(
    (state) => state.ontologies.totalEntities
  );

  const [page, setPage] = useState<number>(0);
  const [rowsPerPage, setRowsPerPage] = useState<number>(10);
  const [search, setSearch] = useState<string>("");

  useEffect(() => {
    dispatch(
      getEntities({ ontologyId, entityType, page, rowsPerPage, search })
    );
  }, [dispatch, ontologyId, entityType, page, rowsPerPage, search]);

  useEffect(() => {
    setPage(0);
  }, [entityType]);

  const navigate = useNavigate();

  // base columns are shown for all entity types.
  const baseColumns: readonly Column[] = [
    {
      name: "Name",
      sortable: true,
      selector: (entity: Entity) => entity.getName(),
    },
    {
      name: "ID",
      sortable: true,
      selector: (entity: Entity) => entity.getShortForm(),
    },
  ];

  // If the entity type is "individuals", add a "associated class" column.
  const individualTypeColumn: Column = {
    name: "Associated Class",
    sortable: true,
    selector: (entity: Entity) => {
      if(entity instanceof Individual) {
        const types = entity.getIndividualTypes();
        const linkedEntities = entity.getLinkedEntities();
        if (types && types.length > 0) {
          return types
              .map((val: any) => {
                if (!(typeof val === "object" && !Array.isArray(val))) {
                  return (
                      linkedEntities.getLabelForIri(val) ||
                      val.split("/").pop() ||
                      val
                  );
                }
                if (typeof val === "object" && !Array.isArray(val)) {
                  // If the object has the "someValuesFrom" property, use the custom format.
                  if (val["http://www.w3.org/2002/07/owl#someValuesFrom"]) {
                    const someValuesFromIri = val["http://www.w3.org/2002/07/owl#someValuesFrom"];
                    const onPropertyIri = val["http://www.w3.org/2002/07/owl#onProperty"];
                    const someValuesLabel =
                        linkedEntities.getLabelForIri(someValuesFromIri) ||
                        (typeof someValuesFromIri === "string" && someValuesFromIri.split("/").pop()) ||
                        someValuesFromIri;
                    const propertyLabel =
                        linkedEntities.getLabelForIri(onPropertyIri) ||
                        (typeof onPropertyIri === "string" && onPropertyIri.split("/").pop()) ||
                        onPropertyIri;
                    return `${propertyLabel} some ${someValuesLabel}`;
                  }
                }
              })
              .join(", ");
        }
      }
      return "";
    },
  };

  // Define domain column for properties
  const domainColumn: Column = {
    name: "Domain",
    sortable: true,
    selector: (entity: Entity) => {
      if(entity instanceof Property) {
        const domains = entity.getDomain();
        if (domains && domains.length > 0) {
          const linkedEntities = entity.getLinkedEntities();
          return domains
              .map((domain: any) => {
                if (typeof domain === "string") {
                  return (
                      linkedEntities.getLabelForIri(domain) ||
                      domain.split("/").pop() ||
                      domain
                  );
                }
                return "";
              })
              .filter(Boolean)
              .join(", ");
        }
      }
      return "";
    },
  };

  // Define range column for properties
  const rangeColumn: Column = {
    name: "Range",
    sortable: true,
    selector: (entity: Entity) => {
      if(entity instanceof Property) {
        const ranges = entity.getRange();
        if (ranges && ranges.length > 0) {
          const linkedEntities = entity.getLinkedEntities();
          return ranges
              .map((range: any) => {
                if (typeof range === "string") {
                  return (
                      linkedEntities.getLabelForIri(range) ||
                      range.split("/").pop() ||
                      range
                  );
                }
                // Handle complex range objects
                if (typeof range === "object" && !Array.isArray(range)) {
                  // Check for owl:intersectionOf
                  const intersectionOf = range["http://www.w3.org/2002/07/owl#intersectionOf"];
                  if (intersectionOf && Array.isArray(intersectionOf)) {
                    return intersectionOf.map((item: any) => {
                      // Handle string IRIs in the intersection
                      if (typeof item === "string") {
                        return (
                            linkedEntities.getLabelForIri(item) ||
                            item.split("/").pop() ||
                            item
                        );
                      }

                      // Handle objects in the intersection, particularly looking for owl:oneOf
                      if (typeof item === "object" && !Array.isArray(item)) {
                        const oneOf = item["http://www.w3.org/2002/07/owl#oneOf"];
                        if (oneOf && Array.isArray(oneOf)) {
                          // Format the oneOf elements as a comma-separated list inside curly braces
                          return `{${oneOf.join(", ")}}`;
                        }
                      }

                      return "";
                    }).filter(Boolean).join(" and ");
                  }
                }
                return "";
              })
              .filter(Boolean)
              .join(", ");
        }
      }
      return "";
    },
  };

  // Merge columns based on the entity type.
  // Merge columns based on the entity type.
  let columns = [...baseColumns];

  if (entityType === "individuals") {
    columns.push(individualTypeColumn);
  } else if (entityType === "properties") {
    columns.push(domainColumn, rangeColumn);
  }

  return (
    <div className="mt-2">
      <DataTable
        columns={columns}
        data={entities}
        dataCount={totalEntities}
        placeholder={`Search ${entityType}...`}
        page={page}
        rowsPerPage={rowsPerPage}
        onPageChange={(pg: number) => {
          setPage(pg);
        }}
        onRowsPerPageChange={(rows: number) => {
          setRowsPerPage((prev) => {
            if (rows !== prev) setPage(0);
            return rows;
          });
        }}
        onSelectRow={(row) => {
          const termUrl = encodeURIComponent(
            encodeURIComponent(row.properties.iri)
          );
          navigate(
            `/ontologies/${ontologyId}/${row.getTypePlural()}/${termUrl}`
          );
        }}
        onFilter={(key: string) => {
          setSearch((prev) => {
            if (key !== prev) setPage(0);
            return key;
          });
        }}
      />
    </div>
  );
}
