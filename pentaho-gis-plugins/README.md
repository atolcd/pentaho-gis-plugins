Pentaho Data Integrator GIS Plugins
================================

This project allows you to manage GIS data in Pentaho's Data Integration.

Works with PDI 5.4, 6.1 and 7.0.

&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; [![Blog article](https://blog.atolcd.com/wp-content/uploads/sites/2/2015/06/pdi_gis_00.png)](https://blog.atolcd.com/une-extension-gis-dans-pentaho-data-integration-5/)


Building the plugins
-------------------
Check out the project if you have not already done so :

        git clone git://github.com/atolcd/pentaho-gis-plugins.git
        cd pentaho-gis-plugins

Install Java 6+, Maven and PDI.

To package the plugins, run the following commands from the base project directory :

        # Install dependencies if needed
        mvn install
        # Create the package
        cd pentaho-gis-plugins
        mvn clean assembly:assembly

Note :

* You can specify the **target version of Pentaho** with one of the available profiles : pentaho-5, pentaho-6 or **pentaho-7** (default). Example :

        mvn clean assembly:assembly -Dpentaho=5


The built package is target/pentaho-gis-plugins-1.1-SNAPSHOT-bin.zip (version can differ)


***Integration with Eclipse***

If you want to use the [Eclipse IDE](https://eclipse.org), you can easily create projects with maven. From the root directory of the project :

        mvn eclipse:eclipse

Assume that the [M2Eclipse](http://www.eclipse.org/m2e) plugin is installed and import the project from Eclipse :

1. From the "Import" item of the "File" menu, select "Existing Projects into Workspace"
2. Next, select the root directory of the project
3. Eclipse suggests 7 projects : select them and Finish
4. You can start working

It has been tested with Eclipse Luna and Mars.


Installing/upgrading the module
---------------------

***Method 1 : Pentaho's Marketplace installation***

In PDI, open the *Marketplace* from the help menu. Select "PDI GIS Plugins" and click "Install this plugin".
After the installation, you need to restart PDI.

When a newer version will be available, you will see an "Upgrade to XXX" button at the right of "Uninstall this plugin" button. Don't use it.
Proceed in two steps : first use "Uninstall this plugin" then start a fresh installation.


***Method 2 : Manual installation***

Extract the content of pentaho-gis-plugins-VERSION-bin.zip in ${PENTAHO_HOME}/plugins/steps.
Example of extraction from the root directory of the project :

        wget https://github.com/atolcd/pentaho-gis-plugins/releases/download/v1.1-snapshot/pentaho-gis-plugins-1.1-SNAPSHOT-bin.zip
        unzip pentaho-gis-plugins-1.1-SNAPSHOT-bin.zip -d ${PENTAHO_HOME}/plugins/steps

To upgrade the plugin, delete files you added before and start a fresh installation.


***Oracle JDBC usage***

If you plan to connect to an Oracle database, add needed jars in lib folder of PDI :

 - PENTAHO_HOME/lib/ojdbc6.jar
 - PENTAHO_HOME/lib/orai18n.jar

You can get them [here](http://www.oracle.com/technetwork/apps-tech/jdbc-112010-090769.html)


Using the plugins
---------------------
You will find new elements in "Geospatial"'s directory :

 - Geospatial Group by
 - GIS File output
 - GIS File input
 - Geoprocessing
 - Geometry information
 - Coordinate system operation
 - Spatial relationship and proximity

Some information is available [here](https://blog.atolcd.com/une-extension-gis-dans-pentaho-data-integration-5/) in french.

Provided steps presentation :

&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; [![Provided steps presentation](https://lh3.googleusercontent.com/proxy/xG_Nit5UEhPvdHnrMbYiLLJhbX0Di6qeDMDgBiDQt6mCblRvfbDi8UGQyvmzTi33Xdt0-oAPIa2hVxPUYVpf=w506-h285-n)](https://www.youtube.com/watch?v=gotnjNSVcaE)

Usage example :

&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; [![Usage example](https://lh3.googleusercontent.com/proxy/RwdveW5Zd1gPHjK0-imga_xMHp2Vgn7Roww1i1S7qlz0BA-do8CT8FLcIMg13kZ9vvurLmSZcRsH4OpXWaIq=w506-h285-n)](https://www.youtube.com/watch?v=IO0Chh0XjgY)


Contributing
---------------------
***Reporting bugs***

1. First check if the version you used is the last one
2. Next check if the issue has not ever been described in the [issues tracker](https://github.com/atolcd/pentaho-gis-plugins/issues)
3. You can [create the issue](https://github.com/atolcd/pentaho-gis-plugins/issues/new)

***Submitting a Pull Request***

1. Fork the repository on GitHub
2. Clone your repository (`git clone https://github.com/XXX/pentaho-gis-plugins.git && cd pentaho-gis-plugins`)
3. Create a local branch that will support your dev (`git checkout -b a-new-dev`)
4. Commit changes to your local branch branch (`git commit -am "Add a new dev"`)
5. Push the branch to the central repository (`git push origin a-new-dev`)
6. Open a [Pull Request](https://github.com/atolcd/pentaho-gis-plugins/pulls)
7. Wait for the PR to be supported


LICENSE
---------------------
This extension is licensed under `GNU Library or "Lesser" General Public License (LGPL)`.

Developed by [Cédric Darbon](https://twitter.com/cedricdarbon) and packaged by [Charles-Henry Vagner](https://github.com/cvagner)


Our company
---------------------
[Atol Conseils et Développements](http://www.atolcd.com)
Follow us on twitter [@atolcd](https://twitter.com/atolcd)
