package elegit.treefx;

import elegit.RefHelper;

public class TagCellLabel extends CellLabel{
    TagCellLabel(RefHelper refHelper, boolean isCurrent) {
        // shouldn't matter if tags are remote or not, will have the same image
        super(refHelper, isCurrent, true, false);
    }
}
