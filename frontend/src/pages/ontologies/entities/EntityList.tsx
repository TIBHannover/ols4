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
          // If the first type is NOT an object (or non-array object), map each IRI to its label.
          if (!(typeof types[0] === "object" && !Array.isArray(types[0]))) {
            // Here linkedEntities.getLabelForIri(iri) is used to fetch the label.
            return types
                .map((iri: string) => {
                  return linkedEntities.getLabelForIri(iri) ||
                      iri.split("/").pop() ||
                      iri;
                })
                .join(", ");
          }
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
