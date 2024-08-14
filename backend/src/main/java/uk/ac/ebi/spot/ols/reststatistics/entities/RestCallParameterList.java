package uk.ac.ebi.spot.ols.reststatistics.entities;

import java.util.List;

public class RestCallParameterList {
	
	private List<RestCallParameter> parameters;
	
	public RestCallParameterList() {
		super();
	}

	public RestCallParameterList(List<RestCallParameter> parameters) {
		super();
		this.parameters = parameters;
	}

	public List<RestCallParameter> getParameters() {
		return parameters;
	}

	public void setParameters(List<RestCallParameter> parameters) {
		this.parameters = parameters;
	}

}
