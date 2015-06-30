Pentaho Data Integrator GIS Plugins
================================

This project provides allows you to manage GIS data in Pentaho's Data Integration.

Works with:

 - PDI 5.4


Building the plugins
-------------------
Check out the project if you have not already done so :

        git clone git://github.com/atolcd/pentaho-gis-plugins.git

To package the plugins, run the following commands from the base project directory:

        # Install dependencies if needed
        mvn install
        # Create the package
        cd pentaho-gis-plugins
        mvn clean assembly:assembly

The built package is target/pentaho-gis-plugins-1.0-SNAPSHOT-bin.zip (version can differ)


Installing/upgrading the module
---------------------

***Method 1 : Pentaho's Marketplace installation*** (available soon)

In PDI, open the *Marketplace* from the help menu. Select "PDI GIS Plugins" and click "Install this plugin".
After the installation, you need to restart PDI.

When a newer version will be available, you will see an "Upgrade to XXX" button at the right of "Uninstall this plugin" button. Don't use it.
Proceed in two steps : first use "Uninstall this plugin" then start a fresh installation.


***Method 2 : Manual installation***

Install Java, Maven and PDI 5.4 and extract the content of pentaho-gis-plugins-VERSION-bin.zip in ${PENTAHO_HOME}/plugins/steps.
Example of extraction from the root of the project :

        unzip target/pentaho-gis-plugins-1.0-SNAPSHOT-bin.zip -d ${PENTAHO_HOME}/plugins/steps

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


LICENSE
---------------------
This extension is licensed under `GNU Library or "Lesser" General Public License (LGPL)`.

Developed by [Cédric Darbon](https://twitter.com/cedricdarbon) and packaged by [Charles-Henry Vagner](https://github.com/cvagner)


Our company
---------------------
[Atol Conseils et Développements](http://www.atolcd.com)
Follow us on twitter [@atolcd](https://twitter.com/atolcd)
