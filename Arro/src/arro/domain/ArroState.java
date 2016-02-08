package arro.domain;


public class ArroState extends NonEmfDomainObject{
	private ArroStateDiagram parent;
	
	public ArroStateDiagram getParent() {
		return parent;
	}
	public void setParent(ArroStateDiagram parent) {
		this.parent = parent;
	}
}
