Note that these FX tests fail when run one after another, unless they each run in a separate VM.

In IntelliJ, you can handle this in the Run Configuration by making sure to set the fork mode
appropriately. Setting it to "method" will definitely do it; setting it to "class" might be good
enough if we have written our tests reasonably. ("class" will run tests faster than "method" will)