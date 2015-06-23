package edugit.treefx;

import edugit.MatchedScrollPane;
import javafx.animation.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Shape;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by makik on 6/16/15.
 */
public class Highlighter{

    public static final Color STANDARD_COLOR = Color.BLUE;
    public static final Color SELECT_COLOR = Color.DARKRED;
    public static final Color[] HIGHLIGHT_COLORS = {Color.RED, Color.MEDIUMSEAGREEN};
    public static final Color EMPHASIZE_COLOR = Color.FORESTGREEN;

    public static void highlightSelectedCell(String cellID, TreeGraphModel model, boolean enable){
        Cell cell = model.cellMap.get(cellID);
        if(cell == null) return;
        if(enable){
            cell.setColor(SELECT_COLOR);
            highlightAllRelatives(cellID, model, HIGHLIGHT_COLORS[0]);
        }else{
            cell.setColor(STANDARD_COLOR);
            highlightAllRelatives(cellID, model, STANDARD_COLOR);
        }
    }

    public static void updateCellEdges(String cellID, String selectedCellID, TreeGraphModel model, boolean enable){
        Cell cell = model.cellMap.get(cellID);
        if(cell == null) return;
        Cell selectedCell = model.cellMap.get(selectedCellID);

        List<Edge> list = new ArrayList<>(cell.edges);
        if(!enable && selectedCellID != null){
            for(Edge e : selectedCell.edges){
                list.remove(e);
            }
        }
        for(Edge e : list){
            e.setHighlighted(enable);
        }
    }

    private static void highlightAllRelatives(String cellID, TreeGraphModel model, Color color){
        highlightAllCells(model.getRelatives(cellID), color);
    }

    private static void highlightAllRelativesWithoutNeighbor(String cellID, String neighborID, TreeGraphModel model, Color color){
        List<Cell> relatives = model.getRelatives(cellID);
        List<Cell> relativesToHighlight = new ArrayList<>();
        for(Cell c : relatives){
            if(!c.getCellId().equals(neighborID) && !model.isNeighbor(c.getCellId(), neighborID)){
                relativesToHighlight.add(c);
            }
        }
        highlightAllCells(relativesToHighlight, color);
    }

    public static void highlightCell(String cellID, String selectedCellID, TreeGraphModel model, boolean enable){
        Cell cell = model.cellMap.get(cellID);
        if(cell == null) return;

        Color color;
        if(enable){
            if(selectedCellID == null){
                color = HIGHLIGHT_COLORS[0];
            }else{
                color = HIGHLIGHT_COLORS[1];
            }
        }else{
            color = STANDARD_COLOR;
        }

        if(!cellID.equals(selectedCellID) && !model.isNeighbor(cellID, selectedCellID)){
            highlightCell(cell, color);
        }

        highlightAllRelativesWithoutNeighbor(cellID, selectedCellID, model, color);
    }

    private static void highlightAllCells(List<Cell> cells, Color c){
        for(Cell cell : cells){
            highlightCell(cell, c);
        }
    }

    private static void highlightCell(Cell cell, Color c){
        cell.setColor(c);
    }

    public static void emphasizeCell(Cell c){
        MatchedScrollPane.scrollTo(c.columnLocationProperty.doubleValue());

        Shape s = (Shape) c.view;

        ScaleTransition sct = new ScaleTransition(Duration.millis(625), s);
        sct.setByX(0.3f);
        sct.setByY(0.3f);
        sct.setCycleCount(6);
        sct.setAutoReverse(true);

        SequentialTransition sqt;

        if(c instanceof InvisibleCell){
            FillTransition st1 = new FillTransition(Duration.millis(1000), s, Color.TRANSPARENT, Highlighter.EMPHASIZE_COLOR);
            st1.setCycleCount(1);

            FillTransition st2 = new FillTransition(Duration.millis(1500), s, Highlighter.EMPHASIZE_COLOR, Color.TRANSPARENT);
            st2.setCycleCount(1);

            sqt = new SequentialTransition(st1, new PauseTransition(Duration.millis(2500)), st2);
            sqt.setCycleCount(1);

        }else{
            FillTransition ft1 = new FillTransition(Duration.millis(1000), s, Highlighter.STANDARD_COLOR, Highlighter.EMPHASIZE_COLOR);
            ft1.setCycleCount(1);

            FillTransition ft2 = new FillTransition(Duration.millis(1500), s, Highlighter.EMPHASIZE_COLOR, Highlighter.STANDARD_COLOR);
            ft2.setCycleCount(1);

            sqt = new SequentialTransition(ft1, new PauseTransition(Duration.millis(2500)), ft2);
            sqt.setCycleCount(1);
        }

        ParallelTransition pt = new ParallelTransition(sqt, sct);
        pt.play();
    }
}
