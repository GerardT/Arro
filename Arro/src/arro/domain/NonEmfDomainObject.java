package arro.domain;

import java.util.UUID;

import workspace.ArroModuleContainer;

public class NonEmfDomainObject {

	private String id;
	private String name;
	
	public NonEmfDomainObject() {
		if(id == null) {
			id = UUID.randomUUID().toString();
		}
		POJOIndependenceSolver.getInstance().RegisterPOJOObject(this);
	}

	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
		POJOIndependenceSolver.getInstance().RegisterPOJOObject(this);
	}
	
	public ArroModuleContainer update() {
	    return null;
	}

}
