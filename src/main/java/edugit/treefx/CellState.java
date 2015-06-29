package main.java.edugit.treefx;

/**
 * Created by makik on 6/29/15.
 */
public enum CellState{
    STANDARD,
    HIGHLIGHTED1,
    HIGHLIGHTED2,
    SELECTED,
    EMPHASIZED;

    public static String getCssStringKey(CellState state){
        switch(state){
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
