import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAppDispatch, useAppSelector } from "../../../app/hooks";
import DataTable, { Column } from "../../../components/DataTable";
import Entity from "../../../model/Entity";
import { getEntities } from "../ontologiesSlice";
import Individual from "../../../model/Individual";

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

  // Merge columns based on the entity type.
  const columns =
      entityType === "individuals"
          ? [...baseColumns, individualTypeColumn]
          : baseColumns;

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
