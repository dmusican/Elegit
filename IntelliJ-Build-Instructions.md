#IntelliJ Build Instructions

1. Open IntelliJ and make sure master is checked out and up to date (change the release version in pom.xml if needed).
2. Navigate to "Edit Configurations" at the top right of the screen by clicking the small down triangle near the green play button.
3. In the upper left corner of the "Edit Configurations" window, click the plus sign and select "Application".
4. Name the build and select Elegit's main class.
5. At the bottom of the screen, there should be another plus sign. Click it, and select "Run Maven Goal". In the "Command Line" text area, type "jfx:native" and click "OK".
6. Click "Apply" then "Ok", and run the configuration you just created in the top right of the screen (click the green play button).

###Done!
