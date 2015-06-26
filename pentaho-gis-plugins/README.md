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


Installing the module
---------------------
Install Java, Maven and PDI 5.4 and extract the content of pentaho-gis-plugins-VERSION-bin.zip in ${PENTAHO_HOME}/plugins/steps.
Example of extraction from the root of the project :

        unzip target/pentaho-gis-plugins-1.0-SNAPSHOT-bin.zip -d ${PENTAHO_HOME}/plugins/steps

If you plan to connect to an Oracle database, add need jars in lib folder of the PDI plugin :
 - PENTAHO_HOME/plugins/steps/pentaho-gis-plugins-1.0-SNAPSHOT-bin/ojdbc6.jar
 - PENTAHO_HOME/plugins/steps/pentaho-gis-plugins-1.0-SNAPSHOT-bin/orai18n.jar

You can get them [here] (http://www.oracle.com/technetwork/apps-tech/jdbc-112010-090769.html)


Using the plugins
---------------------
You will find new elements in "Information géographique"'s directory :
 - Agrégation de géométries
 - Alimentation fichier SIG
 - Extraction depuis fichier SIG
 - Géotraitement
 - Information sur géométrie
 - Opération avec système de coordonnées
 - Relation spatiale et proximité


LICENSE
---------------------
This extension is licensed under `GNU Library or "Lesser" General Public License (LGPL)`.

Developed by [Cédric Darbon] and packaged by [Charles-Henry Vagner] (https://github.com/cvagner)


Our company
---------------------
[Atol Conseils et Développements] (http://www.atolcd.com)
Follow us on twitter [@atolcd] (https://twitter.com/atolcd)
