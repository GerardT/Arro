package arro.domain;

public class ArroParameter {
	//             <param key="Ki" subst="" value="0.1"/>
	private String key;
	private String substitute;  // defines external parameter that can be used outside.
	private String value;
	private String node;
	
	public ArroParameter(String key, String substitute, String value, String node) {
		this.key = key;
		this.substitute = substitute;
		this.value = value;
	}
	
	public void setKey(String key) {
		this.key = key;
	}
	public String getFormalKey() {
		return key;
	}
	public void setSubstitute(String substitute) {
		this.substitute = substitute;
	}
	public String getSubstitute() {
		return substitute;
	}
	public void setValue(String value) {
		this.value = value;
	}
	public String getValue() {
		return value;
	}
	public void setNode(String node) {
		this.node = node;
	}
	public String getNode() {
		return node;
	}
}
