# FZJ-COSY CSStudio
Control System Studio for FZJ COSY

To be able to build it the [cs-studio](https://github.com/ControlSystemStudio/cs-studio) plugins need to be available. They can either be located on the [css p2 update site](http://download.controlsystemstudio.org/updates/4.3) or they need to be available locally (checkout the cs-studio repository and compile it with tycho).

After the above requirements are met, you can proceed to build the FZJ-COSY distribution. To build it run the **mvn -DforceContextQualifier=qualifier clean install** command (replace qualifier with the timestamp e.g. 20150829-1351). This will compile all features and plugins in the FZJ-COSY repository. After that it will package all products defined in the **repository** folder. The final packages will be available in **repository/target/products/** where a zip or a tarball will be created for each product for all selected  configurations (win 32-bit, win 64-bit, linux-gtk 32-bit, linux-gtk 64-bit, macos). To enable or disable build for particular platform (to shorten the build time) modify the pom.xml in the root of this repository.