package elegit.treefx;

import elegit.RefHelper;

public class BranchCellLabel extends CellLabel {
    BranchCellLabel(RefHelper refHelper, boolean isCurrent) {
        super(refHelper, isCurrent, false, false);
    }

    BranchCellLabel(RefHelper refHelper, boolean isCurrent, boolean isRemote){
        super(refHelper, isCurrent, false, isRemote);
    }
}
