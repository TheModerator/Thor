package sss.RjSowden.Thor;

public enum ThorPower {
	LIGHTNING(1, "lightning", "send lightning"),
	EXPLODE(2, "explode", "cause explosion"),
	WOLF(3, "wolves", "spawn wolves"),
	TELEPORT(4, "teleport", "teleport to location"),
	FIREBALL(5, "fireball", "throw a fireball"),
	FIRE(6, "fire", "set on fire"),
	CREEPER(7,"creeper", "spawn creepers"),
	SMALLFIREBALL(8, "napalm", "throw a small fireball"),
	DELETE(9,"delete", "delete the target block"), // pick
	POWER(10,"power", "toggle redstone power"); // redstone torch?


	private final int power;
	private final String name;
	private final String phrase;

	ThorPower (int power, String name, String phrase) { 
		this.power = power; 
		this.name = name;
		this.phrase = phrase;
	}

	String getPhrase () { return this.phrase; }
	String getCommand() { return this.name; }
	final int getValue() { return this.power; }
	public static String commandList = ""; 
	static {
		for (ThorPower p : values()) {
			commandList += p.name + ", ";
		}
	}


	// Not using this static array currently. Point was to allow
	//  switch (variable) case ThorPower.getPower (ThorPower.FIRE.getValue())
	//  but that is really ugly
	private final static ThorPower byId[] = new ThorPower[8 + 1 /* zero unused */];
    static {  
		for (ThorPower p : values()) {
			if (byId.length > p.power)
				byId [p.power] = p;
		}
	}
	public static ThorPower getPower (final int id) {
		if (byId.length > id) 
			return byId [id];
		else
			return null;
	}
}