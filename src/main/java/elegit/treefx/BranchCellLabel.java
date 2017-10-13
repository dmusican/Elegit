package elegit.treefx;

import elegit.models.RefHelper;

public class BranchCellLabel extends CellLabel {
    BranchCellLabel(RefHelper refHelper, boolean isCurrent) {
        super(refHelper, isCurrent, false);
    }
}
