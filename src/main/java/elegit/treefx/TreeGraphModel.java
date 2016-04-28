package main.java.elegit.treefx;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.control.ContextMenu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Thanks to RolandC for providing the base graph code structure:
 * http://stackoverflow.com/questions/30679025/graph-visualisation-like-yfiles-in-javafx/30696075#30696075
 *
 * The underlying model of a tree graph represented with generational cells and directed edges between
 * them
 */
public class TreeGraphModel{

    List<Cell> allCells;
    List<Cell> addedCells;
    List<Cell> removedCells;

    List<Edge> allEdges;
    List<Edge> addedEdges;
    List<Edge> removedEdges;

    // Map of each cell's id to the cell itself
    public Map<String,Cell> cellMap;

    // Updated every time merge is called to hold the number of cells present
    IntegerProperty numCellsProperty;

    // Whether this graph has been through the layout process already or not
    public boolean isInitialSetupFinished;

    // A list of cells in this graph that do not have the default shape
    private List<Cell> cellsWithNonDefaultShapes;

    /**
     * Constructs a new model for a tree graph
     */
    public TreeGraphModel() {
        clear();
        numCellsProperty = new SimpleIntegerProperty();
        isInitialSetupFinished = false;
        cellsWithNonDefaultShapes = new ArrayList<>();
    }

    /**
     * Resets and creates the cell and edge lists, as well as the cell map
     */
    public void clear() {

        allCells = new ArrayList<>();
        addedCells = new ArrayList<>();
        removedCells = new ArrayList<>();

        allEdges = new ArrayList<>();
        addedEdges = new ArrayList<>();
        removedEdges = new ArrayList<>();

        cellMap = new HashMap<>(); // <id,cell>

    }

    /**
     * @return a list of all ids in this graph
     */
    public List<String> getCellIDs(){
        return new ArrayList<>(cellMap.keySet());
    }

    /**
     * @param id the id to check
     * @return whether this graph contains the given id or not
     */
    public boolean containsID(String id){
        return cellMap.containsKey(id);
    }

    /**
     * @param id the id of the cell to check
     * @return whether the given cell is visible or not
     */
    public boolean isVisible(String id){
        return containsID(id) && !(cellMap.get(id) instanceof InvisibleCell);
    }

    /**
     * @return the cells added since the last update
     */
    public List<Cell> getAddedCells() {
        return addedCells;
    }

    /**
     * @return the cells removed since the last update
     */
    public List<Cell> getRemovedCells() {
        return removedCells;
    }

    /**
     * @return the edges added since the last update
     */
    public List<Edge> getAddedEdges() {
        return addedEdges;
    }
    /**
     * @return the edges removed since the last update
     */
    public List<Edge> getRemovedEdges() {
        return removedEdges;
    }

    /**
     * Adds a new cell with the given ID, time, and labels to the tree whose
     * parents are the cells with the given IDs. If visible is false, uses InvisibleCell
     * instead of Cell
     * @param newId the id of the new cell
     * @param time the time of the new cell
     * @param displayLabel the displayLabel of the new cell
     * @param contextMenu the context menu that will appear when right clicking on a cell
     * @param parentIds the IDs of the parents of the new cell, if any
     * @param visible whether the cell will be normal or invisible
     */
    public void addCell(String newId, long time, String displayLabel, List<String> refs, ContextMenu contextMenu, List<String> parentIds, boolean visible){
        String parent1Id = parentIds.size() > 0 ? parentIds.get(0) : null;
        String parent2Id = parentIds.size() > 1 ? parentIds.get(1) : null;

        Cell cell;
        if(visible){
            cell = new Cell(newId, time, parent1Id == null ? null : cellMap.get(parent1Id), parent2Id == null ? null : cellMap.get(parent2Id));
        }else{
            cell = new InvisibleCell(newId, time, parent1Id == null ? null : cellMap.get(parent1Id), parent2Id == null ? null : cellMap.get(parent2Id));
        }
        cell.setLabels(displayLabel, refs);
        cell.setContextMenu(contextMenu);
        addCell(cell);

        if(parent1Id != null) this.addEdge(parent1Id, newId);
        if(parent2Id != null) this.addEdge(parent2Id, newId);
    }

    /**
     * Adds a cell to both the addedCells list and the cell map, and removes
     * any cell with a conflicting ID
     * @param cell the cell to add
     */
    private void addCell(Cell cell) {
        if(cellMap.containsKey(cell.getCellId())){
            Cell oldCell = cellMap.remove(cell.getCellId());
            for(Cell p : cell.getCellParents()){
                p.removeCellChild(oldCell);
            }
            removedCells.add(oldCell);
            this.removeEdges(oldCell);
        }

        addedCells.add(cell);
        cellMap.put(cell.getCellId(), cell);
    }

    /**
     * Adds an edge between the two cells corresponding to the given
     * IDs
     * @param sourceId the parent cell
     * @param targetId the child cell
     */
    public void addEdge(String sourceId, String targetId) {
        Cell sourceCell = cellMap.get(sourceId);
        Cell targetCell = cellMap.get(targetId);

        Edge edge = new Edge(sourceCell, targetCell);

        addedEdges.add(edge);
    }

    /**
     * Removes all edges connected to the given cell
     * @param cell the cell whose edges will be removed
     */
    private void removeEdges(Cell cell){
        for(Edge e : cell.edges){
            removedEdges.add(e);
        }
    }

    /**
     * Sets the label for the cell with the given ID to be the given string
     * @param cellId the id of the cell to label
     * @param label the new label
     */
    public void setCellLabels(String cellId, String label, List<String> refs){
        cellMap.get(cellId).setLabels(label, refs);
    }

    /**
     * Sets the shape of the cell with the given ID to be the given shape.
     * If the shape is not the default shape, adds it to the list of non-
     * default shaped cells.
     * @param cellId the id of the cell to label
     * @param shape the new shape
     */
    public void setCellShape(String cellId, CellShape shape){
        Cell cell = cellMap.get(cellId);
        cell.setShape(shape);
        if(shape == Cell.DEFAULT_SHAPE){
            cellsWithNonDefaultShapes.remove(cell);
        }else{
            cellsWithNonDefaultShapes.add(cell);
        }
    }

    /**
     * Changes every cell back to the default shape
     * @return a list of CellIDs corresponding to the cells that were changed
     */
    public List<String> resetCellShapes(){
        List<String> resetIDs = new ArrayList<>();
        for(Cell cell : cellsWithNonDefaultShapes){
            cell.setShape(Cell.DEFAULT_SHAPE);
            resetIDs.add(cell.getCellId());
        }
        cellsWithNonDefaultShapes = new ArrayList<>();
        return resetIDs;
    }

    /**
     * Checks to see if the two cells referenced by the given IDs are direct
     * neighbors
     * @param cellID the id of the first cell
     * @param neighborID the id of the second cell
     * @return true if direct neighbors, else false
     */
    public boolean isNeighbor(String cellID, String neighborID){
        List<Cell> relatives = getRelatives(cellID);
        for(Cell c : relatives){
            if(c.getCellId().equals(neighborID)){
                return true;
            }
        }
        return false;
    }

    /**
     * Gets a list of a cell's parents and children
     * @param cellID the ID of the cell
     * @return all direct neighbors of the cell
     */
    public List<Cell> getRelatives(String cellID){
        Cell cell = cellMap.get(cellID);
        if(cell == null) return new ArrayList<>();
        List<Cell> relatives = cell.getCellParents();
        relatives.addAll(cell.getCellChildren());
        return relatives;
    }

    /**
     * Updates the lists for added and removed cells, leaving the tree
     * completely updated
     */
    public void merge() {
        // cells
        allCells.addAll(addedCells);
        allCells.removeAll(removedCells);

        addedCells.clear();
        removedCells.clear();

        // edges
        allEdges.addAll(addedEdges);
        allEdges.removeAll(removedEdges);

        addedEdges.clear();
        removedEdges.clear();

        numCellsProperty.set(allCells.size());
    }
}
