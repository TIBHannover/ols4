package uk.ac.ebi.spot.ols.reststatistics.entity;

import java.util.function.Predicate;

public enum RestCallParameterType {
    PATH {
        @Override
        public Predicate<RestCallParameter> getRestCallParameterPredicate() {
            return RestCallParameter::isPathType;
        }
    },
    QUERY {
        @Override
        public Predicate<RestCallParameter> getRestCallParameterPredicate() {
            return RestCallParameter::isQueryType;
        }
    },
    HEADER {
        @Override
        public Predicate<RestCallParameter> getRestCallParameterPredicate() {
            return RestCallParameter::isHeaderType;
        }
    };

    public abstract Predicate<RestCallParameter> getRestCallParameterPredicate();
}
