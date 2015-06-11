package edugit.treefx;

/**
 * Created by makik on 6/11/15.
 */
public class TreeModel extends Model{

    private String prevAddedId;

    public TreeModel(String rootCellId){
        super();

        TreeCell root = new TreeCell(rootCellId, null);
        root.setRoot();

        addCell(root);

        this.prevAddedId = rootCellId;

        this.graphParent = root;
    }

    public void addCell(String newId){
        this.addCell(newId, false);
    }

    public void addCell(String newId, boolean keepSameParent){
        String temp = prevAddedId;
        this.addCell(newId, prevAddedId);
        if(!keepSameParent){
            this.prevAddedId = newId;
        }else{
            this.prevAddedId = temp;
        }
    }

    public void addCell(String newId, String parentId){
        TreeCell cell = new TreeCell(newId, cellMap.get(parentId));
        addCell(cell);

        this.addEdge(parentId, newId);

        prevAddedId = newId;
    }

    private void addCell( Cell cell) {

        addedCells.add(cell);
        cellMap.put(cell.getCellId(), cell);

    }

    public TreeCell getRoot(){
        return (TreeCell)this.graphParent;
    }
}
