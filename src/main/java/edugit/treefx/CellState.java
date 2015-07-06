package main.java.edugit.treefx;

/**
 * Enum for the different highlighting states of a cell
 */
public enum CellState{
    STANDARD,
    HIGHLIGHTED1,
    HIGHLIGHTED2,
    SELECTED,
    EMPHASIZED;

    public String getCssStringKey(){
        switch(this){
            case STANDARD:
                return "-fx-cell-color-standard";
            case HIGHLIGHTED1:
                return "-fx-cell-color-highlight1";
            case HIGHLIGHTED2:
                return "-fx-cell-color-highlight2";
            case SELECTED:
                return "-fx-cell-color-select";
            case EMPHASIZED:
                return "-fx-cell-color-emphasize";
        }
        return "null";
    }
}
