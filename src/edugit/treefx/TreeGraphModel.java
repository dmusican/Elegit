package edugit.treefx;

import java.util.*;

/**
 * Created by makik on 6/10/15.
 *
 * Thanks to Roland for providing this graph structure:
 * http://stackoverflow.com/questions/30679025/graph-visualisation-like-yfiles-in-javafx/30696075#30696075
 *
 * The underlying model of a tree graph represented with generational cells and directed edges between
 * them
 */
public class TreeGraphModel{

    // The root of the tree
    Cell rootCell;

    List<Cell> allCells;
    List<Cell> addedCells;
    List<Cell> removedCells;

    List<Edge> allEdges;
    List<Edge> addedEdges;
    List<Edge> removedEdges;

    // Map of each cell's id to the cell itself
    Map<String,Cell> cellMap;

    // the previously added id, for use with implicit parents
    private String prevAddedId;

    /**
     * Constructs a new model with a cell with the given ID as the root
     * @param rootCellId the root cell's id
     */
    public TreeGraphModel(String rootCellId, String rootCellLabel) {

        // clear model, create lists
        clear();

        this.rootCell = new Cell(rootCellId, null);
        this.rootCell.setDisplayLabel(rootCellLabel);
        this.prevAddedId = rootCellId;
        this.addCell(rootCell);
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
     * @return the root of this tree
     */
    public Cell getRoot(){
        return this.rootCell;
    }

    public List<String> getCellIDs(){
        return new ArrayList<>(cellMap.keySet());
    }

    public boolean containsID(String id){
        return cellMap.containsKey(id);
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
     * Adds a new cell with the given ID and label to the tree whose
     * parent is the most recently added cell previous to this one
     * @param newId the id of the new cell
     * @param label the label of the new cell
     */
    public void addCell(String newId, String label, boolean visible){
        this.addCell(newId, label, prevAddedId, visible);
    }

    /**
     * Adds a new cell with the given ID and label to the tree whose
     * parent is the cell with the given ID
     * @param newId the id of the new cell
     * @param label the label of the new cell
     * @param parentId the ID of the parent of this new cell
     */
    public void addCell(String newId, String label, String parentId, boolean visible){
        Cell cell;
        if(visible){
            cell = new Cell(newId, cellMap.get(parentId));
        }else{
            cell = new InvisibleCell(newId, cellMap.get(parentId));
        }
        cell.setDisplayLabel(label);
        addCell(cell);

        this.addEdge(parentId, newId);

        prevAddedId = newId;
    }

    /**
     * Adds a new cell with the given ID and label to the tree whose
     * parents are the cells with the given IDs
     * @param newId the id of the new cell
     * @param label the label of the new cell
     * @param parent1Id the ID of the first parent of this new cell
     * @param parent2Id the ID of the second parent of this new cell
     */
    public void addCell(String newId, String label, String parent1Id, String parent2Id, boolean visible){
        Cell cell;
        if(visible){
            cell = new Cell(newId, cellMap.get(parent1Id), cellMap.get(parent2Id));
        }else{
            cell = new InvisibleCell(newId, cellMap.get(parent1Id), cellMap.get(parent2Id));
        }
        cell.setDisplayLabel(label);
        addCell(cell);

        this.addEdge(parent1Id, newId);
        this.addEdge(parent2Id, newId);

        prevAddedId = newId;
    }

    /**
     * Adds a cell to both the addedCells list and the cell map
     * @param cell
     */
    private void addCell( Cell cell) {
        addedCells.add(cell);
        cellMap.put(cell.getCellId(), cell);
    }

    /**
     * Adds an edge between the two cells corresponding to the given
     * IDs
     * @param sourceId the parent cell
     * @param targetId the child cell
     */
    public void addEdge( String sourceId, String targetId) {
        Cell sourceCell = cellMap.get(sourceId);
        Cell targetCell = cellMap.get( targetId);

        Edge edge = new Edge( sourceCell, targetCell);

        addedEdges.add(edge);
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
        allCells.addAll( addedCells);
        allCells.removeAll( removedCells);

        addedCells.clear();
        removedCells.clear();

        // edges
        allEdges.addAll( addedEdges);
        allEdges.removeAll( removedEdges);

        addedEdges.clear();
        removedEdges.clear();

    }
}
