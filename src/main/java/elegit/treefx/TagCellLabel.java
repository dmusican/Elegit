package elegit.treefx;

import elegit.RefHelper;

public class TagCellLabel extends CellLabel{
    TagCellLabel(RefHelper refHelper, boolean isCurrent) {
        super(refHelper, isCurrent, true);
    }
}
