package main.java.elegit.treefx;

/**
 * Enum for the different highlighting states of a cell
 */
public enum CellState{
    STANDARD,
    HIGHLIGHTED1,
    HIGHLIGHTED2,
    SELECTED,
    EMPHASIZED;

    /**
     * Each state corresponds to a different cell color, as denoted in the css file.
     * This method returns the corresponding color variable to use for each
     * state
     * @return the CSS variable that holds the color for cells in this state
     */
    public String getCssStringKey(){
        switch(this){
            case HIGHLIGHTED1:
                return "-fx-cell-color-highlight1";
            case HIGHLIGHTED2:
                return "-fx-cell-color-highlight2";
            case SELECTED:
                return "-fx-cell-color-select";
            case EMPHASIZED:
                return "-fx-cell-color-emphasize";
            case STANDARD:
            default:
                return "-fx-cell-color-standard";
        }
    }
}
