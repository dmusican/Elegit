// REFACTORED BY DAVE: RETHREADING
package elegit.treefx;

import elegit.RefHelper;

public class BranchCellLabel extends CellLabel {
    BranchCellLabel(RefHelper refHelper, boolean isCurrent) {
        super(refHelper, isCurrent, false);
    }
}
