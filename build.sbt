name := "Cupola"

version := "0.2.0-SNAPSHOT"

organization := "de.sciss"

homepage := Some( url( "https://github.com/Sciss/Cupola" ))

scalaVersion := "2.10.0"

libraryDependencies ++= Seq(
   "de.sciss" %% "nuagespompe" % "0.35.0"
)

scalacOptions ++= Seq( "-deprecation", "-unchecked" )

retrieveManaged := true

seq( appbundle.settings: _* )

appbundle.target <<= baseDirectory

appbundle.icon := Some( file( "application.icns" ))
