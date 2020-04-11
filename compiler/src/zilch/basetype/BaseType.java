package basetype;

import java.util.List;

public class BaseType {

	private String name;

	public BaseType(String name) {
		this.name = name;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean containsName(final List<? extends BaseType> list, final String name) {
		return list.stream().anyMatch(o -> o.getName().equals(name) );
	}

	public BaseType getByName(final List<? extends BaseType> list, final String name) {
		return list.stream().filter(o -> o.getName().equals(name)).findFirst().orElse(null);
	}

}

