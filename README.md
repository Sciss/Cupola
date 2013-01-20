## README

Software for the sound composition of the Cupola project. (C)opyright 2010-2013 by Hanns Holger Rutz. All rights reserved. Covered by the GNU General Public License v2+ (see licenses folder).

### building

Cupola builds with the simple-build-tool (sbt), using Scala 2.10 and multiple open source libraries. Given that you are on OS X (which means that Java 1.6+ is installed), it should be sufficient to run the following command from the terminal

    $ cd <pathToCupolaProject>
    $ ./sbt appbundle

The result is an OS X application named `Cupola.app` which can be started via double-clicking.

### settings

The application will read the file `cupola-settings.xml` from the same directory. If this file is not found, a new default file will be created. It should contain the Cupola base directory and the SuperCollider directory, e.g.

    <?xml version="1.0" encoding="UTF-8" standalone="no"?>
    <!DOCTYPE properties SYSTEM "http://java.sun.com/dtd/properties.dtd">
    <properties>
    <comment>Cupola Settings</comment>
    <entry key="basepath">/Users/hhrutz/Documents/Cupola</entry>
    <entry key="supercollider">/Applications/SuperCollider_3.4.3</entry>
    </properties>

The Cupola base directory is expected to contain the audio files folders, such as `audio_work` and `rec`, and Open Sound Control data `osc`.
