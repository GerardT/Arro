package arro.domain;

public class ArroSynchronization extends NonEmfDomainObject {
	private boolean start = false;

	public ArroSynchronization(boolean start) {
		this.start = start;
	}

	public boolean getStart() {
		return start;
	}

}
