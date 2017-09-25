#IntelliJ Build Instructions

For Windows and for Mac:

1. Open IntelliJ and make sure master is checked out and up to date (change the release version in pom.xml if needed).
2. Navigate to "Edit Configurations" at the top right of the screen by clicking the small down triangle near the green play button.
3. In the upper left corner of the "Edit Configurations" window, click the plus sign and select "Application".
4. Name the build and select Elegit's main class.
5. At the bottom of the screen, there should be another plus sign. Click it, and select "Run Maven Goal". In the "Command Line" text area, type "jfx:native" and click "OK".
6. Click "Apply" then "Ok", and run the configuration you just created in the top right of the screen (click the green play button).


For Linux:

1. Open IntelliJ and make sure master is checked out and up to date (change the release version in pom.xml if needed).
2. Create a new Run Configuration by going to the Run menu and selecting "Edit Configurations..."
3. Click the + sign on the upper-left for a new configuration, and select Maven.
4. Give the config a name.
5. In the "command-line" box, paste in the following:

```clean compile jfx:build-jar assembly:single```

6. Then from the Run menu, run your new configuration.
7. The builds for distribution can be found in the subdirectory "target."



###Done!
