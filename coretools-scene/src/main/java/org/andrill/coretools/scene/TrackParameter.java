package org.andrill.coretools.scene;

class TrackParameter {
	class Type {
		public static final int BOOLEAN = 1;
		public static final int INTEGER = 2;
		public static final int FLOAT = 3;
		public static final int STRING = 4;
	}

	private String key;
	private String label;
	private String description;
	private int type;
	private String defaultValue;

	public TrackParameter(String key, String label, String description, int type, String defaultValue) {
		this.key = key;
		this.label = label;
		this.description = description;
		this.type = type;
		this.defaultValue = defaultValue;
	}

	public String getKey() { return key; }
	public String getLabel() { return label; }
	public String getDescrtiption() { return description; }
	public int getType() { return type; }
	public String getDefaultValue() { return defaultValue; }
}