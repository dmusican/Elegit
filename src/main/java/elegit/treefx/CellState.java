package elegit.treefx;

/**
 * Enum for the different highlighting states of a cell
 */
public enum CellState{
    STANDARD,
    HIGHLIGHTED1,
    HIGHLIGHTED2,
    SELECTED,
    EMPHASIZED;

    public String getBackgroundColor() {
        switch(this) {
            case HIGHLIGHTED1:
                return "#da60e4";
            case HIGHLIGHTED2:
                return "#16b285";
            case SELECTED:
                return "#ff6e79";
            case EMPHASIZED:
                return "#ff6e79";
            case STANDARD:
            default:
                return "#52B3D9";
        }
    }
}
