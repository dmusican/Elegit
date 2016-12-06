package elegit;

import elegit.treefx.Cell;
import elegit.treefx.CellShape;
import elegit.treefx.TreeLayout;
import javafx.application.Application;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.ArrayList;

/**
 * Created by dmusicant on 12/6/16.
 */
public class TreeLayoutTest {

    private static ArrayList<Cell> allCells = new ArrayList<>();

    @BeforeClass
    public static void setUpJFX() throws Exception{
        // Launch the Elegit application in a thread so we get control back
        Thread t = new Thread("JavaFX Init Thread"){
            public void run(){
                Application.launch(Main.class);
            }
        };
        t.setDaemon(true);
        t.start();

        Main.startLatch.await();
        // Sleep until the JavaFX environment is up and running
        Thread.sleep(500);
    }

    /*
             0     5
            / \  /  \
           1   2     6
            \ /     /
             3    /
             \  /
              4
     */
    @Before
    public void setUp() {
        allCells.clear();

        Cell cell4 = new Cell("cell4", 10, new ArrayList<Cell>(), Cell.CellType.LOCAL);
        allCells.add(cell4);

        ArrayList<Cell> cell3Parents = new ArrayList<>();
        cell3Parents.add(cell4);
        Cell cell3 = new Cell("cell3", 20, cell3Parents, Cell.CellType.LOCAL);
        allCells.add(cell3);

        ArrayList<Cell> cell1Parents = new ArrayList<>();
        cell1Parents.add(cell3);
        Cell cell1 = new Cell("cell1", 30, cell1Parents, Cell.CellType.LOCAL);
        allCells.add(cell1);

        ArrayList<Cell> cell2Parents = new ArrayList<>();
        cell2Parents.add(cell3);
        Cell cell2 = new Cell("cell2", 40, cell2Parents, Cell.CellType.LOCAL);
        allCells.add(cell2);

        ArrayList<Cell> cell6Parents = new ArrayList<>();
        cell6Parents.add(cell4);
        Cell cell6 = new Cell("cell6", 50, cell6Parents, Cell.CellType.LOCAL);
        allCells.add(cell6);

        ArrayList<Cell> cell0Parents = new ArrayList<>();
        cell0Parents.add(cell1);
        cell0Parents.add(cell2);
        Cell cell0 = new Cell("cell0", 60, cell0Parents, Cell.CellType.LOCAL);
        allCells.add(cell0);

        ArrayList<Cell> cell5Parents = new ArrayList<>();
        cell5Parents.add(cell2);
        cell5Parents.add(cell6);
        Cell cell5 = new Cell("cell5", 70, cell5Parents, Cell.CellType.LOCAL);
        allCells.add(cell5);
    }

    @Test
    public void testCellSorting() throws Exception {
        TreeLayout.sortListOfCells(allCells);
        for (Cell cell : allCells) {
            System.out.print(cell.getCellId() + " ");
        }
        System.out.println();
        assertEquals(allCells.get(0).getCellId(),"cell5");
        assertEquals(allCells.get(1).getCellId(),"cell0");
        assertEquals(allCells.get(2).getCellId(),"cell6");
        assertEquals(allCells.get(3).getCellId(),"cell2");
        assertEquals(allCells.get(4).getCellId(),"cell1");
        assertEquals(allCells.get(5).getCellId(),"cell3");
        assertEquals(allCells.get(6).getCellId(),"cell4");
    }

    @Test
    public void testTopographicalSorting() throws Exception {
        TreeLayout.topologicalSortListOfCells(allCells);
        for (Cell cell : allCells) {
            System.out.print(cell.getCellId() + " ");
        }
        System.out.println();
        assertEquals(allCells.get(0).getCellId(),"cell5");
        assertEquals(allCells.get(1).getCellId(),"cell0");
        assertEquals(allCells.get(2).getCellId(),"cell6");
        assertEquals(allCells.get(3).getCellId(),"cell2");
        assertEquals(allCells.get(4).getCellId(),"cell1");
        assertEquals(allCells.get(5).getCellId(),"cell3");
        assertEquals(allCells.get(6).getCellId(),"cell4");
    }


}
