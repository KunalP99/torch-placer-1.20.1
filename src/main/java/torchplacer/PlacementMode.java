package torchplacer;

public enum PlacementMode {
    BOTH,
    WALLS_ONLY,
    FLOOR_ONLY;

    public PlacementMode next() {
        PlacementMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public String getDisplayName() {
        return switch (this) {
            case BOTH -> "Walls & Floor";
            case WALLS_ONLY -> "Walls Only";
            case FLOOR_ONLY -> "Floor Only";
        };
    }
}
