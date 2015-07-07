# Elegit
A graphics-based Git client for teaching people both how to work Git and how Git works.

### Running the source code in IntelliJ:
1. Clone this repository.
2. We recommend you open the repository in IntelliJ.
3. Load the required libraries:
 - In IntelliJ, open the *Project Structure* menu with ⌘; (File > Project Structure)
 - Under the *Project Settings* heading, choose *Libraries*.
 - Add the following libraries to the `Elegit` module by clicking the **+** button and choosing *From Maven...*:
  - JGit: `org.eclipse.jgit:org.eclipse.jgit:3.4.0.201406110918-r`
  - ControlsFX: `org.controlsfx:controlsfx:8.40.9`
  - FontAwesomeFX: `de.jensd:fontawesomefx:8.4`
4. Also in the *Project Structure* menu, under *Project*, make sure the Project SDK is *at least* Java Version 8 Update 40.
5. Run the `main` method of the `Main` class.

***

Built by Kiley Maki and Graham Earley.

Supervised by Dave Musicant.

Carleton College 2015
