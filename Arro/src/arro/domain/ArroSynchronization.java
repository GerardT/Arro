package arro.domain;

public class ArroSynchronization extends NonEmfDomainObject {
	private boolean in = false;

	public ArroSynchronization(boolean in) {
		this.in = in;
	}

	public boolean getIn() {
		return in;
	}

}
