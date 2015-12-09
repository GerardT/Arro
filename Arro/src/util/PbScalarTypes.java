package util;

public class PbScalarTypes {
	private static PbScalarTypes me = null;
	private static String scalars[] = {
		"bool",
		"string",
		"int32",
		"int64",
		"uint32",
		"uint64",
		"sint32",
		"sint64",
		"fixed32",
		"fixed64",
		"sfixed32",
		"sfixed64",
		"double",
		"float",
		"bytes" };
	
	public static PbScalarTypes getInstance() {
		if(me == null) {
			me = new PbScalarTypes();
		}
		return me;
	}

	public String[] getList() {
		return scalars;
	}

	public boolean contains(String name) {
		for(String s: scalars) {
			if(name.equals(s)) {
				return true;
			}
		}
		return false;
	}
}
